package weilantianhai.agent.ir.resolver;

import weilantianhai.agent.ir.IRCommand;

import java.util.List;

public class IRBundle {
    private int version;
    private List<IRCommand> operations;

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public List<IRCommand> getOperations() {
        return operations;
    }

    public void setOperations(List<IRCommand> operations) {
        this.operations = operations;
    }
}
