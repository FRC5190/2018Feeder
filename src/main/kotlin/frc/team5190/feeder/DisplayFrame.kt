package frc.team5190.feeder

import java.awt.Color

import javax.swing.*

class DisplayFrame internal constructor() : JFrame() {

    init {
        this.title = "Remote Path Generator Status"
        this.setSize(450, 300)
        background = Color.RED
        this.isAlwaysOnTop = true
        this.isUndecorated = true
        this.getRootPane().windowDecorationStyle = JRootPane.FRAME
        this.opacity = 0.5f
        this.setLocationRelativeTo(null)
        this.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        this.isVisible = true
    }

    /**
     * Sets the background of the frame. Display color codes:
     *  * RED: NT Disconnected
     *  * GREEN: NT Connected
     *  * YELLOW: Currently generating trajectories
     *  * BLACK: Fatal error, one or more trajectories failed to generate
     */
    override fun setBackground(color: Color) {
        if (background !== Color.BLACK) {
            this.contentPane.background = color
        }
    }

    override fun getBackground(): Color {
        return this.contentPane.background
    }

    companion object {
        private val serialVersionUID = -924699517006012122L
    }

}