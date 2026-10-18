package mai_onsyn.jfx_tools.layout

import javafx.scene.Node
import kotlin.math.max
import kotlin.math.min

open class Box(vararg children: Node) : LayoutContainer<Box>(*children) {
    /** 榛樿涔濆鏍煎榻愶紝榛樿 [Alignment.Box.TOP_START]銆?*/
    var contentAlignment: Alignment.Box = Alignment.Box.TOP_START
        set(value) {
            field = value
            requestLayout()
        }

    override fun layoutChildren() {
        val items = managedChildren()
        if (items.isEmpty()) return

        val ins = insets
        val contentW = max(0.0, width - ins.left - ins.right)
        val contentH = max(0.0, height - ins.top - ins.bottom)

        val defaultH = Alignment.fraction(Alignment.horizontalOf(contentAlignment))
        val defaultV = Alignment.fraction(Alignment.verticalOf(contentAlignment))

        for (child in items) {
            val m = modifierOf(child)
            val padH = m.paddingLeft + m.paddingRight
            val padV = m.paddingTop + m.paddingBottom
            val weighted = Modifier.isSet(m.weight) && m.weight > 0.0

            val outerW = when {
                Modifier.isSet(m.width) -> max(0.0, m.width)
                weighted -> contentW
                Modifier.isSet(m.fillMaxWidth) -> max(0.0, m.fillMaxWidth * contentW)
                else -> min(child.prefWidth(-1.0), max(0.0, contentW - padH)) + padH
            }.coerceAtMost(contentW)
            val outerH = when {
                Modifier.isSet(m.height) -> max(0.0, m.height)
                weighted -> contentH
                Modifier.isSet(m.fillMaxHeight) -> max(0.0, m.fillMaxHeight * contentH)
                else -> min(child.prefHeight(-1.0), max(0.0, contentH - padV)) + padV
            }.coerceAtMost(contentH)

            val alignH = m.alignHorizontal
            val alignV = m.alignVertical
            val hFraction = if (alignH == null) defaultH else Alignment.fraction(alignH)
            val vFraction = if (alignV == null) defaultV else Alignment.fraction(alignV)

            val x = ins.left + max(0.0, contentW - outerW) * hFraction + m.paddingLeft
            val y = ins.top + max(0.0, contentH - outerH) * vFraction + m.paddingTop

            child.resizeRelocate(x, y, max(0.0, outerW - padH), max(0.0, outerH - padV))
        }
    }

    override fun computeMinWidth(h: Double): Double = boxSize(true, true)

    override fun computeMinHeight(w: Double): Double = boxSize(false, true)

    override fun computePrefWidth(h: Double): Double = boxSize(true, false)

    override fun computePrefHeight(w: Double): Double = boxSize(false, false)

    private fun boxSize(horizontal: Boolean, min: Boolean): Double {
        val ins = insets
        var maxValue = 0.0
        for (child in managedChildren()) {
            val m = modifierOf(child)
            val pad = if (horizontal) m.paddingLeft + m.paddingRight else m.paddingTop + m.paddingBottom
            val fixed = if (horizontal) m.width else m.height
            val size = when {
                Modifier.isSet(fixed) -> fixed
                horizontal && min -> child.minWidth(-1.0) + pad
                !horizontal && min -> child.minHeight(-1.0) + pad
                horizontal -> child.prefWidth(-1.0) + pad
                else -> child.prefHeight(-1.0) + pad
            }
            maxValue = max(maxValue, size)
        }
        return maxValue + if (horizontal) ins.left + ins.right else ins.top + ins.bottom
    }
}
