package mai_onsyn.jfx_tools.layout

import javafx.collections.ListChangeListener
import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import kotlin.math.max
import kotlin.math.min

/**
 * [Column] / [Row] / [Box] 的公共基类。
 *
 * 它本身就是一个普通的 [Pane]：
 *
 * * 子节点就是真正的 JavaFX 子节点，`getChildren()`、`lookup("#id")`、CSS 样式类都正常工作，
 *   也可以随意和 JavaFX 原生布局（HBox / VBox / BorderPane / StackPane…）互相嵌套。
 * * 容器自身的 [modifier] 作用于自己（padding → `Region.padding`，固定尺寸 / fill → `min/pref/max`），
 *   所以放进原生布局里也遵守同样的规则。
 * * 每个子节点的布局参数由添加时传入的 [Modifier] 描述；不传就是「按内容自适应（wrap content）」。
 * * 布局分「测量 + 摆放」两步：先算每个子节点在外框尺寸语义下需要多大，再按 [Arrangement] / 对齐 /
 *   权重摆进去。所有尺寸都会被可用空间裁剪（固定尺寸也一样），空间不足时按子节点顺序依次压缩，
 *   所以容器被压扁 / 挤窄时不会有子节点溢出到容器外面。
 *
 * ### 尺寸约定（border-box）
 * `width` / `height` / `fillMaxWidth` / `fillMaxHeight` 都是「包含自己 padding 的外框尺寸」：
 * `Modifier.NONE.width(100.0).padding(8.0)` 占 100 宽，内容区 84 宽。
 * 子节点的 padding 是它和外层之间的空隙，等价于其它框架里的 margin。
 *
 * ### Java 用法
 * ```java
 * Column column = new Column();
 * column.setModifier(Modifier.NONE.fillMaxSize().padding(16));
 * column.setSpacing(8);
 * column.setHorizontalAlignment(Alignment.Horizontal.CENTER);
 * column.add(new Label("标题"), Modifier.NONE.fillMaxWidth());
 * column.add(new Button("确定"), Modifier.NONE.weight(1));
 * ```
 */
abstract class LayoutContainer<C : LayoutContainer<C>>(vararg children: Node) : Pane(*children) {

    /** 子节点 → 布局参数。 */
    private val childModifiers: MutableMap<Node, Modifier> = HashMap()

    /**
     * 容器自身的布局参数，默认 [Modifier.NONE]（按内容自适应）。
     * 设置后立刻作用到容器自身，并触发重新布局。
     */
    var modifier: Modifier = Modifier.NONE
        set(value) {
            field = value
            applyModifierProperties()
        }

    init {
        this.children.addListener(ListChangeListener<Node> { change ->
            while (change.next()) {
                if (change.wasRemoved()) {
                    for (removed in change.removed) {
                        childModifiers.remove(removed)
                    }
                }
            }
        })
        applyModifierProperties()
    }

    // ------------------------------------------------------------------ 子节点

    /** 添加子节点（按内容自适应，使用容器的默认对齐）。 */
    fun add(child: Node): C = add(child, Modifier.NONE)

    /** 添加子节点并指定它的布局参数。 */
    fun add(child: Node, modifier: Modifier): C {
        childModifiers[child] = modifier
        this.children.add(child)
        return self()
    }

    /** 在指定下标插入子节点。 */
    fun add(index: Int, child: Node, modifier: Modifier): C {
        childModifiers[child] = modifier
        this.children.add(index, child)
        return self()
    }

    /** 批量添加子节点（全部按内容自适应）。 */
    fun addAll(vararg children: Node): C = addAll(children.asList(), Modifier.NONE)

    /** 批量添加子节点并指定同一套布局参数。 */
    fun addAll(children: Collection<Node>, modifier: Modifier): C {
        for (child in children) {
            childModifiers[child] = modifier
            this.children.add(child)
        }
        return self()
    }

    /** 批量添加子节点（全部按内容自适应）。 */
    fun addAll(children: Collection<Node>): C = addAll(children, Modifier.NONE)

    /** 修改一个已经在里面的子节点的布局参数。 */
    fun setChildModifier(child: Node, modifier: Modifier): C {
        childModifiers[child] = modifier
        requestLayout()
        return self()
    }

    /** 读取子节点的布局参数（没有设置过则返回 [Modifier.NONE]）。 */
    fun getChildModifier(child: Node): Modifier = childModifiers[child] ?: Modifier.NONE

    /** 移除子节点。 */
    fun removeChild(child: Node): C {
        this.children.remove(child)
        childModifiers.remove(child)
        return self()
    }

    /** 移除全部子节点。 */
    fun clearChildren(): C {
        this.children.clear()
        childModifiers.clear()
        return self()
    }

    // ------------------------------------------------------------------ 自身参数

    /**
     * 把 [modifier] 中属于容器自身的部分作用到 JavaFX 属性上。
     * 继承这个基类时如果重写了，记得调用 `super.applyModifierProperties()`。
     */
    protected open fun applyModifierProperties() {
        val m = modifier
        if (m.isPaddingSet) {
            setPadding(Insets(m.paddingTop, m.paddingRight, m.paddingBottom, m.paddingLeft))
        }
        applyAxisSize(true, m.width, m.minWidth, m.maxWidth, m.fillMaxWidth)
        applyAxisSize(false, m.height, m.minHeight, m.maxHeight, m.fillMaxHeight)
        // 放进原生 HBox / VBox 里时也保持同样的拉伸行为
        if (Modifier.isSet(m.weight) || Modifier.isSet(m.fillMaxWidth)) HBox.setHgrow(this, Priority.ALWAYS)
        if (Modifier.isSet(m.weight) || Modifier.isSet(m.fillMaxHeight)) VBox.setVgrow(this, Priority.ALWAYS)
        requestLayout()
    }

    private fun applyAxisSize(horizontal: Boolean, fixed: Double, minSize: Double, maxSize: Double, fill: Double) {
        if (Modifier.isSet(fixed)) {
            if (horizontal) {
                setMinWidth(fixed)
                setPrefWidth(fixed)
                setMaxWidth(fixed)
            } else {
                setMinHeight(fixed)
                setPrefHeight(fixed)
                setMaxHeight(fixed)
            }
            return
        }
        val computedMin = if (Modifier.isSet(minSize)) minSize else Region.USE_COMPUTED_SIZE
        val computedMax = when {
            Modifier.isSet(maxSize) -> maxSize
            Modifier.isSet(fill) -> Double.MAX_VALUE
            else -> Region.USE_COMPUTED_SIZE
        }
        if (horizontal) {
            setPrefWidth(Region.USE_COMPUTED_SIZE)
            setMinWidth(computedMin)
            setMaxWidth(computedMax)
        } else {
            setPrefHeight(Region.USE_COMPUTED_SIZE)
            setMinHeight(computedMin)
            setMaxHeight(computedMax)
        }
    }

    // ------------------------------------------------------------------ 工具

    @Suppress("UNCHECKED_CAST")
    protected fun self(): C = this as C

    /** 参与布局的子节点（managed 的子节点）。 */
    protected fun managedChildren(): List<Node> {
        val result = ArrayList<Node>(children.size)
        for (child in children) {
            if (child.isManaged) result.add(child)
        }
        return result
    }

    /** 读取子节点的布局参数。 */
    protected fun modifierOf(child: Node): Modifier = childModifiers[child] ?: Modifier.NONE

    /**
     * 子节点在交叉轴上的对齐系数：0.0 = 起始，0.5 = 居中，1.0 = 末尾。
     *
     * @param mainAxisVertical 主轴是否为垂直方向（[Column] 为 true，此时交叉轴是水平方向，
     *   取子节点的 [Modifier.alignHorizontal]；[Row] 为 false，取 [Modifier.alignVertical]）
     */
    protected fun crossFractionOf(m: Modifier, mainAxisVertical: Boolean, fallback: Double): Double {
        if (mainAxisVertical) {
            val alignment = m.alignHorizontal
            return if (alignment == null) fallback else Alignment.fraction(alignment)
        }
        val alignment = m.alignVertical
        return if (alignment == null) fallback else Alignment.fraction(alignment)
    }

    /**
     * 子节点在交叉轴上的「自适应」尺寸（外框，含自身 padding）。
     * 和 Compose 一样，所有请求都会被可用空间裁剪（固定尺寸 / fill 也不例外）。
     */
    protected fun naturalOuterCross(child: Node, m: Modifier, vertical: Boolean, crossSpace: Double): Double {
        val padCross = if (vertical) m.paddingLeft + m.paddingRight else m.paddingTop + m.paddingBottom
        val fixedCross = if (vertical) m.width else m.height
        val fillCross = if (vertical) m.fillMaxWidth else m.fillMaxHeight
        val available = max(0.0, crossSpace - padCross)
        return when {
            Modifier.isSet(fixedCross) -> max(0.0, min(fixedCross, crossSpace))
            Modifier.isSet(fillCross) -> max(0.0, min(fillCross * crossSpace, crossSpace))
            else -> min(if (vertical) child.prefWidth(-1.0) else child.prefHeight(-1.0), available) + padCross
        }
    }

    // ------------------------------------------------------------------ 线性布局引擎（Column / Row 共用）

    /**
     * 线性布局：主轴方向依次排布，交叉轴方向按对齐摆放。
     *
     * @param vertical 主轴是否为垂直方向（[Column] 为 true，[Row] 为 false）
     * @param spacing 固定间距
     * @param arrangement 主轴排布方式
     * @param crossAlignment 交叉轴默认对齐系数（0.0 / 0.5 / 1.0）
     */
    protected fun layoutLinear(
        vertical: Boolean,
        spacing: Double,
        arrangement: Arrangement,
        crossAlignment: Double
    ) {
        val items = managedChildren()
        if (items.isEmpty()) return

        val ins = insets
        val contentW = max(0.0, width - ins.left - ins.right)
        val contentH = max(0.0, height - ins.top - ins.bottom)
        val mainSpace = if (vertical) contentH else contentW
        val crossSpace = if (vertical) contentW else contentH

        val count = items.size
        val outerMain = DoubleArray(count)
        val outerCross = DoubleArray(count)
        val padMainBefore = DoubleArray(count)
        val padMainAfter = DoubleArray(count)
        val padCrossBefore = DoubleArray(count)
        val padCrossAfter = DoubleArray(count)
        val weights = DoubleArray(count)

        var totalWeight = 0.0
        var nonWeightedMain = 0.0

        for (i in 0 until count) {
            val child = items[i]
            val m = modifierOf(child)

            padMainBefore[i] = if (vertical) m.paddingTop else m.paddingLeft
            padMainAfter[i] = if (vertical) m.paddingBottom else m.paddingRight
            padCrossBefore[i] = if (vertical) m.paddingLeft else m.paddingTop
            padCrossAfter[i] = if (vertical) m.paddingRight else m.paddingBottom

            val padMain = padMainBefore[i] + padMainAfter[i]
            val padCross = padCrossBefore[i] + padCrossAfter[i]

            val fixedMain = if (vertical) m.height else m.width
            val fillMain = if (vertical) m.fillMaxHeight else m.fillMaxWidth

            outerCross[i] = naturalOuterCross(child, m, vertical, crossSpace)

            val weight = m.weight
            if (Modifier.isSet(weight) && weight > 0.0) {
                weights[i] = weight
                totalWeight += weight
                outerMain[i] = 0.0
            } else {
                val innerCross = max(0.0, outerCross[i] - padCross)
                val naturalMain = if (vertical) child.prefHeight(innerCross) else child.prefWidth(innerCross)
                val value = when {
                    Modifier.isSet(fixedMain) -> fixedMain
                    Modifier.isSet(fillMain) -> fillMain * mainSpace
                    else -> naturalMain + padMain
                }
                // 主轴同样按可用空间裁剪，避免容器被压缩时子节点「溢出」到外面
                outerMain[i] = max(0.0, min(value, mainSpace))
                nonWeightedMain += outerMain[i]
            }
        }

        // 带权重的子节点瓜分剩余空间（先扣掉固定间距和非权重子节点占用的空间）
        if (totalWeight > 0.0) {
            val free = max(0.0, mainSpace - spacing * max(0, count - 1) - nonWeightedMain)
            for (i in 0 until count) {
                if (weights[i] > 0.0) {
                    outerMain[i] = free * weights[i] / totalWeight
                }
            }
        }

        // 主轴方向按「剩余空间」依次裁剪：容器被压扁时后面的子节点会被压缩到 0，
        // 而不是溢出到容器外面（和 Compose / 原生 JavaFX 布局一致）
        var placed = 0.0
        for (i in 0 until count) {
            val gap = if (i > 0) spacing else 0.0
            val remaining = max(0.0, mainSpace - placed - gap)
            if (outerMain[i] > remaining) outerMain[i] = remaining
            placed += outerMain[i] + gap
        }

        var totalMain = 0.0
        for (i in 0 until count) totalMain += outerMain[i]
        val totalSpacing = spacing * max(0, count - 1)
        val free = mainSpace - totalMain - totalSpacing

        // 主轴排布：固定间距先占位，剩余空间再按 arrangement 分配
        var startExtra = 0.0
        var extraGap = 0.0
        if (free > 0.0) {
            when (arrangement) {
                Arrangement.START -> Unit
                Arrangement.CENTER -> startExtra = free / 2.0
                Arrangement.END -> startExtra = free
                Arrangement.SPACE_BETWEEN -> if (count > 1) extraGap = free / (count - 1)
                Arrangement.SPACE_AROUND -> {
                    extraGap = free / count
                    startExtra = extraGap / 2.0
                }
                Arrangement.SPACE_EVENLY -> {
                    extraGap = free / (count + 1)
                    startExtra = extraGap
                }
            }
        }

        val mainOrigin = if (vertical) ins.top else ins.left
        val crossOrigin = if (vertical) ins.left else ins.top
        var cursor = mainOrigin + startExtra

        for (i in 0 until count) {
            val child = items[i]
            val m = modifierOf(child)
            val innerMain = max(0.0, outerMain[i] - padMainBefore[i] - padMainAfter[i])
            val innerCross = max(0.0, outerCross[i] - padCrossBefore[i] - padCrossAfter[i])
            val crossFraction = crossFractionOf(m, vertical, crossAlignment)
            val crossFree = max(0.0, crossSpace - outerCross[i])
            val crossPos = crossOrigin + crossFree * crossFraction

            if (vertical) {
                child.resizeRelocate(
                    crossPos + padCrossBefore[i],
                    cursor + padMainBefore[i],
                    innerCross,
                    innerMain
                )
            } else {
                child.resizeRelocate(
                    cursor + padMainBefore[i],
                    crossPos + padCrossBefore[i],
                    innerMain,
                    innerCross
                )
            }
            cursor += outerMain[i] + spacing + extraGap
        }
    }

    /**
     * 线性布局的 pref 尺寸。
     *
     * @param vertical 主轴是否为垂直方向
     * @param spacing 固定间距
     * @param horizontal 是否计算水平方向的尺寸（false 表示计算垂直方向）
     */
    protected fun prefLinear(vertical: Boolean, spacing: Double, horizontal: Boolean): Double {
        val items = managedChildren()
        val ins = insets
        val alongMain = vertical != horizontal
        var sum = 0.0
        var maxValue = 0.0
        for (child in items) {
            val m = modifierOf(child)
            val padH = m.paddingLeft + m.paddingRight
            val padV = m.paddingTop + m.paddingBottom
            val fixedMain = if (vertical) m.height else m.width
            val fixedCross = if (vertical) m.width else m.height
            if (alongMain) {
                sum += when {
                    Modifier.isSet(fixedMain) -> fixedMain
                    vertical -> child.prefHeight(-1.0) + padV
                    else -> child.prefWidth(-1.0) + padH
                }
            } else {
                val size = when {
                    Modifier.isSet(fixedCross) -> fixedCross
                    vertical -> child.prefWidth(-1.0) + padH
                    else -> child.prefHeight(-1.0) + padV
                }
                maxValue = max(maxValue, size)
            }
        }
        val base = if (alongMain) sum + spacing * max(0, items.size - 1) else maxValue
        return base + if (horizontal) ins.left + ins.right else ins.top + ins.bottom
    }

    /**
     * 线性布局的 min 尺寸。带 [Modifier.weight] 的子节点在主轴方向可以压缩到 0，
     * 所以整条布局能随窗口一起收缩（自适应）。
     */
    protected fun minLinear(vertical: Boolean, spacing: Double, horizontal: Boolean): Double {
        val items = managedChildren()
        val ins = insets
        val alongMain = vertical != horizontal
        var sum = 0.0
        var maxValue = 0.0
        for (child in items) {
            val m = modifierOf(child)
            val padH = m.paddingLeft + m.paddingRight
            val padV = m.paddingTop + m.paddingBottom
            val fixedMain = if (vertical) m.height else m.width
            val fixedCross = if (vertical) m.width else m.height
            if (alongMain) {
                val weighted = Modifier.isSet(m.weight) && m.weight > 0.0
                sum += when {
                    weighted -> 0.0
                    Modifier.isSet(fixedMain) -> fixedMain
                    vertical -> child.minHeight(-1.0) + padV
                    else -> child.minWidth(-1.0) + padH
                }
            } else {
                val size = when {
                    Modifier.isSet(fixedCross) -> fixedCross
                    vertical -> child.minWidth(-1.0) + padH
                    else -> child.minHeight(-1.0) + padV
                }
                maxValue = max(maxValue, size)
            }
        }
        val base = if (alongMain) sum + spacing * max(0, items.size - 1) else maxValue
        return base + if (horizontal) ins.left + ins.right else ins.top + ins.bottom
    }
}
