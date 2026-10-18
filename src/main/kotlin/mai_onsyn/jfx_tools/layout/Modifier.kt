package mai_onsyn.jfx_tools.layout

import javafx.geometry.Insets

class Modifier private constructor(
    /** 固定宽度（外框），未设置为 [UNSET]。 */
    val width: Double,
    /** 固定高度（外框），未设置为 [UNSET]。 */
    val height: Double,
    /** 最小宽度，未设置为 [UNSET]。 */
    val minWidth: Double,
    /** 最小高度，未设置为 [UNSET]。 */
    val minHeight: Double,
    /** 最大宽度，未设置为 [UNSET]。 */
    val maxWidth: Double,
    /** 最大高度，未设置为 [UNSET]。 */
    val maxHeight: Double,
    /** 宽度占父容器内容区宽度的比例，未设置为 [UNSET]。 */
    val fillMaxWidth: Double,
    /** 高度占父容器内容区高度的比例，未设置为 [UNSET]。 */
    val fillMaxHeight: Double,
    /** 主轴权重（[Column] / [Row] 按权重瓜分剩余空间），未设置为 [UNSET]。 */
    val weight: Double,
    /** 是否显式设置过 padding。 */
    val isPaddingSet: Boolean,
    val paddingTop: Double,
    val paddingRight: Double,
    val paddingBottom: Double,
    val paddingLeft: Double,
    /** 交叉轴水平对齐覆盖值（[Box] / [Column] 的子节点用）。 */
    val alignHorizontal: Alignment.Horizontal?,
    /** 交叉轴垂直对齐覆盖值（[Box] / [Row] 的子节点用）。 */
    val alignVertical: Alignment.Vertical?
) {
    // ------------------------------------------------------------------ 尺寸

    /** 固定宽度（外框）。会清掉之前的 [fillMaxWidth]。 */
    fun width(value: Double): Modifier = copy(width = value, fillMaxWidth = UNSET)

    /** 固定高度（外框）。会清掉之前的 [fillMaxHeight]。 */
    fun height(value: Double): Modifier = copy(height = value, fillMaxHeight = UNSET)

    /** 同时固定宽高（外框）。 */
    fun size(width: Double, height: Double): Modifier = width(width).height(height)

    fun minWidth(value: Double): Modifier = copy(minWidth = value)

    fun minHeight(value: Double): Modifier = copy(minHeight = value)

    fun maxWidth(value: Double): Modifier = copy(maxWidth = value)

    fun maxHeight(value: Double): Modifier = copy(maxHeight = value)

    /** 宽度撑满父容器内容区。 */
    fun fillMaxWidth(): Modifier = fillMaxWidth(1.0)

    /** 宽度占父容器内容区的 [fraction]（0..1）比例。 */
    fun fillMaxWidth(fraction: Double): Modifier = copy(fillMaxWidth = fraction, width = UNSET)

    /** 高度撑满父容器内容区。 */
    fun fillMaxHeight(): Modifier = fillMaxHeight(1.0)

    /** 高度占父容器内容区的 [fraction]（0..1）比例。 */
    fun fillMaxHeight(fraction: Double): Modifier = copy(fillMaxHeight = fraction, height = UNSET)

    /** 宽高都撑满父容器内容区。 */
    fun fillMaxSize(): Modifier = fillMaxWidth().fillMaxHeight()

    /** 主轴权重：和兄弟节点按权重瓜分剩余空间（只在 [Column] / [Row] 里生效）。 */
    fun weight(value: Double): Modifier = copy(weight = value)

    // ------------------------------------------------------------------ 间距

    /** 四边等距留白。 */
    fun padding(all: Double): Modifier = padding(all, all, all, all)

    /** 水平 [horizontal] / 垂直 [vertical] 留白。 */
    fun padding(horizontal: Double = 0.0, vertical: Double = 0.0): Modifier = padding(vertical, horizontal, vertical, horizontal)

    /** 分别指定四边留白。 */
    fun padding(top: Double = 0.0, right: Double = 0.0, bottom: Double = 0.0, left: Double = 0.0): Modifier =
        copy(
            isPaddingSet = true,
            paddingTop = top,
            paddingRight = right,
            paddingBottom = bottom,
            paddingLeft = left
        )

    /** 用 JavaFX 的 [Insets] 指定四边留白。 */
    fun padding(insets: Insets): Modifier = padding(insets.top, insets.right, insets.bottom, insets.left)

    fun paddingTop(value: Double): Modifier = copy(isPaddingSet = true, paddingTop = value)

    fun paddingRight(value: Double): Modifier = copy(isPaddingSet = true, paddingRight = value)

    fun paddingBottom(value: Double): Modifier = copy(isPaddingSet = true, paddingBottom = value)

    fun paddingLeft(value: Double): Modifier = copy(isPaddingSet = true, paddingLeft = value)

    // ------------------------------------------------------------------ 对齐

    /** 覆盖父容器给出的对齐方式（九宫格）。[Column] 只取水平分量，[Row] 只取垂直分量。 */
    fun alignSelf(alignment: Alignment.Box): Modifier = copy(
        alignHorizontal = Alignment.horizontalOf(alignment),
        alignVertical = Alignment.verticalOf(alignment)
    )

    /** 作为 [Column]（或 [Box]）的子节点时，单独指定水平对齐。 */
    fun alignSelf(alignment: Alignment.Horizontal): Modifier = copy(alignHorizontal = alignment)

    /** 作为 [Row]（或 [Box]）的子节点时，单独指定垂直对齐。 */
    fun alignSelf(alignment: Alignment.Vertical): Modifier = copy(alignVertical = alignment)

    /** [alignSelf] 的别名，对应 Compose 里的 `Modifier.align(...)`。 */
    fun align(alignment: Alignment.Box): Modifier = alignSelf(alignment)

    /** [alignSelf] 的别名，对应 Compose 里的 `Modifier.align(...)`。 */
    fun align(alignment: Alignment.Horizontal): Modifier = alignSelf(alignment)

    /** [alignSelf] 的别名，对应 Compose 里的 `Modifier.align(...)`。 */
    fun align(alignment: Alignment.Vertical): Modifier = alignSelf(alignment)

    // ------------------------------------------------------------------ 组合

    /**
     * 合并另一个 Modifier：凡是 [other] 设置过的属性都以 [other] 为准。
     * padding 视为一个整体（不会逐边合并）。
     */
    fun then(other: Modifier): Modifier {
        if (other === NONE) return this
        if (this === NONE) return other
        return Modifier(
            width = if (isSet(other.width)) other.width else width,
            height = if (isSet(other.height)) other.height else height,
            minWidth = if (isSet(other.minWidth)) other.minWidth else minWidth,
            minHeight = if (isSet(other.minHeight)) other.minHeight else minHeight,
            maxWidth = if (isSet(other.maxWidth)) other.maxWidth else maxWidth,
            maxHeight = if (isSet(other.maxHeight)) other.maxHeight else maxHeight,
            fillMaxWidth = if (isSet(other.fillMaxWidth)) other.fillMaxWidth else fillMaxWidth,
            fillMaxHeight = if (isSet(other.fillMaxHeight)) other.fillMaxHeight else fillMaxHeight,
            weight = if (isSet(other.weight)) other.weight else weight,
            isPaddingSet = other.isPaddingSet || isPaddingSet,
            paddingTop = if (other.isPaddingSet) other.paddingTop else paddingTop,
            paddingRight = if (other.isPaddingSet) other.paddingRight else paddingRight,
            paddingBottom = if (other.isPaddingSet) other.paddingBottom else paddingBottom,
            paddingLeft = if (other.isPaddingSet) other.paddingLeft else paddingLeft,
            alignHorizontal = other.alignHorizontal ?: alignHorizontal,
            alignVertical = other.alignVertical ?: alignVertical
        )
    }

    /** Kotlin 里可以用 `modifierA + modifierB`，等价于 [then]。 */
    operator fun plus(other: Modifier): Modifier = then(other)

    override fun toString(): String {
        val parts = ArrayList<String>()
        if (isSet(width)) parts.add("width=$width")
        if (isSet(height)) parts.add("height=$height")
        if (isSet(minWidth)) parts.add("minWidth=$minWidth")
        if (isSet(minHeight)) parts.add("minHeight=$minHeight")
        if (isSet(maxWidth)) parts.add("maxWidth=$maxWidth")
        if (isSet(maxHeight)) parts.add("maxHeight=$maxHeight")
        if (isSet(fillMaxWidth)) parts.add("fillMaxWidth=$fillMaxWidth")
        if (isSet(fillMaxHeight)) parts.add("fillMaxHeight=$fillMaxHeight")
        if (isSet(weight)) parts.add("weight=$weight")
        if (isPaddingSet) parts.add("padding=($paddingTop,$paddingRight,$paddingBottom,$paddingLeft)")
        if (alignHorizontal != null) parts.add("alignH=$alignHorizontal")
        if (alignVertical != null) parts.add("alignV=$alignVertical")
        return if (parts.isEmpty()) "Modifier.NONE" else "Modifier(${parts.joinToString(", ")})"
    }

    private fun copy(
        width: Double = this.width,
        height: Double = this.height,
        minWidth: Double = this.minWidth,
        minHeight: Double = this.minHeight,
        maxWidth: Double = this.maxWidth,
        maxHeight: Double = this.maxHeight,
        fillMaxWidth: Double = this.fillMaxWidth,
        fillMaxHeight: Double = this.fillMaxHeight,
        weight: Double = this.weight,
        isPaddingSet: Boolean = this.isPaddingSet,
        paddingTop: Double = this.paddingTop,
        paddingRight: Double = this.paddingRight,
        paddingBottom: Double = this.paddingBottom,
        paddingLeft: Double = this.paddingLeft,
        alignHorizontal: Alignment.Horizontal? = this.alignHorizontal,
        alignVertical: Alignment.Vertical? = this.alignVertical
    ): Modifier = Modifier(
        width, height, minWidth, minHeight, maxWidth, maxHeight,
        fillMaxWidth, fillMaxHeight, weight,
        isPaddingSet, paddingTop, paddingRight, paddingBottom, paddingLeft,
        alignHorizontal, alignVertical
    )

    companion object {
        /** 表示「未设置」的哨兵值（`Double.NaN`）。 */
        @JvmField
        val UNSET: Double = Double.NaN

        /** 空 Modifier，所有链式调用的起点。 */
        @JvmField
        val NONE: Modifier = Modifier(
            UNSET, UNSET, UNSET, UNSET, UNSET, UNSET,
            UNSET, UNSET, UNSET,
            false, 0.0, 0.0, 0.0, 0.0,
            null, null
        )

        /** [value] 是否被设置过（即不是 [UNSET]）。 */
        @JvmStatic
        fun isSet(value: Double): Boolean = !value.isNaN()
    }
}

val modifier = Modifier.NONE
