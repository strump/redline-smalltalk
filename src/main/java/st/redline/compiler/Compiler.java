/* Redline Smalltalk, Copyright (c) James C. Ladd. All rights reserved. See LICENSE in the root of this distribution. */
package st.redline.compiler;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import st.redline.classloader.Source;
import st.redline.compiler.generated.SmalltalkLexer;
import st.redline.compiler.generated.SmalltalkParser;

import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.List;

public class Compiler {
    private static final Logger log = LoggerFactory.getLogger(Compiler.class);

    private final Source source;

    public Compiler(Source source) {
        this.source = source;
    }

    public byte[] compile() {
        if (!haveSource())
            return null;
        return compileSource();
    }

    private byte[] compileSource() {
        return generateClass(parsedSourceContents());
    }

    public ParseTree parsedSourceContents() {
        return parse(sourceContents());
    }

    private byte[] generateClass(ParseTree tree) {
        return createGenerator(tree).generate();
    }

    private ClassGenerator createGenerator(ParseTree tree) {
        return new ClassGenerator(tree, source);
    }

    private ParseTree parse(String input) {
        final CharStream inputStream = CharStreams.fromString(input, source.fullClassName());

        SmalltalkLexer lexer = new SmalltalkLexer(inputStream);
        SmalltalkParser parser = new SmalltalkParser(new CommonTokenStream(lexer));
        //parser.setErrorHandler(new BailErrorStrategy());
        parser.removeErrorListeners();
        parser.addErrorListener(new SmalltalkParserErrorListener());

        final SmalltalkParser.ScriptContext script = parser.script();
        dumpTree(parser, script);

        return script;
    }

    private String sourceContents() {
        String src = source.contents();
        // dump pre-processed source
        //log.info(src);
        return src;
    }

    private boolean haveSource() {
        return source != null && source.hasContent();
    }

    private void dumpTree(SmalltalkParser parser, SmalltalkParser.ScriptContext scriptContext) {
        final String fullClassName = source.fullClassName().replaceAll("/", ".");
        log.info("Dumping AST of class {} to file", fullClassName);
        final String filename = "D:\\tmp\\redline\\" + fullClassName + ".tree";

        try (FileOutputStream fos = new FileOutputStream(filename)) {
            final List<String> ruleNamesList = Arrays.asList(parser.getRuleNames());
            final String treeStr = TreeUtils.toPrettyTree(scriptContext, ruleNamesList);
            fos.write(treeStr.getBytes());
        }
        catch (Exception exc) {
            log.error("Can't save tree dump. Skipping", exc);
        }
    }
}
