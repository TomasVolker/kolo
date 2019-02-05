package tomasvolker.kolo

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorBuffer
import org.openrndr.shape.Rectangle
import tomasvolker.numeriko.core.dsl.I
import tomasvolker.numeriko.core.interfaces.array2d.double.DoubleArray2D
import tomasvolker.numeriko.core.interfaces.factory.doubleZeros
import tomasvolker.numeriko.core.performance.forEach
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

interface Image {

    val bounds: Rectangle

    val width: Double get() = bounds.width
    val height: Double get() = bounds.height

    fun red(x: Double, y: Double): Double
    fun green(x: Double, y: Double): Double
    fun blue(x: Double, y: Double): Double

    operator fun get(x: Double, y: Double): ColorRGBa = ColorRGBa(red(x, y), green(x, y), blue(x, y))

    operator fun get(x: Int, y: Int): ColorRGBa = get(x.toDouble(), y.toDouble())

}

interface Interpolator2D {

    fun interpolate(array: DoubleArray2D, x: Double, y: Double): Double

    operator fun DoubleArray2D.get(x: Double, y: Double): Double = interpolate(this, x, y)

}

object BilinearInterpolator: Interpolator2D {

    override fun interpolate(array: DoubleArray2D, x: Double, y: Double): Double {
        val minX = floor(x).toInt().coerceIn(0, array.shape0-1)
        val maxX = ceil(x).toInt().coerceIn(0, array.shape0-1)
        val minY = floor(y).toInt().coerceIn(0, array.shape1-1)
        val maxY = ceil(y).toInt().coerceIn(0, array.shape1-1)

        val q00 = array[minX, minY]
        val q01 = array[minX, maxY]
        val q10 = array[maxX, minY]
        val q11 = array[maxX, maxY]

        val xFactor = x % 1.0
        val yFactor = y % 1.0

        return q00 + xFactor * (q10 - q00) + yFactor * (q01 - q00) + xFactor * yFactor * (q00 + q11 - q01 - q10)
    }

}

class ArrayImage(
    val widthInPixels: Int,
    val heightInPixels: Int,
    val interpolator2D: Interpolator2D = BilinearInterpolator
): Image {

    override val width: Double = widthInPixels.toDouble()
    override val height: Double = heightInPixels.toDouble()



    var padding = ColorRGBa.BLACK

    val data = doubleZeros(I[widthInPixels, heightInPixels, 3]).asMutable()

    val red = data.arrayAlongAxis(axis = 2, index = 0).as2D()
    val green = data.arrayAlongAxis(axis = 2, index = 1).as2D()
    val blue = data.arrayAlongAxis(axis = 2, index = 2).as2D()

    override val bounds get() = Rectangle(
        x = 0.0,
        y = 0.0,
        width = width,
        height = height
    )

    fun isValidPosition(x: Double, y: Double) = x in 0.0..width && y in 0.0..height
    fun isValidPosition(x: Int, y: Int) = x in 0 until widthInPixels && y in 0 until heightInPixels

    fun red(x: Int, y: Int): Double =
        if (isValidPosition(x, y))
            red[x, y]
        else
            padding.r

    fun green(x: Int, y: Int): Double =
        if (isValidPosition(x, y))
            green[x, y]
        else
            padding.g

    fun blue(x: Int, y: Int): Double =
        if (isValidPosition(x, y))
            blue[x, y]
        else
            padding.b

    override fun red(x: Double, y: Double): Double =
        if (isValidPosition(x, y))
            red[x, y]
        else
            padding.r

    override fun green(x: Double, y: Double): Double =
        if (isValidPosition(x, y))
            green[x, y]
        else
            padding.g

    override fun blue(x: Double, y: Double): Double =
        if (isValidPosition(x, y))
            blue[x, y]
        else
            padding.b

    override operator fun get(x: Double, y: Double): ColorRGBa = ColorRGBa(red(x, y), green(x, y), blue(x, y))

    operator fun DoubleArray2D.get(x: Double, y: Double) = interpolator2D.interpolate(this, x, y)

    operator fun set(x: Int, y: Int, colorRGBa: ColorRGBa) {
        if (isValidPosition(x, y)) {
            red[x, y] = colorRGBa.r
            green[x, y] = colorRGBa.g
            blue[x, y] = colorRGBa.b
        }
    }

}

fun image(
    width: Int,
    height: Int
) = ArrayImage(width, height)


inline fun image(
    width: Int,
    height: Int,
    init: (x: Int, y: Int)->ColorRGBa
): ArrayImage = ArrayImage(width, height).apply {
    forEach(width, height) { x, y ->
        this[x, y] = init(x, y)
    }
}

fun ColorBuffer.write(image: ArrayImage) {

    shadow.buffer.rewind()

    forEach(image.height.toInt(), image.width.toInt()) { y, x ->
        shadow.write(x, y, image.red(x, y), image.green(x, y), image.blue(x, y), 1.0)
    }

    shadow.upload()

}

fun ArrayImage.write(
    other: Image,
    sourceWindow: Rectangle = other.bounds
) {

    val targetWidth = data.shape(0)
    val targetHeight = data.shape(1)

    val minX = sourceWindow.left
    val minY = sourceWindow.top
    val deltaX = sourceWindow.width
    val deltaY = sourceWindow.height

    forEach(targetWidth, targetHeight) { x, y ->

        val ratioX = x.toDouble() / targetWidth
        val ratioY = y.toDouble() / targetHeight

        this[x, y] = other[
                minX + ratioX * deltaX,
                minY + ratioY * deltaY
        ]

    }

}