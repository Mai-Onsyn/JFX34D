package mai_onsyn.jfx_tools.layout

import javafx.scene.Node
import kotlin.math.max

/**
 * 姘村钩鏂瑰悜鐨勭嚎鎬у竷灞€锛堝搴?Compose 鐨?`Row`锛夈€備富杞?= 姘村钩锛屼氦鍙夎酱 = 鍨傜洿銆? *
 * * 涓昏酱锛堟按骞筹級锛氬瓙鑺傜偣榛樿鎸夊唴瀹瑰搴︽帓鍒楋紝鐢?`Modifier.NONE.weight(w)` 鍙互鎸夋潈閲嶇摐鍒嗗墿浣欏搴? *   锛堝仛宸﹀彸鍒嗘爮銆佹拺婊℃寜閽瓑锛夛紝`Modifier.NONE.fillMaxWidth()` 鎾戞弧鏁磋瀹藉害銆? * * 浜ゅ弶杞达紙鍨傜洿锛夛細榛樿銆屾寜鍐呭鑷€傚簲銆嶏紝鐢?`Modifier.NONE.fillMaxHeight()` 鎾戞弧鏁磋楂樺害锛? *   瀵归綈鐢?[verticalAlignment] 鍐冲畾锛屽瓙鑺傜偣鍙互鐢?`Modifier.NONE.alignSelf(...)` 鍗曠嫭瑕嗙洊銆? * * 绌洪棿涓嶅鏃跺瓙鑺傜偣浼氳鍘嬪埌鍙敤瀹藉害锛屽甫鏉冮噸鐨勫瓙鑺傜偣浼氫竴璧锋敹缂╋紙鑷€傚簲锛夈€? *
 * Kotlin锛? * ```kotlin
 * val row = Row(icon, title)
 * row.modifier = Modifier.NONE.fillMaxWidth()
 * row.spacing = 8.0
 * row.verticalAlignment = Alignment.Vertical.CENTER
 * row.horizontalArrangement = Arrangement.SPACE_BETWEEN
 * row.add(button, Modifier.NONE.width(96.0))
 * ```
 *
 * Java锛? * ```java
 * Row row = new Row(icon, title);
 * row.setModifier(Modifier.NONE.fillMaxWidth());
 * row.setSpacing(8);
 * row.setVerticalAlignment(Alignment.Vertical.CENTER);
 * row.add(button, Modifier.NONE.width(96));
 * ```
 */
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
