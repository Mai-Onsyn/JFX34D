# JFX34D AI API 接口文档（完善版 v2）

> 涉及的接口文件：
> `RendererInterface`、`CameraInterface`、`ModelInterface`、`TransformInterface`、
> `GeometryInterface`、`ShapeInterface`、`IOInterface`。
>
> 约定：与 AI 交互的数据（发给 AI 的 markdown、AI 返回的 json）**一律使用英文**，
> 中文只出现在本文档的说明文字中。

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
- [八、完整交互示例](#八完整交互示例)
- [九、附录](#九附录)

---

## 零、接口总览

### 0.1 接口 → 操作名映射

`RendererInterface` 是对外唯一门面，六个子接口对应六个操作类别。
AI 侧看到的不是方法名，而是 JSON 中的 `type`（操作名），映射关系如下。

| 子接口 | 接口方法 | 操作名（JSON `type`） | 章节 |
| --- | --- | --- | --- |
| `CameraInterface` | `getPosition()` | `GET_CAMERA_POS` | §3.1.1 |
| | `moveRight/Up/Ana/Forward(d)` | `MOVE_CAMERA_POS` | §3.1.2 |
| | `setPosition(pos)` | `SET_CAMERA_POS` | §3.1.3 |
| | `getView()` | `GET_CAMERA_VIEW` | §3.2.1 |
| | `rotateXY/XZ/XW/YZ/YW/ZW(a)` | `ROTATE_CAMERA_VIEW` | §3.2.2 |
| | `setView(v)` | `SET_CAMERA_VIEW` | §3.2.3 |
| `ModelInterface` | `listModel()` | `LIST_MODEL` | §4.1.1 |
| | `addModel(name)` | `CREATE_MODEL` | §4.1.2 |
| | `removeModel(name)` | `DELETE_MODEL` | §4.1.3 |
| | `copyModel(src, dst)` | `COPY_MODEL` | §4.1.4 |
| | `mergeModel(s1, s2, dst)` | `MERGE_MODEL` | §4.1.5 |
| | `applyTransformToVertex(src, dst)` | `APPLY_TRANSFORM_TO_VERTEX` | §4.1.6 |
| `TransformInterface` | `getModelMatrix(name)` | `GET_MODEL_MATRIX` | §4.3 |
| | `transform/move/scale/rotate/clip/setCoordinate` | `TRANSFORM_MODEL`（6 种 method） | §4.2 |
| `ShapeInterface` | `createTetrahedron(center, radius)` | `CREATE_TETRAHEDRON` | §5.1.1 |
| | `create5Cell(center, size)` | `CREATE_5CELL` | §5.1.2 |
| | `create16Cell(center, radius)` | `CREATE_16CELL` | §5.1.3 |
| | `createTesseract(center, edgeLength)` | `CREATE_TESSERACT` | §5.1.4 |
| | `createPrism4(base: Mesh, ws, we)` | `CREATE_PRISM4` | §5.1.5 |
| | `createCone4(base: Mesh, apex)` | `CREATE_CONE4` | §5.1.6 |
| | `createBall4(center, radius)` | `CREATE_BALL4` | §5.1.7 |
| `GeometryInterface` | `getModelInfos(name)` | `GET_MODEL_INFO` | §6.1 |
| | `getTetrahedronInfos(name, tetId)` | `GET_TETRAHEDRON` | §6.2 |
| | `setVertex(name, tetId, n, v)` | `SET_TETRAHEDRON_VERTEX` | §6.3 |
| | `transformMatrix(name, tets, m)` | `TRANSFORM_TETRAHEDRONS` | §6.4 |
| | `addTetrahedron(name, tet)` | `ADD_TETRAHEDRON` | §6.5 |
| | `removeTetrahedron(name, tetId)` | `REMOVE_TETRAHEDRON` | §6.6 |
| | `sliceModel(name, plane, a, b)` | `SLICE_MODEL` | §6.7 |
| `IOInterface` | `saveModel(name, fileName)` | `SAVE_MODEL` | §7.1 |
| | `loadModel(name, file)` | `LOAD_MODEL` | §7.2 |

### 0.2 全局约定

| 项目 | 约定 |
| --- | --- |
| 坐标系 | **左手坐标系**：x+ 右，y+ 上，z+ ana 前，w+ 深度前 |
| 四维坐标 | `(x, y, z, w)`，四个轴名分别为 `x` `y` `z` `w` |
| 轴枚举 | 小写字符串：`"x"` `"y"` `"z"` `"w"`（大小写不敏感，建议统一小写） |
| 平面枚举 | 小写字符串：`"xy"` `"xz"` `"xw"` `"yz"` `"yw"` `"zw"` |
| 角度 | JSON 中**一律使用角度制（degree）**；代码内部为弧度，由解析器换算 |
| 颜色 | 十六进制 ARGB 字符串 `"#AARRGGBB"`，或整数 `0xAARRGGBB` |
| 向量写法 | 数组 `[1, 2, 3, 4]` 或字符串 `"(1 2 3 4)"`，两种都接受 |
| 模型矩阵 | 5×5 **行主序**，行向量写作 `[a, b, c, d, e]` 或 `"(a b c d e)"` |
| 顶点约定 | 顶点是**列向量**，`p_world = M_model · p_local`；平移量写在矩阵**第 5 列**（对齐 `Matrix5x5::transform`） |
| 变换叠加 | `transforms` 数组按顺序依次**左乘**到模型矩阵：`M ← T_i · M` |
| 旋转正方向 | 平面 `(a, b)` 上，**a 轴转向 b 轴为正**：`a' = a·cos θ − b·sin θ`，`b' = a·sin θ + b·cos θ`（模型 / Mesh / 几何；摄像机视角见 §3.2.2，符号相反） |
| 长度单位 | 无单位（世界单位），由使用者自行约定比例 |
| 名称 | 模型名区分大小写，长度 1–64，建议使用英文与数字 |

---

## 一、数据格式

### 1.1 文件数据格式

4D 模型采用与 3D obj 模型格式类似的纯文本顶点与面描述的方式，并进行简化。

每个标签占一行，每行使用空格隔开不同数据，标签使用线性索引，不同标签不共享索引。

| 标签 | 数据长度 |  数据格式   |                   描述                   |           示例            |
| :--: | :------: | :---------: | :--------------------------------------: | :-----------------------: |
|  v   |    4     |    Float    |               顶点位置坐标               |        v 1 1 0 2.5        |
|  vn  |    4     |    Float    |          顶点法向量（单位向量）          |        vn 1 0 0 0         |
|  vc  |    1     |     Int     |             顶点颜色（ARGB）             |       vc 0xFF808080       |
|  t   |    4     | Int/Int/Int | 四面体描述（引用 v/vn/vc 的索引，从0开始） | t 0/0/0 1/1/1 2/2/2 3/3/3 |

> 注：原文档中 `vc` 行的示例误写为 `vn 0xFF808080`，此处已修正。

文件后缀名暂定为 **.4do**。

### 1.2 代码数据格式

采用 **Mesh4D** 类描述 4D 模型（与代码保持一致，模型名由场景层维护，`Mesh4D` 自身不带名称）：

```kotlin
class Mesh4D(
    val tetrahedrons: List<Tetrahedron> = mutableListOf()
) {
    var dirty: Boolean = true
}
```

采用 **Tetrahedron** 类描述 4D 四面体：

```kotlin
data class Tetrahedron(
    val v0: Vertex4D,
    val v1: Vertex4D,
    val v2: Vertex4D,
    val v3: Vertex4D
)
```

采用 **Vertex4D** 类描述 4D 顶点：

```kotlin
data class Vertex4D(
    val pos: Vector4f,
    val color: ColorARGB,
    val normal: Vector4f
)
```

其他相关类型：

| 类型 | 定义位置 | 说明 |
| --- | --- | --- |
| `Matrix5f` | `cpu4dkt/Matrix5f.kt` | 5×5 行主序矩阵，`IDENTITY` 为单位阵 |
| `Coordinate4D` | `utils/Coordinate4D.kt` | 四维基坐标系 `vx, vy, vz, vw` |
| `Direction.Axis` | `utils/Direction.kt` | `X Y Z W` |
| `Direction.Plane` | `utils/Direction.kt` | `XY XZ XW YZ YW ZW` |
| `ColorARGB` | `ogl3d/data/Triangle.kt` | `#AARRGGBB` 打包的颜色 |
| `Mesh`（3D） | `ogl3d/data/Mesh.kt` | 三角形列表 + 3D 变换，见 §5.2 |

### 1.3 枚举取值

**轴（axis，单轴）**：`x` | `y` | `z` | `w`

**平面（plane / axis，双轴）**：`xy` | `xz` | `xw` | `yz` | `yw` | `zw`

> 四维中"绕轴旋转"必须写成"在哪个二维平面内旋转"，一条轴无法唯一确定旋转。
> 其中 `xy` `yz` `xz` 是三维已有的旋转，`xw` `yw` `zw` 是卷入/卷出第四维的旋转。

---

## 二、AI 交互数据格式

### 2.1 发送 markdown

执行并解析了 AI 返回的 json 后，将必要数据按需发送给 AI：

````markdown
# GET_CAMERA_POS(id=1)
Pos=(0, 0, 0, 0)
````

- 只有**显式写成 `"no_result": false`** 的操作才会生成一段这样的 markdown，按操作顺序排列。
- 首行固定为 `# <操作名>(id=<该操作的 id>)`，id 用于让 AI 把结果与请求对上。
- `no_result` 缺省或为 `true` 的操作不产生任何返回段落。

### 2.2 返回 json

包含一个 Int 版本键值对，一个结束标志键值对，一个操作数组键值对。

顶层字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `version` | Int | 是 | 协议版本，当前为 `1` |
| `done` | Boolean | 否（默认 `false`） | 本轮对话是否结束，见开发文档 §5.2 |
| `operations` | Array | 是 | 操作数组，结束本轮时为空数组 |

每个操作必须包含的字段：

- **type**(String)：操作名称，后续返回结果也需要带有该名称
- **id**(Int)：操作 id，由 AI 决定，用于区分不同操作，单次 json 返回的不同操作必须拥有不同 id，多次请求可以重复 id，但仍然建议使用不同 id
- **data**(JSON)：该操作所需的数据；无参数的操作（如 `GET_CAMERA_POS`、`LIST_MODEL`）可省略

可选字段：

- **no_result**(Boolean)：不接收该条命令的执行结果，默认为 true，一般不建议使用，只有在同时执行了多条类似命令时（如放置四面体），且之后使用查询命令能够获取刚才所有操作的结果时，可以这样优化

```json
{
    "version": 1,
    "done": false,
    "operations": [
        {
            "type": "MOVE_CAMERA_POS",
            "id": 1,
            "no_result": false,
            "data": {
                "axis": "x",
                "distance": -5
            }
        },
        {
            "type": "GET_CAMERA_POS",
            "id": 2,
            "no_result": false
        }
    ]
}
```

**结束标记**：本轮对话结束时返回空操作集并带上 `done: true`。

```json
{
    "version": 1,
    "done": true,
    "operations": []
}
```

### 2.3 错误返回

**格式 / 解析错误**（整个 json 无法解析、缺 `type`/`id`、`done:true` 但 operations 非空等）：

````markdown
# Error
You just returned an error format:
```json
AI返回的数据
```
Reason:
```markdown
错误原因（解析器报出的）
```
````

**单条操作执行失败**（格式正确，但参数非法或对象不存在）：只影响该条操作，其余操作继续执行。

```markdown
# CREATE_MODEL(id=5)
Error: model "Cube A" already exists
```

### 2.4 通用参数校验规则

| 校验项 | 规则 |
| --- | --- |
| 操作数组成员 | 必须含 `type`(String)、`id`(Int，非负且本次请求内唯一)；需要参数的操作必须含 `data` |
| 未知操作名 | 该条返回 `Error: unknown operation type "XXX"` |
| 必填参数缺失 | 该条返回 `Error: missing required field "xxx"` |
| 模型名 | 必须存在（查询类）/ 必须不存在且合法（创建类），否则该条报错 |
| 半径、棱长、高度 | 必须 `> 0` |
| 棱柱 / 棱锥边数 | `sides` 必须为 `>= 3` 的整数 |
| 平面拉伸范围 | `ws < we`；若 `ws > we` 解析器自动交换，`ws == we` 报错 |
| 方向向量 | 设置坐标系/视角时校验是否为单位向量、是否两两正交 |
| 索引越界 | 四面体 id、顶点编号越界时该条报错，不改变模型 |

---

## 三、四维摄像机

### 3.1 位置

```kotlin
fun moveRight(d: Float)		// x+
fun moveUp(d: Float)		// y+
fun moveAna(d: Float)		// z+
fun moveForward(d: Float)	// w+
```

d 为负数时向相反方向移动，比如 x-。

所有移动都**仅相对于当前摄像机坐标系**，只在位置上加减坐标轴向量（即 `pos += v_axis * d`），不改变视角。

---

#### 3.1.1 获取位置

**GET_CAMERA_POS**：返回当前摄像机坐标

不需要参数

```markdown
# GET_CAMERA_POS(id=1)
Pos=(0, 0, 0, 0)
```

#### 3.1.2 移动位置

**MOVE_CAMERA_POS**：按轴移动摄像机坐标

必须包含的参数：

- axis(Enum)：摄像机坐标系的坐标轴
- distance(Float)：移动距离，可正可负

**axis** 可选值：`x` | `y` | `z` | `w`，依次对应 `moveRight` / `moveUp` / `moveAna` / `moveForward`

```json
{
    "type": "MOVE_CAMERA_POS",
    "id": 1,
    "no_result": false,
    "data": {
        "axis": "x",
        "distance": -5
    }
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

```json
{
    "type": "SET_CAMERA_POS",
    "id": 2,
    "no_result": false,
    "data": {
        "pos": [0, 0, 0, 0]
    }
}
```

```markdown
# SET_CAMERA_POS(id=2)
Success
Now Pos=(0, 0, 0, 0)
```

### 3.2 视角

转动摄像机坐标轴，共六个旋转面：

```kotlin
/** XY 平面：屏幕内旋转（等于 3D 的 roll） */
fun rotateXY(a: Float)

/** XZ 平面：右方向卷入/卷出第四维 */
fun rotateXZ(a: Float)

/** XW 平面：偏航 yaw（左右环顾，不动 vy、vz） */
fun rotateXW(a: Float)

/** YZ 平面：上方向卷入/卷出第四维 */
fun rotateYZ(a: Float)

/** YW 平面：俯仰 pitch（抬头低头，不动 vx、vz） */
fun rotateYW(a: Float)

/** ZW 平面：前方卷入/卷出第四维（4D 里最有"另类"感的旋转） */
fun rotateZW(a: Float)
```

代码中 a 是弧度；**JSON 中一律传角度**，由解析器换算为弧度。

---

#### 3.2.1 获取视角

**GET_CAMERA_VIEW**：获取摄像机的坐标系向量在世界中的指向

无参数

```markdown
# GET_CAMERA_VIEW(id=3)
vx=(1, 0, 0, 0)
vy=(0, 1, 0, 0)
vz=(0, 0, 1, 0)
vw=(0, 0, 0, 1)
```

#### 3.2.2 旋转视角

**ROTATE_CAMERA_VIEW**：旋转摄像机视角

> 原文档中该操作名误写为 `ROTATA_CAMERA_VIEW`，此处更正为 `ROTATE_CAMERA_VIEW`。

必须包含的参数：

- axis(Enum)：四维旋转平面（二维平面）
- angle(Float)：旋转角度（角度制）

**axis** 可选值：`xy` | `xz` | `xw` | `yz` | `yw` | `zw`

旋转的是**摄像机的四个基向量**（不是模型），按 `Camera4D.rotatePlane` 的定义：

```
a' = a·cos θ + b·sin θ
b' = −a·sin θ + b·cos θ
```

即正角度让基向量 `a` 转向 `b`；因为观察矩阵的行就是这四个基向量，
**画面看起来朝相反方向旋转**（视觉上表现为 `b` 转向 `a`）。

> 注意：这与 §4.2.4 模型/几何的 `ROTATE` 正方向定义**互为反向**——
> 那里旋转的是顶点（`a' = a·cos − b·sin`），这里旋转的是观察基向量。
> 另外坐标系是左手系（§0.2），不要用右手定则判断。

```json
{
    "type": "ROTATE_CAMERA_VIEW",
    "id": 4,
    "no_result": false,
    "data": {
        "axis": "xw",
        "angle": 30
    }
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

#### 3.2.3 设置视角

**SET_CAMERA_VIEW**：直接设置摄像机坐标系

必须包含的参数：

- vx(Vector4f)
- vy(Vector4f)
- vz(Vector4f)
- vw(Vector4f)

每个向量需要校验是否是单位向量，是否两两互相垂直（四个向量四四正交）。

```json
{
    "type": "SET_CAMERA_VIEW",
    "id": 5,
    "no_result": false,
    "data": {
        "vx": [1, 0, 0, 0],
        "vy": [0, 1, 0, 0],
        "vz": [0, 0, 1, 0],
        "vw": [0, 0, 0, 1]
    }
}
```

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

对场景内的模型增加和删除相关，用于 AI 控制场景中的模型。

所有操作都**显式指定模型名**（`name` 字段），场景不维护隐式的"当前模型"状态。

### 4.1、模型操作

#### 4.1.1 查询所有模型

**LIST_MODEL**：列出当前场景中的所有模型名称（按模型索引）

无参数

```markdown
# LIST_MODEL(id=6)
0. "Test Model"
1. "Cube A"
2. "Shape 1"
3. "Dio"
```

索引即模型的添加顺序，后续请求里的 `name` 用名称而不是索引。

#### 4.1.2 创建模型

**CREATE_MODEL**：创建一个空模型，追加到场景模型列表后面

必须包含的参数：

- name(String)：模型名称

需要校验模型名称是否已被占用

```json
{
    "type": "CREATE_MODEL",
    "id": 7,
    "no_result": false,
    "data": {
        "name": "Cube A"
    }
}
```

```markdown
# CREATE_MODEL(id=7)
Successfully create "Cube A"
Now models: 2
```

#### 4.1.3 删除模型

**DELETE_MODEL**：从场景的模型列表中删除指定名称的模型

必须包含的参数：

- name(String)：模型名称

需要校验模型名称是否存在

```markdown
# DELETE_MODEL(id=8)
Successfully delete "Cube A"
Now models: 1
```

#### 4.1.4 复制模型

**COPY_MODEL**：复制一个已有模型（**包含其模型矩阵**）

必须包含的参数：

- src(String)：源模型名称，必须存在
- dst(String)：新模型名称，必须不存在

```json
{
    "type": "COPY_MODEL",
    "id": 9,
    "no_result": false,
    "data": {
        "src": "Cube A",
        "dst": "Cube B"
    }
}
```

```markdown
# COPY_MODEL(id=9)
Successfully copy "Cube A" to "Cube B"
Tetrahedrons: 48
```

#### 4.1.5 合并模型

**MERGE_MODEL**：把两个模型合并成一个新模型

必须包含的参数：

- src1(String)：源模型 1，必须存在
- src2(String)：源模型 2，必须存在
- dst(String)：新模型名称，必须不存在

语义：

- 两个源模型的四面体按各自矩阵变换到世界坐标后拼到 `dst` 中；
- `dst` 的模型矩阵为单位阵（变换已烘焙到顶点）；
- 两个源模型保持不变。

```markdown
# MERGE_MODEL(id=10)
Successfully merge "Cube A" + "Cube B" into "Cube C"
Tetrahedrons: 96
```

#### 4.1.6 应用变换到顶点

**APPLY_TRANSFORM_TO_VERTEX**：把模型的变换矩阵烘焙进顶点，得到一个顶点已在世界坐标的新模型

必须包含的参数：

- name(String)：源模型名称，必须存在
- dest(String)：目标模型名称；等于 `name` 时表示原地烘焙，否则必须不存在

```json
{
    "type": "APPLY_TRANSFORM_TO_VERTEX",
    "id": 11,
    "no_result": false,
    "data": {
        "name": "Cube A",
        "dest": "Cube A"
    }
}
```

```markdown
# APPLY_TRANSFORM_TO_VERTEX(id=11)
Successfully apply transform of "Cube A" to vertices
Now matrix is identity
Tetrahedrons: 48
```

### 4.2、模型变换

对模型的 5×5 transform 矩阵做操作，而不是操作模型顶点。

所有的变换都使用 **TRANSFORM_MODEL** 操作名。

必须包含的参数：

- name(String)：要操作的模型名称
- transforms(JSONArray)：对模型的变换操作

可选择包含的参数：

- apply(Boolean)：决定该操作是否是添加变换，默认为 true，当指定 false 时，将用新的变换组成的矩阵覆盖模型矩阵（即先把矩阵置为单位阵，再依次应用 `transforms`）

每个 transform 为一个 JSON，必须包含两个字段：

- method(String)：变换方式
- data(JSON)：变换需要的数据

**示例**：

```json
{
    "type": "TRANSFORM_MODEL",
    "id": 12,
    "no_result": true,
    "data": {
        "name": "Target Model",
        "apply": false,
        "transforms": [
            {
                "method": "MATRIX",
                "data": {
                    "row0": [1, 0, 0, 0, 0],
                    "row1": [0, 1, 0, 0, 0],
                    "row2": [0, 0, 1, 0, 0],
                    "row3": [0, 0, 0, 1, 0],
                    "row4": [0, 0, 0, 0, 1]
                }
            },
            {
                "method": "TRANSLATE",
                "data": {
                    "y": 5.0,
                    "w": -0.2
                }
            }
        ]
    }
}
```

```json
{
    "type": "GET_MODEL_MATRIX",
    "id": 13,
    "no_result": false,
    "data": {
        "name": "Target Model"
    }
}
```

返回：

```markdown
# TRANSFORM_MODEL(id=12)
Applied transform (如果是设置而不是追加 用 Setted)
"Target Model" current is:
(1 0 0 0 0)
(0 1 0 0 5)
(0 0 1 0 0)
(0 0 0 1 -0.2)
(0 0 0 0 1)
```

（对照 `Matrix5x5::transform(p)`：平移写在**第 5 列**，即第 i 行第 5 个元素，
所以 `TRANSLATE y=5, w=-0.2` 只改动了 `row1` 与 `row3` 的最后一个数。）

**单次请求的所有变换操作只返回最后完成变换后的模型矩阵**。

**第 4.2.x 章节的操作统一为 transforms 的 method 字段，参数均为 transforms 内的 data 的参数**。

#### 4.2.1 矩阵变换

**MATRIX**：将矩阵左乘到指定模型的模型矩阵（`M ← M_arg · M`）

必须包含的参数：

- row0(Vector5f)：矩阵第一行的行向量，下类同
- row1(Vector5f)
- row2(Vector5f)
- row3(Vector5f)
- row4(Vector5f)

#### 4.2.2 平移

**TRANSLATE**：设置模型在四条坐标轴上的移动

至少包含以下参数的其中一个：

- x(Float)：在 x 轴上移动，下类同
- y(Float)
- z(Float)
- w(Float)

未给出的轴按 0 处理。

等价矩阵（`p' = M · p`，平移量在第 5 列）：

```
row0 = (1 0 0 0  x)
row1 = (0 1 0 0  y)
row2 = (0 0 1 0  z)
row3 = (0 0 0 1  w)
row4 = (0 0 0 0  1)
```

#### 4.2.3 缩放 / 镜像

**SCALE**：设置模型在不同轴上的缩放

可单独包含该参数：

- all(Float)：四个轴上的缩放倍率

或者至少包含下列参数的其中一个：

- x(Float)：在 x 轴上缩放，为负值时镜像，下类同
- y(Float)
- z(Float)
- w(Float)

未给出的轴按 1 处理；缩放围绕当前变换坐标系的原点进行。

#### 4.2.4 旋转

**ROTATE**：绕二维平面旋转指定角度

必须包含的参数：

- axis(Enum)：旋转平面
- angle(Float)：旋转角度（角度制）

**axis** 可选值：`xy` | `xz` | `xw` | `yz` | `yw` | `zw`

旋转正负方向约定（全文档统一，Mesh 的 3D `ROTATE` 也用同一约定）：
在平面 `(a, b)` 上旋转角度 `θ` 时，**第一轴 `a` 转向第二轴 `b` 为正**：

```
a' = a·cos θ − b·sin θ
b' = a·sin θ + b·cos θ
其余坐标不变
```

> 坐标系是**左手系**（§0.2），所以不要用右手定则/叉乘来推方向，直接按"`a` 转向 `b`"判断。
> 例：`xy` 平面的正方向是 +X 转向 +Y，在屏幕上（x 向右、y 向上、沿 +z 方向看）表现为**逆时针**。

#### 4.2.5 剪切

**CLIP**：一个轴随另一个轴偏移（切变）

必须包含的参数：

- source(Enum)：源轴
- target(Enum)：目标轴
- amount(Float)：偏移量

**source / target** 可选值：`x` | `y` | `z` | `w`

语义：`p_target ← p_target + amount * p_source`，即目标轴上的坐标随源轴坐标线性偏移。

```json
{
    "method": "CLIP",
    "data": {
        "source": "w",
        "target": "x",
        "amount": 0.5
    }
}
```

#### 4.2.6 基坐标系

**COORDINATE**：变换基坐标系，设置之后的所有变换都会围绕这个坐标系变换，默认为世界坐标系

至少包含下列参数中的其中一个：

- pos(Vector4f)：坐标系原点
- vx(Vector4f)：right 轴
- vy(Vector4f)：up 轴
- vz(Vector4f)：ana 轴
- vw(Vector4f)：front 轴

未给出的字段保持当前基坐标系的值不变（默认为世界坐标系）。给定时校验单位性与正交性。

作用范围：**本次请求内其后的 transforms**；请求结束后重置为世界坐标系。`COORDINATE` 本身不改变模型矩阵。

```json
{
    "method": "COORDINATE",
    "data": {
        "pos": [0, 2, 0, 0],
        "vy": [0, 1, 0, 0]
    }
}
```

### 4.3、获取变换矩阵

使用操作命令 **GET_MODEL_MATRIX**：获取模型变换矩阵

必须包含的参数：

- name(String)：目标模型名称

```json
{
    "type": "GET_MODEL_MATRIX",
    "id": 14,
    "no_result": false,
    "data": {
        "name": "Cube A"
    }
}
```

```markdown
# GET_MODEL_MATRIX(id=14)
"Cube A" current is:
row0=(1, 0, 0, 0, 0)
row1=(0, 1, 0, 0, 0)
row2=(0, 0, 1, 0, 0)
row3=(0, 0, 0, 1, 0)
row4=(0, 0, 0, 0, 1)
```

---

## 五、基础形状

对应 `ShapeInterface`。基础形状直接把四面体写进模型，无需 AI 自己拼顶点。

> **实现备注（重要）**
> `ShapeInterface` 的方法签名**不带模型名**，但 JSON 里 **`name` 是必填字段**：
> 解析器负责取出 `name`、校验模型存在，再把形状写进该模型。
> 因此**不存在**"当前模型 / 目标模型"这类隐式状态。

### 5.1、四维形状

所有形状操作都支持的字段：

| 字段 | 类型 | 必填 | 默认 | 说明 |
| --- | --- | --- | --- | --- |
| `name` | String | **是** | — | 写入哪个模型，必须已存在 |
| `color` | Color | 否 | `#FFB0B0B0` | 统一顶点颜色 |

四面体数量参考（实现生成的数量应与此一致）：

| 形状 | 顶点数 | 胞数 | 四面体数 |
| --- | --- | --- | --- |
| 正四面体 `CREATE_TETRAHEDRON` | 4 | 1 | 1 |
| 五胞体 `CREATE_5CELL` | 5 | 5 | 5 |
| 超八面体 `CREATE_16CELL` | 8 | 16 | 16 |
| 超立方体 `CREATE_TESSERACT` | 16 | 8 | 48（每胞 Kuhn 分解为 6 个四面体） |
| 超棱柱 `CREATE_PRISM4` | 2×base | 侧面胞 + 2 底面 | 取决于 base 网格 |
| 超锥 `CREATE_CONE4` | base + 1 | base 三角形数 | = base 三角形数 |
| 超球 `CREATE_BALL4` | 取决于 `density` | — | 取决于 `density` |

---

#### 5.1.1 正四面体

**CREATE_TETRAHEDRON**：创建正四面体（四个顶点在坐标轴上距中心 radius 的位置）

必须包含的参数：

- name(String)：写入哪个模型，必须已存在
- center(Vector4f)：中心
- radius(Float)：顶点到中心的距离，`> 0`

语义：四个顶点为 `center + (radius, 0, 0, 0)`、`center + (0, radius, 0, 0)`、
`center + (0, 0, radius, 0)`、`center + (0, 0, 0, radius)`。

> 注意：这四个顶点落在超平面 `x + y + z + w = radius` 上，四维体积为 0，
> 它是一个**三维片元**（可用于视觉标记，或作为 `SLICE_MODEL` 的切片平面，见 §6.7）。

```json
{
    "type": "CREATE_TETRAHEDRON",
    "id": 15,
    "no_result": false,
    "data": {
        "name": "Shape 1",
        "center": [0, 0, 0, 0],
        "radius": 1.0,
        "color": "#FFFF5050"
    }
}
```

```markdown
# CREATE_TETRAHEDRON(id=15)
Successfully create TETRAHEDRON in "Shape 1"
Center=(0, 0, 0, 0), radius=1.0
Tetrahedrons added: 1, now 1
```

#### 5.1.2 四维单纯形（五胞体）

**CREATE_5CELL**：创建四维单纯形（5-cell / 超四面体）

必须包含的参数：

- name(String)：写入哪个模型，必须已存在
- center(Vector4f)：中心
- size(Float)：棱长，`> 0`

```json
{
    "type": "CREATE_5CELL",
    "id": 16,
    "no_result": false,
    "data": {
        "name": "Shape 1",
        "center": [0, 0, 0, 0],
        "size": 2.0
    }
}
```

```markdown
# CREATE_5CELL(id=16)
Successfully create 5CELL in "Shape 1"
Center=(0, 0, 0, 0), size=2.0
Tetrahedrons added: 5, now 6
```

#### 5.1.3 超八面体

**CREATE_16CELL**：创建超八面体（16-cell / 超正八面体）

必须包含的参数：

- name(String)：写入哪个模型，必须已存在
- center(Vector4f)：中心
- radius(Float)：顶点到中心的距离，`> 0`

语义：8 个顶点位于 `±radius` 的四条坐标轴上。

```markdown
# CREATE_16CELL(id=17)
Successfully create 16CELL in "Shape 1"
Center=(0, 0, 0, 0), radius=1.5
Tetrahedrons added: 16, now 22
```

#### 5.1.4 超立方体

**CREATE_TESSERACT**：创建超立方体（tesseract）

必须包含的参数：

- name(String)：写入哪个模型，必须已存在
- center(Vector4f)：中心
- edgeLength(Float)：棱长，`> 0`

```json
{
    "type": "CREATE_TESSERACT",
    "id": 18,
    "no_result": false,
    "data": {
        "name": "Cube A",
        "center": [0, 0, 0, 0],
        "edgeLength": 2.0
    }
}
```

```markdown
# CREATE_TESSERACT(id=18)
Successfully create TESSERACT in "Cube A"
Center=(0, 0, 0, 0), edgeLength=2.0
Tetrahedrons added: 48, now 48
```

#### 5.1.5 超棱柱

**CREATE_PRISM4**：以 3D 网格为底面，沿 w 轴拉伸成四维超棱柱（`base × [ws, we]`）

必须包含的参数：

- name(String)：写入哪个模型，必须已存在
- base(Mesh)：3D 底面网格，见 **§5.2**
- ws(Float)：起始 w 坐标
- we(Float)：结束 w 坐标

语义：

- `base` 的 3D 点 `(x, y, z)` 提升为四维点 `(x, y, z, 0)` 后，被放在 `w = ws` 与 `w = we` 两个超平面上；
- 每个底面三角形沿 w 方向拉出一个三棱柱胞，再切成四面体；
- 两端各保留一份 `base` 网格作为胞（cap）。

要求 `ws ≠ we`；`ws > we` 时解析器自动交换。

> 建议 `base` 是**封闭**曲面（球 / 正方体 / 棱柱 / 棱锥 / 圆锥默认都是封闭的），
> 否则生成的超棱柱不封闭，渲染上会看到缺口。

```json
{
    "type": "CREATE_PRISM4",
    "id": 19,
    "no_result": false,
    "data": {
        "name": "Tower",
        "base": {
            "shape": "CUBE",
            "edge": 1.0,
            "color": "#FF66CCFF",
            "transforms": [
                { "method": "SCALE", "data": { "x": 2.0, "z": 2.0 } }
            ]
        },
        "ws": -1.5,
        "we": 1.5
    }
}
```

```markdown
# CREATE_PRISM4(id=19)
Successfully create PRISM4 in "Tower"
Base: CUBE edge=1.0 (12 triangles), color=#FF66CCFF, transforms=[SCALE]
w range=(-1.5, 1.5)
Tetrahedrons added: 48, now 48
```

#### 5.1.6 超锥

**CREATE_CONE4**：以 3D 网格为底面、以四维点为顶点的超锥

必须包含的参数：

- name(String)：写入哪个模型，必须已存在
- base(Mesh)：3D 底面网格，见 **§5.2**
- apex(Vector4f)：四维顶点

语义：

- `base` 的 3D 点 `(x, y, z)` 提升为四维点 `(x, y, z, 0)`，即底面位于 `w = 0` 超平面上；
- `base` 的每个三角形与 `apex` 组成一个四面体。

> 建议 `apex.w ≠ 0`，否则整个超锥退化在 `w = 0` 超平面内（仍合法，但没有四维体积）。

```json
{
    "type": "CREATE_CONE4",
    "id": 20,
    "no_result": false,
    "data": {
        "name": "Tower",
        "base": {
            "shape": "PRISM",
            "sides": 6,
            "radius": 1.0,
            "height": 2.0,
            "color": "#FFFFCC44",
            "transforms": [
                { "method": "ROTATE", "data": { "axis": "xz", "angle": 30 } }
            ]
        },
        "apex": [0, 0, 0, 2.5]
    }
}
```

```markdown
# CREATE_CONE4(id=20)
Successfully create CONE4 in "Tower"
Base: PRISM sides=6 radius=1.0 height=2.0 (20 triangles), transforms=[ROTATE]
Apex=(0, 0, 0, 2.5)
Tetrahedrons added: 20, now 68
```

#### 5.1.7 超球

**CREATE_BALL4**：创建超球（3-sphere 表面，用四面体逼近）

必须包含的参数：

- name(String)：写入哪个模型，必须已存在
- center(Vector4f)：球心
- radius(Float)：半径，`> 0`

可选参数：

- density(Int)：细分程度，`>= 1`，越大越接近球；未给出时用实现的默认值（建议 `2`）

```json
{
    "type": "CREATE_BALL4",
    "id": 21,
    "no_result": false,
    "data": {
        "name": "Ball",
        "center": [0, 0, 0, 0],
        "radius": 2.0,
        "density": 2,
        "color": "#FF88FF88"
    }
}
```

```markdown
# CREATE_BALL4(id=21)
Successfully create BALL4 in "Ball"
Center=(0, 0, 0, 0), radius=2.0, density=2
Tetrahedrons added: 96, now 96
```

---

### 5.2、3D 基本几何体（Mesh 的 JSON 定义）

`CREATE_PRISM4` 与 `CREATE_CONE4` 需要一个 3D 底面网格（`Mesh`）。
AI 不写三角形，而是用 **JSON 描述一个基本几何体 + 一串 3D 变换**，解析器负责生成三角形网格。

当前**只支持**这几种几何体：

| `shape` | 中文名 | 必填参数 | 可选参数（默认值） |
| --- | --- | --- | --- |
| `SPHERE` | 球 | `radius` | `segments`(16)、`rings`(8) |
| `CUBE` | 正方体 | `edge` | — |
| `PRISM` | 正 n 棱柱 | `sides` | `radius`(1.0)、`height`(1.0) |
| `PYRAMID` | 正 n 棱锥 | `sides` | `radius`(1.0)、`height`(1.0) |
| `CONE` | 圆锥 | `radius` | `height`(1.0)、`segments`(16) |

#### 5.2.1 结构

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
| 几何参数 | Float / Int | 见上表 | 与 `shape` 对应，见 §5.2.2 |
| `color` | Color | 否 | 网格统一顶点色，默认 `#FFB0B0B0` |
| `transforms` | Array | 否 | 3D 变换列表，按顺序应用，见 §5.2.3 |

#### 5.2.2 各几何体的参数与朝向约定

坐标与朝向（未施加变换时）：

| `shape` | 位置与朝向 |
| --- | --- |
| `SPHERE` | 球心在原点；用经纬网格三角化，`segments` 为经线分段数（≥3），`rings` 为纬线分段数（≥2） |
| `CUBE` | 中心在原点，棱长 `edge`，棱平行于三条坐标轴 |
| `PRISM` | 轴线沿 **+Y**，Y 方向居中（从 `-height/2` 到 `+height/2`）；底面为正 `sides` 边形，外接圆半径 `radius`，其中一个顶点位于 +X 方向 |
| `PYRAMID` | 轴线沿 **+Y**；底面在 `y = -height/2`，顶点在 `y = +height/2`；底面同上 |
| `CONE` | 轴线沿 **+Y**；底面圆在 `y = -height/2`，顶点在 `y = +height/2`；`segments` 为圆周分段数（≥3） |

> 所有 `shape` 生成的都是**封闭曲面**（三角形网格），可直接作为 `CREATE_PRISM4` / `CREATE_CONE4` 的底面。
> 需要把轴线朝向其他方向时，用 `transforms` 里的 `ROTATE` 旋转即可，例如绕 `xz` 平面转 90° 让轴线指向 +Z。

**各几何体示例**：

```json
{ "shape": "SPHERE", "radius": 1.5, "segments": 24, "rings": 12 }
```

```json
{ "shape": "CUBE", "edge": 1.0 }
```

```json
{ "shape": "PRISM", "sides": 8, "radius": 1.0, "height": 2.0 }
```

```json
{ "shape": "PYRAMID", "sides": 4, "radius": 1.2, "height": 2.0 }
```

```json
{ "shape": "CONE", "radius": 1.0, "height": 2.0, "segments": 32 }
```

#### 5.2.3 3D 变换（transforms）

与模型变换（§4.2）风格一致：每个元素含 `method` 与 `data`，**按数组顺序依次应用**，
即后一个变换作用在前面的结果上。旋转、缩放都以**原点**为中心。

| method | 参数 | 说明 |
| --- | --- | --- |
| `TRANSLATE` | `x` `y` `z`(Float) | 至少给一个，未给出的轴按 0；`p ← p + t` |
| `SCALE` | `all`(Float) 或 `x` `y` `z`(Float) | 未给出的轴按 1；负值表示镜像；`p ← p * s` |
| `ROTATE` | `axis`(Enum) `angle`(Float，角度制) | `axis` 取 `xy` / `xz` / `yz`（3D 只有三个旋转面） |
| `MATRIX` | `row0`~`row3`(Vector4f) | 高级用法：4×4 行主序矩阵左乘，`p ← M · p` |

`ROTATE` 的旋转方向与 §4.2.4 的约定完全一致：角度制，**第一轴转向第二轴为正**（左手系，不要用右手定则）：

| axis | 变换式（其余坐标不变） | 正方向 |
| --- | --- | --- |
| `xy` | `x' = x·cos a − y·sin a`，`y' = x·sin a + y·cos a` | +X 转向 +Y |
| `yz` | `y' = y·cos a − z·sin a`，`z' = y·sin a + z·cos a` | +Y 转向 +Z |
| `xz` | `x' = x·cos a − z·sin a`，`z' = x·sin a + z·cos a` | +X 转向 +Z |

**示例：绕非原点位置旋转**（先移到原点、旋转、再移回去）：

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

#### 5.2.4 颜色

- `color` 给出时，网格所有顶点使用该颜色；
- 未给出时使用默认统一色 `#FFB0B0B0`；
- 颜色写法与全局一致：`"#AARRGGBB"` 字符串或 `0xAARRGGBB` 整数。

#### 5.2.5 3D → 4D 的嵌入规则

1. 按 `shape` 生成 3D 三角形网格；
2. 依次应用 `transforms` 中的 3D 变换，得到最终 3D 坐标 `(x, y, z)`；
3. 提升为四维点 `(x, y, z, 0)`；
4. 由具体操作决定第四维上的处理：
   - `CREATE_PRISM4`：复制到 `w = ws` 与 `w = we` 两个超平面并拉伸；
   - `CREATE_CONE4`：底面固定在 `w = 0` 超平面，与 `apex` 相连。

> 也就是说，`base` 里**不要**写 w 坐标；第四维位置由 `ws` / `we` / `apex` 控制。

#### 5.2.6 校验规则

| 校验项 | 规则 |
| --- | --- |
| `shape` | 必须是上表五个值之一，否则报 `Error: unsupported shape "XXX"` |
| `radius` / `edge` / `height` | `> 0` |
| `sides` | `>= 3` 的整数 |
| `segments` | `>= 3` 的整数 |
| `rings` | `>= 2` 的整数 |
| `transforms[].method` | 必须是 `TRANSLATE` / `SCALE` / `ROTATE` / `MATRIX` 之一 |
| `ROTATE.axis` | 3D 只接受 `xy` / `xz` / `yz`（传 `xw` 等四维平面报错） |
| `SCALE` | 不能三个轴全为 0（会退化成平面） |

---

## 六、模型几何编辑

对应 `GeometryInterface`，直接操作模型上的四面体与顶点。

### 6.1 获取模型信息

**GET_MODEL_INFO**：获取模型的整体信息

必须包含的参数：

- name(String)：模型名称

返回包含四面体数量、顶点数量、包围盒、编辑次数等；**顶点明细可能被截断**
（模型较大时只给统计），需要精确顶点请用 §6.2 按 id 查询。

字段含义：

| 字段 | 含义 |
| --- | --- |
| `Tetrahedrons` | 四面体数量 |
| `Unique vertices` | 去重后的顶点数量 |
| `Edits` | 几何编辑次数（顶点/四面体的增删改），**不含**模型矩阵变换 |
| `Bound` | 顶点经模型矩阵变换后的世界坐标包围盒 |

```json
{
    "type": "GET_MODEL_INFO",
    "id": 22,
    "no_result": false,
    "data": {
        "name": "Cube A"
    }
}
```

```markdown
# GET_MODEL_INFO(id=22)
Model "Cube A"
Tetrahedrons: 48
Unique vertices: 16
Edits: 0
Bound: x[-1, 1] y[-1, 1] z[-1, 1] w[-1, 1]
Note: vertex list truncated, use GET_TETRAHEDRON for details
```

### 6.2 获取单个四面体

**GET_TETRAHEDRON**：查询指定四面体的四个顶点

必须包含的参数：

- name(String)：模型名称
- tet(Int)：四面体 id（该模型内的索引，从 0 开始，按加入顺序）

```json
{
    "type": "GET_TETRAHEDRON",
    "id": 23,
    "no_result": false,
    "data": {
        "name": "Cube A",
        "tet": 0
    }
}
```

```markdown
# GET_TETRAHEDRON(id=23)
Model "Cube A" tetrahedron #0
v0 pos=(1, 1, 1, 1) color=#FFFF4040 normal=(0, 0, 0, 1)
v1 pos=(-1, 1, 1, 1) color=#FFFF4040 normal=(0, 0, 0, 1)
v2 pos=(-1, -1, 1, 1) color=#FF40FF40 normal=(0, 0, 0, 1)
v3 pos=(1, -1, -1, 1) color=#FF4040FF normal=(0, 0, 0, 1)
```

### 6.3 设置单个顶点

**SET_TETRAHEDRON_VERTEX**：修改一个四面体的一个顶点

必须包含的参数：

- name(String)：模型名称
- tet(Int)：四面体 id
- vertex(Int)：顶点编号，取 `0` `1` `2` `3`
- pos(Vector4f)：新的四维位置

可选参数：

- color(Color)：新颜色，缺省保持不变
- normal(Vector4f)：新法向量，缺省保持不变

```json
{
    "type": "SET_TETRAHEDRON_VERTEX",
    "id": 24,
    "no_result": false,
    "data": {
        "name": "Cube A",
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
"Cube A" #0 v2 = pos(0, 0, 0, 3) color=#FFFFFF00
```

### 6.4 用矩阵变换指定四面体

**TRANSFORM_TETRAHEDRONS**：对模型中的部分（或全部）四面体应用 5×5 变换矩阵

与 §4.2 的模型变换不同，这里是**直接改顶点**，模型矩阵不变。
变换在当前变换坐标系（§4.2.6）下解释。

必须包含的参数：

- name(String)：模型名称
- tets(Array)：目标四面体 id 数组，如 `[0, 2, 5]`；**空数组 `[]` 表示全部四面体**
- row0 ~ row4(Vector5f)：5×5 行主序矩阵

```json
{
    "type": "TRANSFORM_TETRAHEDRONS",
    "id": 25,
    "no_result": false,
    "data": {
        "name": "Cube A",
        "tets": [0, 1, 2],
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
"Cube A" transformed 3 tetrahedrons: [0, 1, 2]
Edits: 1
```

对整个模型应用时：

```json
{
    "type": "TRANSFORM_TETRAHEDRONS",
    "id": 26,
    "no_result": false,
    "data": {
        "name": "Cube A",
        "tets": [],
        "row0": [1, 0, 0, 0, 0],
        "row1": [0, 1, 0, 0, 0],
        "row2": [0, 0, 1, 0, 0],
        "row3": [0, 0, 0, 1, 0],
        "row4": [0, 0, 0, 0, 2]
    }
}
```

```markdown
# TRANSFORM_TETRAHEDRONS(id=26)
Success
"Cube A" transformed 48 tetrahedrons: all
Edits: 2
```

### 6.5 添加四面体

**ADD_TETRAHEDRON**：直接指定四个顶点，往模型里加一个四面体

与 §5.1.1 的 `CREATE_TETRAHEDRON` 不同，这个是自己给顶点。

必须包含的参数：

- name(String)：模型名称
- v0 ~ v3(Vertex)：四个顶点

每个 Vertex 的结构：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `pos` | Vector4f | 是 | 位置 |
| `color` | Color | 否 | 缺省用 `#FFB0B0B0`，也可用统一字段 `color` 一次性指定 |
| `normal` | Vector4f | 否 | 缺省由实现根据四面体计算 |

可选参数：

- color(Color)：统一指定四个顶点的颜色（顶点自己带 `color` 时以顶点为准）

```json
{
    "type": "ADD_TETRAHEDRON",
    "id": 27,
    "no_result": false,
    "data": {
        "name": "Cube A",
        "color": "#FF44FF44",
        "v0": { "pos": [0, 0, 0, 0] },
        "v1": { "pos": [1, 0, 0, 0] },
        "v2": { "pos": [0, 1, 0, 0] },
        "v3": { "pos": [0, 0, 1, 1], "color": "#FFFF4444" }
    }
}
```

```markdown
# ADD_TETRAHEDRON(id=27)
Successfully add tetrahedron #48 to "Cube A"
Tetrahedrons: 49
```

### 6.6 删除四面体

**REMOVE_TETRAHEDRON**：删除模型中的一个四面体

必须包含的参数：

- name(String)：模型名称
- tet(Int)：四面体 id

四面体 id 是**该模型内的索引**（从 0 开始，按加入顺序）。删除后，其后所有四面体的 id 减 1，
所以删除之后请重新用 `GET_MODEL_INFO` / `LIST_MODEL` 确认，不要沿用之前的 id。

```markdown
# REMOVE_TETRAHEDRON(id=28)
Successfully remove tetrahedron #48 from "Cube A"
Tetrahedrons: 48
```

### 6.7 超平面切片

**SLICE_MODEL**：用一个超平面把模型切成两半，各自成为新模型

必须包含的参数：

- name(String)：源模型名称
- plane(Tetrahedron)：切片超平面，由**一个四面体的四个顶点**定义，无限延伸
- destA(String)：一半模型的名称，必须不存在
- destB(String)：另一半模型的名称，必须不存在

`plane` 就是 §1.2 的 `Tetrahedron`（与 `sliceModel(name, plane, destA, destB)` 的参数一致），
四个顶点各给一个四维坐标 `pos` 即可，颜色与法向量对切片无意义：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `v0` ~ `v3` | Vertex | 是 | 四个顶点，每个只需 `pos`(Vector4f) |

四个点必须**仿射无关**（不共面），否则无法确定超平面，该条报错。

```json
{
    "type": "SLICE_MODEL",
    "id": 29,
    "no_result": false,
    "data": {
        "name": "Cube A",
        "plane": {
            "v0": { "pos": [0, 0, 0, 0] },
            "v1": { "pos": [1, 0, 0, 0] },
            "v2": { "pos": [0, 1, 0, 0] },
            "v3": { "pos": [0, 0, 1, 0] }
        },
        "destA": "Cube A W-",
        "destB": "Cube A W+"
    }
}
```

```markdown
# SLICE_MODEL(id=29)
Successfully slice "Cube A" by plane (0, 0, 0, 0) (1, 0, 0, 0) (0, 1, 0, 0) (0, 0, 1, 0)
Created "Cube A W-" (24 tetrahedrons)
Created "Cube A W+" (24 tetrahedrons)
```

语义与边界情况：

- 四面体四个顶点确定的超平面记作 `f(p) = 0`；`f(p) < 0` 的顶点归 `destA`，`f(p) > 0` 归 `destB`，`f(p) = 0` 的顶点两侧共有；
- 跨越平面的四面体：实现若支持精确切割则切成两半，否则整体复制到 A、B 两侧（退化为"按胞归属"的近似）；
- 源模型 `name` 保持不变；
- 若某一侧为空，该侧模型仍然创建（0 个四面体），并在返回里注明 `empty`。

---

## 七、文件与模型 IO

对应 `IOInterface`。

> **安全约束**：AI 不应凭空编造文件路径。`SAVE_MODEL` 只允许写到工程内的相对路径；
> `LOAD_MODEL` 的路径由解析器做白名单/沙箱校验，非法路径该条报错。

### 7.1 保存模型

**SAVE_MODEL**：把模型存成 `.4do` 文件

必须包含的参数：

- name(String)：模型名称，必须存在
- file(String)：**不包含后缀**的文件名，用相对路径

解析器自动补上 `.4do` 后缀。

```json
{
    "type": "SAVE_MODEL",
    "id": 30,
    "no_result": false,
    "data": {
        "name": "Cube A",
        "file": "models/cube_a"
    }
}
```

```markdown
# SAVE_MODEL(id=30)
Successfully save "Cube A" to models/cube_a.4do
Tetrahedrons: 48
```

### 7.2 加载模型

**LOAD_MODEL**：从 `.4do` 文件加载模型

必须包含的参数：

- name(String)：模型名称，必须不存在（避免覆盖）
- file(String)：文件路径，**带后缀**

```json
{
    "type": "LOAD_MODEL",
    "id": 31,
    "no_result": false,
    "data": {
        "name": "Imported",
        "file": "models/ball.4do"
    }
}
```

```markdown
# LOAD_MODEL(id=31)
Successfully load "Imported" from models/ball.4do
Tetrahedrons: 96
```

---

## 八、完整交互示例

### 8.1 AI 返回的 json

```json
{
    "version": 1,
    "done": false,
    "operations": [
        {
            "type": "CREATE_MODEL",
            "id": 1,
            "no_result": false,
            "data": { "name": "Tower" }
        },
        {
            "type": "CREATE_PRISM4",
            "id": 2,
            "no_result": false,
            "data": {
                "name": "Tower",
                "base": {
                    "shape": "CONE",
                    "radius": 1.0,
                    "height": 2.0,
                    "segments": 24,
                    "color": "#FF66CCFF",
                    "transforms": [
                        { "method": "ROTATE", "data": { "axis": "xz", "angle": 15 } },
                        { "method": "SCALE", "data": { "y": 1.5 } }
                    ]
                },
                "ws": -2.0,
                "we": 2.0
            }
        },
        {
            "type": "TRANSFORM_MODEL",
            "id": 3,
            "no_result": true,
            "data": {
                "name": "Tower",
                "transforms": [
                    { "method": "TRANSLATE", "data": { "w": -0.5 } },
                    { "method": "ROTATE", "data": { "axis": "xw", "angle": 20 } }
                ]
            }
        },
        {
            "type": "GET_MODEL_INFO",
            "id": 4,
            "no_result": false,
            "data": { "name": "Tower" }
        }
    ]
}
```

### 8.2 返回给 AI 的 markdown

> 其中数量为示例数值，实际以解析器输出为准。

````markdown
# CREATE_MODEL(id=1)
Successfully create "Tower"
Now models: 1

# CREATE_PRISM4(id=2)
Successfully create PRISM4 in "Tower"
Base: CONE radius=1.0 height=2.0 segments=24 (48 triangles), color=#FF66CCFF, transforms=[ROTATE, SCALE]
w range=(-2.0, 2.0)
Tetrahedrons added: 336, now 336

# GET_MODEL_INFO(id=4)
Model "Tower"
Tetrahedrons: 336
Unique vertices: 100
Edits: 0
Bound: x[-1, 1] y[-1.5, 1.5] z[-1, 1] w[-2.5, 1.5]
Note: vertex list truncated, use GET_TETRAHEDRON for details
````

（`id=3` 的操作 `no_result` 为 true，所以没有返回段落；
`TRANSFORM_MODEL` 只改模型矩阵，不算几何编辑，所以 `Edits` 仍是 0，
而包围盒给的是**变换后**的世界坐标范围。）

### 8.3 结束本轮

```json
{
    "version": 1,
    "done": true,
    "operations": []
}
```

---

## 九、附录

### 附录 A：操作名总表

| 操作名 | data 必填参数 | 可选参数 | 返回要点 |
| --- | --- | --- | --- |
| `GET_CAMERA_POS` | 无 | — | `Pos=(x, y, z, w)` |
| `MOVE_CAMERA_POS` | `axis`, `distance` | — | `Now Pos=...` |
| `SET_CAMERA_POS` | `pos` | — | `Now Pos=...` |
| `GET_CAMERA_VIEW` | 无 | — | `vx/vy/vz/vw` |
| `ROTATE_CAMERA_VIEW` | `axis`, `angle` | — | 四个基向量 |
| `SET_CAMERA_VIEW` | `vx`, `vy`, `vz`, `vw` | — | 四个基向量 |
| `LIST_MODEL` | 无 | — | 带索引的模型名列表 |
| `CREATE_MODEL` | `name` | — | 新模型名、模型总数 |
| `DELETE_MODEL` | `name` | — | 剩余模型数 |
| `COPY_MODEL` | `src`, `dst` | — | 四面体数 |
| `MERGE_MODEL` | `src1`, `src2`, `dst` | — | 四面体数 |
| `APPLY_TRANSFORM_TO_VERTEX` | `name`, `dest` | — | 四面体数 |
| `GET_MODEL_MATRIX` | `name` | — | 5 行矩阵 |
| `TRANSFORM_MODEL` | `name`, `transforms` | `apply` | 变换后的模型矩阵 |
| `CREATE_TETRAHEDRON` | `name`, `center`, `radius` | `color` | 新增四面体数 |
| `CREATE_5CELL` | `name`, `center`, `size` | `color` | 新增四面体数 |
| `CREATE_16CELL` | `name`, `center`, `radius` | `color` | 新增四面体数 |
| `CREATE_TESSERACT` | `name`, `center`, `edgeLength` | `color` | 新增四面体数 |
| `CREATE_PRISM4` | `name`, `base`, `ws`, `we` | `color` | base 摘要、w 范围、新增数 |
| `CREATE_CONE4` | `name`, `base`, `apex` | `color` | base 摘要、apex、新增数 |
| `CREATE_BALL4` | `name`, `center`, `radius` | `density`, `color` | 新增四面体数 |
| `GET_MODEL_INFO` | `name` | — | 统计 + 包围盒 |
| `GET_TETRAHEDRON` | `name`, `tet` | — | 四个顶点 |
| `SET_TETRAHEDRON_VERTEX` | `name`, `tet`, `vertex`, `pos` | `color`, `normal` | 新顶点值 |
| `TRANSFORM_TETRAHEDRONS` | `name`, `tets`, `row0`~`row4` | — | 变换的四面体数（`tets: []` = 全部） |
| `ADD_TETRAHEDRON` | `name`, `v0`~`v3` | `color` | 新四面体 id |
| `REMOVE_TETRAHEDRON` | `name`, `tet` | — | 剩余四面体数 |
| `SLICE_MODEL` | `name`, `plane`, `destA`, `destB` | — | 两个新模型与各自四面体数 |
| `SAVE_MODEL` | `name`, `file` | — | 保存路径 |
| `LOAD_MODEL` | `name`, `file` | — | 四面体数 |

### 附录 B：枚举与取值表

| 枚举 | 取值 | 使用位置 |
| --- | --- | --- |
| 单轴 `axis` | `x` `y` `z` `w` | `MOVE_CAMERA_POS`、`TRANSLATE`、`SCALE`、`CLIP` |
| 四维平面 `axis` | `xy` `xz` `xw` `yz` `yw` `zw` | `ROTATE_CAMERA_VIEW`、`ROTATE` |
| 三维平面 `axis` | `xy` `xz` `yz` | Mesh 的 `ROTATE`（§5.2.3） |
| 3D 几何体 `shape` | `SPHERE` `CUBE` `PRISM` `PYRAMID` `CONE` | `CREATE_PRISM4` / `CREATE_CONE4` 的 `base` |
| Mesh 变换 `method` | `TRANSLATE` `SCALE` `ROTATE` `MATRIX` | `base.transforms[]` |
| 模型变换 `method` | `MATRIX` `TRANSLATE` `SCALE` `ROTATE` `CLIP` `COORDINATE` | `TRANSFORM_MODEL` |

### 附录 C：与代码接口的对应与实现备注

| 备注项 | 内容 |
| --- | --- |
| 形状操作的模型名 | `ShapeInterface` 的方法都不带模型名，但 JSON 里 `name` 必填；解析器取出 `name` 校验后传入，不维护隐式的"当前模型"状态 |
| 形状操作无返回值 | `ShapeInterface` 的方法返回 `Unit`，因此返回段落里的数量信息由实现自行统计后拼装 |
| 顶点法向量 | 4D 形状的法向量由实现计算（参考 `cpu4dkt/generator/Hypercube.kt`，超立方体按胞给 4D 法向量） |
| 3D Mesh 变换落点 | `Mesh` 带一个 3D `Transform` 矩阵；`base.transforms` 可以烘焙到顶点，也可以写进该矩阵，只要在该网格嵌入 4D 之前生效即可（§5.2.5） |
| 超平面切片 | `plane` 直接就是 `sliceModel(name, plane: Tetrahedron, ...)` 里的那个四面体（4 个顶点），不额外引入法向量/偏移量表示 |
| 四面体 id | 定义为模型内索引（按加入顺序，从 0 开始），删除后会顺移 |
| 角度单位 | JSON 一律角度制，解析器负责转弧度（`Camera4D`、`Matrix5f` 内部都是弧度） |
| 旋转方向 | 左手系（§0.2），不适用右手定则；模型/几何 `ROTATE` 是"`a` 转向 `b`"为正（`Matrix5x5::rotate`），摄像机 `ROTATE_CAMERA_VIEW` 转的是基向量，符号相反（§3.2.2） |
| 颜色缺省 | 未指定颜色时用统一默认色 `#FFB0B0B0`（若实现沿用按位置着色的方案，需在系统提示词中说明） |
| `no_result` 缺省 | 缺省为 `true`，即默认不返回结果；需要结果必须显式写 `"no_result": false` |

### 附录 D：待定 / 未实现

| 项 | 说明 |
| --- | --- |
| 模型编辑的撤销与历史 | 接口中无撤销操作，`GET_MODEL_INFO` 返回的 `Edits` 仅作计数 |
| 材质与贴图 | 3D `Mesh` 支持 `Texture`/`GLMaterial`，但 4D 形状的 JSON 定义暂只开放单一顶点色 |
| UV 坐标 | 3D 基本几何体暂不生成 UV（`Vertex.uv` 置零），需要贴图时再补 |
| 更多 3D 几何体 | 当前只支持球、正方体、正棱柱、正棱锥、圆锥；圆环、胶囊等后续再加 |
| 布尔运算 / 并集 | `MERGE_MODEL` 只做简单拼接，不做几何布尔运算 |
| `SLICE_MODEL` 精确切割 | 精确切分四面体为可选实现，最低要求是"按胞归属"的近似 |
