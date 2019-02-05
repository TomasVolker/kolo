package tomasvolker.kolo

import org.openrndr.Program
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.configuration
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.FontImageMap
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.isolated
import org.openrndr.math.transforms.scale
import org.openrndr.math.transforms.transform
import org.openrndr.resourceUrl
import kotlin.random.Random

fun main() = application(
    configuration = configuration {
        width = 800
        height = 800
        windowResizable = true
    },
    program = KoloProgram()
)

object Resources

fun Random.nextColor() = ColorRGBa(
    r = nextDouble(),
    g = nextDouble(),
    b = nextDouble()
)

class KoloProgram: Program() {

    val yolo = Yolo3()

    lateinit var video: Video

    lateinit var offscreen: ColorBuffer
    val buffer = image(yolo.INPUT_SIZE, yolo.INPUT_SIZE)

    val font by lazy {
        FontImageMap.fromUrl(
            resourceUrl("IBMPlexMono-Bold.ttf", Resources::class.java),
            16.0
        )
    }

    val colorMap = Random(251).run { yolo.labels.indices.associateWith { nextColor() } }

    override fun setup() {

        offscreen = colorBuffer(yolo.INPUT_SIZE, yolo.INPUT_SIZE)

        video = WebcamVideo.fromDevice(inputFormat = "yuyv422")
        video.restart()

        extend(FPSDisplay(font))

    }

    override fun draw() {

        video.next()

        buffer.write(
            video.currentFrame.apply { padding = ColorRGBa.GRAY },
            sourceWindow = video.bounds.smallestContainingBox()
        )

        val recognitions = yolo.recognizeImage(buffer)

        offscreen.write(buffer)

        drawer.isolated {

            val view = drawer.bounds.biggestContainedBox()

            model = transform {
                translate(view.corner)
                scale(view.width / offscreen.width.toDouble())
                /*translate(offscreen.width / 2.0, 0.0)
                scale(-1.0, 1.0, 1.0)
                translate(-offscreen.width / 2.0, 0.0)*/
            }

            background(ColorRGBa.BLACK)
            image(offscreen)

            fontMap = font

            recognitions.forEach { recognition ->

                val color = colorMap[recognition.clazz] ?: ColorRGBa.BLACK

                strokeWeight = 2.0
                stroke = color
                fill = null
                rectangle(recognition.box)
                fill = color
                text("%s: %.0f".format(
                    yolo.labels[recognition.clazz],
                    recognition.confidence * 100
                ), recognition.box.corner)

            }

        }

    }

}

