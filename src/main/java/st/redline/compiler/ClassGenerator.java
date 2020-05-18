/* Redline Smalltalk, Copyright (c) James C. Ladd. All rights reserved. See LICENSE in the root of this distribution. */
package st.redline.compiler;

import org.antlr.v4.runtime.tree.ParseTree;
import st.redline.classloader.Source;
import st.redline.compiler.generated.SmalltalkVisitor;

public class ClassGenerator {

    private final ParseTree tree;
    private final Source source;
    private final SmalltalkGeneratingVisitor visitor;

    public ClassGenerator(ParseTree tree, Source source) {
        this.tree = tree;
        this.source = source;
        this.visitor = new SmalltalkGeneratingVisitor(source);
    }

    public byte[] generate() {
        visitor.visit(tree);
        return visitor.generatedClassBytes();
    }
}
