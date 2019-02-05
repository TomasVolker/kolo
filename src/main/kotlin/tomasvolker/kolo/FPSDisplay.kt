package tomasvolker.kolo

import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.draw.Drawer
import org.openrndr.draw.FontImageMap
import org.openrndr.draw.isolated
import org.openrndr.math.Matrix44

class FPSDisplay(
    val font: FontImageMap
) : Extension {
    override var enabled: Boolean = true

    var lastTime: Double = 0.0

    override fun setup(program: Program) {
        lastTime = program.seconds
    }

    override fun afterDraw(drawer: Drawer, program: Program) {

        val now = program.seconds

        drawer.isolated {
            drawer.fontMap = font

            // -- set view and projections
            drawer.view = Matrix44.IDENTITY
            drawer.ortho()

            drawer.translate(width - 100.0, height - 20.0)
            drawer.text("fps: %.2f".format(1.0 / (now - lastTime)))
        }

        lastTime = now
    }
}