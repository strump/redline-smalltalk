package st.redline.compiler;

import org.antlr.v4.runtime.*;

public class ParserErrorListener extends BaseErrorListener {
    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine,
                            String msg, RecognitionException e)
    {
        String filename = recognizer.getInputStream().getSourceName();
        if(filename == null) filename = "<null>";

        throw new SmalltalkParserException("Syntax error at "+filename+" line " + line + ":" + charPositionInLine + " " + msg);
    }
}
