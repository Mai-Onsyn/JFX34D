# JFX34D_AI API接口

## 一、数据格式

渲染片元采用 **四个顶点构成的四维四面体**，每个四维四面体都是四维模型的一个面

### 1.1、文件数据格式

4D模型采用与3D obj模型格式类似的纯文本顶点与面描述的方式，并进行简化

每个标签占一行，每行使用空格隔开不同数据，标签使用线性索引，不同标签不共享索引

| 标签 | 数据长度 |  数据格式   |                   描述                   |           示例            |
| :--: | :------: | :---------: | :--------------------------------------: | :-----------------------: |
|  v   |    4     |    Float    |               顶点位置坐标               |        v 1 1 0 2.5        |
|  vn  |    4     |    Float    |          顶点法向量（单位向量）          |        vn 1 0 0 0         |
|  vc  |    1     |     Int     |             顶点颜色（ARGB）             |       vn 0xFF808080       |
|  t   |    4     | Int/Int/Int | 四面体描述（引用v/vn/vc的索引，从0开始） | t 0/0/0 1/1/1 2/2/2 3/3/3 |

文件后缀名暂定为 **.4do**

### 1.2 、代码数据格式

采用 **Mesh4D** 类描述4D模型：

```kotlin
class Mesh4D(
    val tetrahedrons: List<Tetrahedron>，
    val name: String
)
```

采用 **Tetrahedron** 类描述4D四面体：
```kotlin
data class Tetrahedron(
    val v0: Vertex4D,
    val v1: Vertex4D,
    val v2: Vertex4D,
    val v3: Vertex4D
)
```

采用 **Vertex4D** 类描述4D顶点：
```kotlin
data class Vertex4D(
    val pos: Vector4f,
    val color: ColorARGB,
    val normal: Vector4f
)
```

## 二、AI交互数据格式

### 发送 markdown

AI返回json中，如果有请求获取数据，将数据按需发送给AI

```markdown
# Request
type=GET_CAMERA_POS
data=...
# Result
(x, y, z, w)
```

### 返回 json

包含一个 Int 版本键值对，一个操作数组键值对

每个操作包含一个type，用来指定操作，包含一个id，用来区分操作，然后带有该操作的具体数据（由操作定义）

```json
{
    "version": 1,
    "operations": [
        {
            "type": "MOVE_CAMERA_POS",
            "id": 1,	// 任意整数
            "axis": "x",
            "distance": -5
        },
        {
            // 其他操作
        }
    ]
}
```

**如果AI返回的数据无法解析，或格式不对，则返回：**

````markdown
# Error
You just return an error format:
```json
AI返回的数据
```
Reason:
```markdown
错误原因（解析器报出的）
```
````

markdown中，只有英文是真正发给AI的数据，中文都是注释，与AI进行数据交互也都是英文

## 三、四维摄像机

### 3.1 、位置

```kotlin
fun moveRight(d: Float)		// x+
fun moveUp(d: Float)		// y+
fun moveAna(d: Float)		// z+
fun moveForward(d: Float)	// w+
```

d为负数时向相反方向移动 比如x-

所有移动都仅相对于当前摄像机坐标系，只在位置上加减坐标轴向量

---

#### 3.1.1 获取位置

```json
{
    "type"="GET_CAMERA_POS",
    "id"=1
}
```

```markdown
# Request
type=GET_CAMERA_POS
id=1
# Result
(0, 0, 0, 0)
```

---

#### 3.1.2 移动位置

```json	
{
    "type": "MOVE_CAMERA_POS",
    "id": 1,
    "axis": "x",	// 候选：x, y, z, w
    "distance": -5.0	// 任意数字
}
```

```markdown
# Request
type=MOVE_CAMERA_POS
id=1
# Result
Success
Now Pos=(-5, 0, 0, 0)
```

#### 3.1.3 设置位置

```json
{
    "type": "SET_CAMERA_POS",
    "id": 1,
    "pos": "0, 0, 0, 0"
}
```

```markdown
# Request
type=MOVE_CAMERA_POS
id=1
# Result
Success
Now Pos=(0, 0, 0, 0)
```

---

### 3.2、视角

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

a是弧度

---

#### 3.2.1 获取视角

```json
{
    "type": "GET_CAMERA_VIEW",
    "id": 1
}
```

```markdown
# Request
type=GET_CAMERA_VIEW
id=1
# Result
vx=(1, 0, 0, 0)
vy=(0, 1, 0, 0)
vz=(0, 0, 1, 0)
vw=(0, 0, 0, 1)
```

---

#### 3.2.2 旋转视角

```json
{
    "type": "ROTATE_CAMERA_VIEW",
    "id": 1,
    "axis": "xy",	// 候选：xy, xz, xw, yz, yw, zw
    "angle": 45.0	// 任意角度（代码里用的弧度，为了AI理解，这里用角度更好）
}
```

```markdown
# Request
type=ROTATE_CAMERA_VIEW
id=1
# Result
Success
Current is:
vx=(1, 0, 0, 0)
vy=(0, 1, 0, 0)
vz=(0, 0, 1, 0)
vw=(0, 0, 0, 1)
```

---

#### 3.2.3 设置视角

```json
{
    "type": "SET_CAMERA_VIEW",
    "vx": "0 0 0 0",	// 下同，需要校验：必须是单位向量，四个向量必须互相垂直
    "vy": "0 0 0 0",
    "vz": "0 0 0 0",
    "vw": "0 0 0 0"
}
```

```markdown
# Request
type=ROTATE_CAMERA_VIEW
id=1
# Result
Success
Current is:
vx=(1, 0, 0, 0)
vy=(0, 1, 0, 0)
vz=(0, 0, 1, 0)
vw=(0, 0, 0, 1)
```

---

## 四、模型

对场景内的模型增加和删除相关，用于AI控制场景中的模型

### 4.1、模型操作

---

#### 4.1.1 查询所有模型

```json
{
    "type"="LIST_MODEL",
    "id"=1
}
```

```markdown
# Request
type=LIST_MODEL
id=1
# Result
1. "Cube A"
2. "Shape 1"
3. "Dio"
```

----

#### 4.1.2 创建模型

```json
{
    "type"="CREATE_MODEL",
    "id"=1,
    "name"="New Model"
}
```

```markdown
# Request
type=CREATE_MODE
id=1
# Result
Success
```

---

#### 4.1.3 删除模型

```json
{
    "type"="DELETE_MODEL",
    "id"=1,
    "name"="New Model"
}
```

```markdown
# Request
type=DELETE_MODE
id=1
# Result
Success
```

---

### 4.2、模型变换