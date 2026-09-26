# AgentInterface 接口调用文档

**模块**：Agent  
**面向对象**：UI 模块  
**文档版本**：v1.4  
**更新日期**：2026-09-26

> UI 只依赖 `AgentInterface` 和 `ResponseListener` 两个类型。  
> 所有 LLM 调用、IR 解析、渲染执行、任务调度都在 Agent 内部完成，UI 不需要关心。

---

## 一、接口清单

| 接口 | 参数 | 返回 | 调用时机 |
|---|---|---|---|
| `initialize` | `LLMClient, CommandExecutor` | void | 启动时一次 |
| `getInstance` | 无 | `AgentInterface` | 初始化后任意时刻 |
| `setResponseListener` | `ResponseListener` | void | 启动后一次 |
| `start` | 无 | void | 启动后一次 |
| `submitUserInput` | `String` | int | 用户点发送 |

---

## 二、回调

### 2.1 为什么需要回调

Agent 处理用户输入是**异步**的：`submitUserInput` 立即返回，Agent 在后台线程处理，处理完通过回调通知 UI。

如果不走回调，UI 就得阻塞等待几秒，界面会卡死。

### 2.2 `ResponseListener`

UI 实现 `ResponseListener` 接口，在回调里处理 Agent 的反馈。接口有两个方法：

| 方法 | 触发时机 | 参数 |
|---|---|---|
| `onResponse` | **操作成功，返回最终反馈文本** | `int requestId`、`String text` |
| `onError` | 处理失败（LLM 超时、JSON 解析失败、执行异常） | `int requestId`、`String errorMessage` |

**`onResponse` 收到的是什么**：Agent 拼好的**给用户看的自然语言反馈**，例如 `"操作已执行：\n# MOVE_CAMERA_POS(id=1)\nSuccess..."`，**不是原始 Markdown，也不是 LLM 的原始 JSON**。UI 直接把它当作一段可显示的文本即可，不需要做任何解析。

**`onError` 收到的是什么**：一段可读的错误描述，UI 按错误样式（比如红色气泡）显示。

**回调该怎么用**：把收到的文本显示到 UI 上你想显示的地方（聊天区、日志面板、状态栏等），具体位置由 UI 决定。**唯一要注意的是，回调在 Agent 工作线程执行，不是 JavaFX 线程，更新控件前必须用 `Platform.runLater` 切回 JavaFX 线程。**

### 2.3 requestId 的作用

`requestId` 对应 `submitUserInput` 提交时返回的 id。UI 可以用它把“回复”和“提交”对应起来，尤其在用户快速连发多条输入时，避免消息错位。

---

## 三、调用示例

### 3.1 初始化

```java
RendererInterface renderer = RendererInterface.Companion.getINSTANCE();

AgentInterface.initialize(
        LLMClient.deepSeek(),
        new CommandExecutor(renderer)
);

AgentInterface agent = AgentInterface.getInstance();
```

**前置条件**：`RendererInterface.init(...)` 已执行，否则 `getINSTANCE()` 抛异常。

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

### 3.5 完整流程

```
1. RendererInterface.init(...)
2. AgentInterface.initialize(client, executor)    ← catch 异常
3. AgentInterface.getInstance()
4. setResponseListener(你的监听器)
5. start()
6. 用户点发送 → submitUserInput(text) → 拿到 requestId
7. Agent 后台处理
8. 回调触发 → onResponse 或 onError
9. UI 用 Platform.runLater 更新界面
```

---

## 四、线程规则

| 场景 | 规则 |
|---|---|
| 调 `submitUserInput` | 任意线程安全 |
| 调 `start` | 任意线程安全，只生效一次 |
| `onResponse` / `onError` | **在 Agent 线程**，UI 必须 `Platform.runLater` |
| 更新 UI 控件 | 必须在 JavaFX 线程 |

---

## 五、常见问题

**Q1：`getInstance()` 抛 `ExceptionInInitializerError`？**  
A：`initialize()` 还没调用，或调用时抛异常被 catch 了。检查初始化顺序。

**Q2：`submitUserInput` 调用后没反应？**  
A：`start()` 没调用。只入队没人消费。

**Q3：回调没触发？**  
A：`setResponseListener` 没注册，或 `start` 没启动。

**Q4：UI 卡住？**  
A：回调里直接更新了控件，没走 `Platform.runLater`。

**Q5：怎么切换本地模型 / DeepSeek？**  
A：改初始化参数：`LLMClient.local()` 或 `LLMClient.deepSeek()`。