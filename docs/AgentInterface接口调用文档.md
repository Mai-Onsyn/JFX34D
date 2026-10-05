# AgentInterface 接口调用文档

**模块**：Agent  
**面向对象**：UI 模块  
**文档版本**：v1.5  
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

## 二、回调

### 2.1 为什么需要回调

Agent 处理用户输入是**异步**的：`submitUserInput` 立即返回，Agent 在后台线程处理，处理完通过回调通知 UI。

如果不走回调，UI 就得阻塞等待几秒，界面会卡死。

### 2.2 ResponseListener

UI 实现 `ResponseListener` 接口，在回调里处理 Agent 的反馈。接口有两个方法：

| 方法 | 触发时机 | 参数 |
|---|---|---|
| `onResponse` | 操作成功，返回最终反馈文本 | `int requestId`、`String text` |
| `onError` | 处理失败（LLM 超时、JSON 解析失败、执行异常） | `int requestId`、`String errorMessage` |

**onResponse 收到的是什么**：Agent 拼好的**给用户看的自然语言反馈**，例如 `"操作已执行：\n# MOVE_CAMERA_POS(id=1)\nSuccess..."`，**不是原始 Markdown，也不是 LLM 的原始 JSON**。UI 直接把它当作一段可显示的文本即可，不需要做任何解析。

**onError 收到的是什么**：一段可读的错误描述，UI 按错误样式（比如红色气泡）显示。

**回调该怎么用**：把收到的文本显示到 UI 上你想显示的地方（聊天区、日志面板、状态栏等），具体位置由 UI 决定。**唯一要注意的是，回调在 Agent 工作线程执行，不是 JavaFX 线程，更新控件前必须用 `Platform.runLater` 切回 JavaFX 线程。**

### 2.3 requestId 的作用

`requestId` 对应 `submitUserInput` 提交时返回的 id。UI 可以用它把“回复”和“提交”对应起来，尤其在用户快速连发多条输入时，避免消息错位。

---

## 三、调用示例

### 3.1 初始化

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

### 3.2 注册回调

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

### 3.3 启动线程

```java
agent.start();
```

### 3.4 提交用户输入

```java
int requestId = agent.submitUserInput(inputField.getText());
```

提交后立即返回，结果通过回调通知。

### 3.5 查询状态（可选）

```java
// 状态栏刷新
statusLabel.setText(agent.getStatusSummary());
// 例："空闲" / "处理中 (id=3)" / "空闲，队列 2 条" / "处理中 (id=3)，队列 2 条"

// 显示等待条数
pendingLabel.setText("队列 " + agent.getPendingCount() + " 条");

// 转圈动画
if (agent.isBusy()) showSpinner(); else hideSpinner();
```

### 3.6 队列控制（可选）

```java
// 用户点"取消排队"
int cleared = agent.clearPending();

// 用户取消某一条（需要 UI 保存 submitUserInput 返回的 id）
boolean ok = agent.cancelRequest(requestId);
```

### 3.7 关闭

```java
agent.shutdown();
```

### 3.8 完整流程

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

## 四、线程规则

| 场景 | 规则 |
|---|---|
| 调 `submitUserInput` | 任意线程安全 |
| 调 `start` / `shutdown` | 任意线程安全 |
| 调 `getPendingCount` / `isBusy` / `getStatusSummary` | 任意线程安全 |
| 调 `clearPending` / `cancelRequest` | 任意线程安全 |
| `onResponse` / `onError` | 在 Agent 线程，UI 必须 `Platform.runLater` |
| 更新 UI 控件 | 必须在 JavaFX 线程 |

---

## 五、常见问题

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

---

## 六、v1.5 变更记录

相比 v1.4：

- 补充 `shutdown`、`isRunning`、`clearResponseListener`
- 新增队列管理章节：`getPendingCount`、`isBusy`、`clearPending`、`cancelRequest`
- 新增状态查询：`getStatusSummary`
- 补充 `init` 参数从 `camera` 改为 `region`
- 补充队列控制使用示例
- 补充线程规则表
- 补充 Q6-Q8 常见问题