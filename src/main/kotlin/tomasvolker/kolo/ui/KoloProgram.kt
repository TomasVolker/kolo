package tomasvolker.kolo.ui

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import org.openrndr.KEY_SPACEBAR
import org.openrndr.Program
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.configuration
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.isolated
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.math.transforms.transform
import tomasvolker.kolo.model.FfmpegVideo
import tomasvolker.kolo.model.*
import tomasvolker.kolo.resources.Resources
import tomasvolker.kolo.util.biggestContainedBox
import tomasvolker.kolo.util.nextColor
import tomasvolker.kolo.util.smallestContainingBox
import kotlin.random.Random

class KoloArguments(parser: ArgParser) {

    val tiny by parser.flagging(
        "-t", "--tiny",
        help = "use tiny yolo"
    )

    val input by parser.storing(
        "-i", "--input",
        help = "url of the input, device by default"
    ).default("device")

}

fun main(args: Array<out String>) {

    val arguments = ArgParser(args).parseInto(::KoloArguments)

    KoloProgram(
        tiny = arguments.tiny,
        inputUrl = arguments.input
    ).use {

        application(
            program = it,
            configuration = configuration {
                width = 416
                height = 416
                windowResizable = true
            }
        )

    }

}

class KoloProgram(
    tiny: Boolean = true,
    inputUrl: String = "device"
): Program(), AutoCloseable {

    val yolo = Yolo3(tiny)

    val video by lazy {
        if (inputUrl == "device")
            FfmpegVideo.fromDevice()
        else
            FfmpegVideo.fromURL(inputUrl)
    }

    val offscreen by lazy { colorBuffer(yolo.INPUT_SIZE, yolo.INPUT_SIZE) }

    val font by lazy { Resources.fontImageMap("IBMPlexMono-Bold.ttf", 16.0) }

    val buffer = image(yolo.INPUT_SIZE, yolo.INPUT_SIZE)

    val colorMap = Random(251).run { yolo.labels.indices.associateWith { nextColor() } }

    override fun setup() {

        video.restart()

        extend(FPSDisplay(font))

    }

    override fun draw() {

        if (video.hasNext()) {

            video.next()

            buffer.write(
                video.currentFrame,
                sourceWindow = video.bounds.smallestContainingBox()
            )

        }

        val recognitions = yolo.recognizeImage(buffer)

        offscreen.write(buffer)

        drawer.isolated {

            val view = drawer.bounds.biggestContainedBox()

            model = transform {
                translate(view.corner)
                scale(view.width / offscreen.width.toDouble())
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

    override fun close() {
        yolo.close()
    }

}

