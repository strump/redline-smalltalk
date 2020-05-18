/* Redline Smalltalk, Copyright (c) James C. Ladd. All rights reserved. See LICENSE in the root of this distribution. */
package st.redline.compiler;

import org.antlr.v4.runtime.tree.ParseTree;
import st.redline.classloader.Source;
import st.redline.compiler.generated.SmalltalkVisitor;
import st.redline.compiler.visitor.ClassGeneratorVisitor;
import st.redline.compiler.visitor.SmalltalkGeneratingVisitor;

import java.util.Stack;

public class ClassGenerator {

    private final ParseTree tree;
    private final Source source;
    private final SmalltalkGeneratingVisitor visitor;
    private Stack<SmalltalkVisitor<Void>> visitors;
    private byte[] classBytes = null;

    public ClassGenerator(ParseTree tree, Source source) {
        this.tree = tree;
        this.source = source;
        this.visitor = new SmalltalkGeneratingVisitor(this);

        initVisitorsStack();
    }

    private void initVisitorsStack() {
        visitors = new Stack<>();
        visitors.push(new ClassGeneratorVisitor(this));
    }

    public void pushCurrentVisitor(SmalltalkVisitor<Void> currentVisitor) {
        visitors.push(currentVisitor);
    }

    public SmalltalkVisitor<Void> currentVisitor() {
        return visitors.peek();
    }

    public SmalltalkVisitor<Void> popCurrentVisitor() {
        return visitors.pop();
    }

    public byte[] generate() {
        visitor.visit(tree);
        //return visitor.generatedClassBytes();
        return classBytes;
    }

    public String className() {
        return source.className();
    }

    public String fileExtension() {
        return source.fileExtension();
    }

    public String fullClassName() {
        return source.fullClassName();
    }

    public String packageName() {
        return source.packageName();
    }

    public void setClassBytes(byte[] bytes) {
        this.classBytes = bytes;
    }
}
