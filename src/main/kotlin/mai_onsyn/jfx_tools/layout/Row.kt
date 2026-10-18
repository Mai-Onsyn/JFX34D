package mai_onsyn.jfx_tools.layout

import javafx.scene.Node
import kotlin.math.max

open class Row(vararg children: Node) : LayoutContainer<Row>(*children) {
    /** 瀛愯妭鐐逛箣闂寸殑鍥哄畾闂磋窛锛堝儚绱狅級锛岄粯璁?0銆?*/
    var spacing: Double = 0.0
        set(value) {
            field = max(0.0, value)
            requestLayout()
        }

    /** 涓昏酱锛堟按骞筹級鎺掑竷鏂瑰紡锛岄粯璁?[Arrangement.START]銆?*/
    var horizontalArrangement: Arrangement = Arrangement.START
        set(value) {
            field = value
            requestLayout()
        }

    /** 浜ゅ弶杞达紙鍨傜洿锛夐粯璁ゅ榻愶紝榛樿 [Alignment.Vertical.TOP]銆?*/
    var verticalAlignment: Alignment.Vertical = Alignment.Vertical.TOP
        set(value) {
            field = value
            requestLayout()
        }

    override fun layoutChildren() {
        layoutLinear(false, spacing, horizontalArrangement, Alignment.fraction(verticalAlignment))
    }

    override fun computeMinWidth(h: Double): Double = minLinear(false, spacing, true)

    override fun computeMinHeight(w: Double): Double = minLinear(false, spacing, false)

    override fun computePrefWidth(h: Double): Double = prefLinear(false, spacing, true)

    override fun computePrefHeight(w: Double): Double = prefLinear(false, spacing, false)
}
