package st.redline.compiler.visitor;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.objectweb.asm.ClassWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import st.redline.compiler.ClassGenerator;
import st.redline.compiler.generated.SmalltalkParser;

import java.util.HashMap;
import java.util.List;

/* Generator of Smalltalk block method.
 * Each Smalltalk block is compiled to Java inner class method with name "B2B1B4" meaning that every
 * inner block appends name to outer block method name.
 */
public class BlockGeneratorVisitor extends ClassGeneratorVisitor {
    private static final Logger log = LoggerFactory.getLogger(BlockGeneratorVisitor.class);

    protected String blockName;
    private boolean returnRequired;

    public BlockGeneratorVisitor(ClassGenerator classGenerator, ClassWriter cw, String blockName, int blockNumber,
                                 HashMap<String, ExtendedTerminalNode> homeTemporaries,
                                 HashMap<String, ExtendedTerminalNode> homeArguments,
                                 HashMap<String, ExtendedTerminalNode> outerArguments) {
        super(classGenerator, cw);
        this.blockName = blockName;
        this.returnRequired = false;
        this.blockNumber = blockNumber;
        this.homeTemporaries = homeTemporaries;
        this.homeArguments = homeArguments;
        this.outerArguments = outerArguments;
    }

    /* Generate java lambda body with Smalltalk block code inside. */
    public void handleBlock(SmalltalkParser.BlockContext ctx) {
        log.info("  handleBlock {} {}", blockName, blockNumber);
        openBlockLambdaMethod();
        SmalltalkParser.BlockParamListContext blockParamList = ctx.blockParamList();
        if (blockParamList != null)
            blockParamList.accept(currentVisitor());
        SmalltalkParser.SequenceContext blockSequence = ctx.sequence();
        if (blockSequence != null)
            blockSequence.accept(currentVisitor());
        returnRequired = returnRequired(blockSequence);
        closeBlockLambdaMethod(returnRequired);
    }

    public boolean isAnswerBlock() {
        return !returnRequired;
    }

    protected boolean returnRequired(SmalltalkParser.SequenceContext blockSequence) {
        if (blockSequence == null)
            return true;
        SmalltalkParser.StatementsContext statements = blockSequence.statements();
        if (statements == null) {
            // We have no statements in block, so answer nil.
            pushNil(mv);
            return true;
        }
        List<ParseTree> list = statements.children;
        for (ParseTree parseTree : list) {
            if (parseTree instanceof SmalltalkParser.AnswerContext)
                return false;
        }
        return true;
    }

    @Override
    public Void visitBlockParamList(SmalltalkParser.BlockParamListContext ctx) {
        log.info("  visitBlockParamList");
        int index = 0;
        int n = ctx.getChildCount();
        for(int i = 0; i < n; ++i) {
            ParseTree c = ctx.getChild(i);
            if (c instanceof TerminalNode)
                addArgumentToMap(new ExtendedTerminalNode((TerminalNode) c, index++));
        }
        return null;
    }

    protected void closeBlockLambdaMethod(boolean returnRequired) {
        log.info("  closeBlockLambdaMethod: {} {}", blockName, returnRequired);
        if (returnRequired)
            mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    protected void openBlockLambdaMethod() {
        log.info("  openBlockLambdaMethod: {}", blockName);
        mv = cw.visitMethod(ACC_PRIVATE + ACC_STATIC + ACC_SYNTHETIC, blockName, LAMBDA_BLOCK_SIG, null, null);
        mv.visitCode();
    }
}
