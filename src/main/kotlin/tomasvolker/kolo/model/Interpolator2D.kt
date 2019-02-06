package tomasvolker.kolo.model

import tomasvolker.numeriko.core.interfaces.array2d.double.DoubleArray2D
import kotlin.math.ceil
import kotlin.math.floor

interface Interpolator2D {

    fun interpolate(array: DoubleArray2D, i0: Double, i1: Double): Double

    operator fun DoubleArray2D.get(i0: Double, i1: Double): Double = interpolate(this, i0, i1)

}

object BilinearInterpolator: Interpolator2D {

    override fun interpolate(array: DoubleArray2D, i0: Double, i1: Double): Double {
        val min0 = floor(i0).toInt().coerceIn(0, array.shape0-1)
        val max0 = ceil(i0).toInt().coerceIn(0, array.shape0-1)
        val min1 = floor(i1).toInt().coerceIn(0, array.shape1-1)
        val max1 = ceil(i1).toInt().coerceIn(0, array.shape1-1)

        val q00 = array[min0, min1]
        val q01 = array[min0, max1]
        val q10 = array[max0, min1]
        val q11 = array[max0, max1]

        val factor0 = i0 % 1.0
        val factor1 = i1 % 1.0

        return q00 + factor0 * (q10 - q00) + factor1 * (q01 - q00) + factor0 * factor1 * (q00 + q11 - q01 - q10)
    }

}