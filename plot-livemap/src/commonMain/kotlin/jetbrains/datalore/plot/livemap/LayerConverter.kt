/*
 * Copyright (c) 2019. JetBrains s.r.o.
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package jetbrains.datalore.plot.livemap

import jetbrains.datalore.plot.base.Aes
import jetbrains.datalore.plot.base.GeomKind.*
import jetbrains.datalore.plot.base.aes.AestheticsUtil
import jetbrains.datalore.plot.base.geom.LiveMapGeom
import jetbrains.datalore.plot.base.livemap.LivemapConstants.DisplayMode
import jetbrains.datalore.plot.base.livemap.LivemapConstants.ScaleObjects
import jetbrains.datalore.plot.builder.LayerRendererUtil.LayerRendererData
import jetbrains.livemap.api.*


object LayerConverter {
    fun convert(
        letsPlotLayers: List<LayerRendererData>,
        scaleObjects: ScaleObjects,
        scaleZooms: Int,
        geodesic: Boolean
    ): List<LayersBuilder.() -> Unit> {
        return letsPlotLayers.mapIndexed { index, layer ->
            val dataPointsConverter = DataPointsConverter(
                layerIndex = index,
                aesthetics = layer.aesthetics,
                geodesic = geodesic
            )

            val (layerKind, dataPointLiveMapAesthetics) = when (layer.geomKind) {
                POINT -> MapLayerKind.POINT to dataPointsConverter.toPoint(layer.geom)
                H_LINE -> MapLayerKind.H_LINE to dataPointsConverter.toHorizontalLine()
                V_LINE -> MapLayerKind.V_LINE to dataPointsConverter.toVerticalLine()
                SEGMENT -> MapLayerKind.PATH to dataPointsConverter.toSegment(layer.geom)
                RECT -> MapLayerKind.POLYGON to dataPointsConverter.toRect()
                TILE, BIN_2D -> MapLayerKind.POLYGON to dataPointsConverter.toTile()
                DENSITY2D, CONTOUR, PATH -> MapLayerKind.PATH to dataPointsConverter.toPath(layer.geom)
                TEXT -> MapLayerKind.TEXT to dataPointsConverter.toText()
                DENSITY2DF, CONTOURF, POLYGON, MAP -> MapLayerKind.POLYGON to dataPointsConverter.toPolygon()
                LIVE_MAP -> {
                    val layerKind = when ((layer.geom as LiveMapGeom).displayMode) {
                        DisplayMode.POINT -> MapLayerKind.POINT
                        DisplayMode.PIE -> MapLayerKind.PIE
                        DisplayMode.BAR -> MapLayerKind.BAR
                    }
                    val dataPointLiveMapAesthetics = when (layerKind) {
                        MapLayerKind.PIE -> dataPointsConverter.toPie()
                        MapLayerKind.BAR -> dataPointsConverter.toBar()
                        MapLayerKind.POINT -> dataPointsConverter.toPoint(layer.geom)
                        else -> error("Unexpected")
                    }

                    layerKind to dataPointLiveMapAesthetics
                }
                else -> throw IllegalArgumentException("Layer '" + layer.geomKind.name + "' is not supported on Live Map.")
            }

            createLayerBuilder(
                index,
                layerKind,
                dataPointLiveMapAesthetics,
                layer.mappedAes,
                scaleObjects,
                scaleZooms
            )
        }
    }

    private fun createLayerBuilder(
        layerIdx: Int,
        layerKind: MapLayerKind,
        liveMapDataPoints: List<DataPointLiveMapAesthetics>,
        mappedAes: Set<Aes<*>>,
        scaleObjects: ScaleObjects,
        scaleZooms: Int,
    ): LayersBuilder.() -> Unit = {
        fun getScaleRange(scalableStroke: Boolean): ClosedRange<Int>? {
            val negativeZoom = (-1).takeIf { scalableStroke } ?: -2
            val isStatic = Aes.SIZE !in mappedAes
            if (scaleObjects == ScaleObjects.NONE) return null
            if (scaleObjects == ScaleObjects.BOTH) return negativeZoom..scaleZooms
            if (scaleObjects == ScaleObjects.STATIC && isStatic) return negativeZoom..scaleZooms
            if (scaleObjects == ScaleObjects.STATIC && !isStatic) return negativeZoom..0
            error("getScaleRange() - unexpected state. scaleObject: $scaleObjects, isStatic: $isStatic")
        }

        when (layerKind) {
            MapLayerKind.POINT -> points {
                liveMapDataPoints.forEach {
                    point {
                        scalingRange = getScaleRange(scalableStroke = false)
                        layerIndex = layerIdx
                        index = it.index
                        point = it.point
                        label = it.label
                        animation = it.animation
                        shape = it.shape
                        radius = it.radius
                        fillColor = it.fillColor
                        strokeColor = it.strokeColor
                        strokeWidth = 1.0
                    }
                }
            }

            MapLayerKind.POLYGON -> polygons {
                liveMapDataPoints.forEach {
                    polygon {
                        scalingRange = getScaleRange(scalableStroke = true)
                        layerIndex = layerIdx
                        index = it.index
                        multiPolygon = it.geometry
                        geoObject = it.geoObject
                        lineDash = it.lineDash
                        fillColor = it.fillColor
                        strokeColor = it.strokeColor
                        strokeWidth = AestheticsUtil.strokeWidth(it.myP)
                    }
                }
            }

            MapLayerKind.PATH -> paths {
                liveMapDataPoints.forEach {
                    if (it.geometry != null) {
                        path {
                            scalingRange = getScaleRange(scalableStroke = true)
                            layerIndex = layerIdx
                            index = it.index
                            multiPolygon = it.geometry!!
                            lineDash = it.lineDash
                            strokeColor = it.strokeColor
                            strokeWidth = AestheticsUtil.strokeWidth(it.myP)
                            animation = it.animation
                            speed = it.speed
                            flow = it.flow
                        }
                    }
                }
            }

            MapLayerKind.V_LINE -> vLines {
                liveMapDataPoints.forEach {
                    line {
                        scalingRange = getScaleRange(scalableStroke = true)
                        point = it.point
                        lineDash = it.lineDash
                        strokeColor = it.strokeColor
                        strokeWidth = AestheticsUtil.strokeWidth(it.myP)
                    }
                }
            }

            MapLayerKind.H_LINE -> hLines {
                liveMapDataPoints.forEach {
                    line {
                        scalingRange = getScaleRange(scalableStroke = true)
                        point = it.point
                        lineDash = it.lineDash
                        strokeColor = it.strokeColor
                        strokeWidth = AestheticsUtil.strokeWidth(it.myP)
                    }
                }
            }

            MapLayerKind.TEXT -> texts {
                liveMapDataPoints.forEach {
                    text {
                        index = it.index
                        point = it.point
                        fillColor = it.strokeColor // Text is filled by strokeColor
                        strokeColor = it.strokeColor
                        strokeWidth = 0.0
                        label = it.label
                        size = it.size
                        family = it.family
                        fontface = it.fontface
                        hjust = it.hjust
                        vjust = it.vjust
                        angle = it.angle
                    }
                }
            }

            MapLayerKind.PIE -> pies {
                liveMapDataPoints.forEach {
                    pie {
                        scalingRange = getScaleRange(scalableStroke = false)
                        layerIndex = layerIdx
                        fromDataPoint(it)
                    }
                }
            }

            MapLayerKind.BAR -> bars {
                liveMapDataPoints.forEach {
                    bar {
                        scalingRange = getScaleRange(scalableStroke = false)
                        layerIndex = layerIdx
                        fromDataPoint(it)
                    }
                }
            }

            else -> error("Unsupported layer kind: $layerKind")
        }
    }

    private fun Symbol.fromDataPoint(p: DataPointLiveMapAesthetics) {
        point = p.point
        radius = p.radius
        strokeColor = p.strokeColor
        strokeWidth = 1.0
        indices = p.indices
        values = p.valueArray
        colors = p.colorArray
    }
}