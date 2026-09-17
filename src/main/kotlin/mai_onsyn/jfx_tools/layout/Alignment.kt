package mai_onsyn.jfx_tools.layout

/**
 * 对齐方式常量（[Box] / [Column] / [Row] 共用）。
 *
 * * [Horizontal]：左 / 中 / 右。作为 [Column] 的交叉轴对齐，也可以给 [Row] 的子节点通过
 *   `Modifier.alignSelf(Alignment.Horizontal)` 单独指定。
 * * [Vertical]：上 / 中 / 下。作为 [Row] 的交叉轴对齐，也可以给 [Column] 的子节点单独指定。
 * * [Box]：[Box] 支持的 9 种对齐（3 x 3 组合）。
 *
 * Kotlin：`Alignment.Horizontal.CENTER`、`Alignment.Box.BOTTOM_END`
 *
 * Java：`Alignment.Horizontal.CENTER`、`Alignment.Box.BOTTOM_END`、`Alignment.box(...)`
 */
class Alignment private constructor() {

    /** 水平方向对齐。 */
    enum class Horizontal {
        START, CENTER, END
    }

    /** 垂直方向对齐。 */
    enum class Vertical {
        TOP, CENTER, BOTTOM
    }

    /** [Box] 的九宫格对齐。 */
    enum class Box {
        TOP_START, TOP_CENTER, TOP_END,
        CENTER_START, CENTER, CENTER_END,
        BOTTOM_START, BOTTOM_CENTER, BOTTOM_END
    }

    companion object {

        /** 由水平 + 垂直对齐组合出一个九宫格对齐。 */
        @JvmStatic
        fun box(horizontal: Horizontal, vertical: Vertical): Box = when (horizontal) {
            Horizontal.START -> when (vertical) {
                Vertical.TOP -> Box.TOP_START
                Vertical.CENTER -> Box.CENTER_START
                Vertical.BOTTOM -> Box.BOTTOM_START
            }
            Horizontal.CENTER -> when (vertical) {
                Vertical.TOP -> Box.TOP_CENTER
                Vertical.CENTER -> Box.CENTER
                Vertical.BOTTOM -> Box.BOTTOM_CENTER
            }
            Horizontal.END -> when (vertical) {
                Vertical.TOP -> Box.TOP_END
                Vertical.CENTER -> Box.CENTER_END
                Vertical.BOTTOM -> Box.BOTTOM_END
            }
        }

        /** 取出九宫格对齐中的水平分量。 */
        @JvmStatic
        fun horizontalOf(box: Box): Horizontal = when (box) {
            Box.TOP_START, Box.CENTER_START, Box.BOTTOM_START -> Horizontal.START
            Box.TOP_CENTER, Box.CENTER, Box.BOTTOM_CENTER -> Horizontal.CENTER
            Box.TOP_END, Box.CENTER_END, Box.BOTTOM_END -> Horizontal.END
        }

        /** 取出九宫格对齐中的垂直分量。 */
        @JvmStatic
        fun verticalOf(box: Box): Vertical = when (box) {
            Box.TOP_START, Box.TOP_CENTER, Box.TOP_END -> Vertical.TOP
            Box.CENTER_START, Box.CENTER, Box.CENTER_END -> Vertical.CENTER
            Box.BOTTOM_START, Box.BOTTOM_CENTER, Box.BOTTOM_END -> Vertical.BOTTOM
        }

        /** 对齐对应的偏移系数：0.0 = 起始，0.5 = 居中，1.0 = 末尾。 */
        @JvmStatic
        fun fraction(alignment: Horizontal): Double = when (alignment) {
            Horizontal.START -> 0.0
            Horizontal.CENTER -> 0.5
            Horizontal.END -> 1.0
        }

        /** 对齐对应的偏移系数：0.0 = 顶部，0.5 = 居中，1.0 = 底部。 */
        @JvmStatic
        fun fraction(alignment: Vertical): Double = when (alignment) {
            Vertical.TOP -> 0.0
            Vertical.CENTER -> 0.5
            Vertical.BOTTOM -> 1.0
        }
    }
}
