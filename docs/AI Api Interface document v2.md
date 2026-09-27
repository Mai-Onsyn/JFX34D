# JFX34D AI API 接口文档（完善版 v2）

> 本文档是 `docs/AI Api Interface document.md` 的**完善版**，依据
> `src/main/kotlin/mai_onsyn/renderer/interfaces/` 下的接口与 `impl/` 下的实现编写。
>
> 涉及的接口：`RendererInterface`、`SceneInterface`、`CameraInterface`、`ModelInterface`、
> `TransformInterface`、`GeometryInterface`、`ShapeInterface`、`IOInterface`。
>
> **渲染包现状**：除 `IOInterface`（`saveModel` / `loadModel`）外，其余接口**全部已实现**；
> 模型按**名称路径**组成树（见 §0.3），形状操作要求显式给出目标路径。
>
> **原文档未被修改**。约定：与 AI 交互的数据（发给 AI 的 markdown、AI 返回的 json）
> **一律使用英文**，中文只出现在本文档的说明文字中。

---

## 目录

- [零、接口总览](#零接口总览)
- [一、数据格式](#一数据格式)
- [二、AI 交互数据格式](#二ai-交互数据格式)
- [三、四维摄像机](#三四维摄像机)
- [四、模型](#四模型)
- [五、基础形状](#五基础形状)
- [六、模型几何编辑](#六模型几何编辑)
- [七、文件与模型 IO](#七文件与模型-io)
- [八、场景与渲染设置](#八场景与渲染设置)
- [九、完整交互示例](#九完整交互示例)
- [十、附录](#十附录)

---

## 零、接口总览

### 0.1 接口 → 操作名映射

`RendererInterface` 是唯一门面，七个子接口对应七个操作类别。AI 看到的不是方法名，
而是 JSON 里的 `type`（操作名）。**接口签名以当前代码为准**（下表为实际签名）。

| 子接口 | 接口方法（当前签名） | 操作名（JSON `type`） | 章节 |
| --- | --- | --- | --- |
| `CameraInterface` | `getPosition()` | `GET_CAMERA_POS` | §3.1.1 |
| | `moveRight/Up/Ana/Forward(d)` | `MOVE_CAMERA_POS` | §3.1.2 |
| | `setPosition(pos)` | `SET_CAMERA_POS` | §3.1.3 |
| | `getView()` | `GET_CAMERA_VIEW` | §3.2.1 |
| | `rotateXY/XZ/XW/YZ/YW/ZW(a)` | `ROTATE_CAMERA_VIEW` | §3.2.2 |
| | `setView(v)` | `SET_CAMERA_VIEW` | §3.2.3 |
| `ModelInterface` | `listModel(): List<String>` | `LIST_MODEL` | §4.1.1 |
| | `createEmptyModel(name)` | `CREATE_MODEL` | §4.1.2 |
| | `removeModel(name)` | `DELETE_MODEL` | §4.1.3 |
| | `copyModel(src, dst)` | `COPY_MODEL` | §4.1.4 |
| | `mergeModel(s1, s2, dst)` | `MERGE_MODEL` | §4.1.5 |
| | `applyTransformToVertex(src, dst)` | `APPLY_TRANSFORM_TO_VERTEX` | §4.1.6 |
| `TransformInterface` | `getModelMatrix(name): Matrix5f` | `GET_MODEL_MATRIX` | §4.3 |
| | `setModelMatrix(name, m)` | `TRANSFORM_MODEL` + `apply: false` | §4.2 |
| | `transform(name, m)` | `TRANSFORM_MODEL` method `MATRIX` | §4.2.1 |
| | `move(name, v)` | `TRANSFORM_MODEL` method `TRANSLATE` | §4.2.2 |
| | `scale(name, x, y, z, w)` | `TRANSFORM_MODEL` method `SCALE` | §4.2.3 |
| | `rotate(name, axis, angle)` | `TRANSFORM_MODEL` method `ROTATE` | §4.2.4 |
| | `clip(name, src, dest, amount)` | `TRANSFORM_MODEL` method `CLIP` | §4.2.5 |
| | `setCoordinate(name, origin, v)` | `TRANSFORM_MODEL` method `COORDINATE` | §4.2.6 |
| `ShapeInterface` | `createTetrahedron(target, name, center, radius)` | `CREATE_TETRAHEDRON` | §5.2.1 |
| | `create5Cell(target, name, center, size)` | `CREATE_5CELL` | §5.2.2 |
| | `create16Cell(target, name, center, radius)` | `CREATE_16CELL` | §5.2.3 |
| | `createTesseract(target, name, center, edgeLength)` | `CREATE_TESSERACT` | §5.2.4 |
| | `createPrism4(target, name, base: Mesh, ws, we)` | `CREATE_PRISM4` | §5.2.5 |
| | `createCone4(target, name, base: Mesh, apex)` | `CREATE_CONE4` | §5.2.6 |
| | `createBall4(target, name, center, radius, density)` | `CREATE_BALL4` | §5.2.7 |
| `GeometryInterface` | `getModelInfos(name): String` | `GET_MODEL_INFO` | §6.1 |
| | `getTetrahedronInfos(name, tetIndex): String` | `GET_TETRAHEDRON` | §6.2 |
| | `setVertex(name, tetIndex, vertexNum, pos, color?, normal?)` | `SET_TETRAHEDRON_VERTEX` | §6.3 |
| | `transformTetrahedrons(name, tetIndices, transform)` | `TRANSFORM_TETRAHEDRONS` | §6.4 |
| | `addTetrahedron(name, tetrahedron): Int` | `ADD_TETRAHEDRON` | §6.5 |
| | `removeTetrahedron(name, tetIndex)` | `REMOVE_TETRAHEDRON` | §6.6 |
| | `sliceModel(name, plane, pathA, pathB)` | `SLICE_MODEL` | §6.7 |
| `IOInterface` | `saveModel(name, fileName)` | `SAVE_MODEL` | §7.1 |
| | `loadModel(name, file)` | `LOAD_MODEL` | §7.2 |
| `SceneInterface` | `set3DMaxFPS(fps)` | `SET_3D_MAX_FPS` | §8.1 |
| | `get3DFPS()` | `GET_3D_FPS` | §8.2 |
| | `get3D1PercentLowFPS()` | `GET_3D_1PERCENT_LOW_FPS` | §8.3 |
| | `set4DMaxFPS(fps)` | `SET_4D_MAX_FPS` | §8.4 |
| | `get4DFPS()` | `GET_4D_FPS` | §8.5 |
| | `get4D1PercentLowFPS()` | `GET_4D_1PERCENT_LOW_FPS` | §8.6 |
| | `enableTriangleLineRendering(b)` | `SET_TRIANGLE_LINE_RENDERING` | §8.7 |
| | `setBackgroundColor(color)` | `SET_BACKGROUND_COLOR` | §8.8 |
| | `enableLightRendering(b)` | `SET_LIGHT_RENDERING` | §8.9 |
| | `listLights(): String` | `LIST_LIGHTS` | §8.10 |
| | `addLights(light)` | `ADD_LIGHT` | §8.11 |
| | `removeLights(names)` | `REMOVE_LIGHTS` | §8.12 |
| | `setAmbientLight(argb)` | `SET_AMBIENT_LIGHT` | §8.13 |
| | `setDisplaySize(edgeLength)` | `SET_DISPLAY_SIZE` | §8.14 |

> **接口初始化**：`RendererInterface.INSTANCE` 在 `RendererInterface.init(region)` 之前访问会报错；
> 由 `GL4DRegion`（`renderer/core/GL4DRegion.kt`）注入场景与渲染器，见附录 C。

### 0.2 全局约定

| 项目 | 约定 |
| --- | --- |
| 坐标系 | **左手坐标系**：x+ 右，y+ 上，z+ ana 前，w+ 深度前 |
| 四维坐标 | `(x, y, z, w)`，轴名 `x` `y` `z` `w` |
| 三维坐标（光源等） | `(x, y, z)`，轴名同上 |
| 轴枚举 | 小写：`"x"` `"y"` `"z"` `"w"` |
| 平面枚举 | 小写：`"xy"` `"xz"` `"xw"` `"yz"` `"yw"` `"zw"` |
| 角度 | JSON 与接口参数**都是角度制（度）**；`Transform4D.rotate`、`CameraInterfaceImpl.rotate*` 内部用 `toRadians` 转弧度 |
| 颜色 | 十六进制 ARGB 字符串 `"#AARRGGBB"`，或整数 `0xAARRGGBB`（背景色也接受 `"#RRGGBB"`） |
| 向量写法 | 数组 `[1, 2, 3, 4]` 或字符串 `"(1 2 3 4)"`，两种都接受 |
| 模型矩阵 | 5×5 **行主序**，行向量写作 `[a, b, c, d, e]` 或 `"(a b c d e)"` |
| 顶点约定 | 顶点是**列向量**，`p_out = M · p_in`；平移量写在第 5 列（见 `Matrix5f` / `Matrix5x5::transform`） |
| 变换叠加 | 逐个变换**左乘**：`M ← T · M`（`TransformInterfaceImpl.transform`） |
| 旋转正方向 | 平面 `(a, b)` 上 **a 轴转向 b 轴为正**：`a' = a·cos θ − b·sin θ`，`b' = a·sin θ + b·cos θ`（`Transform4D.rotate`）；摄像机视角正方向相反（§3.2.2） |
| 路径分隔符 | `/`，模型路径形如 `"Tower/Upper/Lid"`（见 §0.3） |
| 长度单位 | 无单位（世界单位），由使用者约定比例 |

### 0.3 模型树与路径寻址

**这是当前渲染包最重要的约定**：模型不再靠"当前模型 / 目标模型"这类隐式状态，
而是靠**名称路径**组成一棵树。

#### 路径规则

| 规则 | 说明 |
| --- | --- |
| 路径 | `Mesh4D.name` 就是完整路径，如 `"Tower/Base"`；`/` 分段 |
| 根路径 | 第一段，即 `rootPath`；`LIST_MODEL` **只列根路径**（去重） |
| 中间层不必存在 | `"Tower/Upper/Lid"` 可以直接建出来，不需要先建 `"Tower/Upper"` |
| 父路径 | `parentPath` = 去掉最后一段，如 `"Tower/Upper"`；根节点为 `null` |
| 叶名 | `leafName` = 最后一段，如 `"Lid"` |
| 直接子节点 | `childrenOf(path)`，只比 `parentPath` |
| 全部后代 | `subTree(path)`，按 `"path/"` 前缀匹配，**含隔代** |

```text
Tower                     GROUP   （CREATE_MODEL 建出来的空容器）
├─ Tower/Base             SHAPE   （CREATE_TESSERACT target=Tower name=Base）
├─ Tower/Upper            （不是真实模型，只是名字里的一段）
│  └─ Tower/Upper/Lid     SHAPE   （CREATE_TESSERACT target=Tower/Upper name=Lid）
└─ Tower/All              MERGED  （MERGE_MODEL 产出的）
```

#### 模型种类（kind）

| kind | 含义 | 什么时候出现 |
| --- | --- | --- |
| `GROUP` | 纯容器，自己没有几何 | `CREATE_MODEL` 建出来的空模型 |
| `SHAPE` | 参数化几何：几何是 `params` 的纯函数，可重建 | 七个 `CREATE_*` 形状操作 |
| `CARVED` | 被雕刻过：几何就是顶点本身，`params` 已失效 | 顶点/四面体级编辑、切片、烘焙变换的产物 |
| `MERGED` | 两个模型合并的结果 | `MERGE_MODEL` |

`params` 是一段 JSON（给人/AI 看），例如
`{"type":"Tesseract","center":"(0, 0, 0, 0)","edge length":"2.0","cells":"8"}`。
雕刻降级时会写成 `{"type":"Carved","reason":"vertex edited","from":{原来的 params}}`（见 §6.8）。

#### 寻址规则（哪些操作接受哪些路径）

| 操作类别 | 接受的路径 | 行为 |
| --- | --- | --- |
| `GET_MODEL_INFO` | 任意 | 真实模型给详情；只有子节点的"分组路径"给汇总；不存在则报错 |
| `TRANSFORM_MODEL` | 真实模型，或分组路径 / 带子节点的模型 | 命中真实模型时只作用于它；分组路径时作用于**它自己和全部后代** |
| `GET_MODEL_MATRIX` | **只接受真实存在的路径** | 不展开分组；纯分组路径会报"不存在" |
| 几何编辑（§6.3~§6.7） | **只能是没有子节点的真实模型** | 分组路径、带子节点的模型会被拒绝，并在错误里给出可选路径 |
| `CREATE_*` 形状 | `target` 可为多级路径（不必存在） | 结果路径 = `target` + `/` + `name`，且必须**尚不存在** |
| `COPY_MODEL` / `MERGE_MODEL` / `APPLY_TRANSFORM_TO_VERTEX` | 源必须是真实存在的路径 | 只复制/读取该节点自身，**不递归子模型** |
| `DELETE_MODEL` | 任意前缀 | 删除该路径**及其整棵子树**（`name` 和 `name/...`） |

#### 找不到模型时的报错

报错会带上候选路径，方便一次自纠，例如：

```markdown
Error: Model "Tower/Bass" does not exist. Candidates: Tower/Base, Tower/Upper/Lid
```

候选的挑选顺序：先找同级兄弟里叶名相似（忽略大小写、子串匹配）的，
没有相似的就把同级全部列出（最多 8 个）。

---

## 一、数据格式

### 1.1 文件数据格式

4D 模型文件后缀 `.4do`，纯文本，每个标签占一行，空格分隔，标签使用线性索引，不同标签不共享索引。

| 标签 | 数据长度 |  数据格式   |                   描述                   |           示例            |
| :--: | :------: | :---------: | :--------------------------------------: | :-----------------------: |
|  v   |    4     |    Float    |               顶点位置坐标               |        v 1 1 0 2.5        |
|  vn  |    4     |    Float    |          顶点法向量（单位向量）          |        vn 1 0 0 0         |
|  vc  |    1     |     Int     |             顶点颜色（ARGB）             |       vc 0xFF808080       |
|  t   |    4     | Int/Int/Int | 四面体描述（引用 v/vn/vc 的索引，从 0 开始） | t 0/0/0 1/1/1 2/2/2 3/3/3 |

> 注：原文档中 `vc` 行的示例误写为 `vn 0xFF808080`，此处已修正。
> 读写由 `IOInterface` 负责，**当前未实现**（§7）。

### 1.2 代码数据格式

**模型**（`cpu4dkt/Mesh4D.kt`）：

```kotlin
enum class MeshKind { GROUP, SHAPE, CARVED, MERGED }

class Mesh4D(
    val tetrahedrons: MutableList<Tetrahedron> = mutableListOf(),
    var name: String = "Unnamed Mesh",          // 完整路径，如 "Tower/Base"
    val transform: Transform4D = Transform4D()  // 模型变换矩阵
) {
    var dirty: Boolean = true
    var kind: MeshKind = MeshKind.CARVED
    var params: JSONObject? = null              // 参数化描述，给人/AI 看

    val rootPath: String                        // "Tower"
    val parentPath: String?                     // "Tower/Upper"
    val leafName: String                        // "Lid"
    val isRoot: Boolean

    /** 几何被直接编辑过：降级为 CARVED 并记录原因 */
    fun markCarved(reason: String)

    /** 复制出一个新名字的模型（四面体独立，顶点实例复用；kind/params/矩阵一起带过去） */
    fun copy(newName: String): Mesh4D

    /** 把变换矩阵烘焙进顶点，返回新模型；源模型不变 */
    fun applyTransform(): Mesh4D
}
```

**变换**（`cpu4dkt/Transform4D.kt`）：

```kotlin
class Transform4D(var matrix: Matrix5f = Matrix5f.IDENTITY) {
    fun move(v: Vector4f)
    fun scale(x: Float, y: Float = x, z: Float = x, w: Float = x)
    fun rotate(axis: Direction.Plane, angle: Float)   // 角度制，内部 toRadians
    fun clip(src: Direction.Axis, dest: Direction.Axis, k: Float)
    fun setTransformCoordinate(origin: Vector4f, coordinate: Coordinate4D)
}
```

每个变换都是 `matrix ← T⁻¹ · new · T · matrix`（`T` 是当前变换坐标系），
即**在当前坐标系下解释、并左乘到已有矩阵上**。

**四面体与顶点**（`cpu4dkt/Tetrahedron.kt`）：

```kotlin
data class Tetrahedron(
    val v0: Vertex4D, val v1: Vertex4D, val v2: Vertex4D, val v3: Vertex4D,
    val id: Long = Random.nextLong()     // 随机 id，仅供参考
) {
    val vertices: List<Vertex4D>         // v0..v3
    fun transform(matrix5f: Matrix5f): Tetrahedron
}

data class Vertex4D(val pos: Vector4f, val color: ColorARGB, val normal: Vector4f)
```

> **寻址用索引不用 id**：`GeometryInterface` 的 `tetIndex` 是该模型内四面体列表的**下标**
> （从 0 开始，按加入顺序）。

**其他类型**：

| 类型 | 位置 | 说明 |
| --- | --- | --- |
| `Matrix5f` | `cpu4dkt/Matrix5f.kt` | 5×5 **行主序**；`m * m`、`m * Vector5f`、`m[i]` 读写、`IDENTITY` |
| `Vector5f` | `cpu4dkt/Vector5f.kt` | `(x, y, z, w, u)`；由 `Vector4f` 构造时 `u = 1` |
| `Coordinate4D` | `utils/Coordinate4D.kt` | 四维基坐标系 `vx, vy, vz, vw` |
| `Direction.Axis` | `utils/Direction.kt` | `X Y Z W` |
| `Direction.Plane` | `utils/Direction.kt` | `XY XZ XW YZ YW ZW` |
| `ColorARGB` | `utils/ColorARGB.kt` | `#AARRGGBB` 打包颜色，`toString()` 输出 `#AARRGGBB` |
| `Light` | `ogl3d/data/Light.kt` | 光源（纯数据），见 §8.11 |
| `Mesh`（3D） | `ogl3d/data/Mesh.kt` | 三角形列表 + 3D 变换；形状操作的 `base` 用它 |

### 1.3 枚举与取值

**单轴**：`x` | `y` | `z` | `w`（`MOVE_CAMERA_POS`、`TRANSLATE`、`SCALE`、`CLIP`）

**平面**：`xy` | `xz` | `xw` | `yz` | `yw` | `zw`（`ROTATE_CAMERA_VIEW`、`ROTATE`；
3D 网格的 `ROTATE` 只有 `xy` | `xz` | `yz`）

**模型种类**：`GROUP` | `SHAPE` | `CARVED` | `MERGED`

> 四维中"绕轴旋转"必须写成"在哪个二维平面内旋转"，一条轴无法唯一确定旋转。
> `xy` `yz` `xz` 是三维已有的旋转，`xw` `yw` `zw` 是卷入/卷出第四维的旋转。

---

## 二、AI 交互数据格式

### 2.1 发送 markdown

执行并解析 AI 返回的 json 后，把结果按需发回给 AI：

````markdown
# GET_CAMERA_POS(id=1)
Pos=(0, 0, 0, 0)
````

- 只有**显式写成 `"no_result": false`** 的操作才会产生一段 markdown，按操作顺序排列。
- 首行固定为 `# <操作名>(id=<该操作的 id>)`，id 用于让 AI 把结果与请求对上。
- `no_result` 缺省或为 `true` 的操作不产生任何返回段落。
- 查询类操作（`GET_MODEL_INFO`、`GET_TETRAHEDRON`、`LIST_LIGHTS`）返回的正文本身就是 markdown，
  以 `##` 开头，直接接在首行下面。

### 2.2 返回 json

顶层字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `version` | Int | 是 | 协议版本，当前为 `1` |
| `done` | Boolean | 否（默认 `false`） | 本轮对话是否结束 |
| `operations` | Array | 是 | 操作数组，结束本轮时为空数组 |

每个操作：

- **type**(String)：操作名称
- **id**(Int)：操作 id，由 AI 决定；单次返回内必须唯一
- **data**(JSON)：操作参数；无参数的操作（如 `GET_CAMERA_POS`、`LIST_MODEL`）可省略

可选字段：

- **no_result**(Boolean)：不接收该条命令的执行结果，默认为 `true`，一般不建议使用，
  只有在同时执行了多条类似命令时（如连续创建形状），且之后用查询命令能拿到全部结果时，才可以这样优化

```json
{
    "version": 1,
    "done": false,
    "operations": [
        {
            "type": "MOVE_CAMERA_POS",
            "id": 1,
            "no_result": false,
            "data": { "axis": "x", "distance": -5 }
        },
        {
            "type": "GET_CAMERA_POS",
            "id": 2,
            "no_result": false
        }
    ]
}
```

结束本轮：

```json
{
    "version": 1,
    "done": true,
    "operations": []
}
```

### 2.3 错误返回

**格式 / 解析错误**（整个 json 无法解析、缺 `type`/`id` 等）：

````markdown
# Error
You just returned an error format:
```json
AI返回的数据
```
Reason:
```markdown
错误原因
```
````

**单条操作失败**（格式正确但参数非法、对象不存在、路径不对）：只影响该条，其余操作继续执行。
错误文本直接采用渲染实现抛出的消息，例如：

```markdown
# CREATE_MODEL(id=5)
Error: Model "Cube A" already exists
```

```markdown
# CREATE_TESSERACT(id=6)
Error: Model "Tower/Bass" does not exist. Candidates: Tower/Base
```

```markdown
# SET_TETRAHEDRON_VERTEX(id=7)
Error: "Tower" is a group of 2 sub models and cannot hold geometry, pick one of: Tower/Base, Tower/Upper
```

### 2.4 通用校验规则

| 校验项 | 规则 |
| --- | --- |
| 操作数组成员 | 必须含 `type`(String)、`id`(Int，本次请求内唯一)；需要参数的操作必须含 `data` |
| 未知操作名 | 该条返回错误，不影响其他操作 |
| 必填参数缺失 / 类型不对 | 该条返回错误 |
| 模型路径 | 必须存在（查询/编辑类），或必须不存在（创建/复制目标），见 §0.3 |
| 半径、棱长、高度、density | `> 0`（实现会给 `xxx must be > 0, got ...`） |
| 棱柱 / 棱锥边数 | `sides >= 3` 的整数 |
| 平面拉伸范围 | `ws != we`；`ws > we` 时实现自动交换 |
| 顶点编号 | `0 ~ 3` |
| 四面体索引 | 必须落在 `[0, 数量)`，否则报 `Tetrahedron index N is out of range [0, M) in "path"` |
| 超平面四点 | 必须仿射无关（不共超平面），否则报 `The four points are affinely dependent...` |
| 方向向量 | `SET_CAMERA_VIEW` 的四个基向量需单位且两两正交（校验在接入层做，见 §3.2.3） |
| 切片目标 | `pathA != pathB`，且两者都必须不存在 |

---

## 三、四维摄像机

### 3.1 位置

```kotlin
fun moveRight(d: Float)     // x+
fun moveUp(d: Float)        // y+
fun moveAna(d: Float)       // z+
fun moveForward(d: Float)   // w+
```

d 为负数时反向移动。移动**只相对于当前摄像机坐标系**（`pos += v_axis * d`），不改变视角。

#### 3.1.1 获取位置

**GET_CAMERA_POS**：返回当前摄像机坐标。无参数。

```markdown
# GET_CAMERA_POS(id=1)
Pos=(0, 0, 0, 0)
```

#### 3.1.2 移动位置

**MOVE_CAMERA_POS**：按单轴移动摄像机

必须包含的参数：

- axis(Enum)：`x` | `y` | `z` | `w`，依次对应 `moveRight` / `moveUp` / `moveAna` / `moveForward`
- distance(Float)：移动距离，可正可负

```json
{
    "type": "MOVE_CAMERA_POS",
    "id": 1,
    "no_result": false,
    "data": { "axis": "x", "distance": -5 }
}
```

```markdown
# MOVE_CAMERA_POS(id=1)
Success
Now Pos=(-5, 0, 0, 0)
```

#### 3.1.3 设置位置

**SET_CAMERA_POS**：直接更改摄像机坐标

必须包含的参数：

- pos(Vector4f)：四维坐标

```markdown
# SET_CAMERA_POS(id=2)
Success
Now Pos=(0, 0, 0, 0)
```

### 3.2 视角

视角是四个基向量 `vx, vy, vz, vw`（右、上、ana 前、第四维前）。六个旋转平面：

| 平面 | 含义 |
| --- | --- |
| `xy` | 屏幕内旋转（等于 3D 的 roll） |
| `xz` | 右方向卷入 / 卷出第四维 |
| `xw` | 偏航 yaw（左右环顾，不动 vy、vz） |
| `yz` | 上方向卷入 / 卷出第四维 |
| `yw` | 俯仰 pitch（抬头低头，不动 vx、vz） |
| `zw` | 前方卷入 / 卷出第四维（4D 里最有"另类"感的旋转） |

#### 3.2.1 获取视角

**GET_CAMERA_VIEW**：获取摄像机坐标系向量在世界中的指向。无参数。

```markdown
# GET_CAMERA_VIEW(id=3)
vx=(1, 0, 0, 0)
vy=(0, 1, 0, 0)
vz=(0, 0, 1, 0)
vw=(0, 0, 0, 1)
```

#### 3.2.2 旋转视角

**ROTATE_CAMERA_VIEW**：在指定平面内旋转摄像机视角

> 原文档中该操作名误写为 `ROTATA_CAMERA_VIEW`，此处更正为 `ROTATE_CAMERA_VIEW`。

必须包含的参数：

- axis(Enum)：`xy` | `xz` | `xw` | `yz` | `yw` | `zw`
- angle(Float)：角度制，可负

旋转的是**摄像机的四个基向量**（不是模型），按 `Camera4D.rotatePlane` 的定义：

```
a' = a·cos θ + b·sin θ
b' = −a·sin θ + b·cos θ
```

即正角度让基向量 `a` 转向 `b`；因为观察矩阵的行就是这四个基向量，
**画面看起来朝相反方向旋转**（视觉上表现为 `b` 转向 `a`）。

> 对比 §4.2.4：模型 / 几何的 `ROTATE` 旋转的是顶点（`a' = a·cos − b·sin`），
> 与这里**符号相反**。另外坐标系是左手系（§0.2），不要用右手定则判断方向。

```json
{
    "type": "ROTATE_CAMERA_VIEW",
    "id": 4,
    "no_result": false,
    "data": { "axis": "xw", "angle": 30 }
}
```

```markdown
# ROTATE_CAMERA_VIEW(id=4)
Success
Current is:
vx=(0.8660254, 0, 0, 0.5)
vy=(0, 1, 0, 0)
vz=(0, 0, 1, 0)
vw=(-0.5, 0, 0, 0.8660254)
```

（`xy` 转 90° 会把 `vx` 转到 `+vy`、`vy` 转到 `-vx`。）

#### 3.2.3 设置视角

**SET_CAMERA_VIEW**：直接设置摄像机坐标系

必须包含的参数：

- vx, vy, vz, vw(Vector4f)

要求四个向量是**单位向量**且**两两正交**（四四正交）。不确定合法值时，
更推荐用 `ROTATE_CAMERA_VIEW` 逐步旋转。
校验在接入层做：`CameraInterfaceImpl.setView` 是直接赋值。

```markdown
# SET_CAMERA_VIEW(id=5)
Success
Current is:
vx=(1, 0, 0, 0)
vy=(0, 1, 0, 0)
vz=(0, 0, 1, 0)
vw=(0, 0, 0, 1)
```

---

## 四、模型

模型是一棵**按名称路径组织**的树，规则见 §0.3。本章操作针对树节点本身；
形状创建在第五章，几何编辑在第六章。

### 4.1 模型树操作

#### 4.1.1 查询所有模型

**LIST_MODEL**：列出所有**根路径**（去重，顺序为添加顺序）

无参数

```markdown
# LIST_MODEL(id=6)
0. "Tower"
1. "Gallery"
2. "Sculpt"
```

要列出某个模型下面的部件，用 `GET_MODEL_INFO`（§6.1），它会给出 `Sub models`。

#### 4.1.2 创建模型

**CREATE_MODEL**：创建一个**空模型**（`kind = GROUP`），追加到场景末尾

必须包含的参数：

- name(String)：模型路径，必须未被占用

```json
{
    "type": "CREATE_MODEL",
    "id": 7,
    "no_result": false,
    "data": { "name": "Tower" }
}
```

```markdown
# CREATE_MODEL(id=7)
Successfully create "Tower"
Now models: 2
```

> 空模型本身没有几何，`kind = GROUP`；把形状建在它下面即可（§5.1）。

#### 4.1.3 删除模型

**DELETE_MODEL**：删除该路径**及其整棵子树**（`name` 和所有 `name/...`）

必须包含的参数：

- name(String)：要删除的前缀路径，必须存在

```markdown
# DELETE_MODEL(id=8)
Successfully delete "Tower"
Removed 3 sub tree nodes, now models: 1
```

#### 4.1.4 复制模型

**COPY_MODEL**：复制一个模型节点

必须包含的参数：

- src(String)：源路径，必须存在
- dst(String)：新路径，必须不存在

复制内容：全部四面体（四面体独立、**顶点实例复用**）、`kind`、`params`、以及**变换矩阵**。
注意**不递归子模型**——只复制 `src` 这一个节点。

```markdown
# COPY_MODEL(id=9)
Successfully copy "Tower/Base" to "Tower/BaseCopy"
Tetrahedrons: 48
```

#### 4.1.5 合并模型

**MERGE_MODEL**：把两个模型合并成一个新模型

必须包含的参数：

- src1(String)：源 1，必须存在
- src2(String)：源 2，必须存在
- dst(String)：新路径，必须不存在

语义：两个源的**变换矩阵先烘焙进顶点**再拼接，结果 `kind = MERGED`，
`params = {"type":"Merged","source A":...,"source B":...}`；两个源模型保持不变。

```markdown
# MERGE_MODEL(id=10)
Successfully merge "Tower/Base" + "Tower/Upper/Lid" into "Tower/All"
Tetrahedrons: 96
```

#### 4.1.6 应用变换到顶点

**APPLY_TRANSFORM_TO_VERTEX**：把变换矩阵烘焙进顶点，产出一个顶点已在世界坐标的新模型

必须包含的参数：

- name(String)：源路径，必须存在
- dest(String)：目标路径，必须不存在

语义：`p_new = M · p_old`；结果的 `kind` 为 `CARVED`，
`params = {"type":"Transformed","source":...,"transformed matrix":...}`；源模型不变。

```markdown
# APPLY_TRANSFORM_TO_VERTEX(id=11)
Successfully apply transform of "Tower/Base"
Created "Tower/Baked", matrix is identity
Tetrahedrons: 48
```

### 4.2 模型变换

对模型的 5×5 变换矩阵做操作（不改顶点）。统一使用 **TRANSFORM_MODEL**。

必须包含的参数：

- name(String)：目标路径；**可以是分组路径**，此时作用于它自己和全部后代
- transforms(JSONArray)：变换操作列表

可选参数：

- apply(Boolean)：默认 `true`（追加变换）；为 `false` 时**覆盖**模型矩阵
  （即调 `setModelMatrix`：先把矩阵置为单位阵，再依次应用 `transforms`）

每个 transform 必须包含：

- method(String)：变换方式
- data(JSON)：变换数据

```json
{
    "type": "TRANSFORM_MODEL",
    "id": 12,
    "no_result": false,
    "data": {
        "name": "Tower",
        "transforms": [
            { "method": "MATRIX", "data": {
                "row0": [1, 0, 0, 0, 0],
                "row1": [0, 1, 0, 0, 5],
                "row2": [0, 0, 1, 0, 0],
                "row3": [0, 0, 0, 1, -0.2],
                "row4": [0, 0, 0, 0, 1]
            } },
            { "method": "TRANSLATE", "data": { "w": -0.5 } }
        ]
    }
}
```

```markdown
# TRANSFORM_MODEL(id=12)
Applied transform (如果是覆盖而不是追加 用 Setted)
"Tower" current is:
(1 0 0 0 0)
(0 1 0 0 5)
(0 0 1 0 0)
(0 0 0 1 -0.2)
(0 0 0 0 1)
Note: applied to "Tower" and 2 sub models
```

**一次请求里的多条变换只返回最后变换完的矩阵**。

#### 4.2.1 矩阵变换

**MATRIX**：把给定矩阵**左乘**到模型矩阵（`M ← M_arg · M`）

必须包含：`row0` ~ `row4`（Vector5f，5×5 行主序）。

#### 4.2.2 平移

**TRANSLATE**：在四条轴上平移，至少给一个：`x` / `y` / `z` / `w`（Float），未给的轴按 0。

等价矩阵（平移量在第 5 列）：

```
row0 = (1 0 0 0  x)
row1 = (0 1 0 0  y)
row2 = (0 0 1 0  z)
row3 = (0 0 0 1  w)
row4 = (0 0 0 0  1)
```

#### 4.2.3 缩放 / 镜像

**SCALE**：可单独给 `all`(Float)（四轴同倍率），或至少给一个 `x` / `y` / `z` / `w`（Float）。
未给的轴按 1；负值表示镜像（负行列式，面法向翻转）。缩放围绕当前变换坐标系原点。

#### 4.2.4 旋转

**ROTATE**：绕二维平面旋转，必须给：

- axis(Enum)：`xy` | `xz` | `xw` | `yz` | `yw` | `zw`
- angle(Float)：角度制

正方向约定（`Transform4D.rotate`）：平面 `(a, b)` 上 **a 转向 b 为正**

```
a' = a·cos θ − b·sin θ
b' = a·sin θ + b·cos θ      其余坐标不变
```

> 左手系（§0.2），不要用右手定则；摄像机视角的正方向与这里相反（§3.2.2）。
> 例：`xw` 转 90° 把 +X 轴转到 +W 轴。

#### 4.2.5 剪切

**CLIP**：一个轴随另一个轴偏移（切变），必须给：

- source(Enum)：源轴（不改变），`x` | `y` | `z` | `w`
- target(Enum)：目标轴（被改变）
- amount(Float)：偏移量

语义：`p_target ← p_target + amount · p_source`；`source == target` 时是 `p ← (1 + amount)·p`。

```json
{ "method": "CLIP", "data": { "source": "w", "target": "x", "amount": 0.5 } }
```

#### 4.2.6 基坐标系

**COORDINATE**：设置变换的基坐标系（之后同一请求里的变换都围绕它）

至少给一个：`pos`(Vector4f)、`vx`、`vy`、`vz`、`vw`。
未给的字段保持原值（默认是世界坐标系）。

`Transform4D` 内部保存 `T` 与 `T⁻¹`，后续 `TRANSLATE` / `SCALE` / `ROTATE` / `CLIP`
都以该坐标系为基准，同时矩阵仍然是左乘。

```json
{ "method": "COORDINATE", "data": { "pos": [0, 2, 0, 0] } }
```

### 4.3 获取变换矩阵

**GET_MODEL_MATRIX**：获取模型的 5×5 变换矩阵

必须包含的参数：

- name(String)：目标路径，**必须精确存在**（纯分组路径会报"不存在"）

```json
{
    "type": "GET_MODEL_MATRIX",
    "id": 13,
    "no_result": false,
    "data": { "name": "Tower/Base" }
}
```

```markdown
# GET_MODEL_MATRIX(id=13)
"Tower/Base" current is:
row0=(1, 0, 0, 0, 0)
row1=(0, 1, 0, 0, 0)
row2=(0, 0, 1, 0, 0)
row3=(0, 0, 0, 1, 0)
row4=(0, 0, 0, 0, 1)
```

---

## 五、基础形状

对应 `ShapeInterface`。形状操作直接把参数化几何写进一个新节点，无需 AI 自己拼顶点。

### 5.1 通用规则

每个形状操作都要给两个路径参数：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `target` | String | 是 | 目标模型路径（接口的第一个参数），**可带多级路径，不必已存在**；空字符串表示建在根路径 |
| `name` | String | 是 | 新部件的名字（接口的第二个参数），也可带子路径 |

结果路径 = `target` + `/` + `name`，规则：

| 校验 | 规则 |
| --- | --- |
| 结果路径必须不存在 | 否则报 `Model "path" already exists`（避免两个形状挤进同一个 SHAPE 把参数弄脏） |
| `name` 不能为空 | 去掉首尾 `/` 与空白后必须非空 |
| `name` 不能含 `#` | `#` 留给"某个胞"的写法（§6.2） |
| 路径不能有空段 | 不允许出现 `//` |

生成的结果节点：

- `kind = SHAPE`（参数化，几何是 `params` 的纯函数）
- `params` 记录生成参数，可用 `GET_MODEL_INFO` 读回
- 颜色由实现自动分配（按胞的色相调色板），**形状接口没有颜色参数**；
  要指定顶点颜色只能用 §6.3 / §6.5 逐顶点/逐胞设置（代价是降级为 `CARVED`）

所有参数校验失败都会报 `IllegalArgumentException`，消息形如
`radius must be > 0, got -1.0`、`Prism w range must not be empty, got ws = we = 1.0`。

### 5.2 七种四维形状

| 操作 | 必填参数（除 target / name） | 胞数（四面体数） | 默认颜色 |
| --- | --- | --- | --- |
| `CREATE_TETRAHEDRON` | `center`, `radius` | 1 | 单色（HSV 200°） |
| `CREATE_5CELL` | `center`, `size`（棱长） | 5 | 5 个胞均分色相 |
| `CREATE_16CELL` | `center`, `radius` | 16 | 16 个胞均分色相 |
| `CREATE_TESSERACT` | `center`, `edgeLength` | 48（8 胞 × 6） | 8 胞固定调色板 |
| `CREATE_PRISM4` | `base`, `ws`, `we` | 3 × base 三角形数 | 用 base 顶点色 |
| `CREATE_CONE4` | `base`, `apex` | base 三角形数 | base 顶点色 + apex 白色 |
| `CREATE_BALL4` | `center`, `radius`, `density` | 8 · k³ · 6 | 8 个胞均分色相 |

其中 `CREATE_BALL4` 的 `k = clamp(round(density), 1, 12)`：`density = 1` 时 48 个胞，
`density = 2` 时 384 个，`density = 3` 时 1296 个。

---

#### 5.2.1 正四面超平面

**CREATE_TETRAHEDRON**：四个顶点在四条坐标轴上距中心 `radius` 的位置

必须包含：`target`, `name`, `center`(Vector4f), `radius`(Float, `> 0`)

四个顶点为 `center + (radius,0,0,0)`、`center + (0,radius,0,0)`、
`center + (0,0,radius,0)`、`center + (0,0,0,radius)`；法向量 = 该超平面的法向量。
它落在超平面 `x+y+z+w = radius` 上，四维体积为 0，是一个**三维片元**（1 个胞）。

```json
{
    "type": "CREATE_TETRAHEDRON",
    "id": 14,
    "no_result": false,
    "data": {
        "target": "Gallery",
        "name": "Tet",
        "center": [0, 0, 0, 0],
        "radius": 1.0
    }
}
```

```markdown
# CREATE_TETRAHEDRON(id=14)
Successfully create "Gallery/Tet" (TETRAHEDRON)
Center=(0, 0, 0, 0), radius=1.0
Tetrahedrons: 1
```

#### 5.2.2 四维单纯形（五胞体）

**CREATE_5CELL**：`target`, `name`, `center`, `size`（棱长，`> 0`）

标准 4-单纯形缩放到给出的**棱长**，5 个胞，每胞"去掉一个顶点"，法向沿"顶点 − 重心"朝外。

```markdown
# CREATE_5CELL(id=15)
Successfully create "Gallery/Five" (5CELL)
Center=(0, 0, 0, 0), size=2.0
Tetrahedrons: 5
```

#### 5.2.3 超八面体

**CREATE_16CELL**：`target`, `name`, `center`, `radius`（`> 0`）

8 个顶点在四条轴的 `±radius` 处，16 个胞，每胞从四条轴各取一个顶点，法向 = 符号组合方向。

```markdown
# CREATE_16CELL(id=16)
Successfully create "Gallery/Sixteen" (16CELL)
Center=(0, 0, 0, 0), radius=1.5
Tetrahedrons: 16
```

#### 5.2.4 超立方体

**CREATE_TESSERACT**：`target`, `name`, `center`, `edgeLength`（`> 0`）

16 个顶点、8 个立方体胞，每胞 Kuhn 分解为 6 个四面体 = **48 个胞**。
8 个胞使用固定调色板，顺序为 `+X, -X, +Y, -Y, +Z, -Z, +W, -W`。

```json
{
    "type": "CREATE_TESSERACT",
    "id": 17,
    "no_result": false,
    "data": {
        "target": "Tower",
        "name": "Base",
        "center": [0, 0, 0, 0],
        "edgeLength": 2.0
    }
}
```

```markdown
# CREATE_TESSERACT(id=17)
Successfully create "Tower/Base" (TESSERACT)
Center=(0, 0, 0, 0), edgeLength=2.0
Tetrahedrons: 48
```

#### 5.2.5 超棱柱

**CREATE_PRISM4**：以 3D 网格为底面，沿 w 轴从 `ws` 拉伸到 `we`

必须包含：`target`, `name`, `base`(Mesh, 见 §5.3), `ws`(Float), `we`(Float)

语义：

- `base` 的 3D 点 `(x, y, z)` 提升为四维点 `(x, y, z, ws)` 与 `(x, y, z, we)`；
- base 的每个三角形沿 w 拉出一个三棱柱胞，拆成 **3 个四面体**；
- **不额外生成两端盖面**：侧面胞自身就带着位于 `w = ws` / `w = we` 上的端面；
- 退化三角形（有两点重合）会被跳过；
- 要求 base 至少有一个三角形；`ws == we` 报错，`ws > we` 自动交换。

> 建议 `base` 是**封闭**曲面（球 / 正方体 / 棱柱 / 棱锥 / 圆锥默认封闭），否则结果不封闭。

```json
{
    "type": "CREATE_PRISM4",
    "id": 18,
    "no_result": false,
    "data": {
        "target": "Tower",
        "name": "Body",
        "base": {
            "shape": "PRISM",
            "sides": 6,
            "radius": 1.0,
            "height": 2.0,
            "color": "#FF66CCFF",
            "transforms": [
                { "method": "ROTATE", "data": { "axis": "xz", "angle": 30 } }
            ]
        },
        "ws": -1.5,
        "we": 1.5
    }
}
```

```markdown
# CREATE_PRISM4(id=18)
Successfully create "Tower/Body" (PRISM4)
Base: PRISM sides=6 radius=1.0 height=2.0 (20 triangles), transforms=[ROTATE]
w range=(-1.5, 1.5)
Tetrahedrons: 60
```

#### 5.2.6 超锥

**CREATE_CONE4**：以 3D 网格为底面（提升到 `w = 0`），以四维点 `apex` 为顶点

必须包含：`target`, `name`, `base`(Mesh), `apex`(Vector4f)

语义：base 的每个三角形与 `apex` 组成**一个四面体**（胞数 = base 三角形数）；
法向由四维广义叉积求出后统一朝外；apex 用白色，其余顶点用 base 的顶点色。

> 建议 `apex.w != 0`，否则整个超锥退化在 `w = 0` 超平面内（仍合法，但没有四维体积）。

```markdown
# CREATE_CONE4(id=19)
Successfully create "Tower/Tip" (CONE4)
Base: CUBE edge=1.0 (12 triangles), transforms=[]
Apex=(0, 0, 0, 2.5)
Tetrahedrons: 12
```

#### 5.2.7 超球

**CREATE_BALL4**：创建四维球的**三维边界**（3-球面）

必须包含：`target`, `name`, `center`(Vector4f), `radius`(Float, `> 0`), `density`(Float, `> 0`)

做法：把超立方体的 8 个胞各细分成 `k×k×k` 个小立方体（每个再拆 6 个四面体），
把所有顶点沿径向投影到半径 `radius` 的球面上（cubed sphere），相邻胞共享边界，结果封闭无缝。
`k = clamp(round(density), 1, 12)`，胞数 = `8 · k³ · 6`。

```json
{
    "type": "CREATE_BALL4",
    "id": 20,
    "no_result": false,
    "data": {
        "target": "Tower",
        "name": "Top",
        "center": [0, 0, 0, 2.2],
        "radius": 0.9,
        "density": 2.0
    }
}
```

```markdown
# CREATE_BALL4(id=20)
Successfully create "Tower/Top" (BALL4)
Center=(0, 0, 0, 2.2), radius=0.9, density=2.0
Tetrahedrons: 384
```

### 5.3 3D 基本几何体（Mesh 的 JSON 定义）

`CREATE_PRISM4` 与 `CREATE_CONE4` 需要一个 3D 底面网格（`base: Mesh`）。
AI 不写三角形，而是用 **JSON 描述一个基本几何体 + 一串 3D 变换**，由接入层生成三角形网格
（渲染包里现成的 3D 生成器只有 `ogl3d/generator/Cube.kt` 的 `createCube`；
球 / 棱柱 / 棱锥 / 圆锥按下面的规范生成即可）。

当前支持这几种几何体：

| `shape` | 中文名 | 必填参数 | 可选参数（默认值） |
| --- | --- | --- | --- |
| `SPHERE` | 球 | `radius` | `segments`(16)、`rings`(8) |
| `CUBE` | 正方体 | `edge` | — |
| `PRISM` | 正 n 棱柱 | `sides` | `radius`(1.0)、`height`(1.0) |
| `PYRAMID` | 正 n 棱锥 | `sides` | `radius`(1.0)、`height`(1.0) |
| `CONE` | 圆锥 | `radius` | `height`(1.0)、`segments`(16) |

#### 5.3.1 结构

```json
{
    "shape": "CUBE",
    "edge": 1.0,
    "color": "#FF66CCFF",
    "transforms": [
        { "method": "TRANSLATE", "data": { "y": 0.5 } },
        { "method": "ROTATE", "data": { "axis": "xz", "angle": 30 } }
    ]
}
```

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `shape` | Enum | 是 | `SPHERE` / `CUBE` / `PRISM` / `PYRAMID` / `CONE` |
| 几何参数 | Float / Int | 见上表 | 与 `shape` 对应 |
| `color` | Color | 否 | 网格统一顶点色，默认 `#FFB0B0B0`。`base` 是接入层自己构造的 3D `Mesh`，顶点本来就带 `ColorARGB` |
| `transforms` | Array | 否 | 3D 变换列表，按顺序应用，见 §5.3.3 |

#### 5.3.2 各几何体的朝向约定

| `shape` | 位置与朝向（未施加变换时） |
| --- | --- |
| `SPHERE` | 球心在原点；经纬网格三角化，`segments` 为经线分段数（≥3），`rings` 为纬线分段数（≥2） |
| `CUBE` | 中心在原点，棱长 `edge`，棱平行于三条坐标轴 |
| `PRISM` | 轴线沿 **+Y**，Y 方向居中（`-height/2` ~ `+height/2`）；底面为正 `sides` 边形，外接圆半径 `radius`，一个顶点在 +X 方向 |
| `PYRAMID` | 轴线沿 **+Y**；底面在 `y = -height/2`，顶点在 `y = +height/2`；底面同上 |
| `CONE` | 轴线沿 **+Y**；底面圆在 `y = -height/2`，顶点在 `y = +height/2`；`segments` 为圆周分段数（≥3） |

需要让轴线朝别的方向时，用 `transforms` 里的 `ROTATE` 旋转。

#### 5.3.3 3D 变换（transforms）

按数组顺序依次应用（后一个作用在前一个的结果上）；旋转、缩放都以**原点**为中心。

| method | 参数 | 说明 |
| --- | --- | --- |
| `TRANSLATE` | `x` `y` `z`(Float，至少一个，缺省 0) | `p ← p + t` |
| `SCALE` | `all`(Float) 或 `x` `y` `z`(Float，缺省 1) | 负值镜像；`p ← p * s` |
| `ROTATE` | `axis`(`xy`/`xz`/`yz`) + `angle`(Float，角度制) | 与 §4.2.4 同一约定：第一轴转向第二轴为正 |
| `MATRIX` | `row0`~`row3`(Vector4f) | 4×4 行主序矩阵左乘 |

`ROTATE` 的公式（其余坐标不变）：

| axis | 变换式 |
| --- | --- |
| `xy` | `x' = x·cos a − y·sin a`，`y' = x·sin a + y·cos a` |
| `yz` | `y' = y·cos a − z·sin a`，`z' = y·sin a + z·cos a` |
| `xz` | `x' = x·cos a − z·sin a`，`z' = x·sin a + z·cos a` |

**绕非原点位置旋转**：先移到原点、旋转、再移回去。

```json
{
    "shape": "PRISM",
    "sides": 4,
    "radius": 1.0,
    "height": 1.0,
    "transforms": [
        { "method": "TRANSLATE", "data": { "x": -2.0 } },
        { "method": "ROTATE", "data": { "axis": "yz", "angle": 45 } },
        { "method": "TRANSLATE", "data": { "x": 2.0 } }
    ]
}
```

#### 5.3.4 校验规则

| 校验项 | 规则 |
| --- | --- |
| `shape` | 必须是上表五个值之一 |
| `radius` / `edge` / `height` | `> 0` |
| `sides` / `segments` | `>= 3` 的整数 |
| `rings` | `>= 2` 的整数 |
| `transforms[].method` | `TRANSLATE` / `SCALE` / `ROTATE` / `MATRIX` |
| `ROTATE.axis` | 3D 只接受 `xy` / `xz` / `yz`（传 `xw` 等四维平面报错） |
| 三角面总数 | 必须 `>= 1`（否则报 `Prism base mesh has no triangle` / `Cone base mesh has no triangle`） |

### 5.4 3D → 4D 的嵌入规则

1. 按 `shape` 生成 3D 三角形网格；
2. 依次应用 `transforms` 里的 3D 变换，得到最终 3D 坐标 `(x, y, z)`；
3. 提升为四维点 `(x, y, z, 0)`，顶点色与法向量一并带入（法向的 `z` 分量进 `w` 位）；
4. 由具体操作决定第四维的位置：
   - `CREATE_PRISM4`：复制到 `w = ws` 与 `w = we` 两个超平面并拉伸；
   - `CREATE_CONE4`：底面固定在 `w = 0` 超平面，与 `apex` 相连。

> 也就是说，`base` 里**不要**写 w 坐标；第四维位置由 `ws` / `we` / `apex` 控制。

---

## 六、模型几何编辑

对应 `GeometryInterface`。这是**雕刻级**接口，会直接改顶点/四面体，
并把目标节点从 `SHAPE` 降级为 `CARVED`（§6.8）。AI 的主路径应该是第五章的形状操作。

> **寻址限制**：本类操作只接受**没有子节点的真实模型**（`requireGeometry`）。
> 传分组路径会报 `"Tower" is a group path and holds no geometry, pick one of: ...`；
> 传带子节点的模型会报 `"Tower" is a group of N sub models and cannot hold geometry, pick one of: ...`。

### 6.1 获取模型信息

**GET_MODEL_INFO**：`name`(String)。返回的正文由渲染实现直接生成（markdown）。

**真实模型**（有子节点时会列出直接子节点）：

````markdown
# GET_MODEL_INFO(id=21)
## "Tower/Base"
Kind: SHAPE
Params: {"type":"Tesseract","center":"(0, 0, 0, 0)","edge length":"2.0","cells":"8"}
Tetrahedrons: 48
Bound: x[-1.00, 1.00] y[-1.00, 1.00] z[-1.00, 1.00] w[-1.00, 1.00]
Vertex list omitted, use GET_TETRAHEDRON with index in [0, 48)
Transform matrix:
```text
1.00, 0.00, 0.00, 0.00, 0.00
0.00, 1.00, 0.00, 0.00, 0.00
0.00, 0.00, 1.00, 0.00, 0.00
0.00, 0.00, 0.00, 1.00, 0.00
0.00, 0.00, 0.00, 0.00, 1.00
```
Sub models:
- Tower/Upper  GROUP  tets=0
````

字段说明：

| 字段 | 含义 |
| --- | --- |
| `Kind` | `GROUP` / `SHAPE` / `CARVED` / `MERGED` |
| `Params` | 参数化描述（`GROUP` 且没被雕刻过时可能没有这一行） |
| `Tetrahedrons` | 该节点自己的四面体数量 |
| `Bound` | 顶点经**变换矩阵**后的世界坐标包围盒；没有几何时为 `empty` |
| `Vertex list omitted...` | 不 dump 顶点，要用 §6.2 按下标查 |
| `Transform matrix` | 5 行，`%.2f`，包在 ` ```text ` 代码块里 |
| `Sub models` | 直接子节点（路径 / kind / 各自胞数） |

**纯分组路径**（只有子节点，自己没有实体）：

```markdown
# GET_MODEL_INFO(id=22)
## "Deep/Mid" (group path, no geometry itself)
Sub models: 1
- Deep/Mid/Leaf  SHAPE  tets=48
Total tetrahedrons: 48
```

路径不存在时报错并带候选（§0.3）。

### 6.2 获取单个四面体

**GET_TETRAHEDRON**：`name`(String)、`tet`(Int，该模型内的下标，从 0 开始)

```markdown
# GET_TETRAHEDRON(id=23)
## Tower/Base#0
v0: pos=(1.00, 1.00, 1.00, 1.00) color=#FFFF6666 normal=(1.00, 0.00, 0.00, 0.00)
v1: pos=(-1.00, 1.00, 1.00, 1.00) color=#FFFF6666 normal=(1.00, 0.00, 0.00, 0.00)
v2: pos=(1.00, -1.00, 1.00, 1.00) color=#FFFF6666 normal=(1.00, 0.00, 0.00, 0.00)
v3: pos=(1.00, 1.00, -1.00, 1.00) color=#FFFF6666 normal=(1.00, 0.00, 0.00, 0.00)
```

坐标为**局部坐标**（未乘模型矩阵）；`color` 是 `#AARRGGBB`。
下标越界报 `Tetrahedron index N is out of range [0, M) in "path"`。

### 6.3 设置单个顶点

**SET_TETRAHEDRON_VERTEX**：修改一个四面体的一个顶点

必须包含：

- name(String)：目标模型路径
- tet(Int)：四面体下标
- vertex(Int)：顶点编号，`0` ~ `3`
- pos(Vector4f)：新的四维位置

可选：

- color(Color)：新颜色，缺省保持原值
- normal(Vector4f)：新法向量，缺省保持原值

```json
{
    "type": "SET_TETRAHEDRON_VERTEX",
    "id": 24,
    "no_result": false,
    "data": {
        "name": "Tower/Base",
        "tet": 0,
        "vertex": 2,
        "pos": [0, 0, 0, 3],
        "color": "#FFFFFF00"
    }
}
```

```markdown
# SET_TETRAHEDRON_VERTEX(id=24)
Success
"Tower/Base" #0 v2 = pos(0.00, 0.00, 0.00, 3.00) color=#FFFFFF00
Now kind is CARVED (params no longer describe it)
```

### 6.4 用矩阵变换指定四面体

**TRANSFORM_TETRAHEDRONS**：对指定四面体应用 5×5 变换矩阵（**直接改顶点**，模型矩阵不变）

必须包含：

- name(String)：目标模型路径
- tets(Array)：目标四面体下标数组；**空数组 `[]` 表示该模型内全部四面体**
- row0 ~ row4(Vector5f)：5×5 行主序矩阵

```json
{
    "type": "TRANSFORM_TETRAHEDRONS",
    "id": 25,
    "no_result": false,
    "data": {
        "name": "Tower/Lid",
        "tets": [0, 2],
        "row0": [1, 0, 0, 0, 0.5],
        "row1": [0, 1, 0, 0, 0],
        "row2": [0, 0, 1, 0, 0],
        "row3": [0, 0, 0, 1, 0],
        "row4": [0, 0, 0, 0, 1]
    }
}
```

```markdown
# TRANSFORM_TETRAHEDRONS(id=25)
Success
"Tower/Lid" transformed 2 tetrahedrons: [0, 2]
Now kind is CARVED
```

### 6.5 添加四面体

**ADD_TETRAHEDRON**：直接指定四个顶点，往模型里追加一个四面体

必须包含：

- name(String)：目标模型路径
- v0 ~ v3(Vertex)：四个顶点

Vertex 结构：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `pos` | Vector4f | 是 | 位置 |
| `color` | Color | 否 | 缺省用 `#FFB0B0B0`；也可用操作级的统一字段 `color` 一次指定四个顶点 |
| `normal` | Vector4f | 否 | 缺省由接入层给定 |

```json
{
    "type": "ADD_TETRAHEDRON",
    "id": 26,
    "no_result": false,
    "data": {
        "name": "Sculpt/Patch",
        "color": "#FFFFD933",
        "v0": { "pos": [-4, -7, 0, 0] },
        "v1": { "pos": [-2.6, -7, 0, 0] },
        "v2": { "pos": [-3.4, -6.1, 0, 0] },
        "v3": { "pos": [-3.4, -7, 1.2, 0.8], "color": "#FFFF4444" }
    }
}
```

```markdown
# ADD_TETRAHEDRON(id=26)
Successfully add tetrahedron #0 to "Sculpt/Patch"
Tetrahedrons: 1
Now kind is CARVED
```

> 往一个空模型（`GROUP`）里加胞是合法的，它会变成 `CARVED`。

### 6.6 删除四面体

**REMOVE_TETRAHEDRON**：`name`(String)、`tet`(Int)

**下标语义**：四面体下标是模型内索引（从 0 开始，按加入顺序）；删除后其后的下标**顺移**，
所以删除之后请重新查询，不要沿用旧下标。只影响该节点，不影响其他部件。

```markdown
# REMOVE_TETRAHEDRON(id=27)
Successfully remove tetrahedron #0 from "Sculpt/Patch"
Tetrahedrons: 0
Now kind is CARVED
```

### 6.7 超平面切片

**SLICE_MODEL**：用一个超平面把模型**精确**切成两半，产出两个新模型

必须包含：

- name(String)：源模型路径（必须是无子节点的真实模型）
- plane(Tetrahedron)：超平面，由**四个仿射无关的顶点**定义，坐标取**源模型的局部坐标系**
- pathA(String)：`f(p) < 0` 那一半的路径，必须不存在
- pathB(String)：`f(p) >= 0` 那一半的路径，必须不存在且与 `pathA` 不同

`plane` 就是 §1.2 的 `Tetrahedron`，四个顶点各给一个 `pos` 即可（颜色/法向量对切片无意义）：

| 字段 | 类型 | 必填 |
| --- | --- | --- |
| `v0` ~ `v3` | Vertex（只需 `pos`） | 是 |

```json
{
    "type": "SLICE_MODEL",
    "id": 28,
    "no_result": false,
    "data": {
        "name": "Sculpt/Box",
        "plane": {
            "v0": { "pos": [0, -7, 0, 0] },
            "v1": { "pos": [0, -6, 0, 0] },
            "v2": { "pos": [0, -7, 1, 0] },
            "v3": { "pos": [0, -7, 0, 1] }
        },
        "pathA": "Sculpt/Left",
        "pathB": "Sculpt/Right"
    }
}
```

```markdown
# SLICE_MODEL(id=28)
Successfully slice "Sculpt/Box"
Created "Sculpt/Left" (42 tetrahedrons, f < 0)
Created "Sculpt/Right" (6 tetrahedrons, f >= 0)
Source "Sculpt/Box" is unchanged
```

语义与边界情况：

- 超平面法向由四维广义叉积求出，`f(p) = n·p − d`；`f < 0` 归 `pathA`，`f >= 0` 归 `pathB`
  （落在平面上的顶点按"非负"一侧参与分类）；
- **精确切割**：被跨越的四面体按 `1:3` 或 `2:2` 切开，三棱柱部分再拆成 3 个四面体，
  重复顶点造成的退化子胞直接丢弃 —— 两侧体积之和与源模型一致（体积守恒）；
- 新顶点用线性插值，颜色与法向量一并插值（法向重新归一化）；
- 两个产物都是 `CARVED`，`params = {"type":"Sliced","source":...,"side":"A/B","normal":...,"plane":...}`；
- 源模型保持不变。

### 6.8 雕刻降级（CARVED）

只要发生了顶点/四面体级编辑，目标节点就会 `markCarved`：
`kind` 变成 `CARVED`，`params` 被改写成

```json
{
    "type": "Carved",
    "reason": "vertex edited",
    "from": { "type": "Tesseract", "center": "(0, 0, 0, 0)", "edge length": "2.0", "cells": "8" }
}
```

`from` 里放的是降级前的那份 `params`（原来没有就放空对象 `{}`）。

`reason` 取值与触发操作：

| reason | 触发 |
| --- | --- |
| `vertex edited` | `SET_TETRAHEDRON_VERTEX` |
| `tetrahedrons transformed` | `TRANSFORM_TETRAHEDRONS` |
| `tetrahedron appended` | `ADD_TETRAHEDRON` |
| `tetrahedron removed` | `REMOVE_TETRAHEDRON` |

已经 `CARVED` 的节点不会被重复改写。想恢复"参数化"只能重新用 `CREATE_*` 建一个。

---

## 七、文件与模型 IO

对应 `IOInterface`。**当前未实现**（两个方法都还是 `TODO`），这里只固定接口契约。

### 7.1 保存模型

**SAVE_MODEL**：把模型存成 `.4do` 文件

必须包含的参数：

- name(String)：模型路径，必须存在
- file(String)：**不包含后缀**的文件名，用相对路径（接入层补 `.4do`）

```markdown
# SAVE_MODEL(id=29)
Successfully save "Tower/Base" to models/base.4do
Tetrahedrons: 48
```

### 7.2 加载模型

**LOAD_MODEL**：从 `.4do` 文件加载模型

必须包含的参数：

- name(String)：加载后的模型路径，必须不存在（避免覆盖）
- file(String)：文件路径，**带后缀**

```markdown
# LOAD_MODEL(id=30)
Successfully load "Imported" from models/ball.4do
Tetrahedrons: 96
```

> 安全约定：AI 不应凭空编造绝对路径；路径应由使用者在对话里给出，
> 并由接入层限定在工作目录 / 模型库内。

---

## 八、场景与渲染设置

对应 `SceneInterface`，只看帧率、改显示与光照，**不改任何模型数据**。
这些操作都没有目标路径参数。

### 8.1 设置 3D 最大帧率

**SET_3D_MAX_FPS**：`fps`(Float, `> 0`)

```json
{ "type": "SET_3D_MAX_FPS", "id": 31, "no_result": false, "data": { "fps": 60.0 } }
```

```markdown
# SET_3D_MAX_FPS(id=31)
Success
3D max FPS = 60.0
```

### 8.2 获取 3D 帧率

**GET_3D_FPS**：无参数。返回最近约 1 秒的平均帧率。

```markdown
# GET_3D_FPS(id=32)
3D FPS=59.87
```

### 8.3 获取 3D 1% Low 帧率

**GET_3D_1PERCENT_LOW_FPS**：无参数。返回最近约 1 秒内**最慢的 1% 帧**的平均帧率，用于判断卡顿。

```markdown
# GET_3D_1PERCENT_LOW_FPS(id=33)
3D 1% Low FPS=41.20
```

### 8.4 设置 4D 最大帧率

**SET_4D_MAX_FPS**：`fps`(Float, `> 0`)。4D 渲染器是独立线程（默认上限 100）。

```markdown
# SET_4D_MAX_FPS(id=34)
Success
4D max FPS = 30.0
```

### 8.5 获取 4D 帧率

**GET_4D_FPS**：无参数。

```markdown
# GET_4D_FPS(id=35)
4D FPS=30.02
```

### 8.6 获取 4D 1% Low 帧率

**GET_4D_1PERCENT_LOW_FPS**：无参数。

```markdown
# GET_4D_1PERCENT_LOW_FPS(id=36)
4D 1% Low FPS=27.44
```

### 8.7 仅线框渲染

**SET_TRIANGLE_LINE_RENDERING**：`enable`(Boolean)

`true` 只用线框画（4D 投影出来的）三角形，`false` 恢复实体填充。
只影响 `D4` 来源的网格，3D 场景自带的网格始终实体渲染。

```json
{ "type": "SET_TRIANGLE_LINE_RENDERING", "id": 37, "no_result": false, "data": { "enable": true } }
```

```markdown
# SET_TRIANGLE_LINE_RENDERING(id=37)
Success
Triangle line rendering: ON
```

### 8.8 设置背景色

**SET_BACKGROUND_COLOR**：`color`

颜色写法：`"#RRGGBB"` / `"#RRGGBBAA"` 字符串（优先），或 `[r, g, b]` / `[r, g, b, a]` 浮点数组（0~1）。

```json
{ "type": "SET_BACKGROUND_COLOR", "id": 38, "no_result": false, "data": { "color": "#FF2A2A30" } }
```

```markdown
# SET_BACKGROUND_COLOR(id=38)
Success
Background color = #FF2A2A30
```

### 8.9 光照渲染

**SET_LIGHT_RENDERING**：`enable`(Boolean)

`true` 开启光照（光源 + 环境光参与着色），`false` 关闭（用纯顶点色/材质色显示）。

```markdown
# SET_LIGHT_RENDERING(id=39)
Success
Light rendering: OFF
```

### 8.10 列出光源

**LIST_LIGHTS**：无参数。返回场景里所有光源（3D 场景默认有一盏点光源 `Light`）。

```markdown
# LIST_LIGHTS(id=40)
## Lights in 3D Scene
- Light Light{pos=(0.00, 50.00, 0.00), color=#FFFFFFFF, direction=None, intensity=0.60, range=600.00, att.A=0.00, att.B=0.00)}
- KeyLight Light{pos=(0.00, 40.00, 0.00), color=#FFFFFFFF, direction=None, intensity=0.80, range=600.00, att.A=0.00, att.B=0.00)}
- RimLight Light{pos=(10.00, 5.00, -10.00), color=#FF8080FF, direction=(0.00, -1.00, 0.00), intensity=1.20, range=200.00, att.A=0.00, att.B=0.00)}
```

每行的 `direction=None` 表示点光源，有方向表示面光源。**光源名字在最前面**，删除时按名字删。

### 8.11 添加光源

**ADD_LIGHT**：往场景里加一盏光源（每次一盏）

`light` 对象字段：

| 字段 | 类型 | 必填 | 默认 | 说明 |
| --- | --- | --- | --- | --- |
| `name` | String | 否 | `"Light"` | 光源名，删除时用它 |
| `pos` | Vector3f | 否 | `(0, 50, 0)` | 世界空间位置 |
| `color` | Color | 否 | `#FFFFFFFF` | 光色 |
| `direction` | Vector3f \| null | 否 | `null` | `null` = 点光源；给了方向 = 面光源，只照朝向的**反方向**一侧 |
| `intensity` | Float | 否 | `0.6` | 强度，乘在颜色上 |
| `range` | Float | 否 | `600` | 有效距离，超过则不参与照明 |
| `attenuationA` | Float | 否 | `0.00007` | 距离衰减系数 `1 / (1 + a·d + b·d²)` |
| `attenuationB` | Float | 否 | `0.00003` | 同上 |

```json
{
    "type": "ADD_LIGHT",
    "id": 41,
    "no_result": false,
    "data": {
        "light": {
            "name": "KeyLight",
            "pos": [0, 40, 0],
            "color": "#FFFFFFFF",
            "intensity": 0.8,
            "range": 600
        }
    }
}
```

面光源（朝下照）：

```json
{
    "type": "ADD_LIGHT",
    "id": 42,
    "no_result": false,
    "data": {
        "light": {
            "name": "RimLight",
            "pos": [10, 5, -10],
            "color": "#FF8080FF",
            "direction": [0, -1, 0],
            "intensity": 1.2,
            "range": 200
        }
    }
}
```

```markdown
# ADD_LIGHT(id=41)
Success
Now 2 lights: Light, KeyLight
```

### 8.12 删除光源

**REMOVE_LIGHTS**：`names`(Array of String)，一次可以删多个；名字不存在会报错。

```json
{ "type": "REMOVE_LIGHTS", "id": 43, "no_result": false, "data": { "names": ["RimLight"] } }
```

```markdown
# REMOVE_LIGHTS(id=43)
Success
Removed: RimLight
Now 2 lights: Light, KeyLight
```

```markdown
# REMOVE_LIGHTS(id=44)
Error: There is no lights named [NoSuchLight]
```

### 8.13 设置环境光

**SET_AMBIENT_LIGHT**：`color`(Color)。环境光是**整个场景一份**（不是单个光源的属性），
默认 `#FF4D4D4D`（0.3, 0.3, 0.3, 1）。

```json
{ "type": "SET_AMBIENT_LIGHT", "id": 45, "no_result": false, "data": { "color": "#FF595959" } }
```

```markdown
# SET_AMBIENT_LIGHT(id=45)
Success
Ambient light = #FF595959
```

### 8.14 设置视口尺寸

**SET_DISPLAY_SIZE**：`edgeLength`(Float, `> 0`)

设置四维超平面屏幕的边长（`Renderer4D.viewPortLength`，默认 `8`）。
它决定四维 → 三维投影的缩放：3D 侧的缩放倍率是 `edgeLength * 0.5`，
所以**数值越大，画面里的模型越大**。

```json
{ "type": "SET_DISPLAY_SIZE", "id": 46, "no_result": false, "data": { "edgeLength": 16.0 } }
```

```markdown
# SET_DISPLAY_SIZE(id=46)
Success
Display size = 16.0
```

---

## 九、完整交互示例

### 9.1 用户需求

> 建一个叫 Tower 的模型，主体是一个六棱柱拉伸出来的超棱柱，顶上放一个超球，
> 整体绕 xw 平面转 20 度，然后告诉我它现在长什么样。

### 9.2 AI 返回的 json

```json
{
    "version": 1,
    "done": false,
    "operations": [
        {
            "type": "CREATE_MODEL",
            "id": 1,
            "no_result": true,
            "data": { "name": "Tower" }
        },
        {
            "type": "CREATE_PRISM4",
            "id": 2,
            "no_result": true,
            "data": {
                "target": "Tower",
                "name": "Body",
                "base": {
                    "shape": "PRISM",
                    "sides": 6,
                    "radius": 1.0,
                    "height": 2.0,
                    "color": "#FF66CCFF"
                },
                "ws": -1.5,
                "we": 1.5
            }
        },
        {
            "type": "CREATE_BALL4",
            "id": 3,
            "no_result": true,
            "data": {
                "target": "Tower",
                "name": "Top",
                "center": [0, 0, 0, 2.2],
                "radius": 0.9,
                "density": 2.0
            }
        },
        {
            "type": "TRANSFORM_MODEL",
            "id": 4,
            "no_result": false,
            "data": {
                "name": "Tower",
                "transforms": [
                    { "method": "ROTATE", "data": { "axis": "xw", "angle": 20 } }
                ]
            }
        },
        {
            "type": "GET_MODEL_INFO",
            "id": 5,
            "no_result": false,
            "data": { "name": "Tower" }
        },
        {
            "type": "LIST_MODEL",
            "id": 6,
            "no_result": false
        }
    ]
}
```

### 9.3 返回给 AI 的 markdown

> 数量为示例数值，实际以实现输出为准。

````markdown
# TRANSFORM_MODEL(id=4)
Applied transform
"Tower" current is:
(0.94 0 0 -0.34 0)
(0 1 0 0 0)
(0 0 1 0 0)
(0.34 0 0 0.94 0)
(0 0 0 0 1)
Note: applied to "Tower" and 2 sub models

# GET_MODEL_INFO(id=5)
## "Tower"
Kind: GROUP
Tetrahedrons: 0
Bound: empty
Transform matrix:
```text
0.94, 0.00, 0.00, -0.34, 0.00
0.00, 1.00, 0.00, 0.00, 0.00
0.00, 0.00, 1.00, 0.00, 0.00
0.34, 0.00, 0.00, 0.94, 0.00
0.00, 0.00, 0.00, 0.00, 1.00
```
Sub models:
- Tower/Body  SHAPE  tets=60
- Tower/Top  SHAPE  tets=384

# LIST_MODEL(id=6)
0. "Tower"
````

（`id=1`、`id=2`、`id=3` 都是 `no_result: true`，所以没有返回段落。）

### 9.4 继续雕刻（另一个请求）

把主体按 `x = 0` 切开，得到左右两半，再把它们左右拉开：

```json
{
    "version": 1,
    "done": false,
    "operations": [
        {
            "type": "SLICE_MODEL",
            "id": 1,
            "no_result": false,
            "data": {
                "name": "Tower/Body",
                "plane": {
                    "v0": { "pos": [0, 0, 0, 0] },
                    "v1": { "pos": [0, 1, 0, 0] },
                    "v2": { "pos": [0, 0, 1, 0] },
                    "v3": { "pos": [0, 0, 0, 1] }
                },
                "pathA": "Tower/Left",
                "pathB": "Tower/Right"
            }
        },
        {
            "type": "TRANSFORM_MODEL",
            "id": 2,
            "no_result": false,
            "data": {
                "name": "Tower/Left",
                "transforms": [
                    { "method": "TRANSLATE", "data": { "x": -2.5 } }
                ]
            }
        },
        {
            "type": "TRANSFORM_MODEL",
            "id": 3,
            "no_result": false,
            "data": {
                "name": "Tower/Right",
                "transforms": [
                    { "method": "TRANSLATE", "data": { "x": 2.5 } }
                ]
            }
        }
    ]
}
```

### 9.5 结束本轮

```json
{
    "version": 1,
    "done": true,
    "operations": []
}
```

---

## 十、附录

### 附录 A：操作名总表

| 操作名 | data 必填参数 | 可选参数 | 返回要点 |
| --- | --- | --- | --- |
| `GET_CAMERA_POS` | 无 | — | `Pos=(x, y, z, w)` |
| `MOVE_CAMERA_POS` | `axis`, `distance` | — | `Now Pos=...` |
| `SET_CAMERA_POS` | `pos` | — | `Now Pos=...` |
| `GET_CAMERA_VIEW` | 无 | — | `vx/vy/vz/vw` |
| `ROTATE_CAMERA_VIEW` | `axis`, `angle` | — | 旋转后的四个基向量 |
| `SET_CAMERA_VIEW` | `vx`, `vy`, `vz`, `vw` | — | 四个基向量 |
| `LIST_MODEL` | 无 | — | 根路径列表（带下标） |
| `CREATE_MODEL` | `name` | — | 新模型路径、模型总数 |
| `DELETE_MODEL` | `name` | — | 删除的子树节点数、剩余模型数 |
| `COPY_MODEL` | `src`, `dst` | — | 四面体数 |
| `MERGE_MODEL` | `src1`, `src2`, `dst` | — | 四面体数 |
| `APPLY_TRANSFORM_TO_VERTEX` | `name`, `dest` | — | 新路径、四面体数 |
| `GET_MODEL_MATRIX` | `name` | — | 5 行矩阵 |
| `TRANSFORM_MODEL` | `name`, `transforms` | `apply` | 变换后的模型矩阵（分组路径会注明作用了几个子模型） |
| `CREATE_TETRAHEDRON` | `target`, `name`, `center`, `radius` | — | 新路径、胞数 |
| `CREATE_5CELL` | `target`, `name`, `center`, `size` | — | 新路径、胞数 |
| `CREATE_16CELL` | `target`, `name`, `center`, `radius` | — | 新路径、胞数 |
| `CREATE_TESSERACT` | `target`, `name`, `center`, `edgeLength` | — | 新路径、胞数 |
| `CREATE_PRISM4` | `target`, `name`, `base`, `ws`, `we` | — | base 摘要、w 范围、胞数 |
| `CREATE_CONE4` | `target`, `name`, `base`, `apex` | — | base 摘要、apex、胞数 |
| `CREATE_BALL4` | `target`, `name`, `center`, `radius`, `density` | — | 新路径、胞数 |
| `GET_MODEL_INFO` | `name` | — | `##` 详情或分组汇总（正文由渲染侧生成） |
| `GET_TETRAHEDRON` | `name`, `tet` | — | `## path#i` + 四个顶点 |
| `SET_TETRAHEDRON_VERTEX` | `name`, `tet`, `vertex`, `pos` | `color`, `normal` | 新顶点值、降级提示 |
| `TRANSFORM_TETRAHEDRONS` | `name`, `tets`, `row0`~`row4` | — | 变换的胞数（`tets: []` = 全部） |
| `ADD_TETRAHEDRON` | `name`, `v0`~`v3` | `color` | 新胞下标 |
| `REMOVE_TETRAHEDRON` | `name`, `tet` | — | 剩余胞数 |
| `SLICE_MODEL` | `name`, `plane`, `pathA`, `pathB` | — | 两个新路径与各自胞数 |
| `SAVE_MODEL` | `name`, `file` | — | 保存路径（未实现） |
| `LOAD_MODEL` | `name`, `file` | — | 胞数（未实现） |
| `SET_3D_MAX_FPS` | `fps` | — | 新的最大帧率 |
| `GET_3D_FPS` | 无 | — | `3D FPS=xx.xx` |
| `GET_3D_1PERCENT_LOW_FPS` | 无 | — | `3D 1% Low FPS=xx.xx` |
| `SET_4D_MAX_FPS` | `fps` | — | 新的最大帧率 |
| `GET_4D_FPS` | 无 | — | `4D FPS=xx.xx` |
| `GET_4D_1PERCENT_LOW_FPS` | 无 | — | `4D 1% Low FPS=xx.xx` |
| `SET_TRIANGLE_LINE_RENDERING` | `enable` | — | `ON` / `OFF` |
| `SET_BACKGROUND_COLOR` | `color` | — | 新的背景色 |
| `SET_LIGHT_RENDERING` | `enable` | — | `ON` / `OFF` |
| `LIST_LIGHTS` | 无 | — | 光源列表（名字在最前） |
| `ADD_LIGHT` | `light` | — | 当前光源名列表 |
| `REMOVE_LIGHTS` | `names` | — | 删除结果、剩余光源 |
| `SET_AMBIENT_LIGHT` | `color` | — | 新的环境光 |
| `SET_DISPLAY_SIZE` | `edgeLength` | — | 新的视口边长 |

### 附录 B：枚举与取值表

| 枚举 | 取值 | 使用位置 |
| --- | --- | --- |
| 单轴 | `x` `y` `z` `w` | `MOVE_CAMERA_POS`、`TRANSLATE`、`SCALE`、`CLIP` |
| 四维平面 | `xy` `xz` `xw` `yz` `yw` `zw` | `ROTATE_CAMERA_VIEW`、`ROTATE` |
| 三维平面 | `xy` `xz` `yz` | `base.transforms[]` 的 `ROTATE` |
| 模型变换 method | `MATRIX` `TRANSLATE` `SCALE` `ROTATE` `CLIP` `COORDINATE` | `TRANSFORM_MODEL` |
| Mesh 变换 method | `TRANSLATE` `SCALE` `ROTATE` `MATRIX` | `base.transforms[]` |
| 3D 几何体 | `SPHERE` `CUBE` `PRISM` `PYRAMID` `CONE` | `base.shape` |
| 模型种类 | `GROUP` `SHAPE` `CARVED` `MERGED` | `GET_MODEL_INFO` 的 `Kind` |
| 布尔开关 | `true` / `false` | `SET_TRIANGLE_LINE_RENDERING`、`SET_LIGHT_RENDERING` |
| 光源类型 | `direction` 为 `null` = 点光源，否则面光源 | `ADD_LIGHT` |

### 附录 C：与代码接口的对应与实现备注

| 备注项 | 内容 |
| --- | --- |
| 模型寻址 | `Mesh4D.name` 是完整路径；层级由名字前缀表达，中间层不必存在（§0.3） |
| 形状的 target / name | `ShapeInterface` 每个方法是 `(target, name, ...)`；JSON 沿用同名：`target` = 目标模型路径，`name` = 新部件名，结果路径是两者拼接 |
| 形状颜色 | 形状接口没有颜色参数；颜色由 `cpu4dkt/generator/BasicShapes.kt` 的 `cellPalette` / `cellColors` 自动分配。要指定颜色只能用 §6.3 / §6.5 |
| `GET_MODEL_INFO` | 返回的 markdown 由 `GeometryInterfaceImpl` 直接拼好，含 `Transform matrix` 的 `text` 代码块；`Sub models` 只列直接子节点 |
| 索引 vs id | `Tetrahedron` 虽然带随机 `id`，但接口一律用**模型内下标**（`tetIndex`）寻址 |
| 分组变换 | `TransformInterfaceImpl.targets()`：命中真实模型时只作用它；否则/同时作用于 `subTree`，两者都存在时**都作用** |
| `GET_MODEL_MATRIX` | 用 `requireContains`，**只接受精确路径**，不展开分组 |
| 删除语义 | `removeModel` 删掉 `name` 与所有 `name/...`；`copyModel` 只复制该节点本身 |
| 角度单位 | JSON 与接口参数都是角度制；`Transform4D.rotate`、`CameraInterfaceImpl.rotate*` 内部 `toRadians` |
| 旋转方向 | 左手系（§0.2）；模型/几何 `ROTATE` 是"a 转向 b"为正，摄像机 `ROTATE_CAMERA_VIEW` 转基向量、符号相反（§3.2.2） |
| 变换坐标系统 | `COORDINATE` 改的是 `Transform4D` 内部的 `T` / `T⁻¹`，之后该模型的变换都在这个坐标系下解释 |
| 切片是精确切割 | `TetrahedronSlicer` 做真正的 `1:3` / `2:2` 切分（体积守恒），不是"按胞归属"的近似 |
| 切片坐标系 | `plane` 的四个点写在**源模型的局部坐标系**里，不受模型矩阵影响 |
| 视口尺寸 | `SET_DISPLAY_SIZE` = `Renderer4D.viewPortLength`，3D 侧缩放 `edgeLength * 0.5` |
| 1% Low | `FrequencyCounter.getOnePercentLowFrequency()`：取窗口内最慢 1% 帧，算它们的平均帧率 |
| 接口初始化 | `RendererInterface.INSTANCE` 在 `RendererInterface.init(GL4DRegion)` 之前会抛异常；`RendererInterfaceImpl` 负责把 `region` 分发给各子接口 |
| `no_result` 缺省 | 缺省为 `true`；需要结果必须显式写 `"no_result": false` |

### 附录 D：实现状态

| 接口 | 状态 |
| --- | --- |
| `CameraInterface` | ✅ 全部实现 |
| `ModelInterface` | ✅ 全部实现（`createEmptyModel` / `removeModel` / `copyModel` / `mergeModel` / `applyTransformToVertex`） |
| `TransformInterface` | ✅ 全部实现（含 `getModelMatrix` / `setModelMatrix`） |
| `GeometryInterface` | ✅ 全部实现（查询、顶点与胞编辑、精确超平面切割） |
| `ShapeInterface` | ✅ 全部实现（七种形状，含超球 cubed-sphere 细分） |
| `SceneInterface` | ✅ 全部实现（帧率、线框、背景色、光照、光源、环境光、视口尺寸） |
| `IOInterface` | ❌ 未实现（`saveModel` / `loadModel` 仍是 `TODO`） |

三维基本几何体（§5.3 的 JSON DSL）属于**接入层**职责：
渲染包内现成的 3D 生成器只有 `ogl3d/generator/Cube.kt` 的 `createCube`，
其余几何体按 §5.3 规范生成三角形网格即可。

### 附录 E：待定

| 项 | 说明 |
| --- | --- |
| `.4do` 读写 | `IOInterface` 未实现，`vc` / `vn` 标签的落盘格式也还没被代码用到 |
| 材质与贴图 | 3D `Mesh` 支持 `Texture` / `GLMaterial`，但形状接口没有材质参数 |
| UV 坐标 | 3D 基本几何体暂不生成 UV（`Vertex.uv` 置零），需要贴图时再补 |
| 更多 3D 几何体 | 目前规范里只有球、正方体、正棱柱、正棱锥、圆锥 |
| 撤销 / 历史 | 接口里没有撤销；`SHAPE → CARVED` 的降级是单向的 |
| 布尔运算 | `MERGE_MODEL` 只是拼接（顶点烘焙后相加），不做几何布尔运算 |
| 多材质胞 | 颜色是逐顶点的，一个 `SHAPE` 的颜色策略按形状固定；要自定义只能靠雕刻 |
