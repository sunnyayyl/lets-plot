/*
 * Copyright (c) 2022. JetBrains s.r.o.
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package org.jetbrains.letsPlot.core.plot.builder

import org.jetbrains.letsPlot.commons.geometry.DoubleRectangle
import org.jetbrains.letsPlot.commons.values.Color
import org.jetbrains.letsPlot.core.plot.base.CoordinateSystem
import org.jetbrains.letsPlot.core.plot.base.render.svg.SvgComponent
import org.jetbrains.letsPlot.core.plot.base.tooltip.GeomTargetCollector
import org.jetbrains.letsPlot.core.plot.builder.assemble.TestingPlotContext
import org.jetbrains.letsPlot.core.plot.builder.frame.FrameOfReferenceBase

object DemoAndTest {
    fun buildGeom(
        layer: GeomLayer,
        xyAesBounds: DoubleRectangle,
        coord: CoordinateSystem,
        flippedAxis: Boolean,
        targetCollector: GeomTargetCollector
    ): SvgComponent {
        return FrameOfReferenceBase.buildGeom(
            plotContext = TestingPlotContext.create(layer),
            layer = layer,
            xyAesBounds = xyAesBounds,
            coord = coord,
            flippedAxis = flippedAxis,
            targetCollector = targetCollector,
            backgroundColor = Color.WHITE
        )
    }
}