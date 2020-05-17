package st.redline.compiler;

public class SmalltalkParserException extends RuntimeException {
    public SmalltalkParserException() {
    }

    public SmalltalkParserException(String message) {
        super(message);
    }

    public SmalltalkParserException(String message, Throwable cause) {
        super(message, cause);
    }
}
