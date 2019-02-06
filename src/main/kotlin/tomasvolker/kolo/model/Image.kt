package tomasvolker.kolo.model

import org.openrndr.color.ColorRGBa
import org.openrndr.shape.Rectangle

interface Image {

    val bounds: Rectangle

    val width: Double get() = bounds.width
    val height: Double get() = bounds.height

    fun red(x: Double, y: Double): Double
    fun green(x: Double, y: Double): Double
    fun blue(x: Double, y: Double): Double

    operator fun get(x: Double, y: Double): ColorRGBa =
        ColorRGBa(red(x, y), green(x, y), blue(x, y))

    operator fun get(x: Int, y: Int): ColorRGBa = get(x.toDouble(), y.toDouble())

}