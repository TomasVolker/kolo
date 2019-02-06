package tomasvolker.kolo.model

import org.openrndr.color.ColorRGBa
import org.openrndr.ffmpeg.adopted.FFmpegFrameGrabber
import org.openrndr.ffmpeg.adopted.Frame
import tomasvolker.numeriko.core.performance.forEach
import org.openrndr.draw.colorBuffer as _colorBuffer
import java.io.File
import java.nio.ByteBuffer

class FfmpegVideo private constructor(url: String): Video {

    val frameGrabber = FFmpegFrameGrabber(url).apply {
        start()
    }

    private var frame: Frame? = frameGrabber.grabImage()

    var lastSystemTimestamp = System.currentTimeMillis()

    init {
        requireNotNull(frame) {
            "No first frame found"
        }
    }

    private var buffer = image(
        frame?.imageWidth ?: error(""),
        frame?.imageHeight ?: error("")
    ).apply {
        frame?.let { write(it) }
    }

    private fun ArrayImage.write(frame: Frame) {

        val imageBuffer = frame.image[0] as ByteBuffer
        imageBuffer.rewind()

        forEach(frame.imageHeight, frame.imageWidth) { y, x ->

            val red = imageBuffer.get().toUByte().toInt() / 255.0
            val green = imageBuffer.get().toUByte().toInt() / 255.0
            val blue = imageBuffer.get().toUByte().toInt() / 255.0

            this[x, y] = ColorRGBa(red, green, blue)

        }

    }

    override val currentFrame: ArrayImage
        get() = buffer


    override fun restart() {
        frameGrabber.restart()
        next()
    }

    override fun hasNext(): Boolean = frame != null

    override fun next() {

        val lastTimestamp = frame?.timestamp ?: System.currentTimeMillis() * 1000

        do {

            frame = frameGrabber.grabImage()

            val delta = frame?.let { it.timestamp - lastTimestamp } ?: 0

            val frameSystemTime = lastSystemTimestamp + delta / 1000

        } while(System.currentTimeMillis() - frameSystemTime > 500)

        frame?.let {
            buffer.write(it)
            lastSystemTimestamp = System.currentTimeMillis()
        }


    }

    companion object {

        fun fromURL(url: String): FfmpegVideo = FfmpegVideo(url)

        fun fromFile(file: File) = FfmpegVideo(file.toURI().toURL().toExternalForm())

        fun fromFile(filename: String) = fromFile(File(filename))

        fun listDevices(): List<String> =
            FFmpegFrameGrabber.getDeviceDescriptions().toList()

        fun defaultDevice(): String {
            val osName = System.getProperty("os.name").toLowerCase()
            return when {
                "windows" in osName -> {
                    "video=Integrated Webcam"
                }
                "mac os x" in osName -> {
                    "0"
                }
                "linux" in osName -> {
                    "/dev/video0"
                }
                else -> throw RuntimeException("unsupported os: $osName")
            }
        }

        fun fromDevice(
            deviceName: String = defaultDevice(),
            width: Int = -1,
            height: Int = -1,
            framerate: Double = -1.0,
            inputFormat: String? = null
        ): FfmpegVideo {
            val osName = System.getProperty("os.name").toLowerCase()
            val format = when {
                "windows" in osName -> {
                    "dshow"
                }
                "mac os x" in osName -> {
                    "avfoundation"
                }
                "linux" in osName -> {
                    "video4linux2"
                }
                else -> throw RuntimeException("unsupported os: $osName")
            }

            return FfmpegVideo(deviceName).apply {
                frameGrabber.inputFormat = inputFormat
                frameGrabber.format = format
                if (width != -1 && height != -1) {
                    frameGrabber.imageWidth = width
                    frameGrabber.imageHeight = height
                }
                if (framerate != -1.0) {
                    frameGrabber.frameRate = framerate
                }
            }
        }
    }

}