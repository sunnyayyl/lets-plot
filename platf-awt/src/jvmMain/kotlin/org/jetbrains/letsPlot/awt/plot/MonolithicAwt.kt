/*
 * Copyright (c) 2023. JetBrains s.r.o.
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package org.jetbrains.letsPlot.awt.plot

import org.jetbrains.letsPlot.awt.plot.component.DefaultErrorMessageComponent
import org.jetbrains.letsPlot.commons.geometry.DoubleRectangle
import org.jetbrains.letsPlot.commons.geometry.DoubleVector
import org.jetbrains.letsPlot.commons.logging.PortableLogging
import org.jetbrains.letsPlot.core.plot.builder.FigureBuildInfo
import org.jetbrains.letsPlot.core.spec.FailureHandler
import org.jetbrains.letsPlot.core.util.MonolithicCommon
import org.jetbrains.letsPlot.datamodel.svg.dom.SvgSvgElement
import java.awt.Dimension
import java.awt.Rectangle
import javax.swing.JComponent

private val LOG = PortableLogging.logger("MonolithicAwt")

object MonolithicAwt {
    fun buildPlotFromRawSpecs(
        plotSpec: MutableMap<String, Any>,
        plotSize: DoubleVector?,
        plotMaxWidth: Double?,
        svgComponentFactory: (svg: SvgSvgElement) -> JComponent,
        executor: (() -> Unit) -> Unit,
        errorMessageComponentFactory: (String) -> JComponent = DefaultErrorMessageComponent.factory,
        computationMessagesHandler: ((List<String>) -> Unit)
    ): JComponent {

        return try {
            @Suppress("NAME_SHADOWING")
            val plotSpec = MonolithicCommon.processRawSpecs(plotSpec, frontendOnly = false)
            buildPlotFromProcessedSpecs(
                plotSpec,
                plotSize,
                plotMaxWidth,
                svgComponentFactory,
                executor,
                errorMessageComponentFactory = errorMessageComponentFactory,
                computationMessagesHandler
            )
        } catch (e: RuntimeException) {
            handleException(e, errorMessageComponentFactory)
        }
    }

    fun buildPlotFromProcessedSpecs(
        plotSpec: MutableMap<String, Any>,
        plotSize: DoubleVector?,
        plotMaxWidth: Double?,
        svgComponentFactory: (svg: SvgSvgElement) -> JComponent,
        executor: (() -> Unit) -> Unit,
        errorMessageComponentFactory: (message: String) -> JComponent = DefaultErrorMessageComponent.factory,
        computationMessagesHandler: (List<String>) -> Unit,
    ): JComponent {

        return try {
            val buildResult = MonolithicCommon.buildPlotsFromProcessedSpecs(
                plotSpec,
                plotSize,
                plotMaxWidth,
                plotPreferredWidth = null
            )
            if (buildResult.isError) {
                val errorMessage = (buildResult as MonolithicCommon.PlotsBuildResult.Error).error
                return errorMessageComponentFactory(errorMessage)
            }

            val success = buildResult as MonolithicCommon.PlotsBuildResult.Success
            val computationMessages = success.buildInfos.flatMap { it.computationMessages }
            computationMessagesHandler(computationMessages)
            return if (success.buildInfos.size == 1) {
                // a single plot
                val buildInfo = success.buildInfos[0]
                FigureToAwt(
                    buildInfo,
                    svgComponentFactory, executor
                ).eval()
            } else {
                // ggbunch
                buildGGBunchComponent(
                    success.buildInfos,
                    svgComponentFactory, executor
                )
            }

        } catch (e: RuntimeException) {
            handleException(e, errorMessageComponentFactory)
        }
    }

    private fun buildGGBunchComponent(
        plotInfos: List<FigureBuildInfo>,
        svgComponentFactory: (svg: SvgSvgElement) -> JComponent,
        executor: (() -> Unit) -> Unit
    ): JComponent {

        val bunchComponent = DisposableJPanel(null)

        bunchComponent.border = null
//        bunchComponent.background = Colors.parseColor(Defaults.BACKDROP_COLOR).let {
//            Color(
//                it.red,
//                it.green,
//                it.blue,
//                it.alpha
//            )
//        }
        bunchComponent.isOpaque = false

        for (plotInfo in plotInfos) {
            val itemComp = FigureToAwt(
                plotInfo,
                svgComponentFactory, executor
            ).eval()
            val bounds = plotInfo.bounds
            itemComp.bounds = Rectangle(
                bounds.origin.x.toInt(),
                bounds.origin.y.toInt(),
                bounds.dimension.x.toInt(),
                bounds.dimension.y.toInt()
            )
            bunchComponent.add(itemComp)
        }

        val bunchBounds = plotInfos.map { it.bounds }
            .fold(DoubleRectangle(DoubleVector.ZERO, DoubleVector.ZERO)) { acc, bounds ->
                acc.union(bounds)
            }

        val bunchDimensions = Dimension(
            bunchBounds.width.toInt(),
            bunchBounds.height.toInt()
        )

        bunchComponent.preferredSize = bunchDimensions
        bunchComponent.minimumSize = bunchDimensions
        bunchComponent.maximumSize = bunchDimensions
        return bunchComponent
    }

    private fun handleException(
        e: RuntimeException,
        errorMessageComponentFactory: (message: String) -> JComponent
    ): JComponent {
        val failureInfo = FailureHandler.failureInfo(e)
        if (failureInfo.isInternalError) {
            LOG.error(e) { "Unexpected situation in 'MonolithicAwt'" }
        }
        return errorMessageComponentFactory(failureInfo.message)
    }
}
