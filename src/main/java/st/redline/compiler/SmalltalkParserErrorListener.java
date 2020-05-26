package st.redline.compiler;

import org.antlr.v4.runtime.*;

public class SmalltalkParserErrorListener extends BaseErrorListener {
    private final boolean isLexer;

    public SmalltalkParserErrorListener() {
        this(true);
    }

    public SmalltalkParserErrorListener(boolean isLexer) {
        this.isLexer = isLexer;
    }

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine,
                            String msg, RecognitionException e)
    {
        String filename = recognizer.getInputStream().getSourceName();
        if(filename == null) filename = "<null>";

        final String exceptionMsg = (isLexer ? "Lexer" : "Parser") +" error at "+filename+" line " + line + ":" + charPositionInLine + " " + msg;

        throw new SmalltalkParserException(exceptionMsg);
    }
}
