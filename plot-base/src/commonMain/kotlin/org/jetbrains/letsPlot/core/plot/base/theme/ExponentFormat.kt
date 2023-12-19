/*
 * Copyright (c) 2023. JetBrains s.r.o.
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package org.jetbrains.letsPlot.core.plot.base.theme

enum class ExponentFormat(
    val superscript: Boolean,
) {
    E(superscript = false),
    POW(superscript = true)
}