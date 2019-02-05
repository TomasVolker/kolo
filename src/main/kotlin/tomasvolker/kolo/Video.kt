package tomasvolker.kolo

import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle

interface Video {

    val currentFrame: ArrayImage

    val width: Int get() = currentFrame.data.shape(0)
    val height: Int get() = currentFrame.data.shape(1)

    val bounds: Rectangle get() = Rectangle(
        Vector2.ZERO,
        width.toDouble(),
        height.toDouble()
    )

    fun restart()

    fun next()

    fun hasNext(): Boolean

}
