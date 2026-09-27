package mai_onsyn.renderer.cpu4dkt

import com.alibaba.fastjson2.JSONObject

enum class MeshKind {
    GROUP,    // 容器：有子节点，自己没几何（父级自动创建出来的就是它）
    SHAPE,    // 几何形状：几何是 params 的纯函数，可重建、可一行描述
    CARVED,   // 被雕刻过的几何：几何就是顶点本身，参数已失效
    MERGED
}

class Mesh4D @JvmOverloads constructor (
    val tetrahedrons: MutableList<Tetrahedron> = mutableListOf(),
    var name: String = "Unnamed Mesh",
    val transform: Transform4D = Transform4D()
) {
    var dirty: Boolean = true
    var kind: MeshKind = MeshKind.CARVED
    var params: JSONObject? = null

    val rootPath: String    get() = name.substringBefore('/')
    val parentPath: String? get() = name.substringBeforeLast('/', "").ifEmpty { null }
    val leafName: String    get() = name.substringAfterLast('/')
    val isRoot: Boolean     get() = !name.contains('/')

    /** 几何被直接编辑过：参数不再能描述它，降级为 CARVED 并记录原因（params 只给人/AI 看） */
    fun markCarved(reason: String) {
        if (kind == MeshKind.CARVED) return
        params = JSONObject(mapOf(
            "type" to "Carved",
            "reason" to reason,
            "from" to (params ?: JSONObject())
        ))
        kind = MeshKind.CARVED
    }

    fun copy(newName: String) : Mesh4D {
        return Mesh4D(name = newName).also { mesh4D ->
            this.tetrahedrons.forEach { tetrahedron ->
                // 这里是浅拷贝 Vertex是复用的 但是操作最小对象是Vertex 所以应该没问题
                mesh4D.tetrahedrons.add(tetrahedron.copy())
            }
            mesh4D.kind = this.kind
            mesh4D.params = this.params
            mesh4D.transform.matrix = Matrix5f(this.transform.matrix.data.copyOf())
            mesh4D.dirty = true
        }
    }

    /** 把当前变换矩阵烘焙进顶点，返回顶点已变换的新模型；源模型不变 */
    fun applyTransform(): Mesh4D {
        return Mesh4D(name = name).also { mesh4D ->
            this.tetrahedrons.forEach { tetrahedron ->
                mesh4D.tetrahedrons.add(tetrahedron.transform(transform.matrix))
            }
        }
    }
}