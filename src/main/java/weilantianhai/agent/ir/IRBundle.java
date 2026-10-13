package weilantianhai.agent.ir;

import java.util.List;

public class IRBundle {
    private int version;
    private boolean done;
    /** done=true 时给用户看的一句话总结；缺失时为 null。 */
    private String content;
    private List<IRCommand> operations;

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<IRCommand> getOperations() {
        return operations;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public void setOperations(List<IRCommand> operations) {
        this.operations = operations;
    }
}
