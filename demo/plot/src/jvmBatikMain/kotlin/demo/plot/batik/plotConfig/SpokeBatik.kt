/*
 * Copyright (c) 2023. JetBrains s.r.o.
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package demo.plot.batik.plotConfig

import demo.common.batik.demoUtils.PlotSpecsDemoWindowBatik
import demo.plot.common.model.plotConfig.Spoke

fun main() {
    with(Spoke()) {
        PlotSpecsDemoWindowBatik(
            "Spoke plot",
            plotSpecList()
        ).open()
    }
}