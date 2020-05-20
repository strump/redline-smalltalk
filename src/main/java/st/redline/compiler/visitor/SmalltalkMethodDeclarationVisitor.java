package st.redline.compiler.visitor;

import org.antlr.v4.runtime.tree.TerminalNode;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import st.redline.compiler.ClassGenerator;
import st.redline.compiler.generated.SmalltalkParser;

import java.util.HashMap;

public class SmalltalkMethodDeclarationVisitor extends BlockGeneratorVisitor {
    private static final Logger log = LoggerFactory.getLogger(SmalltalkMethodDeclarationVisitor.class);
    public static final String PRIM_CLASS_FULL_NAME = "st/redline/core/PrimClass";

    private String className;
    private String methodGroupName;
    private boolean isClassMethod = false;
    private MethodVisitor parentMV;

    private String methodSelector;
    //private List<String> methodArgs = null;

    public SmalltalkMethodDeclarationVisitor(ClassGenerator classGenerator, String className, String methodGroupName,
                                             boolean isClassMethod, ClassWriter cw, MethodVisitor mv, int blockNumber,
                                             HashMap<String, ExtendedTerminalNode> homeTemporaries,
                                             HashMap<String, ExtendedTerminalNode> homeArguments,
                                             HashMap<String, ExtendedTerminalNode> outerArguments) {
        super(classGenerator, cw, null, blockNumber, homeTemporaries, homeArguments, outerArguments);

        this.className = className;
        this.methodGroupName = methodGroupName;
        this.isClassMethod = isClassMethod;
        this.parentMV = mv;
    }

    @Override
    public void handleBlock(SmalltalkParser.BlockContext ctx) {
        throw new UnsupportedOperationException("Don't create blocks with SmalltalkMethodDeclarationVisitor class. " +
                "Use BlockGeneratorVisitor instead.");
    }

    /* Generate method block and add it to class:
       <code>
       reference(className).addMethod(methodSelector, () -> {
           // Method body
       });
       </code>
     */
    @Override
    public Void visitMethodDeclaration(SmalltalkParser.MethodDeclarationContext ctx) {
        //TODO: add pushLine(...)
        visitMethodHeader(ctx.methodHeader());
        initBlockName();
        log.info("visitMethodDeclaration: class {}, selector {}, method {}", className, methodSelector, blockName);

        //Generating Java method from method declaration `sequence`.
        openBlockLambdaMethod();
        SmalltalkParser.SequenceContext blockSequence = ctx.sequence();
        if (blockSequence != null)
            blockSequence.accept(currentVisitor());
        boolean returnRequired = returnRequired(blockSequence);
        closeBlockLambdaMethod(returnRequired);

        //Lambda method is finished. Switching to previous MethodVisitor
        this.mv = parentMV;
        int line = ctx.start.getLine();
        visitLine(mv, line);

        //Generate: <code> reference(className).addMethod(selector, lambda) </code>
        line = ctx.sequence().start.getLine(); // first line in method code
        pushReference(mv, className);
        addCheckCast(mv, PRIM_CLASS_FULL_NAME); // Cast PrimObject to PrimClass
        pushLiteral(mv, methodSelector); //Put first argument of "addMethod" call
        pushNewMethod(mv, fullClassName(), blockName, LAMBDA_BLOCK_SIG, line); //Put second argument of "addMethod" call
        pushAddMethodCall(mv);

        return null;
    }

    private void initBlockName() {
        /*if (isClassMethod) {
            blockName = className + "$$"; //Class methods has additional $ sign in JVM method name
        }
        else {
            blockName = className + "$";
        }
        blockName += methodSelector.replaceAll(":", "_"); //TODO: Method selector could containe special chars (=+-?).
                                                          // Replace those chars with valid method name symbols
        */

        blockNumber ++;
        blockName = "B" + blockNumber;
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

    private void pushAddMethodCall(MethodVisitor mv) {
        mv.visitMethodInsn(INVOKEVIRTUAL, "st/redline/core/PrimClass", "addMethod", "(Ljava/lang/String;Lst/redline/core/PrimObject;)V", false);
    }

    private void addCheckCast(MethodVisitor mv, String typeName) {
        mv.visitTypeInsn(CHECKCAST, typeName);
    }
}