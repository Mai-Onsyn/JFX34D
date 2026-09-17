package mai_onsyn.jfx_tools.layout

import javafx.scene.layout.Region

/**
 * 绌虹櫧鍗犱綅鑺傜偣锛堝搴?Compose 鐨?`Spacer`锛夈€? *
 * 榛樿灏哄涓?0锛岄厤鍚?`Modifier.NONE.weight(w)` 灏辨槸銆屽脊绨с€嶏紝鍙互鎶婁袱渚х殑鍐呭椤跺紑锛? * 涔熷彲浠ョ洿鎺ョ粰瀹冨浐瀹氬昂瀵稿綋闂撮殧鐢ㄣ€傚畠涓嶅弬涓庨紶鏍囦簨浠讹紙`mouseTransparent`锛夛紝
 * 浣嗕粛鐒舵槸涓€涓?[Region]锛屽彲浠ョ敤 CSS 璁剧疆鑳屾櫙鑹插綋鍒嗛殧绾夸娇鐢ㄣ€? *
 * Kotlin锛? * ```kotlin
 * row.add(Spacer(), Modifier.NONE.weight(1.0))      // 寮圭哀锛氭妸鍚庨潰鐨勫唴瀹归《鍒板彸杈? * row.add(Spacer(12.0))                             // 12px 鍥哄畾闂撮殧
 * ```
 *
 * Java锛? * ```java
 * row.add(new Spacer(), Modifier.NONE.weight(1));
 * row.add(new Spacer(12));
 * ```
 */
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
