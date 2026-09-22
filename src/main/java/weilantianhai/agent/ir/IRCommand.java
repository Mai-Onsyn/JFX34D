package weilantianhai.agent.ir;

import weilantianhai.agent.ir.operations.IOperation;

public class IRCommand {
    private String type;//操作类型
    private int id;//操作id
    private boolean done;//结束标识
    private boolean noResult;
    private IOperation impl;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public boolean isNoResult() {
        return noResult;
    }

    public void setNoResult(boolean noResult) {
        this.noResult = noResult;
    }

    public IOperation getImpl() {
        return impl;
    }

    public void setImpl(IOperation impl) {
        this.impl = impl;
    }
}