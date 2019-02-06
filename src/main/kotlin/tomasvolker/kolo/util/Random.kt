package tomasvolker.kolo.util

import org.openrndr.color.ColorRGBa
import kotlin.random.Random

fun Random.nextColor() = ColorRGBa(
    r = nextDouble(),
    g = nextDouble(),
    b = nextDouble()
)

