package weilantianhai.agent.ir;

public class IR {
    private String op;          // JSON 里的操作名，如 "move"
    private String id;          // 操作 ID，可选
    private IOperation data;    // 多态：具体是 MoveOperation / RotateOperation ...

    public String getOp() { return op; }
    public void setOp(String op) { this.op = op; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public IOperation getData() { return data; }
    public void setData(IOperation data) { this.data = data; }
}