/*
 * Copyright (c) 2023. JetBrains s.r.o.
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package jetbrains.datalore.plot.base.stat

import jetbrains.datalore.base.gcommon.collect.Ordering
import jetbrains.datalore.plot.base.Aes
import jetbrains.datalore.plot.base.DataFrame
import jetbrains.datalore.plot.base.StatContext
import jetbrains.datalore.plot.base.data.TransformVar
import jetbrains.datalore.plot.common.data.SeriesUtil

class SummaryStat(
    private val yAggFunction: (List<Double>) -> Double,
    private val yMinAggFunction: (List<Double>) -> Double,
    private val yMaxAggFunction: (List<Double>) -> Double,
    private val sortedQuantiles: List<Double>
) : BaseStat(DEF_MAPPING) {

    override fun consumes(): List<Aes<*>> {
        return listOf(Aes.X, Aes.Y)
    }

    override fun apply(data: DataFrame, statCtx: StatContext, messageConsumer: (s: String) -> Unit): DataFrame {
        if (!hasRequiredValues(data, Aes.Y)) {
            return withEmptyStatValues()
        }

        val ys = data.getNumeric(TransformVar.Y)
        val xs = if (data.has(TransformVar.X)) {
            data.getNumeric(TransformVar.X)
        } else {
            List(ys.size) { 0.0 }
        }

        val statData = buildStat(xs, ys, statCtx)
        if (statData.isEmpty()) {
            return withEmptyStatValues()
        }

        val builder = DataFrame.Builder()
        for ((variable, series) in statData) {
            builder.putNumeric(variable, series)
        }
        return builder.build()
    }

    private fun buildStat(
        xs: List<Double?>,
        ys: List<Double?>,
        statCtx: StatContext
    ): Map<DataFrame.Variable, List<Double>> {
        val binnedData = SeriesUtil.filterFinite(xs, ys)
            .let { (xs, ys) -> xs zip ys }
            .groupBy(keySelector = { it.first }, valueTransform = { it.second })

        if (binnedData.isEmpty()) {
            return emptyMap()
        }

        val statX = ArrayList<Double>()
        val statY = ArrayList<Double>()
        val statYMin = ArrayList<Double>()
        val statYMax = ArrayList<Double>()
        val statAggValues: Map<DataFrame.Variable, MutableList<Double>> = statCtx.mappedStatVariables()
            .associateWith { mutableListOf() }
        for ((x, bin) in binnedData) {
            val sortedBin = Ordering.natural<Double>().sortedCopy(bin)
            statX.add(x)
            statY.add(yAggFunction(sortedBin))
            statYMin.add(yMinAggFunction(sortedBin))
            statYMax.add(yMaxAggFunction(sortedBin))
            for ((statVar, aggValues) in statAggValues) {
                val aggFunction = aggFunctionByStat(statVar)
                aggValues.add(aggFunction(sortedBin))
            }
        }

        return mapOf(
            Stats.X to statX,
            Stats.Y to statY,
            Stats.Y_MIN to statYMin,
            Stats.Y_MAX to statYMax,
        ) + statAggValues
    }

    private fun aggFunctionByStat(statVar: DataFrame.Variable): (List<Double>) -> Double {
        return when (statVar) {
            Stats.COUNT -> AggregateFunctions::count
            Stats.SUM -> AggregateFunctions::sum
            Stats.MEAN -> AggregateFunctions::mean
            Stats.MEDIAN -> AggregateFunctions::median
            Stats.Y_MIN -> AggregateFunctions::min
            Stats.Y_MAX -> AggregateFunctions::max
            Stats.LOWER_QUANTILE -> { values -> AggregateFunctions.quantile(values, sortedQuantiles[0]) }
            Stats.MIDDLE_QUANTILE -> { values -> AggregateFunctions.quantile(values, sortedQuantiles[1]) }
            Stats.UPPER_QUANTILE -> { values -> AggregateFunctions.quantile(values, sortedQuantiles[2]) }
            else -> throw IllegalStateException(
                "Unsupported stat variable: '${statVar.name}'\n" +
                "Use one of: ..count.., ..sum.., ..mean.., ..median.., ..ymin.., ..ymax.., ..lq.., ..mq.., ..uq.."
            )
        }
    }

    companion object {
        val DEF_QUANTILES = listOf(0.25, 0.5, 0.75)

        private val DEF_MAPPING: Map<Aes<*>, DataFrame.Variable> = mapOf(
            Aes.X to Stats.X,
            Aes.Y to Stats.Y,
            Aes.YMIN to Stats.Y_MIN,
            Aes.YMAX to Stats.Y_MAX
        )
    }
}