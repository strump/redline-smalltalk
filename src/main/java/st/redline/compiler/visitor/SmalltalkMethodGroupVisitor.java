package st.redline.compiler.visitor;

import org.antlr.v4.runtime.tree.TerminalNode;
import st.redline.compiler.generated.SmalltalkParser;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class SmalltalkMethodGroupVisitor extends SmalltalkGeneratingVisitor {
    private String className;
    private String methodGroupName;
    private boolean isClassMethod = false;

    private String methodSelector;
    private List<String> methodArgs = null;

    public SmalltalkMethodGroupVisitor(String className, String methodGroupName, boolean isClassMethod) {
        super(null);
        this.className = className;
        this.methodGroupName = methodGroupName;
        this.isClassMethod = isClassMethod;
    }

    @Override
    public Void visitMethodDeclaration(SmalltalkParser.MethodDeclarationContext ctx) {
        visitMethodHeader(ctx.methodHeader());
        final SmalltalkParser.SequenceContext methodBodyTree = ctx.sequence();
        //TODO: generate method body based on `methodBodyTree`.
        return null;
    }

    @Override
    public Void visitMethodHeader(SmalltalkParser.MethodHeaderContext ctx) {
        if (ctx.IDENTIFIER() != null) {
            methodSelector = ctx.IDENTIFIER().getText();
            methodArgs = Collections.emptyList();
        }
        else if (ctx.binaryMethodHeader() != null) {
            methodSelector = ctx.binaryMethodHeader().BINARY_SELECTOR().getText();
            methodArgs = Collections.singletonList(ctx.binaryMethodHeader().IDENTIFIER().getText());
        }
        else if (ctx.keywordMethodHeader() != null) {
            final SmalltalkParser.KeywordMethodHeaderContext methodHeaderCtx = ctx.keywordMethodHeader();
            StringBuilder selectorBuilder = new StringBuilder();
            for(TerminalNode keyword: methodHeaderCtx.KEYWORD()) {
                selectorBuilder.append(keyword.getText());
            }
            methodSelector = selectorBuilder.toString();

            methodArgs = new LinkedList<>();
            for(TerminalNode ident : methodHeaderCtx.IDENTIFIER()) {
                methodArgs.add(ident.toString());
            }
        }
        return null;
    }
}
