package tomasvolker.kolo

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.FontImageMap
import org.openrndr.ffmpeg.FFMPEGVideoPlayer
import org.openrndr.resourceUrl

fun main() = application {
    program {

        val font = FontImageMap.fromUrl(
            resourceUrl("IBMPlexMono-Bold.ttf", Resources::class.java),
            16.0
        )

        val videoPlayer = FFMPEGVideoPlayer.fromDevice(inputFormat = "yuyv422")
        videoPlayer.start()
        extend(FPSDisplay(font))
        extend {
            drawer.background(ColorRGBa.BLACK)
            videoPlayer.next()
            videoPlayer.draw(drawer)
        }
    }
}