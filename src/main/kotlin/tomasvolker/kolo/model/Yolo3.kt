package tomasvolker.kolo.model

import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.tensorflow.Graph
import tomasvolker.kolo.util.fromCorners
import tomasvolker.kolo.util.intersectionOverUnion
import tomasvolker.komputo.dsl.TensorflowSession
import tomasvolker.numeriko.core.dsl.I
import tomasvolker.numeriko.core.functions.argmax
import tomasvolker.numeriko.core.index.All
import tomasvolker.numeriko.core.index.Last
import tomasvolker.numeriko.core.index.rangeTo
import tomasvolker.numeriko.core.interfaces.array1d.double.DoubleArray1D
import tomasvolker.numeriko.core.interfaces.arraynd.double.DoubleArrayND
import tomasvolker.numeriko.core.interfaces.factory.doubleZeros
import tomasvolker.numeriko.core.interfaces.iteration.fastForEachIndices
import tomasvolker.numeriko.core.interfaces.slicing.get
import java.io.File

class Yolo3(tiny: Boolean = false): AutoCloseable {

    val labels = File("yolo/labels.txt").readLines()

    val INPUT_SIZE = 416
    val INPUT_NODE_NAME = "inputs"
    val OUTPUT_NODE_NAME = "output_boxes"

    private val THRESHOLD = 0.5f

    private val graphPath = if (tiny)
        "yolo/tiny_yolo.pb"
    else
        "yolo/yolo.pb"

    val graph: Graph = Graph().apply {
        importGraphDef(File(graphPath).readBytes())
    }

    val session = TensorflowSession(graph)

    private val imageBuffer = doubleZeros(I[1, INPUT_SIZE, INPUT_SIZE, 3]).asMutable()

    fun recognizeImage(image: ArrayImage): List<Recognition> =
        runTensorFlow(image).processModelOutput()

    private fun runTensorFlow(image: ArrayImage): DoubleArrayND {

        image.data.fastForEachIndices { (x, y, c) ->
            imageBuffer[0, y, x, c] = 255 * image.data[x, y, c]
        }

        return session.execute {
            feed(INPUT_NODE_NAME, imageBuffer)
            fetch(OUTPUT_NODE_NAME)
        }.first()

    }

    data class RecognitionIOU(
        val recognition: Recognition,
        val iou: Double
    )

    fun DoubleArrayND.processModelOutput(): List<Recognition> {

        val result = mutableMapOf<Int, MutableSet<Recognition>>()

        val boxCount = shape(1)

        for (box in 0 until boxCount) {

            val cell = (this[0, box, All] as DoubleArrayND).as1D()
            if(cell[4] < THRESHOLD) continue

            val candidate = cell.toCandidate().maxAsRecognition()

            val previousRecognitions = result.getOrPut(candidate.clazz) { mutableSetOf() }

            val previous = previousRecognitions
                .asSequence()
                .map {
                    RecognitionIOU(
                        it,
                        iou(candidate.box, it.box)
                    )
                }
                .filter { it.iou > 0.3 }
                .maxBy { it.iou }


            if (candidate.confidence > previous?.recognition?.confidence ?: 0.0) {
                previousRecognitions.remove(previous?.recognition)
                previousRecognitions.add(candidate)
            }

        }


        return result.flatMap { it.value }
    }

    private fun DoubleArray1D.toCandidate() =
        Candidate(
            box = Rectangle.fromCorners(
                corner1 = Vector2(this[0], this[1]),
                corner2 = Vector2(this[2], this[3])
            ),
            confidence = this[4],
            probabilities = this[5..Last]
        )

    override fun close() {
        session.close()
    }

    data class Candidate(
        val box: Rectangle,
        val confidence: Double,
        val probabilities: DoubleArray1D
    ) {

        fun maxAsRecognition() = Recognition(
            box = box,
            confidence = confidence,
            clazz = probabilities.argmax()
        )

    }

}

fun iou(box1: Rectangle, box2: Rectangle) = intersectionOverUnion(box1, box2)


