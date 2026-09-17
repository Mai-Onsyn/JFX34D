package mai_onsyn.jfx_tools.layout

import javafx.scene.Node
import kotlin.math.max

/**
 * 鍨傜洿鏂瑰悜鐨勭嚎鎬у竷灞€锛堝搴?Compose 鐨?`Column`锛夈€備富杞?= 鍨傜洿锛屼氦鍙夎酱 = 姘村钩銆? *
 * * 涓昏酱锛堝瀭鐩达級锛氬瓙鑺傜偣榛樿鎸夊唴瀹归珮搴︽帓鍒楋紝鐢?`Modifier.NONE.weight(w)` 鍙互鎸夋潈閲嶇摐鍒嗗墿浣欓珮搴︼紝
 *   鐢?`Modifier.NONE.fillMaxHeight()` 鍙互鎾戞弧鏁村垪楂樺害锛孾Arrangement] 鎺у埗鏁翠綋鎺掑竷銆? * * 浜ゅ弶杞达紙姘村钩锛夛細榛樿銆屾寜鍐呭鑷€傚簲銆嶏紝鐢?`Modifier.NONE.fillMaxWidth()` 鎾戞弧鏁村垪瀹藉害锛? *   瀵归綈鐢?[horizontalAlignment] 鍐冲畾锛屽瓙鑺傜偣鍙互鐢?`Modifier.NONE.alignSelf(...)` 鍗曠嫭瑕嗙洊銆? * * 绌洪棿涓嶅鏃跺瓙鑺傜偣浼氳鍘嬪埌鍙敤瀹藉害锛屽甫鏉冮噸鐨勫瓙鑺傜偣浼氫竴璧锋敹缂╋紙鑷€傚簲锛夈€? *
 * Kotlin锛? * ```kotlin
 * val column = Column(label, button)
 * column.modifier = Modifier.NONE.fillMaxSize().padding(16.0)
 * column.spacing = 8.0
 * column.horizontalAlignment = Alignment.Horizontal.CENTER
 * column.verticalArrangement = Arrangement.SPACE_BETWEEN
 * column.add(item, Modifier.NONE.weight(1.0).fillMaxWidth())
 * ```
 *
 * Java锛? * ```java
 * Column column = new Column(label, button);
 * column.setModifier(Modifier.NONE.fillMaxSize().padding(16));
 * column.setSpacing(8);
 * column.setHorizontalAlignment(Alignment.Horizontal.CENTER);
 * column.add(item, Modifier.NONE.weight(1).fillMaxWidth());
 * ```
 */
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
