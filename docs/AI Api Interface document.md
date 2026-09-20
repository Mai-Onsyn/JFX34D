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

执行与解析了AI返回的json后，将必要数据按需发送给AI

```markdown
# GET_CAMERA_POS(id=1)
返回结果
```

### 返回 json

包含一个 Int 版本键值对，一个操作数组键值对

每个操作必须包含的字段如下：
- **type**(String): 操作名称，后续返回结果也需要带有该名称
- **id**(Int): 操作id，由AI决定，用于区分不同操作，单次json返回的不同操作必须拥有不同id，多次请求可以重复id，但仍然建议使用不同id
- **data**(JSON): 该操作所需的数据

可选字段：
- **no_result**(Boolean): 不接收该条命令的执行结果，默认为true，一般不建议使用，只有在同时执行了多条类似命令时(如放置四面体)，且之后使用查询命令能够获取刚才所有操作的结果时，可以这样优化

```json
{
    "version": 1,
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

**GET_CAMERA_POS**：返回当前摄像机坐标

不需要参数

```markdown
Pos=(0, 0, 0, 0)
```

#### 3.1.2 移动位置

**MOVE_CAMERA_POS**: 按轴移动摄像机坐标

必须包含的参数：
- axis(Enum): 摄像机坐标系的坐标轴，可选值为
- distance(Float): 移动距离，正负

**axis**可选值：| x | y | z | w |

```markdown
Success
Now Pos=(-5, 0, 0, 0)
```

#### 3.1.3 设置位置

**SET_CAMERA_POS**: 直接更改摄像机坐标

必须包含的参数：
- pos(Vector4f): 四维坐标

```markdown
Success
Now Pos=(0, 0, 0, 0)
```

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

**GET_CAMERA_VIEW**: 获取摄像机的坐标系向量在世界中的指向

无参数

```markdown
vx=(1, 0, 0, 0)
vy=(0, 1, 0, 0)
vz=(0, 0, 1, 0)
vw=(0, 0, 0, 1)
```

#### 3.2.2 旋转视角

**ROTATA_CAMERA_VIEW**: 旋转摄像机视角

必须包含的参数：
- axis(Enum): 四维旋转轴(二维平面)
- angle(Float): 旋转角度 (代码里用的弧度，为了AI理解，这里用角度更好)

**axis**可选值：| xy | xz | xw | yz | yw | zw |

```markdown
Success
Current is:
vx=(1, 0, 0, 0)
vy=(0, 1, 0, 0)
vz=(0, 0, 1, 0)
vw=(0, 0, 0, 1)
```

#### 3.2.3 设置视角

**SET_CAMERA_VIEW**: 直接设置摄像机坐标系

必须包含的参数：
- vx(Vector4f)
- vy(Vector4f)
- vz(Vector4f)
- vw(Vector4f)

每个向量需要校验是否是单位向量，是否三三互相垂直

```markdown
Success
Current is:
vx=(1, 0, 0, 0)
vy=(0, 1, 0, 0)
vz=(0, 0, 1, 0)
vw=(0, 0, 0, 1)
```

## 四、模型

对场景内的模型增加和删除相关，用于AI控制场景中的模型

### 4.1、模型操作

#### 4.1.1 查询所有模型

**LIST_MODEL**: 列出当前场景中的所有模型名称(按模型索引)

无参数

```markdown
0. "Test Model"
1. "Cube A"
2. "Shape 1"
3. "Dio"
```

#### 4.1.2 创建模型

**CREATE_MODEL**: 创建一个模型，追加到场景模型列表后面

必须包含的参数：
- name(String): 模型名称

需要校验模型名称是否已被占用

```markdown
Successfully create <name>
```

#### 4.1.3 删除模型

**DELETE_MODEL**: 从场景的模型列表中删除指定名称的模型

必须包含的参数：
- name(String): 模型名称

需要校验模型名称是否存在

```markdown
Successfully delete <name>
```

#### 4.1.4 获取变换矩阵

使用操作命令
**GET_MODEL_MATRIX**: 获取模型变换矩阵

必须包含的参数：
- name(String): 目标模型名称

```markdown
row0=(1, 0, 0, 0)
row1=(0, 1, 0, 0)
row2=(0, 0, 1, 0)
row3=(0, 0, 0, 1)
```

### 4.2、模型变换

对模型的5*5 transform矩阵做操作，而不是操作模型顶点

所有的变换都使用 **TRANSFORM_MODEL** 操作名

必须包含的参数：
- name(String): 要操作的模型名称
- transforms(JSONArray): 对模型的变换操作

可选择包含的参数：
- apply(Boolean): 决定该操作是否是添加变换，默认为true，当指定false时，将用新的变换组成的矩阵覆盖模型矩阵

每个transform为一个JSON，必须包含两个字段：
- method(String): 变换方式
- data(JSON): 变换需要的数据

**示例**：

```json
[
    {
        "type": "TRANSFORM_MODEL",
        "id": 2,
        "no_result": true,
        "data": {
            "name": "Target Model",
            "apply": false,
            "transforms": [
                {
                    "method": "MATRIX",
                    "data": {
                        "row0": "(1 0 0 0 0)",
                        "row1": "(0 1 0 0 0)",
                        "row2": "(0 0 1 0 0)",
                        "row3": "(0 0 0 1 0)",
                        "row4": "(0 0 0 0 1)"
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
    },
    {
        "type": "GET_MODEL_MATRIX",
        "id": 3
    }
]
```

返回
```markdown
Applied transform (如果是设置而不是追加 用Setted)
<name> current is:
(1 0 0 0 0)
(0 1 0 0 0)
(0 0 1 0 0)
(0 0 0 1 0)
(0 0 0 0 1)
```

**单次请求的所有变换操作只返回最后完成变换后的模型矩阵**

**第4.2.x章节的操作统一为transforms的method字段，参数均为transforms内的data的参数**

#### 4.2.1 矩阵变换

**MATRIX**: 将矩阵左乘到指定模型的模型矩阵

必须包含的参数：
- row0(Vector5f): 矩阵第一行的行向量，下类同
- row1(Vectot5f)
- row2(Vectot5f)
- row3(Vectot5f)
- row4(Vectot5f)

#### 4.2.2 平移

**TRANSLATE**: 设置模型在四条坐标轴上的移动

至少包含以下参数的其中一个：
- x(Float): 在x轴上移动，下类同
- y(Float)
- z(Float)
- w(Float)

#### 4.2.3 缩放 / 镜像

**SCALE**: 设置模型在不同轴上的缩放

可单独包含该参数：
- all(Float): 四个轴上的缩放倍率

或者至少包含下列参数的其中一个：
- x(Float): 在x轴上缩放，为负值时镜像，下类同
- y(Float)
- z(Float)
- w(Float)

#### 4.2.4 旋转

**ROTATE**: 绕二维平面旋转指定角度

必须包含的参数：
- axis(Enum): 旋转轴
- angle(Float): 旋转角度

**axis**可选值：| xy | xz | xw | yz | yw | zw |

#### 4.2.5 剪切

**CLIP**: 一个轴随另一个轴偏移

必须包含的参数：
- source(Enum): 源轴
- target(Enum): 目标轴
- amount(Float): 偏移量

source/target可选值：| x | y | z | w |

#### 4.2.6 基坐标系

**COORDINATE**: 变换基坐标系，设置之后的所有变换都会围绕这个坐标系变换，默认为世界坐标系

至少包含下列参数中的其中一个：
- pos(Vector4f): 坐标系原点
- vx(Vector4f): right轴
- vy(Vector4f): up轴
- vz(Vector4f): ana轴
- vw(Vector4f): front轴

### 4.3 模型编辑

// TODO("Not implement yet")