package mai_onsyn.jfx_tools.layout

/**
 * 主轴排布方式：子节点沿 [Column]（垂直）或 [Row]（水平）的主轴如何分布。
 *
 * 分配规则是：先给所有子节点它们需要的空间，再把剩余空间：
 *
 * * [START]：全部留在末尾（默认）
 * * [CENTER]：两侧各一半
 * * [END]：全部留在开头
 * * [SPACE_BETWEEN]：只分到子节点之间的间隙里（首尾贴边）
 * * [SPACE_AROUND]：每个子节点两侧各分到一半的间隙
 * * [SPACE_EVENLY]：所有间隙（含首尾）等宽
 *
 * 容器自身的 `spacing` 是固定间距，会先占掉空间，剩下的才按上面规则分配；
 * 空间不够时一律退化为 [START] 排布并按 `spacing` 依次排列（可能溢出，不会重叠）。
 */
enum class Arrangement {
    START, CENTER, END, SPACE_BETWEEN, SPACE_AROUND, SPACE_EVENLY
}
