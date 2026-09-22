package weilantianhai.agent.execute;

import weilantianhai.agent.ir.IRCommand;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ExecutionResult {
    public enum Status { DONE, EXECUTED }

    private final Status status;
    private final List<IRCommand> executed;

    private ExecutionResult(Status status, List<IRCommand> executed) {
        this.status = status;
        this.executed = executed;
    }

    public static ExecutionResult done(){
        return new ExecutionResult(Status.DONE, Collections.emptyList());
    }

    public static ExecutionResult executed(List<IRCommand> cmds){
     return new ExecutionResult(Status.EXECUTED, cmds);
    }

    public boolean isDone(){
        return status == Status.DONE;
    }

    public List<IRCommand> getExecuted(){
        return executed;
    }

}
