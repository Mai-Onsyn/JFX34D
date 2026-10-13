# 数据集生成脚本（dataset）

给 JFX34D 的 AI Agent 批量出题、跑题、观察、打分、记录，最终产出一份**可量化评估模型能力**的数据集。

## 一分钟上手

在 IDE 里直接 **Run** `weilantianhai.dataset.ui.DatasetApp`。

窗口里会看到：

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
| `--tasks=<path>` | 任务清单 JSONL；缺省用**内置 14 条**最小集（同 `src/main/resources/dataset/builtin.jsonl`） |
| `--data=<dir>` | 数据集根目录；缺省 `.workbuddy/datasets` |
| `--auto` | 全自动模式：不弹打分框，直接按结构化判据落盘（适合刷量） |
| `--model=deepseek\|qwen` | 起始**被测试模型**；缺省 `deepseek` |
| `--generate` | **只出题不跑循环**：生成 JSONL 后退出（配合下面三个参数） |
| `--count=<n>` | `--generate` 的题目数量；缺省 20 |
| `--difficulty=<d>` | `simple` / `medium` / `complex` / `trap` / `mixed`；缺省 `mixed` |
| `--category=<c>` | `camera` / `camera_bookmark` / `model_tree` / `transform` / `shape` / `carve` / `composite` / `mixed`；缺省 `mixed` |

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

## 状态复位：题目之间不能有依赖

**每道题执行前都会复位**：清空模型树/机位，相机回到 `(0,0,-10,0)` 单位姿态，
放回一个固定的 seed 模型（当前是 `Tesseract`）。

这是数据集的基本原则（否则"这题失败"可能只是因为"上一题残留"），但它意味着：

> **题目之间不能有状态依赖。**

反例（曾经的内置题）：「把名为 Box 的模型改名为 Crate」——Box 来自上一题，
但上一题跑完就复位了，Box 根本不存在，**这条记录测的不是 `RENAME_MODEL`**。

正确写法是**自带 setup**：

```
「创建一个名为 Box 的空模型，然后把它改名为 Crate」   ← 建 + 改写在一句里
```

`reset()` 之后会**自检**：比对模型树/机位/相机是否都回到了基线，
不一致就抛异常（"复位没复位干净"是静默错误，会让后续所有题的结论都不可信）。

## 产出

```
.workbuddy/datasets/
├── index.jsonl                              全局索引（精简字段）
├── raw/2026-10-11/
│   ├── deepseek__camera_bookmark.jsonl       ← 按 模型__类别 分类
│   ├── deepseek__shape.jsonl
│   └── qwen__camera_bookmark.jsonl
└── summary/
    ├── compare__2026-10-11.md                 ← 模型量化评估总表
    ├── 2026-10-11__deepseek.json
    └── ...
```

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
| `dataset.tasks` | 任务定义、加载与**LLM 自动出题**（`TaskGoal` / `TaskLoader` / `TaskGenerator`） |
| `dataset.core` | 执行与快照（`AgentTaskRunner` / `SceneSnapshotter` / `CheckEvaluator` / `TaskRunRecord`） |
| `dataset.store` | 分类落盘 + **模型名归一化迁移**（`DatasetStore`，含 `main` 可直接跑） |
| `dataset.summary` | 指标聚合与报表（`MetricsAggregator`） |
| `dataset.ui` | 4D 窗口 + 打分组件 + 模型切换 + 出题对话框（`DatasetApp` / `ScorePanel` / `DatasetLauncher`） |
| `agent.trace` | 每轮轨迹（`RoundTrace` / `TraceListener`），是数据集的数据源 |
| `agent.llm.LLMClient` | LLM 调用；`withTemperature` / `withModel` 支持不改私有字段就换配置 |

## 自测

> 临时自测类已在交付前删除，避免污染 `src/main/java`。
>
> 复验方式：写一个 `main`，用 `javac`（classpath 需含 `target/classes` 与 fastjson2/joml/kotlin-stdlib）
> + `java` 跑即可（**全部不联网**）。累计两轮共 **74 条断言**：
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
8. **题目之间禁止状态依赖** —— 见上文「状态复位」一节。写题清单时最容易犯这个错。
