package weilantianhai.agent.ir;

public class IRException extends Exception {
    private final String code;

    public IRException(String code, String message) {
        super(message);
        this.code = code;
    }

    public IRException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode(){
        return code;
    }
}
