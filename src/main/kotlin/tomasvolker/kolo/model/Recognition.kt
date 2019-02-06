package tomasvolker.kolo.model

import org.openrndr.shape.Rectangle

data class Recognition(
    val box: Rectangle,
    val confidence: Double,
    val clazz: Int
)