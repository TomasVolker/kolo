package tomasvolker.kolo

import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.tensorflow.Graph
import tomasvolker.komputo.dsl.TensorflowSession
import tomasvolker.numeriko.core.dsl.I
import tomasvolker.numeriko.core.functions.argmax
import tomasvolker.numeriko.core.functions.exp
import tomasvolker.numeriko.core.index.All
import tomasvolker.numeriko.core.index.Last
import tomasvolker.numeriko.core.index.rangeTo
import tomasvolker.numeriko.core.interfaces.array1d.double.DoubleArray1D
import tomasvolker.numeriko.core.interfaces.array1d.double.applyElementWise
import tomasvolker.numeriko.core.interfaces.arraynd.double.DoubleArrayND
import tomasvolker.numeriko.core.interfaces.arraynd.double.applyElementWise
import tomasvolker.numeriko.core.interfaces.factory.doubleZeros
import tomasvolker.numeriko.core.interfaces.slicing.get
import java.io.File
import java.util.*
import kotlin.math.exp

fun sigmoid(x: Double) = 1.0 / (1.0 + exp(-x))

fun softmax(array: DoubleArray1D): DoubleArray1D {

    val exp = exp(array).asMutable()

    val sum = exp.sum()

    return if (sum.isNaN())
        exp.applyElementWise { 1.0 / array.size }
    else
        exp.applyElementWise { it / sum }

}

fun iou(box1: Rectangle, box2: Rectangle) = intersectionOverUnion(box1, box2)

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

data class Recognition(
    val box: Rectangle,
    val confidence: Double,
    val clazz: Int
)

class Yolo3: AutoCloseable {

    val labels = listOf(
        "person",
        "bicycle",
        "car",
        "motorbike",
        "aeroplane",
        "bus",
        "train",
        "truck",
        "boat",
        "traffic light",
        "fire hydrant",
        "stop sign",
        "parking meter",
        "bench",
        "bird",
        "cat",
        "dog",
        "horse",
        "sheep",
        "cow",
        "elephant",
        "bear",
        "zebra",
        "giraffe",
        "backpack",
        "umbrella",
        "handbag",
        "tie",
        "suitcase",
        "frisbee",
        "skis",
        "snowboard",
        "sports ball",
        "kite",
        "baseball bat",
        "baseball glove",
        "skateboard",
        "surfboard",
        "tennis racket",
        "bottle",
        "wine glass",
        "cup",
        "fork",
        "knife",
        "spoon",
        "bowl",
        "banana",
        "apple",
        "sandwich",
        "orange",
        "broccoli",
        "carrot",
        "hot dog",
        "pizza",
        "donut",
        "cake",
        "chair",
        "sofa",
        "pottedplant",
        "bed",
        "diningtable",
        "toilet",
        "tvmonitor",
        "laptop",
        "mouse",
        "remote",
        "keyboard",
        "cell phone",
        "microwave",
        "oven",
        "toaster",
        "sink",
        "refrigerator",
        "book",
        "clock",
        "vase",
        "scissors",
        "teddy bear",
        "hair drier",
        "toothbrush"
    )

    val INPUT_SIZE = 416
    val INPUT_NAME = "inputs"
    val OUTPUT_NAME = "output_boxes"


    private val THRESHOLD = 0.5f
    private val MAX_RESULTS = 15


    private val graphPath = "data/yolo/yolo.pb"

    val graph: Graph = Graph().apply {
        importGraphDef(File(graphPath).readBytes())
    }

    val session = TensorflowSession(graph)

    private val imageBuffer = doubleZeros(I[1, INPUT_SIZE, INPUT_SIZE, 3]).asMutable()


    fun recognizeImage(image: ArrayImage): List<Recognition> {
        return classifyImage(runTensorFlow(image))
    }

    private fun runTensorFlow(image: ArrayImage): DoubleArrayND {

        imageBuffer.arrayAlongAxis(axis = 0, index = 0).setValue(image.data)

        imageBuffer.applyElementWise { 255 * it }

        return session.execute { 
            feed(INPUT_NAME, imageBuffer)
            fetch(OUTPUT_NAME)
        }.first()

    }

    fun classifyImage(tensorFlowOutput: DoubleArrayND): List<Recognition> {

        val result = mutableMapOf<Int, MutableSet<Recognition>>()

        val boxCount = tensorFlowOutput.shape(1)

        for (box in 0 until boxCount) {

            val cell = (tensorFlowOutput[0, box, All] as DoubleArrayND).as1D()
            if(cell[4] < THRESHOLD) continue

            val candidate = cell.parseCandidate().maxAsRecognition()

            val previousRecognitions = result.getOrPut(candidate.clazz) { mutableSetOf() }

            val previous = previousRecognitions.firstOrNull { iou(candidate.box, it.box) > 0.3 }

            if (candidate.confidence > previous?.confidence ?: 0.0) {
                previousRecognitions.remove(previous)
                previousRecognitions.add(candidate)
            }

        }


        return result.flatMap { it.value }
    }

    private fun DoubleArray1D.parseCandidate() =
        Candidate(
            box = Rectangle.fromCorners(
                corner1 = Vector2(this[0], this[1]),
                corner2 = Vector2(this[2], this[3])
            ),
            confidence = this[4],
            probabilities = this[5..Last]
        )


    private fun calculateTopPredictions(
        recognition: Candidate,
        predictionQueue: PriorityQueue<Recognition>
    ) {

        for (i in 0 until recognition.probabilities.size) {
            val probabilities = recognition.probabilities
            val argMax = probabilities.argmax()
            val max = probabilities[argMax]
            val confidenceInClass = max * recognition.confidence

            if (recognition.confidence > 0.5) {

                predictionQueue +=
                    Recognition(
                        box = recognition.box,
                        confidence = confidenceInClass,
                        clazz = argMax
                    )

            }
        }
    }

    override fun close() {
        session.close()
    }

}
