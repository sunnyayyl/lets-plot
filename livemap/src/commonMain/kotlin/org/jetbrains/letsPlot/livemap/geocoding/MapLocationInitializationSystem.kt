/*
 * Copyright (c) 2023. JetBrains s.r.o.
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package org.jetbrains.letsPlot.livemap.geocoding

import org.jetbrains.letsPlot.commons.intern.async.Async
import org.jetbrains.letsPlot.commons.intern.typedGeometry.Rect
import org.jetbrains.letsPlot.commons.intern.typedGeometry.Vec
import org.jetbrains.letsPlot.commons.intern.typedGeometry.center
import org.jetbrains.letsPlot.livemap.Client
import org.jetbrains.letsPlot.livemap.World
import org.jetbrains.letsPlot.livemap.config.DEFAULT_LOCATION
import org.jetbrains.letsPlot.livemap.core.ecs.AbstractSystem
import org.jetbrains.letsPlot.livemap.core.ecs.EcsComponentManager
import org.jetbrains.letsPlot.livemap.geocoding.MapLocationGeocoder.Companion.convertToWorldRects
import org.jetbrains.letsPlot.livemap.mapengine.LiveMapContext
import org.jetbrains.letsPlot.livemap.mapengine.viewport.Viewport
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

class MapLocationInitializationSystem(
    componentManager: EcsComponentManager,
    private val myZoom: Double?,
    private val myLocationRect: Async<Rect<org.jetbrains.letsPlot.livemap.World>>?
) : AbstractSystem<LiveMapContext>(componentManager) {
    private lateinit var myLocation: LocationComponent
    private lateinit var myViewport: Viewport
    private lateinit var myDefaultLocation: List<Rect<org.jetbrains.letsPlot.livemap.World>>
    private var myNeedLocation = true

    override fun initImpl(context: LiveMapContext) {
        myLocation = getSingleton()
        myViewport = context.mapRenderContext.viewport
        myDefaultLocation = DEFAULT_LOCATION.convertToWorldRects(context.mapProjection)
    }

    override fun updateImpl(context: LiveMapContext, dt: Double) {
        if (!myNeedLocation) return

        if (myLocationRect != null) {
            myLocationRect.map {
                myNeedLocation = false
                listOf(it).run(myViewport::calculateBoundingBox)
                    .calculatePosition { zoom, coordinates ->
                        initMapPoisition(context, zoom, coordinates)
                    }
            }
        } else {
            if (myLocation.isReady()) {
                myNeedLocation = false

                (myLocation.locations.takeIf { it.isNotEmpty() } ?: myDefaultLocation)

                .run(myViewport::calculateBoundingBox)
                    .calculatePosition { zoom, coordinates ->
                        initMapPoisition(context, zoom, coordinates)
                    }
            }
        }
    }

    private fun Rect<org.jetbrains.letsPlot.livemap.World>.calculatePosition(positionConsumer: (zoom: Double, coordinates: Vec<org.jetbrains.letsPlot.livemap.World>) -> Unit) {
        val zoom: Double = myZoom
            ?: if (dimension.x != 0.0 || dimension.y != 0.0) {
                calculateMaxZoom(dimension, myViewport.size)
            } else {
                calculateMaxZoom(
                    myViewport.calculateBoundingBox(myDefaultLocation).dimension,
                    myViewport.size
                )
            }

        positionConsumer(zoom, center)
    }

    private fun initMapPoisition(ctx: LiveMapContext, zoom: Double, coordinates: Vec<org.jetbrains.letsPlot.livemap.World>) {
        val integerZoom = floor(zoom)
        ctx.camera.requestZoom(integerZoom)
        ctx.camera.requestPosition(coordinates)

        ctx.initialPosition = coordinates
        ctx.initialZoom = integerZoom.toInt()
    }

    private fun calculateMaxZoom(rectSize: Vec<org.jetbrains.letsPlot.livemap.World>, containerSize: Vec<org.jetbrains.letsPlot.livemap.Client>): Double {
        val xZoom = calculateMaxZoom(rectSize.x, containerSize.x)
        val yZoom = calculateMaxZoom(rectSize.y, containerSize.y)
        val zoom = min(xZoom, yZoom)
        return max(myViewport.minZoom.toDouble(), min(zoom, myViewport.maxZoom.toDouble()))
    }

    private fun calculateMaxZoom(regionLength: Double, containerLength: Double): Double {
        return when {
            regionLength == 0.0 -> myViewport.maxZoom.toDouble()
            containerLength == 0.0 -> myViewport.minZoom.toDouble()
            else -> (ln(containerLength / regionLength) / ln(2.0))
        }
    }
}