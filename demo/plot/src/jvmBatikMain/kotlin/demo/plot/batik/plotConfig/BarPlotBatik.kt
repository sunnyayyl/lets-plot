/*
 * Copyright (c) 2019. JetBrains s.r.o.
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package demo.plot.batik.plotConfig

import demo.plot.common.model.plotConfig.BarPlot
import demo.common.batik.demoUtils.PlotSpecsDemoWindowBatik

fun main() {
    with(BarPlot()) {
        PlotSpecsDemoWindowBatik(
            "Bar plot",
            plotSpecList()
        ).open()
    }
}
