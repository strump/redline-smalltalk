package st.redline.compiler.visitor;

import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import st.redline.classloader.SmalltalkClassLoader;
import st.redline.compiler.ClassGenerator;
import st.redline.compiler.SmalltalkCompilationError;
import st.redline.compiler.generated.SmalltalkParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

/* Generator of class bytecode. Such generator is created once for every *.st source file assuming that
 * single .st file contains one class definition (see source.fullClassName()).
 */
public class ClassGeneratorVisitor extends SmalltalkGeneratingVisitor {
    private static final Logger log = LoggerFactory.getLogger(ClassGeneratorVisitor.class);

    protected final static String LAMBDA_BLOCK_SIG = "(Lst/redline/core/PrimObject;Lst/redline/core/PrimObject;Lst/redline/core/PrimContext;)Lst/redline/core/PrimObject;";
    private final static String SEND_MESSAGES_SIG = "(Lst/redline/core/PrimObject;Lst/redline/core/PrimContext;)Lst/redline/core/PrimObject;";

    protected MethodVisitor mv;
    protected final ClassWriter cw;
    private HashMap<String, ExtendedTerminalNode> temporaries = new HashMap<>();
    protected HashMap<String, ExtendedTerminalNode> homeTemporaries;
    private HashMap<String, ExtendedTerminalNode> arguments = new HashMap<>();
    protected HashMap<String, ExtendedTerminalNode> outerArguments;
    protected HashMap<String, ExtendedTerminalNode> homeArguments;
    private Stack<KeywordRecord> keywords = new Stack<>();
    protected int blockNumber = 0;
    private boolean referencedJVM = false;
    private boolean sendToSuper = false;
    private List<SmalltalkGeneratingVisitor.BlockAnswerRecord> tryCatchRecords;
    private Label tryStartLabel;
    private Label tryEndLabel;

    public ClassGeneratorVisitor(ClassGenerator classGenerator) {
        this(classGenerator, new ClassWriter(ClassWriter.COMPUTE_FRAMES));
    }

    public ClassGeneratorVisitor(ClassGenerator classGenerator, ClassWriter classWriter) {
        super(classGenerator);
        this.cw = classWriter;
    }

    public ClassGeneratorVisitor(ClassGenerator classGenerator, ClassWriter classWriter, MethodVisitor methodVisitor) {
        super(classGenerator);
        cw = classWriter;
        mv = methodVisitor;
    }

    /* Generate full bytecode for a single class. */
    @Override
    public Void visitScript(SmalltalkParser.ScriptContext ctx) {
        openJavaClass();
        createPackageNameMethod();
        createImportForMethod();
        openSendMessagesMethod();
        ctx.rootSequence().accept(currentVisitor());
        closeSendMessagesMethod();
        closeJavaClass();
        return null;
    }

    private void closeSendMessagesMethod() {
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void createPackageNameMethod() {
        mv = cw.visitMethod(ACC_PROTECTED, "packageName", "()Ljava/lang/String;", null, null);
        mv.visitCode();
        mv.visitLdcInsn(packageName());
        mv.visitInsn(ARETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }

    private void createImportForMethod() {
        if (DEFAULT_IMPORTED_PACKAGE.equals(packageName()))
            return;
        mv = cw.visitMethod(ACC_PROTECTED, "importFor", "(Ljava/lang/String;)Ljava/lang/String;", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKEVIRTUAL, "st/redline/core/PrimObject", "classLoader", "()Lst/redline/classloader/SmalltalkClassLoader;", false);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKEVIRTUAL, "st/redline/core/PrimObject", "packageName", "()Ljava/lang/String;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "st/redline/classloader/SmalltalkClassLoader", "importForBy", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(3, 2);
        mv.visitEnd();
    }

    private void openSendMessagesMethod() {
        mv = cw.visitMethod(ACC_PROTECTED, "sendMessages", SEND_MESSAGES_SIG, null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 1);
    }

    private void openJavaClass() {
        log.info("openJavaClass: {}", fullClassName());
        cw.visit(BYTECODE_VERSION, ACC_PUBLIC + ACC_SUPER, fullClassName(), null, superclassName(), null);
        cw.visitSource(className() + sourceFileExtension(), null);
        cw.visitInnerClass("java/lang/invoke/MethodHandles$Lookup", "java/lang/invoke/MethodHandles", "Lookup", ACC_PUBLIC + ACC_FINAL + ACC_STATIC);
        makeJavaClassInitializer();
    }

    private void closeJavaClass() {
        log.info("closeJavaClass: {}", fullClassName());
        cw.visitEnd();
        classGen.setClassBytes(cw.toByteArray());
    }

    /* Generate constructor
     * <code>
       <init>() {
           super();
           importAll(packageName());
           PrimContext context = new PrimContext(this);
           selfClass(this);
           sendMessages(this, context);
       }
       </code>
     */
    private void makeJavaClassInitializer() {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        Label l0 = new Label();
        mv.visitLabel(l0);
        mv.visitLineNumber(0, l0);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, superclassName(), "<init>", "()V", false);

        // import current package
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKEVIRTUAL, "st/redline/core/PrimObject", "packageName", "()Ljava/lang/String;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "st/redline/core/PrimObject", "importAll", "(Ljava/lang/String;)V", false);

        // create a Context
        mv.visitTypeInsn(NEW, contextName());
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, contextName(), "<init>", "(Lst/redline/core/PrimObject;)V", false);
        mv.visitVarInsn(ASTORE, 1);

        // set the class of this as self.
        mv.visitVarInsn(ALOAD, 0); // this
        mv.visitVarInsn(ALOAD, 0); // receiver
        mv.visitMethodInsn(INVOKEVIRTUAL, fullClassName(), "selfClass", "(Lst/redline/core/PrimObject;)V", false);

        // call sendMessages with parameters: this & context
        mv.visitVarInsn(ALOAD, 0); // this
        mv.visitVarInsn(ALOAD, 0); // receiver
        mv.visitVarInsn(ALOAD, 1); // context
        mv.visitMethodInsn(INVOKEVIRTUAL, fullClassName(), "sendMessages", SEND_MESSAGES_SIG, false);
        mv.visitInsn(POP);

        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    @Override
    public Void visitRootSequence(SmalltalkParser.RootSequenceContext ctx) {
        log.info("visitRootSequence");
        SmalltalkParser.TempsContext temps = ctx.temps();
        if (temps != null)
            temps.accept(currentVisitor());
        SmalltalkParser.RootStatementsContext statements = ctx.rootStatements();
        if (statements != null)
            statements.accept(currentVisitor());
        SmalltalkParser.TerminatingExpressionContext terminatingExpression = ctx.terminatingExpression();
        if (terminatingExpression != null)
            terminatingExpression.accept(currentVisitor());
        return null;
    }

    @Override
    public Void visitSequence(SmalltalkParser.SequenceContext ctx) {
        log.info("visitSequence");
        SmalltalkParser.TempsContext temps = ctx.temps();
        if (temps != null)
            temps.accept(currentVisitor());
        SmalltalkParser.StatementsContext statements = ctx.statements();
        if (statements != null)
            statements.accept(currentVisitor());
        return null;
    }

    @Override
    public Void visitTemps(@NotNull SmalltalkParser.TempsContext ctx) {
        addToTemporaryVariableMap(ctx.IDENTIFIER());
        addTemporariesToContext();
        return null;
    }

    /* Generate constructor
     * <code>
       primContext.initTemporaries(temporaries.size());
       </code>
     */
    private void addTemporariesToContext() {
        visitLine(mv, lineNumberOfFirstTemporary());
        pushContext(mv);
        pushNumber(mv, temporaries.size());
        mv.visitMethodInsn(INVOKEVIRTUAL, contextName(), "initTemporaries", "(I)V", false);
    }

    private int lineNumberOfFirstTemporary() {
        return temporaries.entrySet().iterator().next().getValue().getLine();
    }

    private boolean isTemporary(String key) {
        return temporaries != null && temporaries.containsKey(key);
    }

    private boolean isHomeTemporary(String key) {
        return homeTemporaries != null && homeTemporaries.containsKey(key);
    }

    private int indexOfTemporary(String key) {
        return temporaries.get(key).getIndex();
    }

    private int indexOfHomeTemporary(String key) {
        return homeTemporaries.get(key).getIndex();
    }

    private void addToTemporaryVariableMap(List<TerminalNode> nodes) {
        for (TerminalNode node : nodes)
            addToTemporaryVariableMap(node);
    }

    private void addToTemporaryVariableMap(TerminalNode node) {
        addToTemporaryVariableMap(node.getText(), temporaries.size(), node);
    }

    private void addToTemporaryVariableMap(String key, int index, TerminalNode node) {
        if (temporaries.containsKey(key))
            throw new RuntimeException("Temporary '" + key + "' already defined.");
        temporaries.put(key, new SmalltalkGeneratingVisitor.ExtendedTerminalNode(node, index));
    }

    protected int countOf(String input, char ch) {
        int count = 0;
        for (int i = 0, l = input.length(); i < l; i++)
            if (input.charAt(i) == ch)
                count++;
        return count;
    }

    private boolean haveKeyword() {
        return !keywords.isEmpty();
    }

    protected void initializeKeyword() {
        keywords.push(new SmalltalkGeneratingVisitor.KeywordRecord());
    }

    protected void addToKeyword(String keyword) {
        keywords.peek().keyword.append(keyword);
    }

    private void addArgumentToKeyword(String firstArgument) {
        assert keywords.peek().firstArgument.length() == 0;
        keywords.peek().firstArgument.append(firstArgument);
    }

    protected String removeKeyword() {
        return keywords.pop().keyword.toString();
    }

    private SmalltalkGeneratingVisitor.KeywordRecord peekKeyword() {
        return keywords.peek();
    }

    private boolean isArgument(String key) {
        return arguments != null && arguments.containsKey(key);
    }

    private boolean isOuterArgument(String key) {
        return outerArguments != null && outerArguments.containsKey(key);
    }

    private boolean isHomeArgument(String key) {
        return homeArguments != null && homeArguments.containsKey(key);
    }

    private int indexOfArgument(String key) {
        return arguments.get(key).getIndex();
    }

    private int indexOfOuterArgument(String key) {
        return outerArguments.get(key).getIndex();
    }

    private int indexOfHomeArgument(String key) {
        return homeArguments.get(key).getIndex();
    }

    protected void addArgumentToMap(SmalltalkGeneratingVisitor.ExtendedTerminalNode node) {
        log.info("addArgumentToMap {}", node.getText());
        assert !arguments.containsKey(node.getText());
        String key = node.getText();
        if (key.startsWith(":"))  // <- block arguments are prefixed with ':'
            key = key.substring(1);
        arguments.put(key, node);
    }

    @Override
    public Void visitStatementExpressions(@NotNull SmalltalkParser.StatementExpressionsContext ctx) {
        log.info("  visitStatementExpressions");
        ctx.expressions().accept(currentVisitor());
        return null;
    }

    @Override
    public Void visitStatementExpressionsAnswer(@NotNull SmalltalkParser.StatementExpressionsAnswerContext ctx) {
        log.info("  visitStatementExpressionsAnswer");
        ctx.expressions().accept(currentVisitor());
        ctx.answer().accept(currentVisitor());
        return null;
    }

    @Override
    public Void visitStatementAnswer(@NotNull SmalltalkParser.StatementAnswerContext ctx) {
        log.info("  visitStatementAnswer");
        SmalltalkParser.AnswerContext answer = ctx.answer();
        visitLine(mv, answer.CARROT().getSymbol().getLine());
        SmalltalkParser.ExpressionContext expression = answer.expression();
        expression.accept(currentVisitor());
        mv.visitInsn(ARETURN);
        return null;
    }

    @Override
    public Void visitAnswer(@NotNull SmalltalkParser.AnswerContext ctx) {
        log.info("  visitAnswer");
        TerminalNode carrot = ctx.CARROT();
        visitLine(mv, carrot.getSymbol().getLine());
        SmalltalkParser.ExpressionContext expression = ctx.expression();
        expression.accept(currentVisitor());
        mv.visitInsn(ARETURN);
        return null;
    }

    @Override
    public Void visitExpression(@NotNull SmalltalkParser.ExpressionContext ctx) {
        log.info("  visitExpression");
        referencedJVM = false;
        removeJVMGeneratorVisitor();
        SmalltalkParser.BinarySendContext binarySend = ctx.binarySend();
        if (binarySend != null)
            return binarySend.accept(currentVisitor());
        SmalltalkParser.KeywordSendContext keywordSend = ctx.keywordSend();
        if (keywordSend != null)
            return keywordSend.accept(currentVisitor());
        SmalltalkParser.CascadeContext cascade = ctx.cascade();
        if (cascade != null)
            return cascade.accept(currentVisitor());
        SmalltalkParser.AssignmentContext assignment = ctx.assignment();
        if (assignment != null)
            return assignment.accept(currentVisitor());
        SmalltalkParser.PrimitiveContext primitiveContext = ctx.primitive();
        if (primitiveContext != null)
            return primitiveContext.accept(currentVisitor());
        throw new RuntimeException("visitExpression no alternative found.");
    }

    private void removeJVMGeneratorVisitor() {
        if (currentVisitor() instanceof JVMGeneratorVisitor)
            classGen.popCurrentVisitor();
    }

    @Override
    public Void visitPrimitive(@NotNull SmalltalkParser.PrimitiveContext ctx) {
        log.info("  visitPrimitive");
        throw new RuntimeException("Smalltalk <primitive> should be replaced with JVM primitive: id.");
    }

    @Override
    public Void visitUnarySend(@NotNull SmalltalkParser.UnarySendContext ctx) {
        log.info("  visitUnarySend");
        ctx.operand().accept(currentVisitor());
        SmalltalkParser.UnaryTailContext unaryTail = ctx.unaryTail();
        if (unaryTail != null)
            return unaryTail.accept(currentVisitor());
        return null;
    }

    @Override
    public Void visitUnaryTail(@NotNull SmalltalkParser.UnaryTailContext ctx) {
        log.info("  visitUnaryTail");
        ctx.unaryMessage().accept(currentVisitor());
        SmalltalkParser.UnaryTailContext unaryTail = ctx.unaryTail();
        if (unaryTail != null)
            return unaryTail.accept(currentVisitor());
        return null;
    }

    @Override
    public Void visitUnarySelector(@NotNull SmalltalkParser.UnarySelectorContext ctx) {
        log.info("  visitUnarySelector {}", ctx.IDENTIFIER().getSymbol().getText());
        TerminalNode selectorNode = ctx.IDENTIFIER();
        visitLine(mv, selectorNode.getSymbol().getLine());
        invokePerform(mv, selectorNode.getSymbol().getText(), 0, sendToSuper);
        sendToSuper = false;
        return null;
    }

    @Override
    public Void visitBinarySend(@NotNull SmalltalkParser.BinarySendContext ctx) {
        log.info("  visitBinarySend");
        ctx.unarySend().accept(currentVisitor());
        SmalltalkParser.BinaryTailContext binaryTail = ctx.binaryTail();
        if (binaryTail != null)
            return binaryTail.accept(currentVisitor());
        return null;
    }

    @Override
    public Void visitBinaryTail(@NotNull SmalltalkParser.BinaryTailContext ctx) {
        log.info("  visitBinaryTail");
        ctx.binaryMessage().accept(currentVisitor());
        SmalltalkParser.BinaryTailContext binaryTail = ctx.binaryTail();
        if (binaryTail != null)
            return binaryTail.accept(currentVisitor());
        return null;
    }

    @Override
    public Void visitKeywordSend(@NotNull SmalltalkParser.KeywordSendContext ctx) {
        log.info("  visitKeywordSend");
        ctx.binarySend().accept(currentVisitor());
        if (referencedJVM)
            classGen.pushCurrentVisitor(new JVMGeneratorVisitor(classGen, cw, mv));
        ctx.keywordMessage().accept(currentVisitor());
        return null;
    }

    @Override
    public Void visitCascade(@NotNull SmalltalkParser.CascadeContext ctx) {
        log.info("  visitCascade");
        SmalltalkParser.BinarySendContext binarySend = ctx.binarySend();
        if (binarySend != null)
            binarySend.accept(currentVisitor());
        SmalltalkParser.KeywordSendContext keywordSend = ctx.keywordSend();
        if (keywordSend != null)
            keywordSend.accept(currentVisitor());
        for (SmalltalkParser.MessageContext message : ctx.message())
            message.accept(currentVisitor());
        return null;
    }

    @Override
    public Void visitAssignment(@NotNull SmalltalkParser.AssignmentContext ctx) {
        log.info("  visitAssignment");
        SmalltalkParser.ExpressionContext expression = ctx.expression();
        if (expression == null)
            throw new RuntimeException("visitAssignment expression expected.");
        expression.accept(currentVisitor());
        SmalltalkParser.VariableContext variable = ctx.variable();
        if (variable == null)
            throw new RuntimeException("visitAssignment variable expected.");
        TerminalNode identifierNode = variable.IDENTIFIER();
        String identifier = identifierNode.getSymbol().getText();
        visitLine(mv, identifierNode.getSymbol().getLine());
        if (isTemporary(identifier)) {
            pushDuplicate(mv);
            storeTemporary(mv, indexOfTemporary(identifier));
        } else if (isHomeTemporary(identifier)) {
            pushDuplicate(mv);
            storeHomeTemporary(mv, indexOfHomeTemporary(identifier));
        } else {
            // Assume the variable is an instance var.
            pushDuplicate(mv);
            storeInstVar(mv, identifier);
        }
        return null;
    }

    @Override
    public Void visitMessage(@NotNull SmalltalkParser.MessageContext ctx) {
        log.info("  visitMessage");
        SmalltalkParser.UnaryMessageContext unaryMessage = ctx.unaryMessage();
        if (unaryMessage != null)
            return unaryMessage.accept(currentVisitor());
        SmalltalkParser.KeywordMessageContext keywordMessage = ctx.keywordMessage();
        if (keywordMessage != null)
            return keywordMessage.accept(currentVisitor());
        SmalltalkParser.BinaryMessageContext binaryMessage = ctx.binaryMessage();
        if (binaryMessage != null)
            return binaryMessage.accept(currentVisitor());
        throw new RuntimeException("visitMessage no alternative found.");
    }

    @Override
    public Void visitUnaryMessage(@NotNull SmalltalkParser.UnaryMessageContext ctx) {
        log.info("  visitUnaryMessage");
        SmalltalkParser.UnarySelectorContext unarySelector = ctx.unarySelector();
        if (unarySelector != null)
            unarySelector.accept(currentVisitor());
        return null;
    }

    @Override
    public Void visitBinaryMessage(@NotNull SmalltalkParser.BinaryMessageContext ctx) {
        log.info("  visitBinaryMessage {}", ctx.BINARY_SELECTOR().getSymbol().getText());
        TerminalNode binarySelector = ctx.BINARY_SELECTOR();
        SmalltalkParser.UnarySendContext unarySend = ctx.unarySend();
        if (unarySend != null)
            unarySend.accept(currentVisitor());
        SmalltalkParser.OperandContext operand = ctx.operand();
        if (operand != null)
            operand.accept(currentVisitor());
        visitLine(mv, binarySelector.getSymbol().getLine());
        invokePerform(mv, binarySelector.getSymbol().getText(), 1, sendToSuper);
        sendToSuper = false;
        return null;
    }

    @Override
    public Void visitKeywordMessage(@NotNull SmalltalkParser.KeywordMessageContext ctx) {
        log.info("  visitKeywordMessage");
        initializeKeyword();
        initializeTryCatch();
        for (SmalltalkParser.KeywordPairContext keywordPair : ctx.keywordPair())
            keywordPair.accept(currentVisitor());
        visitLine(mv, ctx.keywordPair().get(0).KEYWORD().getSymbol().getLine());
        String keyword = removeKeyword();
        setupTryBlock();
        invokePerform(mv, keyword, countOf(keyword, ':'), sendToSuper);
        setupCatchBlock();
        sendToSuper = false;
        return null;
    }

    private void initializeTryCatch() {
        tryCatchRecords = new ArrayList<>();
        tryStartLabel = new Label();
        tryEndLabel = new Label();
    }

    private void setupTryBlock() {
        if (tryCatchRecords.isEmpty())
            return;
        log.info("  setupTryBlock");
        for (SmalltalkGeneratingVisitor.BlockAnswerRecord record : tryCatchRecords)
            mv.visitTryCatchBlock(tryStartLabel, tryEndLabel, record.handlerLabel, record.exceptionName);
        mv.visitLabel(tryStartLabel);
    }

    private void setupCatchBlock() {
        if (tryCatchRecords.isEmpty())
            return;
        log.info("  setupCatchBlock");
        for (SmalltalkGeneratingVisitor.BlockAnswerRecord record : tryCatchRecords) {
            mv.visitJumpInsn(GOTO, tryEndLabel);
            mv.visitLabel(record.handlerLabel);
            mv.visitMethodInsn(INVOKEVIRTUAL, record.exceptionName, "answer", "()Lst/redline/core/PrimObject;", false);
            mv.visitInsn(ARETURN);
        }
        mv.visitLabel(tryEndLabel);
    }

    @Override
    public Void visitKeywordPair(@NotNull SmalltalkParser.KeywordPairContext ctx) {
        log.info("  visitKeywordPair {}", ctx.KEYWORD().getSymbol().getText());
        TerminalNode keyword = ctx.KEYWORD();
        String part = keyword.getSymbol().getText();
        visitLine(mv, keyword.getSymbol().getLine());
        addToKeyword(part);
        SmalltalkParser.BinarySendContext binarySend = ctx.binarySend();
        if (binarySend != null)
            return binarySend.accept(currentVisitor());
        throw new RuntimeException("visitKeywordPair binary send expected.");
    }

    @Override
    public Void visitOperand(@NotNull SmalltalkParser.OperandContext ctx) {
        log.info("  visitOperand");
        SmalltalkParser.LiteralContext literal = ctx.literal();
        if (literal != null)
            return literal.accept(currentVisitor());
        SmalltalkParser.ReferenceContext reference = ctx.reference();
        if (reference != null)
            return reference.accept(currentVisitor());
        SmalltalkParser.SubexpressionContext subexpression = ctx.subexpression();
        if (subexpression != null)
            return subexpression.accept(currentVisitor());
        throw new RuntimeException("visitOperand no alternative found.");
    }

    @Override
    public Void visitLiteral(@NotNull SmalltalkParser.LiteralContext ctx) {
        log.info("  visitLiteral");
        SmalltalkParser.ParsetimeLiteralContext parsetimeLiteral = ctx.parsetimeLiteral();
        if (parsetimeLiteral != null)
            return parsetimeLiteral.accept(currentVisitor());
        SmalltalkParser.RuntimeLiteralContext runtimeLiteral = ctx.runtimeLiteral();
        if (runtimeLiteral != null)
            return runtimeLiteral.accept(currentVisitor());
        throw new RuntimeException("visitLiteral no alternative found.");
    }

    @Override
    public Void visitRuntimeLiteral(@NotNull SmalltalkParser.RuntimeLiteralContext ctx) {
        log.info("  visitRuntimeLiteral");
        SmalltalkParser.BlockContext block = ctx.block();
        if (block != null)
            return block.accept(currentVisitor());
        SmalltalkParser.DynamicDictionaryContext dynamicDictionary = ctx.dynamicDictionary();
        if (dynamicDictionary != null)
            return dynamicDictionary.accept(currentVisitor());
        SmalltalkParser.DynamicArrayContext dynamicArray = ctx.dynamicArray();
        if (dynamicArray != null)
            return dynamicArray.accept(currentVisitor());
        throw new RuntimeException("visitRuntimeLiteral no alternative found.");
    }

    @Override
    public Void visitParsetimeLiteral(@NotNull SmalltalkParser.ParsetimeLiteralContext ctx) {
        log.info("  visitParsetimeLiteral");
        SmalltalkParser.PseudoVariableContext pseudoVariable = ctx.pseudoVariable();
        if (pseudoVariable != null)
            return pseudoVariable.accept(currentVisitor());
        SmalltalkParser.NumberContext number = ctx.number();
        if (number != null)
            return number.accept(currentVisitor());
        SmalltalkParser.CharConstantContext charConstant = ctx.charConstant();
        if (charConstant != null)
            return charConstant.accept(currentVisitor());
        SmalltalkParser.LiteralArrayContext literalArray = ctx.literalArray();
        if (literalArray != null)
            return literalArray.accept(currentVisitor());
        SmalltalkParser.StringContext string = ctx.string();
        if (string != null)
            return string.accept(currentVisitor());
        SmalltalkParser.SymbolContext symbol = ctx.symbol();
        if (symbol != null)
            return symbol.accept(currentVisitor());
        throw new RuntimeException("visitParsetimeLiteral no alternative found.");
    }

    @Override
    public Void visitPseudoVariable(@NotNull SmalltalkParser.PseudoVariableContext ctx) {
        log.info("  visitPseudoVariable {}", ctx.RESERVED_WORD().getSymbol().getText());
        TerminalNode pseudoVariable = ctx.RESERVED_WORD();
        String name = pseudoVariable.getSymbol().getText();
        if ("self".equals(name))
            pushReceiver(mv);
        else if ("nil".equals(name))
            pushNil(mv);
        else if ("true".equals(name))
            pushTrue(mv);
        else if ("false".equals(name))
            pushFalse(mv);
        else if ("super".equals(name))
            pushSuper(mv, pseudoVariable.getSymbol().getLine());
        else
            throw new RuntimeException("visitPseudoVariable unknown variable: " + name);
        return null;
    }

    @Override
    public Void visitLiteralArray(@NotNull SmalltalkParser.LiteralArrayContext ctx) {
        log.info("  visitLiteralArray");
        pushNewObject(mv, "smalltalkArray", "", ctx.LITARR_START().getSymbol().getLine());
        return visitChildren(ctx);
    }

    @Override
    public Void visitLiteralArrayRest(@NotNull SmalltalkParser.LiteralArrayRestContext ctx) {
        log.info("  visitLiteralArrayRest");
        if (ctx.parsetimeLiteral() != null)
            for (SmalltalkParser.ParsetimeLiteralContext literal : ctx.parsetimeLiteral()) {
                throw new RuntimeException("Handle LiteralArray element");
            }
        return null;
    }

    @Override
    public Void visitCharConstant(@NotNull SmalltalkParser.CharConstantContext ctx) {
        log.info("  visitCharConstant {}", ctx.CHARACTER_CONSTANT().getSymbol().getText());
        TerminalNode constant = ctx.CHARACTER_CONSTANT();
        String value = constant.getSymbol().getText().substring(1);
        pushNewObject(mv, "smalltalkCharacter", value, constant.getSymbol().getLine());
        return null;
    }

    @Override
    public Void visitStInteger(@NotNull SmalltalkParser.StIntegerContext ctx) {
        log.info("  visitInteger {}{}", ctx.MINUS() != null  ? "-" : "", nodeFor(ctx.DIGIT()).getText());
        boolean minus = ctx.MINUS() != null;
        SmalltalkGeneratingVisitor.BasicNode number = nodeFor(ctx.DIGIT());
        String value = minus ? "-" + number.getText() : number.getText();
        pushNewObject(mv, "smalltalkInteger", value, number.getLine());
        return null;
    }

    @Override
    public Void visitStFloat(@NotNull SmalltalkParser.StFloatContext ctx) {
        log.info("  visitFloat");
        throw new RuntimeException("visitFloat handle me.");
    }

    @Override
    public Void visitString(@NotNull SmalltalkParser.StringContext ctx) {
        log.info("  visitString {}", ctx.STRING().getSymbol().getText());
        TerminalNode node = ctx.STRING();
        String value = node.getSymbol().getText();
        value = value.substring(1, value.length() - 1);
        pushNewObject(mv, "smalltalkString", value, node.getSymbol().getLine());
        return null;
    }

    @Override
    public Void visitSymbol(@NotNull SmalltalkParser.SymbolContext ctx) {
        log.info("  visitSymbol #{}", nodeFor(ctx).getText());
        SmalltalkGeneratingVisitor.BasicNode node = nodeFor(ctx);
        String symbol = node.getText();
        if (haveKeyword())
            addArgumentToKeyword(symbol);
        pushNewObject(mv, "smalltalkSymbol", symbol, node.getLine());
        return null;
    }

    private SmalltalkGeneratingVisitor.BasicNode nodeFor(SmalltalkParser.SymbolContext ctx) {
        SmalltalkParser.BareSymbolContext bareSymbolContext = ctx.bareSymbol();
        TerminalNode node = bareSymbolContext.IDENTIFIER();
        if (node != null)
            return new SmalltalkGeneratingVisitor.ExtendedTerminalNode(node, 0);
        node = bareSymbolContext.BINARY_SELECTOR();
        if (node != null)
            return new SmalltalkGeneratingVisitor.ExtendedTerminalNode(node, 0);
        List<TerminalNode> keywords = bareSymbolContext.KEYWORD();
        if (keywords != null && !keywords.isEmpty())
            return nodeFor(keywords);
        List<TerminalNode> pipe = bareSymbolContext.PIPE();
        if (pipe != null && !pipe.isEmpty())
            return nodeFor(pipe);
        throw new RuntimeException("Node cannot be determined from context.");
    }

    private SmalltalkGeneratingVisitor.BasicNode nodeFor(List<TerminalNode> nodes) {
        int line = nodes.get(0).getSymbol().getLine();
        StringBuilder text = new StringBuilder();
        for (TerminalNode n : nodes)
            text.append(n.getSymbol().getText());
        return new SmalltalkGeneratingVisitor.BasicNode(line, text.toString(), 0);
    }

    @Override
    public Void visitReference(@NotNull SmalltalkParser.ReferenceContext ctx) {
        log.info("  visitReference {}", ctx.variable().IDENTIFIER().getSymbol().getText());
        TerminalNode identifier = ctx.variable().IDENTIFIER();
        String name = identifier.getSymbol().getText();
        visitLine(mv, identifier.getSymbol().getLine());
        if (isTemporary(name))
            pushTemporary(mv, indexOfTemporary(name));
        else if (isArgument(name))
            pushArgument(mv, indexOfArgument(name));
        else if ("JVM".equals(name))
            referencedJVM = true;
        else if (isHomeTemporary(name))
            pushHomeTemporary(mv, indexOfHomeTemporary(name));
        else if (isHomeArgument(name))
            pushHomeArgument(mv, indexOfHomeArgument(name));
        else if (isOuterArgument(name))
            pushOuterArgument(mv, indexOfOuterArgument(name));
        else if (Character.isUpperCase(name.codePointAt(0)))
            pushReference(mv, name); // May be Class reference or InstVar.
        else
            pushInstVar(mv, name); // Assume instVar.
        return null;
    }

    @Override
    public Void visitBlock(@NotNull SmalltalkParser.BlockContext ctx) {
        log.info("  visitBlock {} {}", peekKeyword(), blockNumber);
        SmalltalkGeneratingVisitor.KeywordRecord keywordRecord = peekKeyword();
        String name = makeBlockMethodName(keywordRecord);
        boolean methodBlock = keywordRecord.keyword.toString().endsWith("withMethod:");
        HashMap<String, SmalltalkGeneratingVisitor.ExtendedTerminalNode> homeTemps = homeTemporaries;
        if (homeTemps == null && !methodBlock)
            homeTemps = temporaries;
        HashMap<String, SmalltalkGeneratingVisitor.ExtendedTerminalNode> homeArgs = homeArguments;
        if (homeArgs == null && !methodBlock)
            homeArgs = arguments;
        BlockGeneratorVisitor blockGeneratorVisitor = new BlockGeneratorVisitor(classGen, cw, name, blockNumber,
                homeTemps, homeArgs, arguments);
        classGen.pushCurrentVisitor(blockGeneratorVisitor);
        blockGeneratorVisitor.handleBlock(ctx);
        blockNumber = blockGeneratorVisitor.blockNumber;
        removeJVMGeneratorVisitor();
        classGen.popCurrentVisitor();
        int line = ctx.BLOCK_START().getSymbol().getLine();
        if (methodBlock)
            pushNewMethod(mv, fullClassName(), name, LAMBDA_BLOCK_SIG, line);
        else {
            String blockAnswerClassName = makeBlockAnswerClassName(name);
            pushNewBlock(mv, fullClassName(), name, LAMBDA_BLOCK_SIG, line, blockGeneratorVisitor.isAnswerBlock(), blockAnswerClassName);
            if (blockGeneratorVisitor.isAnswerBlock()) {
                loadBlockAnswerClass(blockAnswerClassName);
                tryCatchRecords.add(new SmalltalkGeneratingVisitor.BlockAnswerRecord(blockAnswerClassName));
            }
        }
        return null;
    }

    private void loadBlockAnswerClass(String blockAnswerClassName) {
        log.info("  loadBlockAnswerClass: {}", blockAnswerClassName);
        byte[] classBytes = createBlockAnswerClass(blockAnswerClassName);
        SmalltalkClassLoader classLoader = classLoader();
        classLoader.defineClass(classBytes);
    }

    private SmalltalkClassLoader classLoader() {
        return (SmalltalkClassLoader) Thread.currentThread().getContextClassLoader();
    }

    private byte[] createBlockAnswerClass(String blockAnswerClassName) {
        String className = blockAnswerClassName.replaceAll("\\.", "/");
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        MethodVisitor mv;
        cw.visit(BYTECODE_VERSION, ACC_PUBLIC + ACC_SUPER, className, null, "st/redline/core/PrimBlockAnswer", null);
        cw.visitSource(className() + sourceFileExtension(), null);
        mv = cw.visitMethod(ACC_PUBLIC, "<init>", "(Lst/redline/core/PrimObject;)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0); //this
        mv.visitVarInsn(ALOAD, 1); //first argument
        mv.visitMethodInsn(INVOKESPECIAL, "st/redline/core/PrimBlockAnswer", "<init>", "(Lst/redline/core/PrimObject;)V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    @Override
    public Void visitSubexpression(@NotNull SmalltalkParser.SubexpressionContext ctx) {
        log.info("  visitSubexpression");
        // 2 + (  (3 * 4) - 1  )
        // evaluated last (   (evaluated first)  evaluated second  )
        SmalltalkParser.ExpressionContext expression = ctx.expression();
        expression.accept(currentVisitor());
        return null;
    }

    @Override
    public Void visitVariable(@NotNull SmalltalkParser.VariableContext ctx) {
        throw new RuntimeException("visitVariable should have been handed before now.");
    }

    private String makeBlockAnswerClassName(String blockName) {
        return packageName() + '.' + className() + "$A" + blockName;
    }

    private String makeBlockMethodName(SmalltalkGeneratingVisitor.KeywordRecord keywordRecord) {
        StringBuilder name = new StringBuilder();
        if (keywordRecord.firstArgument.length() == 0) {
            blockNumber++;
            name.append("B").append(blockNumber);
        } else
            name.append(keywordRecord.firstArgument.toString());
        return name.toString();
    }

    @Override
    public Void visitMethodGroup(SmalltalkParser.MethodGroupContext ctx) {
        final String className = ctx.className().getText();
        boolean isClassMethod = false;
        if (ctx.classSelector() != null) {
            final String classSelector = ctx.classSelector().getText().trim();
            if (!classSelector.equals("class")) {
                throw new SmalltalkCompilationError("Unknown selector '"+classSelector+"'");
            }
            isClassMethod = true;
        }
        final String methodGroupName = ctx.methodGroupName().getText();

        SmalltalkMethodGroupVisitor methodGroupVisitor = new SmalltalkMethodGroupVisitor(className, methodGroupName, isClassMethod);

        final List<SmalltalkParser.MethodDeclarationContext> methods = ctx.methodDeclaration();
        if (methods != null) {
            for (SmalltalkParser.MethodDeclarationContext methodDecl : methods) {
                methodDecl.accept(methodGroupVisitor);
            }
        }

        return null;
    }

    @Override
    public Void visitMethodDeclaration(SmalltalkParser.MethodDeclarationContext ctx) {
        throw new UnsupportedOperationException("Method declaration is not supported yet");
    }

    @Override
    public Void visitMethodHeader(SmalltalkParser.MethodHeaderContext ctx) {
        throw new UnsupportedOperationException("Method header is not supported yet");
    }
}
