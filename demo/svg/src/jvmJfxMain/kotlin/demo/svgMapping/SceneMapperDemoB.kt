/*
 * Copyright (c) 2023. JetBrains s.r.o.
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package demo.svgMapping

import demo.common.jfx.demoUtils.SvgViewerDemoWindowJfx
import demo.svgMapping.model.DemoModelB

fun main() {
    val svgRoot = DemoModelB.createModel()
    svgRoot.width().set(500.0)
    svgRoot.height().set(500.0)
    SvgViewerDemoWindowJfx(
        "Svg Elements (B)",
        listOf(svgRoot)
    ).open()
}
