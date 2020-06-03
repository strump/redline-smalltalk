package st.redline.core;

public class StRuntimeError extends RuntimeException {
    private final PrimObject stMessage;

    public StRuntimeError(PrimObject stMessage) {
        super(stMessage.toString());
        this.stMessage = stMessage;
    }

    public PrimObject getStMessage() {
        return stMessage;
    }
}
