package weilantianhai.agent.interfaces;

public interface ResponseListener {
    /** AI 处理完成，返回最终回复文本，UI 显示到聊天区 */
    void onResponse(int requestId, String responseText);

    /** 处理失败，返回错误描述 */
    void onError(int requestId, String errorMessage);
}