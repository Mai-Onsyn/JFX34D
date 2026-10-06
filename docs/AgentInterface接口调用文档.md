# AgentInterface 接口调用文档

**模块**：Agent  
**面向对象**：UI 模块  
**文档版本**：v1.6  
**更新日期**：2026-10-06

> UI 只依赖 `AgentInterface` 和 `ResponseListener` 两个类型。  
> 所有 LLM 调用、IR 解析、渲染执行、任务调度都在 Agent 内部完成，UI 不需要关心。

---

## 一、接口清单

### 1.1 生命周期

| 接口 | 参数 | 返回 | 调用时机 | 状态 |
|---|---|---|---|---|
| `initialize` | `LLMClient, CommandExecutor` | void | 启动时一次 | 已实现 |
| `getInstance` | 无 | `AgentInterface` | 初始化后任意时刻 | 已实现 |
| `start` | 无 | void | 启动后一次 | 已实现 |
| `shutdown` | 无 | void | UI 关闭时 | 已实现 |
| `isRunning` | 无 | boolean | 任意时刻 | 已实现 |

### 1.2 提交输入

| 接口 | 参数 | 返回 | 调用时机 | 状态 |
|---|---|---|---|---|
| `submitUserInput` | `String` | int（requestId） | 用户点发送 | 已实现 |

### 1.3 接收回复

| 接口 | 参数 | 返回 | 调用时机 | 状态 |
|---|---|---|---|---|
| `setResponseListener` | `ResponseListener` | void | 启动后一次 | 已实现 |
| `clearResponseListener` | 无 | void | 需要时 | 已实现 |

### 1.4 队列管理

| 接口 | 参数 | 返回 | 用途 | 状态 |
|---|---|---|---|---|
| `getPendingCount` | 无 | int | 等待队列长度 | 已实现 |
| `isBusy` | 无 | boolean | 是否正在处理任务 | 已实现 |
| `clearPending` | 无 | int | 清空等待队列，返回被清除条数 | 已实现 |
| `cancelRequest` | `int requestId` | boolean | 取消指定请求 | 已实现 |

### 1.5 状态查询

| 接口 | 参数 | 返回 | 用途 | 状态 |
|---|---|---|---|---|
| `getStatusSummary` | 无 | String | 一行状态摘要，供状态栏显示 | 已实现 |

---

## 二、任务队列

### 2.1 为什么需要队列

Agent 处理一次用户输入，要经过 **LLM 请求 → IR 解析 → 四维操作执行 → 拼装结果**，整个过程**耗时数秒**，且**必须串行**：上一条操作执行完之前，下一条不能开始，否则：

- LLM 请求会互相竞争 API 配额。
- 四维场景状态会被并发修改。
- 回调和 `currentRequestId` 会对不上。

所以 Agent 内部用一个**先进先出的任务队列**：`submitUserInput` 只入队就立即返回，worker 线程从队头取任务，**一次只处理一条**，处理完再取下一条。

### 2.2 队列的生命周期

```
用户点发送
    ↓
submitUserInput(text)   → 入队，立即返回 requestId（不阻塞 UI）
    ↓
队列（等待处理的任务，FIFO）
    ↓
worker 线程取出队头 → 处理 → 回调 onResponse / onError
    ↓
取下一 条 … 直到队列空
```

**关键点**：

- **提交即返回**：`submitUserInput` 不等待处理完成，UI 不会卡。
- **串行处理**：同时提交 3 条，按提交顺序依次处理。
- **回调异步**：每条处理完独立触发回调，用 `requestId` 对应到具体哪次提交。

### 2.3 什么时候会出现队列非空

| 场景 | 队列长度 |
|---|---|
| 用户点一次发送，等回复 | 提交时 0 → 正在执行时 0 → 回调后 0 |
| 用户连续快速点 3 次发送 | 队列里 2 条，第 1 条正在执行 |
| 用户一次说了很长一段，LLM 慢慢处理 | 提交时 1 → 执行中 0 |

**队列非空是正常现象，说明用户提交速度超过了处理速度。**

### 2.4 UI 应该怎么用队列信息

队列信息给 UI 三个用途：

**用途 1：状态栏显示**

```java
statusLabel.setText(agent.getStatusSummary());
```

返回值形如：

- `"空闲"` — 没任务，用户随便发
- `"空闲，队列 2 条"` — 没有正在处理的，有 2 条在排队
- `"处理中 (id=3)"` — id=3 正在执行
- `"处理中 (id=3)，队列 2 条"` — 正在执行 + 有排队

**用途 2：显示等待提示**

如果用户快速发了多条，UI 可以在每条气泡上显示“等待中”，直到它对应的 `requestId` 触发回调。

```java
int reqId = agent.submitUserInput(text);
// 气泡上写："等待中…"
// onResponse(reqId, ...) 时，把它改成结果
```

这正是 `ChatPanel` 现在做的——`pendingFeedback` 用 `requestId` 关联气泡，回调时更新。

**用途 3：转圈动画**

```java
if (agent.isBusy()) showSpinner(); else hideSpinner();
```

`isBusy` 为 true 时显示“AI 正在思考”，否则隐藏。

### 2.5 队列控制

**用户想撤销所有排队中的请求**：

```java
int cleared = agent.clearPending();
// 返回被清掉的条数，UI 可以提示："已取消 N 条排队"
```

**只清空等待队列，不打断正在执行的任务。** 正在跑的那条会继续完成，回调照常触发。

**用户想撤销某一条**（比如气泡上有个“取消”按钮）：

```java
boolean ok = agent.cancelRequest(requestId);
```

- 返回 `true`：从队列中移除成功。
- 返回 `false`：该请求**已经在执行中**，无法取消（或者已不存在）。

**注意**：UI 需要保存每次 `submitUserInput` 返回的 `requestId`，才能调 `cancelRequest`。`ChatPanel` 现在的 `pendingFeedback` map 的 key 就是 `requestId`，可以直接复用。

### 2.6 完整示例

用户连发三条：

```java
int id1 = agent.submitUserInput("向右移动 5");    // 入队，返回 1
int id2 = agent.submitUserInput("向上移动 2");    // 入队，返回 2
int id3 = agent.submitUserInput("向左移动 1");    // 入队，返回 3

// 此时 worker 可能已经取走 id1 开始处理
agent.getPendingCount();    // 大概率 2（id2、id3 在排队）
agent.isBusy();             // true（id1 正在执行）
agent.getStatusSummary();   // "处理中 (id=1)，队列 2 条"

// 用户后悔了，取消排队
int cleared = agent.clearPending();   // 返回 2，队列清空
agent.getPendingCount();              // 0

// id1 继续执行，完成后回调 onResponse(1, "...")
// id2、id3 永远不会被处理，也不会触发回调
```

**UI 侧要处理的问题**：id2、id3 对应的气泡不会收到回调，会一直显示“等待中”。所以调 `clearPending()` 后，**UI 应该主动把所有 `pendingFeedback` 里还没触发回调的气泡标记为“已取消”**。

### 2.7 常见误区

**误区 1：以为 `submitUserInput` 会等处理完才返回**

不是。它入队就返回，处理在后台。

**误区 2：以为队列长度是“正在处理的条数”**

不是。`getPendingCount()` 只数**等待中**的，**不含正在执行的那条**。所以“1 条正在处理 + 0 条等待”时，`getPendingCount()` 返回 0。

**误区 3：以为 `clearPending` 会中断正在执行的任务**

不会。只清空等待队列。正在跑的会跑完。

**误区 4：以为 `cancelRequest` 可以取消任何请求**

只能取消**还在队列中等待**的。已经开始执行的无法取消。

---

## 三、回调

### 3.1 为什么需要回调

Agent 处理用户输入是**异步**的：`submitUserInput` 立即返回，Agent 在后台线程处理，处理完通过回调通知 UI。

如果不走回调，UI 就得阻塞等待几秒，界面会卡死。

### 3.2 ResponseListener

UI 实现 `ResponseListener` 接口，在回调里处理 Agent 的反馈。接口有两个方法：

| 方法 | 触发时机 | 参数 |
|---|---|---|
| `onResponse` | 操作成功，返回最终反馈文本 | `int requestId`、`String text` |
| `onError` | 处理失败（LLM 超时、JSON 解析失败、执行异常） | `int requestId`、`String errorMessage` |

**onResponse 收到的是什么**：Agent 拼好的**给用户看的自然语言反馈**，例如 `"操作已执行：\n# MOVE_CAMERA_POS(id=1)\nSuccess..."`，**不是原始 Markdown，也不是 LLM 的原始 JSON**。UI 直接把它当作一段可显示的文本即可，不需要做任何解析。

**onError 收到的是什么**：一段可读的错误描述，UI 按错误样式（比如红色气泡）显示。

**回调该怎么用**：把收到的文本显示到 UI 上你想显示的地方（聊天区、日志面板、状态栏等），具体位置由 UI 决定。**唯一要注意的是，回调在 Agent 工作线程执行，不是 JavaFX 线程，更新控件前必须用 `Platform.runLater` 切回 JavaFX 线程。**

### 3.3 requestId 的作用

`requestId` 对应 `submitUserInput` 提交时返回的 id。UI 可以用它把“回复”和“提交”对应起来，尤其在用户快速连发多条输入时，避免消息错位。

---

## 四、调用示例

### 4.1 初始化

```java
// 前置：RendererInterface.Companion.init(region) 已执行
AgentInterface.initialize(
        LLMClient.deepSeek(),
        new CommandExecutor(RendererInterface.Companion.getINSTANCE())
);

AgentInterface agent = AgentInterface.getInstance();
```

**前置条件**：`RendererInterface.Companion.init(region)` 已执行，否则 `getINSTANCE()` 抛异常。

**异常处理**：初始化可能失败（配置缺失、API key 缺失），必须 catch 并提示用户。

### 4.2 注册回调

```java
agent.setResponseListener(new ResponseListener() {
    @Override
    public void onResponse(int requestId, String text) {
        Platform.runLater(() -> {
            // 把 text 按操作反馈样式显示
        });
    }

    @Override
    public void onError(int requestId, String errorMessage) {
        Platform.runLater(() -> {
            // 把 errorMessage 按错误样式显示
        });
    }
});
```

### 4.3 启动线程

```java
agent.start();
```

### 4.4 提交用户输入

```java
int requestId = agent.submitUserInput(inputField.getText());
```

提交后立即返回，结果通过回调通知。

### 4.5 查询状态

```java
statusLabel.setText(agent.getStatusSummary());
pendingLabel.setText("队列 " + agent.getPendingCount() + " 条");
if (agent.isBusy()) showSpinner(); else hideSpinner();
```

### 4.6 队列控制

```java
// 用户点"取消排队"
int cleared = agent.clearPending();
// UI 记得把 pendingFeedback 里没回调的气泡标记为"已取消"

// 用户取消某一条
boolean ok = agent.cancelRequest(requestId);
```

### 4.7 关闭

```java
agent.shutdown();
```

### 4.8 完整流程

```
1. RendererInterface.Companion.init(region)
2. AgentInterface.initialize(client, executor)    ← catch 异常
3. AgentInterface.getInstance()
4. setResponseListener(你的监听器)
5. start()
6. 用户点发送 → submitUserInput(text) → 拿到 requestId
7. Agent 后台处理
8. 回调触发 → onResponse 或 onError
9. UI 用 Platform.runLater 更新界面
10. UI 关闭 → shutdown()
```

---

## 五、线程规则

| 场景 | 规则 |
|---|---|
| 调 `submitUserInput` | 任意线程安全 |
| 调 `start` / `shutdown` | 任意线程安全 |
| 调 `getPendingCount` / `isBusy` / `getStatusSummary` | 任意线程安全 |
| 调 `clearPending` / `cancelRequest` | 任意线程安全 |
| `onResponse` / `onError` | 在 Agent 线程，UI 必须 `Platform.runLater` |
| 更新 UI 控件 | 必须在 JavaFX 线程 |

---

## 六、常见问题

**Q1：getInstance() 抛 ExceptionInInitializerError？**  
A：`initialize()` 还没调用，或调用时抛异常被 catch 了。检查初始化顺序。

**Q2：submitUserInput 调用后没反应？**  
A：`start()` 没调用。只入队没人消费。

**Q3：回调没触发？**  
A：`setResponseListener` 没注册，或 `start` 没启动。

**Q4：UI 卡住？**  
A：回调里直接更新了控件，没走 `Platform.runLater`。

**Q5：怎么切换本地模型 / DeepSeek？**  
A：改初始化参数：`LLMClient.local()` 或 `LLMClient.deepSeek()`。

**Q6：getStatusSummary() 返回值有哪些？**  
A：

- `"空闲"` — 无任务
- `"空闲，队列 N 条"` — 无正在执行，队列有 N 条等待
- `"处理中 (id=X)"` — 正在执行 id=X
- `"处理中 (id=X)，队列 N 条"` — 正在执行 + 有等待

**Q7：clearPending 会不会影响正在执行的请求？**  
A：不会。只清空等待队列，正在执行的那条会继续跑完并触发回调。

**Q8：cancelRequest 什么时候返回 false？**  
A：两种：请求已在执行中（无法取消），或请求不在队列里（已被取走或 id 不存在）。

**Q9：getPendingCount() 会数正在执行的那条吗？**  
A：不会。只数等待中的。所以“1 条正在处理”时，它返回 0。

**Q10：clearPending 后，被清掉的那几条还会回调吗？**  
A：不会。它们已经从队列移除，worker 不会处理，也不会触发 `onResponse` / `onError`。UI 需要主动把对应气泡标记为“已取消”。

---
