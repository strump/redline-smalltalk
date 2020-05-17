/* Redline Smalltalk, Copyright (c) James C. Ladd. All rights reserved. See LICENSE in the root of this distribution. */
package st.redline.compiler;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
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

    private ParseTree parsedSourceContents() {
        return parse(sourceContents());
    }

    private byte[] generateClass(ParseTree tree) {
        return createGenerator(tree).generate();
    }

    private Generator createGenerator(ParseTree tree) {
        return new Generator(tree, createVisitor());
    }

    private SmalltalkGeneratingVisitor createVisitor() {
        return new SmalltalkGeneratingVisitor(source);
    }

    private ParseTree parse(String input) {
        SmalltalkLexer lexer = new SmalltalkLexer(new ANTLRInputStream(input));
        SmalltalkParser parser = new SmalltalkParser(new CommonTokenStream(lexer));

        //dumpTree(parser);

        return parser.script();
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

    private void dumpTree(SmalltalkParser parser) {
        final String filename = "D:\\tmp\\redline\\" + source.fullClassName().replaceAll("/", ".") + ".tree";

        try (FileOutputStream fos = new FileOutputStream(filename)){
            final List<String> ruleNamesList = Arrays.asList(parser.getRuleNames());
            final String treeStr = TreeUtils.toPrettyTree(parser.script(), ruleNamesList);
            fos.write(treeStr.getBytes());
        } catch (Exception e) {
            log.error("Can't save tree dump", e);
        }
    }
}
