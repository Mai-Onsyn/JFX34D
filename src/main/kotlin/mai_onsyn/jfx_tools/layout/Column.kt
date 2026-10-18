package mai_onsyn.jfx_tools.layout

import javafx.scene.Node
import kotlin.math.max

open class Column(vararg children: Node) : LayoutContainer<Column>(*children) {
    /** 瀛愯妭鐐逛箣闂寸殑鍥哄畾闂磋窛锛堝儚绱狅級锛岄粯璁?0銆?*/
    var spacing: Double = 0.0
        set(value) {
            field = max(0.0, value)
            requestLayout()
        }

    /** 涓昏酱锛堝瀭鐩达級鎺掑竷鏂瑰紡锛岄粯璁?[Arrangement.START]銆?*/
    var verticalArrangement: Arrangement = Arrangement.START
        set(value) {
            field = value
            requestLayout()
        }

    /** 浜ゅ弶杞达紙姘村钩锛夐粯璁ゅ榻愶紝榛樿 [Alignment.Horizontal.START]銆?*/
    var horizontalAlignment: Alignment.Horizontal = Alignment.Horizontal.START
        set(value) {
            field = value
            requestLayout()
        }

    override fun layoutChildren() {
        layoutLinear(true, spacing, verticalArrangement, Alignment.fraction(horizontalAlignment))
    }

    override fun computeMinWidth(h: Double): Double = minLinear(true, spacing, true)

    override fun computeMinHeight(w: Double): Double = minLinear(true, spacing, false)

    override fun computePrefWidth(h: Double): Double = prefLinear(true, spacing, true)

    override fun computePrefHeight(w: Double): Double = prefLinear(true, spacing, false)
}
