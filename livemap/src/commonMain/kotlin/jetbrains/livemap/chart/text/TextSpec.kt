/*
 * Copyright (c) 2023. JetBrains s.r.o.
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package jetbrains.livemap.chart.text

import jetbrains.datalore.base.geometry.DoubleRectangle
import jetbrains.datalore.base.geometry.DoubleVector
import jetbrains.datalore.base.math.toRadians
import jetbrains.datalore.base.typedGeometry.Vec
import jetbrains.datalore.base.typedGeometry.explicitVec
import jetbrains.datalore.vis.canvas.Font
import jetbrains.datalore.vis.canvas.FontStyle
import jetbrains.datalore.vis.canvas.FontWeight
import jetbrains.datalore.vis.canvas.TextAlign
import jetbrains.livemap.Client
import jetbrains.livemap.core.graphics.TextMeasurer
import kotlin.math.abs
import kotlin.math.max

class TextSpec(
    label: String,
    fontStyle: FontStyle,
    fontWeight: FontWeight,
    size: Int,
    family: String,
    degreeAngle: Double,
    val hjust: Double,
    val vjust: Double,
    textMeasurer: TextMeasurer,
    val drawBorder: Boolean,
    // label parameters
    labelPadding: Double,
    val labelRadius: Double,
    val labelSize: Double,
    lineheight: Double
) {
    val lines = label.split('\n').map(String::trim)
    val font = Font(
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontSize = size.toDouble(),
        fontFamily = family
    )
    val dimension: Vec<Client>
    val angle: Double = toRadians(-degreeAngle)
    val lineHeight = lineheight * size
    val textSize = textMeasurer.measure(lines, font, lineHeight)
    val textAlign = when (hjust) {
        0.0 -> TextAlign.START
        1.0 -> TextAlign.END
        else -> TextAlign.CENTER
    }
    val padding = font.fontSize * labelPadding
    val rectangle: DoubleRectangle

    init {
        dimension = rotateTextSize(textSize.mul(2.0), angle)

        val width = textSize.x + padding * 2
        val height = textSize.y + padding * 2
        rectangle = DoubleRectangle(
            -width * hjust,
            -height * (1 - vjust),
            width,
            height
        )
    }

    private fun rotateTextSize(textSize: DoubleVector, angle: Double): Vec<Client> {
        val p1 = DoubleVector(textSize.x / 2, +textSize.y / 2).rotate(angle)
        val p2 = DoubleVector(textSize.x / 2, -textSize.y / 2).rotate(angle)

        val maxX = max(abs(p1.x), abs(p2.x))
        val maxY = max(abs(p1.y), abs(p2.y))
        return explicitVec(maxX * 2, maxY * 2)
    }
}