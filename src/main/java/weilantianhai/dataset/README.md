# 数据集生成脚本（dataset）

给 JFX34D 的 AI Agent 批量出题、跑题、观察、打分、记录，最终产出一份**可量化评估模型能力**的数据集。

## 一分钟上手

在 IDE 里直接 **Run** `weilantianhai.dataset.ui.DatasetLauncher`
（**不是** `DatasetApp` —— 原因见「注意事项 1」）。

启动后**先停在开始页，不会立刻跑题**：

```
┌─ 开始页 ───────────────────────────────────────────┐
│  JFX34D 数据集生成器                                │
│  先生成/选好题库，再开始。跑的过程中也能换。          │
│                                                     │
│  被测试模型  [ deepseek          ▾ ]                │
│  题库        [ builtin.jsonl     ▾ ]  14 题          │
│  数据集目录  [ data/dataset        ]                │
│  ☑ 写入数据集      关闭 = 只跑不落盘（试跑）         │
│  ☐ 题目之间保留场景 开启 = 下一题接着上一题          │
│                                                     │
│        [ 开 始 ]    [ 出题… ]                        │
└─────────────────────────────────────────────────────┘
```

- 点「**出题…**」可以**在开始之前**先让 LLM 生成一份题库；生成完它会自动出现在下拉框里并选中。
- 「**写入数据集**」是个开关：关掉就只跑不落盘（试跑，不打脏已有数据集）。
- **复位**、**初始场景** 两个开关在**顶栏**（开始页只盖住 4D 视口，顶栏一直可用）——
  顶栏那个「存为初始场景」按钮能把当前场景定为起点，详见下文「复位到哪：三种模式」。
- 开始页**只盖住中间的 4D 视口**，顶栏与底部日志都照常可用。

点「开始」后才进入执行页：

```
┌──────────────────────────────────────────────────────────┐
│ 任务 3/14 ｜ camera_bookmark ｜ 正在执行…                  │
├──────────────────────────────────────────────────────────┤
│ ┌ 相机机位 ┐                            ┌ 数据集打分 ────┐ │
│ │ [0] 俯视 │                            │ [3] 创建一个机 │ │
│ │          │        （4D 视口）          │ 位…            │ │
│ │          │                            │ 自动判据：通过 │ │
│ │          │                            │  ( )正确 ( )否 │ │
│ │          │                            │ 评语:[       ] │ │
│ │          │                            │ [提交][跳过]   │ │
│ └──────────┘                            └────────────────┘ │
├──────────────────────────────────────────────────────────┤
│ [save] task=3 verdict=correct rounds=1 4.2s               │
└──────────────────────────────────────────────────────────┘
```

每道题：脚本复位场景 → 喂给 AI → 你在窗口里看结果对不对 → 右上角打分 → 提交 → 落盘 → 下一题。

## 命令行参数

| 参数 | 说明 |
|---|---|
| `--bank=<name>` | **题库名**：`data/tasks/` 下的文件名，或 `"(内置 14 题)"`；缺省内置 |
| `--tasks=<path>` | 直接指定题库文件路径（优先级高于 `--bank`，同时作为 `--generate` 的输出路径） |
| `--data=<dir>` | 数据集根目录；缺省 `data/dataset`（**不再是** `.workbuddy/datasets`） |
| `--auto` | 全自动模式：不弹打分框，直接按结构化判据落盘（适合刷量） |
| `--model=deepseek\|qwen` | 起始**被测试模型**；缺省 `deepseek` |
| `--no-start-page` | 跳过开始页直接开跑（给自动/批量场景用） |
| `--keep-scene` | **完全不复位**：各题接着上一题继续（最自由，但各题起点不再一致） |
| `--baseline` | 复位到**初始场景**（`data/scene/baseline`，需先在界面里点过「存为初始场景」） |
| `--generate` | **只出题不跑循环**：生成 JSONL 后退出（配合下面三个参数） |
| `--count=<n>` | `--generate` 的题目数量；缺省 20 |
| `--difficulty=<d>` | `simple` / `medium` / `complex` / `trap` / `mixed`；缺省 `mixed` |
| `--category=<c>` | `camera` / `camera_bookmark` / `model_tree` / `transform` / `shape` / `carve` / `composite` / `mixed`；缺省 `mixed` |

## 题库：生成、切换、多套并存

题库就是 `data/tasks/` 下的一个个 `*.jsonl`。程序把它们当作**可切换的题集**：

- 开始页的「题库」下拉框列出全部题库（外加「(内置 14 题)」这一项）；
- 执行页顶栏也有同一个下拉框，**题目之间随时换，下一题生效**（当前题不打断）；
- 「出题…」生成的每份题库都是一个新文件（`generated-<时间戳>.jsonl`），生成完自动选中。

首次启动会把内置 14 题**导出**成 `data/tasks/builtin.jsonl`（带注释头，说明两条硬规则），
想手写题库就直接改它或另存一份 —— 不用去翻 Java 源码里的 `TaskLoader.builtin()`。

> 空题库不会出现在下拉框里（选了会跑 0 题，看起来像坏了）。

### 命名与编号（两条容易踩的规则）

**① 同名不顶掉，会自动岔开。** 文件名是 `<你输入的名字>-<yyyyMMdd-HHmmss>.jsonl`。
时间戳精度只到**秒**，所以同一秒内连点两次「开始生成」就会撞名 ——
而落盘走的是 `Files.writeString`（截断覆盖），撞名就会**静默顶掉**前一次的结果。
因此 `newBankFile` 在时间戳之外还查一遍：目标已存在就依次试 `-2`、`-3`… 找第一个空位。

```
selftest-tmp-20261013-223321.jsonl
selftest-tmp-20261013-223321-2.jsonl     ← 同一秒内的第二次
selftest-tmp-20261013-223321-3.jsonl
```

名字里的 `/ \ 空格` 等会换成下划线（用户能直接输这个名字，不能让它跑到别的目录去）。

**② 新题库从 1 号开始。** 起始题号看的是**输出文件**，不是"当前选中的题库"：

| 情况 | 起始号 |
|---|---|
| 写进一个**全新**的题库文件 | `1` |
| 写进一个**已存在**的文件（`--generate --tasks=已有.jsonl`） | 该文件最大号 + 1 |

早期版本拿"当前选中的题库"当基准，于是用内置题的 1~14 打底，新题库一律从 **15** 起 ——
新题集里凭空多出一段编号，看起来像丢了十几道题。

## 「写入数据集」开关

开始页与顶栏各有一个开关，绑定的是**同一个属性**，在哪边拨都立刻生效。

| 状态 | 行为 |
|---|---|
| **开**（默认） | 正常落盘：写 `raw/`、追加 `index.jsonl`、跑完写 `summary/` |
| **关** | **试跑**：题目照跑、照打分、照看 trace，但**什么都不写**；汇总只打到日志里（拿本次内存记录算），并弹窗提醒「未落盘」 |

用途：调题库措辞、试新模型、验证某类题能不能自动判 —— 这些探索不该污染已经攒好的数据集。

## AI 读数据集（两级接口）

数据集不只是给人看的，也**开放给被测试的 AI 查**。两个操作（详见 `system.md` §八）：

| 操作 | data | 返回 |
|---|---|---|
| `LIST_DATASET_CATEGORIES` | 无 | **分类目录**：每类的记录数、判对数、涉及模型 |
| `GET_DATASET_RECORDS` | `category`(必填)、`model`、`verdict`、`limit`(默认 5，上限 50) | 该类的记录：题目 + 人工结论 + 人工评语 + 当时的回答 |

设计成两级是刻意的：一次性灌整个数据集既浪费上下文又全是噪声，
先看目录再挑一类，才是"我这类题以前怎么做的 / 哪里容易错"的自然问法。

**有意不返回 `check`** —— 它是机器判定的**标准答案**，`note` 还常写着"陷阱题：应当拒绝"。
回给正在做题的模型就等于泄题，评测也就没意义了。只回**题目 + 人工结论 + 人工评语**。

实现见 `agent/ir/operations/DatasetQuery.java`（只读 `data/dataset/raw/`，不碰渲染器）。
默认根目录锚到**项目根**（向上找 `pom.xml`），可用 `-Djfx34d.dataset.root=<绝对路径>` 覆盖。

## 切换被测试模型

窗口顶栏右侧有「被测试模型」下拉框，可在 **DeepSeek** 与 **本地 Qwen（LM Studio，`http://localhost:1234/v1`）** 之间切换。

- 切换是**任务之间**生效的：`AgentInterfaceImpl` 每一轮开头会快照一次 client，
  所以正在跑的轮次仍用旧模型，不会半路换签证。
- 每条记录都会带上**当时实际使用的模型**（`env.model` 为 `deepseek` / `qwen`，
  `env.api_model` 为如 `qwen/qwen3.5-9b` 的具体 API 名），
  所以中途切模型也不会把统计搅浑。
- 汇总表按 `env.model` 分组，两个模型会各占一行。

## LLM 自动出题

三种用法，效果一样，都是让模型基于**真实注册的操作清单**出题：

| 方式 | 操作 |
|---|---|
| 窗口按钮 | 顶栏「出题…」→ 选数量/难度/类别 → 开始生成（后台线程，不卡界面） |
| 命令行 | `--generate --count=40 --difficulty=complex --category=model_tree --tasks=out.jsonl` |
| 代码 | `new TaskGenerator(client, options).generate(startId)` |

### 难度分级

| 难度 | 说明 | 例子 |
|---|---|---|
| `simple` | 单步操作，参数照抄 | 「创建一个叫 Box 的空模型」 |
| `medium` | 2~3 步，通常需要**先查询再操作** | 「先列出所有模型，然后把 Tesseract 隐藏」 |
| `complex` | 多步且**后一步依赖前一步结果**，涉及路径寻址 / 坐标计算 / 批量 | 「建 Tower，内含 Base 与 Top，各沿 w 轴平移 ±2，最后把 Tower 的子模型全合并」 |
| `trap` | **故意不可能或语义冲突**，考察模型会不会正确地失败而不硬造 | 「创建机位，位置 (0,0,0,0) 看向 (0,0,0,0)」（零向量视线，应被拒绝） |

`mixed` 的配比是 simple 25% / medium 35% / complex 30% / trap 10%。

### 出题器怎么保证「题目是能做的」

1. **操作清单从注册表实时导出** —— `OperationRegistry` 没有「列出所有 type」的 API，
   所以 `TaskGenerator.operationCatalog()` 逐个 `creat(type)` 探测已知类型，
   注册表增删操作时出题清单会自动跟随，不会给出引擎里不存在的操作。
2. **出题 prompt 与 agent 的 `system.md` 隔离** —— 若把 agent 的 IR 格式喂给模型，
   它会不自觉地把「答案格式」写进题目里。所以出题器用一套**独立 prompt**，
   只告诉它有哪些操作、要什么参数、以及题目的 JSON 格式。
3. **可完成性自检** —— 生成后再让模型逐条判断「只靠这些操作能否完成」，
   判为 `false` 的整条丢弃（`--generate` 默认开启；`Options.selfFilter` 可关）。
   自检时把温度压到 0 以保确定性。
4. **要求题目自带 setup** —— 题目里出现的模型必须是**这道题自己创建的**，
   把「创建」和后续「修改/读取」写在同一条 prompt 里。
   这样不会出现「把 Box 平移 5」而 Box 从未被创建这种**根本做不了**的题。
   （不依赖任何"复位后场景里有什么"的假设 —— 起点可能是空场景，也可能是你存的初始场景。）

### 出题器产出的 JSONL 长这样

```json
{"id": 21, "category": "model_tree", "prompt": "创建一个叫 Tower 的模型，在里面创建一个边长为 1 的超立方体 Base，把它沿 w 轴平移 2", "check": "model:Tower/Base exists", "note": "LLM 出题（难度 complex）"}
```

注意 `check` 只能写**你确实能算准**的断言 —— 写错了会误判，
不确定的题就留空交给人工打分。

## 任务格式（JSONL）

```json
{"id": 3, "category": "camera_bookmark", "prompt": "创建一个机位叫「俯视原点」，位置 (0,10,0,0) 看向原点", "check": "bookmark:俯视原点 exists; bookmark:俯视原点 pos ≈ [0,10,0,0]"}
```

- `category` 与 `OperationRegistry` 的六大类对齐，决定落盘到哪个文件。
- `check` 是**可机器判定的断言组**（`;` 分隔），见下。留空表示只能人工打分。

### 判据语法

```
-- 场景类：看"世界变成了什么"
bookmark:<名字> exists              机位存在
bookmark:<名字> pos ≈ [x,y,z,w]     机位位置（容差 1e-3）
bookmark:<名字> absent              机位不存在
model:<路径> exists                 模型路径存在
model:<路径> absent                 模型路径不存在
model:<路径> subtree_exists         该路径**或其任一后代**存在（合并类操作用）
model:<路径> vertices == N          模型顶点数
model:<路径> tetrahedrons == N      模型四面体数
camera.pos ≈ [x,y,z,w]              相机位置
bookmark.count == N                 机位总数
model.count == N                    模型总数

-- 文本类：看 AI"说了什么"（纯查询题）
reply contains <片段>               回复里出现该子串（自动归一化空白与全角标点）
reply matches <正则>                回复匹配该正则

-- 反向类：看 AI"有没有正确地失败"（陷阱题）
reject with <片段>                  该任务**应当失败**，且回执里出现该片段
```

**三类判据的取舍**：

| 题型 | 用什么 | 为什么 |
|---|---|---|
| 改变世界的操作 | `model:` / `bookmark:` / `camera.pos` | 末态快照能客观反映 |
| 纯查询（只是 GET） | `reply contains` / `reply matches` | 世界没变，快照恒等，只能看回答 |
| 陷阱（应当拒绝） | `reject with` | **语义相反**：报错才是通过；硬造结果判不通过 |

自动判据**只做预填建议**，人工打分可以覆盖它。陷阱题在打分面板上会额外标注「应当拒绝，拒绝=正确」。

## 模型标识 vs API 模型名（重要）

两者**必须分开**，混用会让同一个模型在统计里裂成两行：

| 概念 | 取值 | 存在哪 | 用途 |
|---|---|---|---|
| **分组标识** | `deepseek` / `qwen`（受控词表） | 文件名、`env.model`、汇总表 `模型` 列 | 分组统计 |
| **API 模型名** | `deepseek-flash` / `qwen/qwen3.5-9b` | `env.api_model` | 追溯实际请求 |

**踩过的坑**：早期版本把 `deepseek.properties` 的 `model=`（`deepseek-flash`）当分组标识用，
后来改成下拉框 key（`deepseek`），于是汇总表出现 `deepseek` 与 `deepseek-flash` 两行、
样本数各算一半。

### 如果历史数据已经裂了：跑一次归一化

```bash
java -cp <classpath> weilantianhai.dataset.store.DatasetStore .workbuddy/datasets
```

它会把 `env.model` 里的 API 名按内置别名表归一到分组标识
（`deepseek-flash`/`deepseek-v4-flash`/`deepseek-v4.1-flash`/`deepseek-chat` → `deepseek`；
`qwen/qwen3.5-9b`/`qwen3.5-9b` → `qwen`），**并把原值补写进 `env.api_model`（信息不丢）**。

安全性：先全部读进内存确认，再整体重写；**原 raw 先挪到 `<root>/.migrated-backup-<ts>/`**
（注意备份在 `raw/` **之外** —— `loadAllRaw()` 会递归扫 `raw/`，备份留在里面会被重复读，条数翻倍）。
迁移后 `index.jsonl` 会自动重建。

## 复位到哪：三种模式

**每题开始前复位**是数据集的基本原则 —— 否则"这题失败"可能只是因为"上一题残留"。
但"起点一致"不等于"非得是空场景"。顶栏两个开关组合出三种模式：

| 保留上一题场景 | 复位到初始场景 | 每题开始时 |
|---|---|---|
| ☐ 关 | ☐ 关 | 清成**空场景**（什么都不塞） |
| ☐ 关 | ☑ 开 | 回到 `data/scene/baseline` 里存的那份场景 |
| ☑ 开 | — | **完全不复位**，接着上一题继续 |

### 「存为初始场景」——测复杂操作的关键

顶栏有个「**存为初始场景**」按钮：把**当前场景**存下来当起点。
存过之后，「复位到初始场景」默认开启，之后每题都从这份场景开始。

于是想测复杂操作时不必每道题都自带 setup：

1. 先跑一个「把场景搭好」的任务（记得关掉「写入数据集」，别落盘）；
2. 看着场景里东西搭齐了，点「存为初始场景」；
3. 换到你真正的测试题库开跑 —— 每题都从这份场景出发。

典型场景：先建一个带 `Base`/`Top` 子模型的 `Tower` 并存下来，
之后就能连着测 `TRANSFORM_MODEL` / `MERGE_ALL_SUB_MODELS` / `SLICE_MODEL` 等等，
而不用每道题都重复一遍"先建 Tower"。

> 场景存在 `data/scene/baseline/`（每个根模型一个 `.4do` + 一份 `meta.json`）。
> 可用 `-Djfx34d.baseline.dir=<绝对路径>` 换到别处 —— 想保留多套初始场景时有用。

**存读格式的已知损耗**（都是引擎 `.4do` 格式本身决定的，不是 bug）：

- 读回来一律是 `CARVED`（顶点已烘焙）—— **形状参数丢了**，
  复位后 `GET_MODEL_INFO` 看到的不再是原来的类型与参数；
- 模型自己的变换矩阵会被**烘进顶点**，读回来是单位矩阵 ——
  别拿初始场景里的模型去比对 `GET_MODEL_MATRIX` 的原始值；
- **没有四面体的模型（空 GROUP 容器）不落盘**，自然也不会被恢复
  （子模型照常恢复，路径不受影响，因为 `Mesh4D.name` 就是完整路径）。

### 不再塞那个 `Tesseract` 了

早先复位会放回一个 `Tesseract`。那个凭空多出来的模型一路带偏：
`LIST_MODEL` 里多一个用户没建的东西、`model.count` 得按"1 + 新建数"算、
出题器要专门声明它的存在、提示词又得补一条"模型不存在怎么办"。

现在复位**只做减法**（清空）或**回到你存的场景**，不再无中生有。
于是内置题里 `model.count` 就按真实的场景算（6/7 号题已相应改过）。

### 复位自检

`reset()` 之后立刻采快照与**期望状态**比对（空场景 → 空表；初始场景 → `meta` 里记的那些），
不一致就抛异常 —— 因为"复位没复位干净"是**静默**错误（界面上看不出来），
会让后续所有题的结论都不可信。

注意：开了「保留上一题场景」就没有复位，也就没有这道自检 —— 它本来就是为"每题复位"设计的。

### 题目之间仍然默认不能有状态依赖

只要还用着复位（前两种模式），题目之间就**不能互相依赖**：

反例（曾经的内置题）：「把名为 Box 的模型改名为 Crate」——Box 来自上一题，
但上一题跑完就复位了，Box 根本不存在，**这条记录测的不是 `RENAME_MODEL`**。

正确写法是**自带 setup**：

```
「创建一个名为 Box 的空模型，然后把它改名为 Crate」   ← 建 + 改写在一句里
```

（如果这类 setup 很长很琐碎，正是该用**初始场景**的时候：把它一次搭好、存下来。）

## 产出

数据集与题库都在项目根的 **`data/`** 下（**不在** `.workbuddy/` —— 那是工具自己的临时目录，
已在 `.gitignore` 里；而数据集是要归档、要对比、要交给别人的实验产物）：

```
<项目根>/
├── data/
│   ├── dataset/                               ← 数据集（要提交、要归档）
│   │   ├── index.jsonl                        全局索引（精简字段）
│   │   ├── raw/2026-10-11/
│   │   │   ├── deepseek__camera_bookmark.jsonl   ← 按 模型__类别 分类
│   │   │   ├── deepseek__shape.jsonl
│   │   │   └── qwen__camera_bookmark.jsonl
│   │   └── summary/
│   │       ├── compare__2026-10-11.md            ← 模型量化评估总表
│   │       ├── 2026-10-11__deepseek.json
│   │       └── ...
│   └── tasks/                                 ← 题库（可多套并存）
│       ├── builtin.jsonl                       内置 14 题（首次启动自动导出）
│       └── generated-20261013-215033.jsonl     LLM 出题器的产物
│   └── scene/baseline/                        ← 初始场景（「存为初始场景」的产物）
│       ├── Tower.4do                           每个根模型一个文件
│       └── meta.json                           保存时间 + 会被恢复的模型列表
└── src/main/resources/dataset/builtin.jsonl   ← 内置题的**源码版**（兜底，不再直接写这里）
```

路径统一由 `weilantianhai.dataset.DatasetPaths` 定义，别在别处写死字符串。

> 文件名与汇总表用的是**模型标识** `deepseek` / `qwen`（即下拉框取值、`env.model`），
> 而具体 API 名（如 `qwen/qwen3.5-9b`）记在 `env.api_model` 里 ——
> 免得同一个模型改个 API 名就把统计拆成两行。

### 汇总表长这样

| 模型 | API 模型 | 解析成功率 | 类型匹配率 | 数据匹配率 | 平均轮数 | 平均耗时 | 任务正确率 | 样本数 | 待人工 |
|---|---|---|---|---|---|---|---|---|---|
| deepseek | deepseek-flash | 100.0% | 100.0% | 86.2% | 2.32 | 2.0s | 93.3% | 25 | 10 |
| qwen | qwen/qwen3.5-9b | 93.5% | 89.7% | 81.0% | 3.40 | 5.1s | 71.3% | 25 | 10 |

**指标口径（漏斗式分母，重要）**：

```
所有轮
  └─ 解析成功   parse_success_rate = 解析成功轮 / 所有轮
       └─ 类型匹配   type_match_rate = 类型匹配轮 / 解析成功轮
            └─ 数据匹配   data_match_rate = 数据匹配轮 / 类型匹配轮
```

分母逐级收窄。若统一用总轮数当分母，解析失败会同时惩罚后两级，还会出现「类型率 > 解析率」的荒谬结果。

**`任务正确率` 的分母是「有结构化判据的任务数」，不是样本数** ——
无判据的题（只能人工主观打分）既不该算对也不该算错，它们的数量单独用 `待人工` 列暴露。
不这样区分的话，10 道主观题会把 25 道样本的正确率硬拉到 60%。

三个基础标记全部**机械判定**，不需要人打分：

| 标记 | 判定来源 |
|---|---|
| `parse_ok` | `IRParser.parseAsBundle` 是否抛异常 |
| `type_ok` | 从回执里反推「参数错 / 未知类型」，见 `TraceAnalysis` |
| `exec_ok` | 从回执里反推「执行期业务错」，如模型不存在 |

## 模块结构

| 包 | 职责 |
|---|---|
| `dataset.DatasetPaths` | **路径总表**：`data/dataset`、`data/tasks`，以及"锚到项目根"的逻辑 |
| `dataset.tasks` | 任务定义、加载、**题库管理**与**LLM 自动出题**（`TaskGoal` / `TaskBanks` / `TaskLoader` / `TaskGenerator`） |
| `dataset.core` | 执行与快照（`AgentTaskRunner` / `SceneSnapshotter` / **`SceneBaseline`** / `CheckEvaluator` / `TaskRunRecord`） |
| `dataset.store` | 分类落盘 + **模型名归一化迁移**（`DatasetStore`，含 `main` 可直接跑） |
| `dataset.summary` | 指标聚合与报表（`MetricsAggregator`） |
| `dataset.ui` | 4D 窗口 + 打分组件 + **开始页** + 模型/题库切换（`DatasetApp` / `StartPanel` / `ScorePanel` / `DatasetLauncher`） |
| `agent.trace` | 每轮轨迹（`RoundTrace` / `TraceListener`），是数据集的数据源 |
| `agent.llm.LLMClient` | LLM 调用；`withTemperature` / `withModel` 支持不改私有字段就换配置 |
| `agent.ir.operations.DatasetQuery` | **AI 读数据集**的两级接口（`LIST_DATASET_CATEGORIES` / `GET_DATASET_RECORDS`） |

## 自测

> 临时自测类已在交付前删除，避免污染 `src/main/java`。
>
> 复验方式：写一个 `main`，用 `javac`（classpath 需含 `target/classes` 与 fastjson2/joml/kotlin-stdlib）
> + `java` 跑即可（**全部不联网**）。累计五轮共 **137 条断言**：
>
> **第一轮（37 条）**
> 1. `LLMClient.withTemperature` / `withModel` 的不可变性与字段保留
> 2. `TaskGenerator.extractJsonArray` 的边角：markdown 围栏、前后废话、嵌套数组、
>    字符串内的 `[` `]`、转义引号、无数组、`null` 输入
> 3. `CheckEvaluator` 的 11 种场景判据（含 `vertices == N` 的 `indexOf` 分割点、一票否决、`absent`）
> 4. `MetricsAggregator` 的漏斗口径与 Markdown 表头
>
> **第二轮（37 条，10-11 下午）**
> 5. `reply contains` / `reply matches`：含全角标点归一化、无回复文本 → 无法判定
> 6. `reject with`：正确拒绝、硬造结果、理由不匹配、无回执 → 无法判定
> 7. `model:<路径> subtree_exists`：自身/后代/不存在三种
> 8. **`正确率分母只算有判据的题`**（3 条样本里 1 条无判据 → 分母 2 而非 3）
> 9. **模型归一化迁移**：端到端跑一遍 `normalizeModels`，验证条数不翻倍、
>    `env.model` 全改掉、原值进 `api_model`、备份不在 `raw/` 内
>
> **第三轮（35 条，10-13）—— 题库与数据集查询**
> 10. `DatasetPaths` 常量、锚到项目根、默认 store 不再指向 `.workbuddy`
> 11. `TaskBanks`：题库列举、路径解析、空/坏题库不列出、注释头可读、`newBankFile` 命名
> 12. **数据集两级接口**：注册表 39 个操作、分类目录统计口径、按 `category`/`model`/`verdict`/`limit` 过滤
> 13. **`check` 不泄漏**（标准答案不进返回）、`note` 不泄漏、`.` 开头的备份目录不被读
> 14. 容错：类别不存在时列出可用类别、缺 `category` 报错而非崩溃、空数据集给出解释
>
> **第四轮（14 条，10-13）—— 题库命名与编号**
> 15. **同名不顶掉**：连续 5 次生成拿到 5 个互不相同的名字（同一秒内自动岔到 `-2`…`-5`），
>     且每次返回的都是**空位**（`Files.exists` 为 false）
> 16. 前缀安全化：`a/b\c d` 不跑出 `data/tasks`、"空白/null 前缀"回退为 `generated`
> 17. `saveAsBank` 存下后能读回、并出现在题库下拉列表里
>
> **第五轮（14 条，10-13）—— 去掉 seed 模型 + 初始场景**
> 18. 内置题库在取消 `Tesseract` 后仍自洽：14 题、每题有判据、
>     **没有任何题再引用 Tesseract**、`model.count == 1`（按空场景算）、纯查询题自带 setup
> 19. 初始场景：目录可被 `-Djfx34d.baseline.dir` 覆盖、未保存时 `exists()=false`、
>     能从 `meta.json` 读出期望模型、`describe()` 带保存时间与模型数
> 20. 坏 `meta.json` → `expectedModels()` 返回空表而不是抛异常

## 注意事项

1. **必须在 IDE / Maven 里运行** —— 渲染侧是 Kotlin，且 `target/classes` 的 UI 部分是旧布局，`javac` 手搓 classpath 跑不起来。
   启动请用 `DatasetLauncher`（不是 `DatasetApp`）—— 主类继承 `Application` 会在 classpath 模式下报「缺少 JavaFX 运行时组件」。
2. **不要同时开 ChatPanel** —— `setResponseListener` 是「最后一个生效」，两边会互相覆盖。
3. **重建一次项目** —— `CameraRecord.lookAt` 依赖 Kotlin 的 `Camera4DUtilsKt.lookAt4D`，若没编译进 `target/classes` 会 `NoClassDefFoundError`。
4. **自动判据是启发式的** —— 它给的是建议值，`scorer` 字段区分 `auto` / `human`，两者建议分开统计。
5. **本地 Qwen 要先起 LM Studio** —— 默认地址 `http://localhost:1234/v1`、模型名 `qwen/qwen3.5-9b`；
   没起服务时切换过去会在第一轮报连接错误，记录里会落成 `ERROR`。
6. **出题质量取决于出题模型** —— 出题器用的就是当前下拉框选中的模型。
   小模型出 `complex` 题容易跑偏（步骤自相矛盾），建议**用 DeepSeek 出题、再用两个模型分别跑题**。
7. **配置文件里的模型名和你以为的可能不一样** —— 核对 `src/main/resources/deepseek.properties`
   的 `model=`。当前写的是 `deepseek-flash`；若 DeepSeek 那边实际只认 `deepseek-v4.1-flash`，
   请改配置（`LLMClient` 会原样把它发给 API）。**改名后不用动数据集** ——
   分组标识是 `deepseek`（受控词表），API 名只进 `env.api_model`。
8. **默认每题复位，所以题目之间不能有状态依赖** —— 见上文「复位到哪：三种模式」一节。
   要跨题引用就开「保留上一题场景」；setup 太长就把它做成**初始场景**存下来。
   写题清单时最容易犯这个错。
9. **数据集与题库在 `data/`，是要提交的实验产物** —— 别把它们塞回 `.workbuddy/`（那是被
   `.gitignore` 忽略的工具目录，东西会被当缓存清掉、也拿不出去）。`.gitignore` 里只额外忽略了
   迁移时产生的 `data/dataset/.migrated-backup-*/`。
10. **打包成 JAR 后 `data/` 不会自动跟着走** —— 系统提示词在 classpath 里（进 jar），
    而 `data/` 是文件系统目录（不进 jar）。见下面一节。

## 打包后，提示词与数据集怎么被读到

两者走的是**完全不同的两条路**，这是打包时最容易踩的坑。

### 系统提示词 —— 在 classpath 里，自动跟着走

`PromptLoader`（`agent/llm/PromptLoader.java`）是这么读的：

```java
try (InputStream in = PromptLoader.class.getResourceAsStream("/prompts/system.md")) { ... }
```

`getResourceAsStream` 查的是**类路径**。Maven 会把 `src/main/resources/**` 原样打进
`target/classes`（开发时）或 JAR 内（打包后），所以：

> **`system.md` 在打包后位于 `jar!/prompts/system.md`，`getResourceAsStream` 照样读得到。
> 不需要任何额外处理，改了它重新打包即可。**

（`resources/dataset/builtin.jsonl` 同理 —— 它也在 classpath 里。但注意它只是**兜底源码版**，
运行时真正读的是 `data/tasks/`。）

### 数据集与题库 —— 在文件系统里，不进 jar

`data/dataset`、`data/tasks` 是 `Path.of("data", "...")` 这类**文件系统路径**，
既不在 classpath 里，也不会被 Maven 打进 JAR。所以打包后：

- **读不到**：从别的工作目录启动 → 相对路径 `data/` 落空 → 数据集接口返回"空数据集"；
- **写不进**：JAR 内部的资源是只读的，就算硬塞进去也写不了。

三个可选做法：

| 做法 | 说明 | 适用 |
|---|---|---|
| **外部数据目录**（推荐） | 把 `data/` 放在 jar 旁边，启动时保证工作目录指向它，或用 `-Djfx34d.dataset.root=<绝对路径>` / `--data=<dir>` 指定 | 数据集本来就要能直接看、直接改、单独归档 —— 塞进 jar 反而取不出来 |
| **只读资源** | 把题库放进 `src/main/resources/` 用 `getResourceAsStream` 读（像 `system.md`），但**写不了**，产出仍要落到外部目录 | 只需要一份"随程序分发的固定题集" |
| **shade / fat jar** | 只影响**类与 classpath 资源**，默认**不会**把 `data/` 打进去（除非显式配 `<resource>`） | 别指望它顺手带上 `data/` |

### 还有一个更硬的前提

`DatasetApp` 依赖 **JavaFX + OpenGL（OpenGLFX）+ 渲染侧 Kotlin**，
不是随手 `java -jar` 就能跑的：要有 JavaFX 运行时、OpenGL 上下文与 native 库
（`jni/*.dll`），窗口尺寸/线程也都在 FX 线程上。
真要"打包成可运行程序"，这些得一并处理 —— 相比之下，提示词和 `data/` 反而是最简单的两块。
