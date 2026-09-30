package mai_onsyn.renderer.interfaces.impl

import mai_onsyn.renderer.cpu4dkt.Mesh4D
import mai_onsyn.renderer.cpu4dkt.SimpleScene4D

fun SimpleScene4D.requireContains(name: String): Mesh4D {
    val find = this.meshList.find { it.name == name }
    if (find == null) throw NoSuchElementException(noSuchMeshMessage(name))
    return find
}

fun SimpleScene4D.requireNotContains(name: String) {
    val find = this.meshList.find { it.name == name }
    if (find != null) throw IllegalArgumentException("Model \"$name\" already exists")
}

/**
 * 找不到模型时的报错文本，带上候选，让 AI 一次自纠而不是再查一轮。
 * 优先给同级的兄弟路径里名字接近的；一个都不像就把同级全部列出来，
 * 因为"什么都不给"对 AI 来说是最没用的报错。
 */
fun SimpleScene4D.noSuchMeshMessage(name: String): String {
    val parent = name.substringBeforeLast('/', "").ifEmpty { null }
    val pool = (if (parent != null) childrenOf(parent).map { it.name } else meshList.map { it.name })
        .filter { it != name }
        .distinct()
    if (pool.isEmpty()) return "Model \"$name\" does not exist"

    val leaf = name.substringAfterLast('/').lowercase()
    val similar = pool.filter { it.substringAfterLast('/').lowercase().contains(leaf) }
    val candidates = (if (similar.isNotEmpty()) similar else pool).take(8)
    return "Model \"$name\" does not exist. Candidates: ${candidates.joinToString(", ")}"
}



/**
 * name 命中一个模型时只作用于它；name 是一个分组路径（如 "Tower"）时作用于
 * 它自己和它下面所有子模型，这样一个模型上的整体变换不用逐个部件调用。
 */
fun SimpleScene4D.targets(name: String): List<Mesh4D> {
    val exact = this.findMesh(name)
    val sub = this.subTree(name)
    if (exact == null && sub.isEmpty()) this.requireContains(name)   // 抛出带候选的异常
    return listOfNotNull(exact) + sub
}