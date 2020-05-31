package st.redline.core;

public class StRuntimeError extends RuntimeException {
    private final PrimObject msg;

    public StRuntimeError(PrimObject msg) {
        this.msg = msg;
    }

    public PrimObject getMsg() {
        return msg;
    }
}
