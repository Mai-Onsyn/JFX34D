# AgentInterface 接口设计文档

模块：Agent
面向对象：UI 模块
文档版本：v1.0
更新日期：2026-09-21
当前状态：全部接口未实现

本文档定义 Agent 模块对 UI 暴露的统一门面 AgentInterface。
UI 只依赖此门面与 ResponseListener，不接触 LLM 调用、IR 解析、任务线程等内部细节。

---

## 一、接口类型总览

| 类型 | 接口数量 | 说明 |
| --- | --- | --- |
| 生命周期 | 3 | 启动、停止、运行状态 |
| 提交输入 | 2 | 用户输入入队 |
| 接收回复 | 2 | 回调设置与移除 |
| 任务队列管理 | 4 | 队列长度、忙闲、清空、取消 |
| 状态查询 | 3 | 当前请求、最近回复、状态摘要 |
| 对话历史 | 2 | 历史获取与清空 |
| 配置 | 4 | 模型切换、操作数上限 |

---

## 二、接口详细定义

### 2.1 生命周期

| 接口名 | 参数 | 返回值 | 功能 | 优先级 | 状态 |
| --- | --- | --- | --- | --- | --- |
| start | 无 | void | 启动 Agent 工作线程，开始处理队列。UI 初始化时调用一次 | P0 | 未实现 |
| shutdown | 无 | void | 停止 Agent，等待当前任务结束，清理资源。UI 关闭时调用 | P0 | 未实现 |
| isRunning | 无 | boolean | 查询 Agent 是否在运行 | P1 | 未实现 |

---

### 2.2 提交输入

| 接口名 | 参数 | 返回值 | 功能 | 优先级 | 状态 |
| --- | --- | --- | --- | --- | --- |
| submitUserInput | String userInput | long | 提交一条用户自然语言输入到队列，立即返回请求 ID，不阻塞 UI。输入为空抛异常 | P0 | 未实现 |
| submitUserInputWithContext | String userInput, Map<String,Object> context | long | 带上下文提交（如当前选中模型、摄像机状态），暂不实现 | P2 | 未实现 |

---

### 2.3 接收回复

| 接口名 | 参数 | 返回值 | 功能 | 优先级 | 状态 |
| --- | --- | --- | --- | --- | --- |
| setResponseListener | ResponseListener listener | void | 设置 AI 回复回调。UI 初始化时调用一次。回调在 Agent 线程执行 | P0 | 未实现 |
| clearResponseListener | 无 | void | 移除回调，停止接收回复 | P1 | 未实现 |

> **标注**：接收回复这一组里，`ResponseListener` 是 **UI 同学需要实现的接口**，由 Agent 同学在内部回调时调用（AgentInterface 上的 `setResponseListener` / `clearResponseListener` 仍由 Agent 实现、UI 调用）。也就是说，UI 负责提供 `onResponse` / `onError` 的实现，Agent 拿到后会在线程中主动调用它们把结果推给 UI。

ResponseListener 需实现的方法：

| 方法名 | 参数 | 功能 | 优先级 | 状态 |
| --- | --- | --- | --- | --- |
| onResponse | long requestId, String responseText | AI 处理完成，返回最终对话文本，UI 显示到聊天区 | P0 | 未实现 |
| onError | long requestId, String errorMessage | 处理失败（LLM 超时、JSON 解析失败、执行异常等），UI 显示错误 | P0 | 未实现 |

---

### 2.4 任务队列管理

| 接口名 | 参数 | 返回值 | 功能 | 优先级 | 状态 |
| --- | --- | --- | --- | --- | --- |
| getPendingCount | 无 | int | 当前等待处理的任务数（不含正在执行的） | P1 | 未实现 |
| isBusy | 无 | boolean | 是否正在处理任务 | P1 | 未实现 |
| clearPending | 无 | int | 清空等待队列，返回被清除的任务数。正在执行的任务不受影响 | P1 | 未实现 |
| cancelRequest | long requestId | boolean | 取消指定请求。若已在执行则返回 false，若还在队列中则移除并返回 true | P2 | 未实现 |

---

### 2.5 状态查询

| 接口名 | 参数 | 返回值 | 功能 | 优先级 | 状态 |
| --- | --- | --- | --- | --- | --- |
| getCurrentRequestId | 无 | long | 当前正在处理的请求 ID，无则返回 -1 | P1 | 未实现 |
| getLastResponse | 无 | String | 最近一次 AI 回复文本，UI 重连或刷新时可取 | P2 | 未实现 |
| getStatusSummary | 无 | String | 返回状态摘要，如 "空闲" / "处理中 (id=3)" / "队列 2 条"，供 UI 状态栏显示 | P1 | 未实现 |

---

### 2.6 对话历史

| 接口名 | 参数 | 返回值 | 功能 | 优先级 | 状态 |
| --- | --- | --- | --- | --- | --- |
| getHistory | 无 | List<ChatMessage> | 返回完整对话历史，UI 可渲染历史消息 | P2 | 未实现 |
| clearHistory | 无 | void | 清空对话历史，不影响当前队列 | P2 | 未实现 |

ChatMessage 结构：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| requestId | long | 关联的请求 ID |
| role | String | "user" 或 "ai" |
| content | String | 消息文本 |
| timestamp | long | 毫秒时间戳 |

---

### 2.7 配置

| 接口名 | 参数 | 返回值 | 功能 | 优先级 | 状态 |
| --- | --- | --- | --- | --- | --- |
| setModel | String modelName | void | 切换 LLM 模型，下次请求生效 | P2 | 未实现 |
| getModel | 无 | String | 当前使用的模型名 | P2 | 未实现 |
| setMaxOperations | int max | void | 设置单次 IR 最大操作数 | P2 | 未实现 |
| getMaxOperations | 无 | int | 当前上限 | P2 | 未实现 |

---

## 三、优先级说明

| 优先级 | 含义 | 建议实现顺序 |
| --- | --- | --- |
| P0 | UI 核心流程必须，缺失则无法跑通 | 第一批 |
| P1 | UI 体验完善，状态显示与队列控制 | 第二批 |
| P2 | 增强功能，可后补 | 第三批 |

---

## 四、UI 侧最小使用流程

1. 初始化：start() → setResponseListener(我的监听器)
2. 用户发送：submitUserInput("建一个超立方体") → 拿到 requestId
3. 回调触发：onResponse(requestId, "已创建超立方体") → UI 切回 JavaFX 线程显示
4. 状态栏：定时调 getStatusSummary() 刷新
5. 关闭：shutdown()

---

## 五、设计原则

- 单一门面：UI 只 import AgentInterface 和 ResponseListener。
- 提交即返回：submitUserInput 不阻塞，回复走回调。
- 线程边界清晰：回调在 Agent 线程，UI 用 Platform.runLater 切换。
- 错误不抛 UI：所有异常走 onError，UI 只负责显示。
- 可裁剪：P2 接口先留签名，实现后补。

---

## 六、当前实现状态

所有接口均未实现。
本文件仅作为 UI 与 Agent 的接口契约，后续按优先级逐步落地。
