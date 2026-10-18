package mai_onsyn.jfx_tools.layout

import javafx.scene.layout.Region

open class Spacer() : Region() {
    constructor(size: Double) : this() {
        setPrefSize(size, size)
    }

    constructor(width: Double, height: Double) : this() {
        setPrefSize(width, height)
    }

    init {
        isMouseTransparent = true
        isFocusTraversable = false
    }
}
