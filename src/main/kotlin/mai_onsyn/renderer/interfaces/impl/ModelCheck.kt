package mai_onsyn.renderer.interfaces.impl

import mai_onsyn.renderer.cpu4dkt.Mesh4D
import mai_onsyn.renderer.cpu4dkt.SimpleScene4D

fun SimpleScene4D.requireContains(name: String): Mesh4D {
    val find = this.meshList.find { it.name == name }
    if (find == null) throw NoSuchElementException("Model \"$name\" does not exist")
    return find
}

fun SimpleScene4D.requireNotContains(name: String) {
    val find = this.meshList.find { it.name == name }
    if (find != null) throw IllegalArgumentException("Model \"$name\" already exists")
}