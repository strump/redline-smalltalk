package st.redline.compiler.visitor;

import org.antlr.v4.runtime.tree.TerminalNode;
import org.objectweb.asm.ClassWriter;
import st.redline.compiler.ClassGenerator;
import st.redline.compiler.generated.SmalltalkParser;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class SmalltalkMethodDeclarationVisitor extends BlockGeneratorVisitor {
    private String className;
    private String methodGroupName;
    private boolean isClassMethod = false;

    private String methodSelector;
    //private List<String> methodArgs = null;

    public SmalltalkMethodDeclarationVisitor(ClassGenerator classGenerator, String className, String methodGroupName,
                                             boolean isClassMethod, ClassWriter cw, int blockNumber,
                                             HashMap<String, ExtendedTerminalNode> homeTemporaries,
                                             HashMap<String, ExtendedTerminalNode> homeArguments,
                                             HashMap<String, ExtendedTerminalNode> outerArguments) {
        super(classGenerator, cw, null, blockNumber, homeTemporaries, homeArguments, outerArguments);

        this.className = className;
        this.methodGroupName = methodGroupName;
        this.isClassMethod = isClassMethod;
    }

    @Override
    public void handleBlock(SmalltalkParser.BlockContext ctx) {
        throw new UnsupportedOperationException("Don't create blocks with SmalltalkMethodDeclarationVisitor class. " +
                "Use BlockGeneratorVisitor instead.");
    }

    @Override
    public Void visitMethodDeclaration(SmalltalkParser.MethodDeclarationContext ctx) {
        visitMethodHeader(ctx.methodHeader());
        initBlockName();

        //Generating Java method from method declaration `sequence`.
        openBlockLambdaMethod();
        SmalltalkParser.SequenceContext blockSequence = ctx.sequence();
        if (blockSequence != null)
            blockSequence.accept(currentVisitor());
        boolean returnRequired = returnRequired(blockSequence);
        closeBlockLambdaMethod(returnRequired);

        return null;
    }

    private void initBlockName() {
        if (isClassMethod) {
            blockName = className + "$$"; //Class methods has additional $ sign in JVM method name
        }
        else {
            blockName = className + "$";
        }
        blockName += methodSelector.replaceAll(":", "_");
    }

    @Override
    public Void visitMethodHeader(SmalltalkParser.MethodHeaderContext ctx) {
        if (ctx.IDENTIFIER() != null) {
            methodSelector = ctx.IDENTIFIER().getText();
        }
        else if (ctx.binaryMethodHeader() != null) {
            methodSelector = ctx.binaryMethodHeader().BINARY_SELECTOR().getText();
            addArgumentToMap(new ExtendedTerminalNode(ctx.binaryMethodHeader().IDENTIFIER(), 0));
        }
        else if (ctx.keywordMethodHeader() != null) {
            final SmalltalkParser.KeywordMethodHeaderContext methodHeaderCtx = ctx.keywordMethodHeader();
            StringBuilder selectorBuilder = new StringBuilder();
            for(TerminalNode keyword: methodHeaderCtx.KEYWORD()) {
                selectorBuilder.append(keyword.getText());
            }
            methodSelector = selectorBuilder.toString();

            int index = 0;
            for(TerminalNode ident : methodHeaderCtx.IDENTIFIER()) {
                addArgumentToMap(new ExtendedTerminalNode(ident, index));
            }
        }
        return null;
    }
}
