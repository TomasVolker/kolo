package tomasvolker.kolo.resources

import org.openrndr.draw.FontImageMap
import org.openrndr.resourceUrl

object Resources {

    fun url(name: String) = resourceUrl(name, Resources::class.java)

    fun fontImageMap(
        name: String,
        size: Double,
        contentScale: Double = 1.0
    ) = FontImageMap.fromUrl(
        fontUrl = url(name),
        size = 16.0,
        contentScale = contentScale
    )

}
