package tomasvolker.kolo

import org.openrndr.color.ColorRGBa
import org.openrndr.ffmpeg.adopted.FFmpegFrameGrabber
import org.openrndr.ffmpeg.adopted.Frame
import tomasvolker.numeriko.core.performance.forEach
import org.openrndr.draw.colorBuffer as _colorBuffer
import java.io.File
import java.nio.ByteBuffer

class WebcamVideo private constructor(url: String): Video {

    val frameGrabber = FFmpegFrameGrabber(url).apply {
        start()
    }

    private var frame: Frame? = run {
        frameGrabber.grabImage()
    }

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
        fun fromURL(url: String): WebcamVideo {
            return WebcamVideo(url)
        }

        fun fromFile(filename: String): WebcamVideo {
            return WebcamVideo(File(filename).toURI().toURL().toExternalForm())
        }

        fun listDevices(): List<String> {
            return FFmpegFrameGrabber.getDeviceDescriptions().toList()
        }

        fun defaultDevice(): String {
            val osName = System.getProperty("os.name").toLowerCase()
            val device: String
            device = when {
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
            return device
        }

        fun defaultInputFormat(): String? {
            val osName = System.getProperty("os.name").toLowerCase()
            val format: String?
            format = when {
                "windows" in osName -> {
                    null
                }
                "mac os x" in osName -> {
                    null
                }
                "linux" in osName -> {
                    "mjpeg"
                }
                else -> throw RuntimeException("unsupported os: $osName")
            }
            return format
        }

        fun fromDevice(
            deviceName: String = defaultDevice(),
            width: Int = -1,
            height: Int = -1,
            framerate: Double = -1.0,
            inputFormat: String? = defaultInputFormat()
        ): WebcamVideo {
            val osName = System.getProperty("os.name").toLowerCase()
            val format: String
            format = when {
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

            val player = WebcamVideo(deviceName)
            player.frameGrabber.inputFormat = inputFormat
            player.frameGrabber.format = format
            if (width != -1 && height != -1) {
                player.frameGrabber.imageWidth = width
                player.frameGrabber.imageHeight = height
            }
            if (framerate != -1.0) {
                player.frameGrabber.frameRate = framerate
            }
            return player
        }
    }

}