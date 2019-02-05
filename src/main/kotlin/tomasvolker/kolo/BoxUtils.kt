package tomasvolker.kolo

import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

val Rectangle.aspectRatio get() = width / height

fun Rectangle.biggestContainedBox(aspectRatio: Double = 1.0) =
        if (aspectRatio < this.aspectRatio)
            Rectangle.fromCenter(center, width = height * aspectRatio, height = height)
        else
            Rectangle.fromCenter(center, width = width, height = width / aspectRatio)

fun Rectangle.smallestContainingBox(aspectRatio: Double = 1.0) =
    if (aspectRatio > this.aspectRatio)
        Rectangle.fromCenter(center, width = height * aspectRatio, height = height)
    else
        Rectangle.fromCenter(center, width = width, height = width / aspectRatio)

fun Rectangle.enlarge(factor: Double) =
    Rectangle.fromCenter(center, width * factor, height * factor)

fun Rectangle.enlarge(factorX: Double, factorY: Double) =
    Rectangle.fromCenter(center, width * factorX, height * factorY)

val Rectangle.Companion.EMPTY get() = Rectangle(0.0, 0.0, 0.0, 0.0)

fun Rectangle.Companion.fromCorners(
    corner1: Vector2,
    corner2: Vector2
) = fromCenter(
    center = (corner1 + corner2) / 2.0,
    width = abs(corner2.x - corner1.x),
    height = abs(corner2.y - corner1.y)
)

fun Rectangle.Companion.fromSides(
    left: Double,
    top: Double,
    right: Double,
    bottom: Double
) = Rectangle(
    x = left,
    y = top,
    width = (right - left).coerceAtLeast(0.0),
    height = (bottom - top).coerceAtLeast(0.0)
)

val Rectangle.left get() = x
val Rectangle.right get() = x + width
val Rectangle.top get() = y
val Rectangle.bottom get() = y + height

infix fun Rectangle.intersect(
    other: Rectangle
): Rectangle =
        Rectangle.fromSides(
            top = max(this.top, other.top),
            left = max(this.left, other.left),
            right = min(this.right, other.right),
            bottom = min(this.bottom, other.bottom)
        )


fun unionArea(
    box1: Rectangle,
    box2: Rectangle
): Double =
        box1.area + box2.area - (box1 intersect box2).area


fun intersectionOverUnion(
    box1: Rectangle,
    box2: Rectangle
): Double =
    1.0 / ((box1.area + box2.area) / (box1 intersect box2).area - 1.0)
