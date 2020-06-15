/* Redline Smalltalk, Copyright (c) James C. Ladd. All rights reserved. See LICENSE in the root of this distribution. */
package st.redline.compiler.visitor;

import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.objectweb.asm.*;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import st.redline.compiler.ClassGenerator;
import st.redline.compiler.generated.SmalltalkBaseVisitor;
import st.redline.compiler.generated.SmalltalkParser;
import st.redline.compiler.generated.SmalltalkVisitor;
import st.redline.core.PrimContext;
import st.redline.core.PrimObject;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/* Base class for all other visitors. Contains common method for bytecode generation. */
public class SmalltalkGeneratingVisitor extends SmalltalkBaseVisitor<Void> implements SmalltalkVisitor<Void>, Opcodes {
    private static final Logger log = LogManager.getLogger(SmalltalkGeneratingVisitor.class);

    public static final String DEFAULT_IMPORTED_PACKAGE = "st.redline.kernel";

    public static final String PRIM_OBJECT_CLASS = PrimObject.class.getCanonicalName().replace('.', '/'); //"st/redline/core/PrimObject";
    public static final String PRIM_CONTEXT_CLASS = PrimContext.class.getCanonicalName().replace('.', '/'); //"st/redline/core/PrimContext";

    protected static final String[] PERFORM_METHOD_SIGNATURES = {
            "(Ljava/lang/String;)Lst/redline/core/PrimObject;",
            "(Lst/redline/core/PrimObject;Ljava/lang/String;)Lst/redline/core/PrimObject;",
            "(Lst/redline/core/PrimObject;Lst/redline/core/PrimObject;Ljava/lang/String;)Lst/redline/core/PrimObject;",
            "(Lst/redline/core/PrimObject;Lst/redline/core/PrimObject;Lst/redline/core/PrimObject;Ljava/lang/String;)Lst/redline/core/PrimObject;",
            "(Lst/redline/core/PrimObject;Lst/redline/core/PrimObject;Lst/redline/core/PrimObject;Lst/redline/core/PrimObject;Ljava/lang/String;)Lst/redline/core/PrimObject;",
            "(Lst/redline/core/PrimObject;Lst/redline/core/PrimObject;Lst/redline/core/PrimObject;Lst/redline/core/PrimObject;Lst/redline/core/PrimObject;Ljava/lang/String;)Lst/redline/core/PrimObject;",
            "(Lst/redline/core/PrimObject;Lst/redline/core/PrimObject;Lst/redline/core/PrimObject;Lst/redline/core/PrimObject;Lst/redline/core/PrimObject;Lst/redline/core/PrimObject;Ljava/lang/String;)Lst/redline/core/PrimObject;",
            "(Lst/redline/core/PrimObject;Lst/redline/core/PrimObject;Lst/redline/core/PrimObject;Lst/redline/core/PrimObject;Lst/redline/core/PrimObject;Lst/redline/core/PrimObject;Lst/redline/core/PrimObject;Ljava/lang/String;)Lst/redline/core/PrimObject;",
            "(Lst/redline/core/PrimObject;Lst/redline/core/PrimObject;Lst/redline/core/PrimObject;Lst/redline/core/PrimObject;Lst/redline/core/PrimObject;Lst/redline/core/PrimObject;Lst/redline/core/PrimObject;Lst/redline/core/PrimObject;Ljava/lang/String;)Lst/redline/core/PrimObject;"
    };
    protected static final String PERFORM_METHOD_ARRAY_SIGNATURE = "([Lst/redline/core/PrimObject;Ljava/lang/String;)Lst/redline/core/PrimObject;";
    protected static final Map<String, Integer> OPCODES = new HashMap<>();
    protected static final int BYTECODE_VERSION;

    static {
        int compareTo18 = new BigDecimal(System.getProperty("java.specification.version")).compareTo(new BigDecimal("1.8"));
        if (compareTo18 >= 0) {
            BYTECODE_VERSION = V1_8;
        } else {
            throw new RuntimeException("Java 1.8 or above required.");
        }
    }

    //Link to ClassGenerator with generator context
    protected final ClassGenerator classGen;

    public SmalltalkGeneratingVisitor(ClassGenerator classGenerator) {
        this.classGen = classGenerator;
    }

    public Void visitScript(@NotNull SmalltalkParser.ScriptContext ctx) {
        classGen.currentVisitor().visitScript(ctx);
        return null;
    }

    public String className() {
        return classGen.className();
    }

    public String sourceFileExtension() {
        return classGen.fileExtension();
    }

    public String fullClassName() {
        return classGen.fullClassName();
    }

    public String packageName() {
        return classGen.packageName();
    }

    public String superclassName() {
        return "st/redline/core/PrimModule";
    }

    public String contextName() {
        return "st/redline/core/PrimContext";
    }

    public SmalltalkVisitor<Void> currentVisitor() {
        return classGen.currentVisitor();
    }

    public int opcodeValue(String opcode) {
        if (!OPCODES.containsKey(opcode))
            throw new IllegalStateException("Unknown OPCODE '" + opcode + "'.");
        return OPCODES.get(opcode);
    }

    public void pop(MethodVisitor mv) {
        mv.visitInsn(POP);
    }

    public void pushBoolean(MethodVisitor mv, boolean value) {
        mv.visitInsn(value ? ICONST_1 : ICONST_0);
    }

    public void pushLiteral(MethodVisitor mv, String literal) {
        mv.visitLdcInsn(literal);
    }

    public void pushDuplicate(MethodVisitor mv) {
        mv.visitInsn(DUP);
    }

    public void pushThis(MethodVisitor mv) {
        mv.visitVarInsn(ALOAD, 0);
    }

    public void pushReceiver(MethodVisitor mv) {
        mv.visitVarInsn(ALOAD, 1);
    }

    public void pushSuper(MethodVisitor mv, int line) {
        visitLine(mv, line);
        pushReceiver(mv);
    }

    public void pushContext(MethodVisitor mv) {
        mv.visitVarInsn(ALOAD, 2);
    }

    public void pushNull(MethodVisitor mv) {
        mv.visitInsn(ACONST_NULL);
    }

    /* Generate code:
     * <code>
       primContext.temporaryAt(index);
       </code>
     */
    public void pushTemporary(MethodVisitor mv, int index) {
        pushContext(mv);
        pushNumber(mv, index);
        mv.visitMethodInsn(INVOKEVIRTUAL, contextName(), "temporaryAt", "(I)Lst/redline/core/PrimObject;", false);
    }

    /* Generate code:
     * <code>
       primContext.homeTemporaryAt(index);
       </code>
     */
    public void pushHomeTemporary(MethodVisitor mv, int index) {
        pushContext(mv);
        pushNumber(mv, index);
        mv.visitMethodInsn(INVOKEVIRTUAL, contextName(), "homeTemporaryAt", "(I)Lst/redline/core/PrimObject;", false);
    }

    /* Generate code:
     * <code>
       static PrimContext.temporaryPutAt(primObject, index, primContext);
       </code>
     */
    public void storeTemporary(MethodVisitor mv, int index) {
        pushNumber(mv, index);
        pushContext(mv);
        mv.visitMethodInsn(INVOKESTATIC, contextName(), "temporaryPutAt", "(Lst/redline/core/PrimObject;IL" + contextName() + ";)V", false);
    }

    /* Generate code:
     * <code>
       primContext.instVarAt(var);
       </code>
     */
    public void pushInstVar(MethodVisitor mv, String var) {
        pushContext(mv);
        pushLiteral(mv, var);
        mv.visitMethodInsn(INVOKEVIRTUAL, contextName(), "instVarAt", "(Ljava/lang/String;)Lst/redline/core/PrimObject;", false);
    }

    /* Generate code:
     * <code>
       static PrimContext.instVarPutAt(primObject, identifier, primContext);
       </code>
     */
    public void storeInstVar(MethodVisitor mv, String identifier) {
        pushLiteral(mv, identifier);
        pushContext(mv);
        mv.visitMethodInsn(INVOKESTATIC, contextName(), "instVarPutAt", "(Lst/redline/core/PrimObject;Ljava/lang/String;L" + contextName() + ";)V", false);
    }

    /* Generate code:
     * <code>
       static PrimContext.homeTemporaryPutAt(primObject, index, primContext);
       </code>
     */
    public void storeHomeTemporary(MethodVisitor mv, int index) {
        pushNumber(mv, index);
        pushContext(mv);
        mv.visitMethodInsn(INVOKESTATIC, contextName(), "homeTemporaryPutAt", "(Lst/redline/core/PrimObject;IL" + contextName() + ";)V", false);
    }

    /* Generate code:
     * <code>
       primContext.argumentAt(index);
       </code>
     */
    public void pushArgument(MethodVisitor mv, int index) {
        pushContext(mv);
        pushNumber(mv, index);
        mv.visitMethodInsn(INVOKEVIRTUAL, contextName(), "argumentAt", "(I)Lst/redline/core/PrimObject;", false);
    }

    /* Generate code:
     * <code>
       primContext.outerArgumentAt(index);
       </code>
     */
    public void pushOuterArgument(MethodVisitor mv, int index) {
        pushContext(mv);
        pushNumber(mv, index);
        mv.visitMethodInsn(INVOKEVIRTUAL, contextName(), "outerArgumentAt", "(I)Lst/redline/core/PrimObject;", false);
    }

    /* Generate code:
     * <code>
       primContext.homeArgumentAt(index);
       </code>
     */
    public void pushHomeArgument(MethodVisitor mv, int index) {
        pushContext(mv);
        pushNumber(mv, index);
        mv.visitMethodInsn(INVOKEVIRTUAL, contextName(), "homeArgumentAt", "(I)Lst/redline/core/PrimObject;", false);
    }

    /* Generate code:
     * <code>
       primObject.reference(name);
       </code>
     */
    public void pushReference(MethodVisitor mv, String name) {
        pushReceiver(mv);
        pushLiteral(mv, name);
        mv.visitMethodInsn(INVOKEVIRTUAL, PRIM_OBJECT_CLASS, "reference", "(Ljava/lang/String;)Lst/redline/core/PrimObject;", false);
    }

    /* Generate code:
     * <code>
       primObject.reference(name);
       </code>
     */
    public void pushResolveClass(MethodVisitor mv, String className) {
        pushReceiver(mv);
        pushLiteral(mv, className);
        mv.visitMethodInsn(INVOKEVIRTUAL, PRIM_OBJECT_CLASS, "resolveClass", "(Ljava/lang/String;)Lst/redline/core/PrimClass;", false);
    }

    /* Generate code:
     * <code>
       primObject.referenceNil();
       </code>
     */
    public void pushNil(MethodVisitor mv) {
        pushReceiver(mv);
        mv.visitMethodInsn(INVOKEVIRTUAL, PRIM_OBJECT_CLASS, "referenceNil", "()Lst/redline/core/PrimObject;", false);
    }

    /* Generate code:
     * <code>
       primObject.referenceTrue();
       </code>
     */
    public void pushTrue(MethodVisitor mv) {
        pushReceiver(mv);
        mv.visitMethodInsn(INVOKEVIRTUAL, PRIM_OBJECT_CLASS, "referenceTrue", "()Lst/redline/core/PrimObject;", false);
    }

    /* Generate code:
     * <code>
       primObject.referenceFalse();
       </code>
     */
    public void pushFalse(MethodVisitor mv) {
        pushReceiver(mv);
        mv.visitMethodInsn(INVOKEVIRTUAL, PRIM_OBJECT_CLASS, "referenceFalse", "()Lst/redline/core/PrimObject;", false);
    }

    /* Generate code (assuming arguments are already on stack):
     * <code>
       primObject.perform(arg0, selector);
       primObject.perform(arg0, arg1, selector);
       primObject.perform(arg0, arg1, ..., argN, selector);
       primObject.superPerform(arg0, selector);
       primObject.superPerform(arg0, arg1, selector);
       primObject.superPerform(arg0, arg1, ..., argN, selector);
       </code>
     */
    public void invokePerform(MethodVisitor mv, String selector, int argumentCount, boolean sendToSuper) {
        pushLiteral(mv, selector);
        String methodName = (sendToSuper) ? "superPerform" : "perform";
        if (argumentCount < PERFORM_METHOD_SIGNATURES.length) {
            mv.visitMethodInsn(INVOKEVIRTUAL, PRIM_OBJECT_CLASS, methodName, PERFORM_METHOD_SIGNATURES[argumentCount], false);
        }
        else {
            mv.visitMethodInsn(INVOKEVIRTUAL, PRIM_OBJECT_CLASS, methodName, PERFORM_METHOD_ARRAY_SIGNATURE, false);
        }
    }

    public void visitLine(MethodVisitor mv, int line) {
        Label l0 = new Label();
        mv.visitLabel(l0);
        mv.visitLineNumber(line, l0);
    }

    /* Push integer on stack */
    public static void pushNumber(MethodVisitor mv, int value) {
        switch (value) {
            case 0: mv.visitInsn(ICONST_0); break;
            case 1: mv.visitInsn(ICONST_1); break;
            case 2: mv.visitInsn(ICONST_2); break;
            case 3: mv.visitInsn(ICONST_3); break;
            case 4: mv.visitInsn(ICONST_4); break;
            case 5: mv.visitInsn(ICONST_5); break;
            default:
                if (value > 5 && value < 128)
                    mv.visitIntInsn(BIPUSH, value);
                else // SIPUSH not supported yet.
                    throw new IllegalStateException("push of integer value " + value + " not yet supported.");
        }
    }

    /* Convert Java value to Smalltalk PrimObject value and put on stack:
     * <code>
       primObject.{type}(value);
       </code>
       `type` could be "smalltalkArray", "smalltalkCharacter", "smalltalkInteger", "smalltalkString" or "smalltalkSymbol"
     */
    public void pushNewObject(MethodVisitor mv, String type, String value, int line) {
        visitLine(mv, line);
        pushReceiver(mv);
        pushLiteral(mv, value);
        mv.visitMethodInsn(INVOKEVIRTUAL, PRIM_OBJECT_CLASS, type, "(Ljava/lang/Object;)Lst/redline/core/PrimObject;", false);
    }

    /* Create BlockClosure class to wrap smalltalk block lambda (with ^ answer or without)
     * <code>
       primObject.smalltalkBlock(lambdaObject, context);
       primObject.smalltalkBlockAnswer(lambdaObject, context);
       </code>
       `type` could be "smalltalkArray", "smalltalkCharacter", "smalltalkInteger", "smalltalkString" or "smalltalkSymbol"
     */
    public void pushNewBlock(MethodVisitor mv, String className, String name, String sig, int line, boolean answerBlock, String answerBlockClassName) {
        pushReceiver(mv);
        pushNewLambda(mv, className, name, sig, line);
        pushContext(mv);
        if (!answerBlock) {
            mv.visitMethodInsn(INVOKEVIRTUAL, PRIM_OBJECT_CLASS, "smalltalkBlock", "(Ljava/lang/Object;Lst/redline/core/PrimContext;)Lst/redline/core/PrimObject;", false);
        } else {
            pushLiteral(mv, answerBlockClassName);
            mv.visitMethodInsn(INVOKEVIRTUAL, PRIM_OBJECT_CLASS, "smalltalkBlockAnswer", "(Ljava/lang/Object;Lst/redline/core/PrimContext;Ljava/lang/String;)Lst/redline/core/PrimObject;", false);
        }
    }

    public void pushNewMethod(MethodVisitor mv, String className, String name, String sig, int line) {
        pushReceiver(mv);
        pushNewLambda(mv, className, name, sig, line);
        mv.visitMethodInsn(INVOKEVIRTUAL, PRIM_OBJECT_CLASS, "smalltalkMethod", "(Ljava/lang/Object;)Lst/redline/core/PrimMethod;", false);
    }

    /* Generate lambda function with LambdaBlock interface. Body of the lambda function is static method
       created by BlockGeneratorVisitor.
       <code>
       (thiz, reciever, context) -> {
           //lambda body here
       }
       </code>
     */
    public void pushNewLambda(MethodVisitor mv, String className, String methodName, String sig, int line) {
        visitLine(mv, line);

        final Handle bootstrapMethodHandle = new Handle(Opcodes.H_INVOKESTATIC, "java/lang/invoke/LambdaMetafactory", "metafactory",
                "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;", false);
        final Handle lambdaMethodHandle = new Handle(Opcodes.H_INVOKESTATIC, className, methodName, sig, false);
        mv.visitInvokeDynamicInsn("apply", "()Lst/redline/core/LambdaBlock;", bootstrapMethodHandle,
                Type.getType(sig), lambdaMethodHandle, Type.getType(sig));
    }

    protected static class BasicNode {

        private final int index;
        private final int line;
        private final String text;

        public BasicNode(int line, String text, int index) {
            this.line = line;
            this.text = text;
            this.index = index;
        }

        public int getIndex() {
            return index;
        }

        public int getLine() {
            return line;
        }

        public String getText() {
            return text;
        }
    }

    protected static class ExtendedTerminalNode extends BasicNode {

        public ExtendedTerminalNode(TerminalNode node, int index) {
            super(node.getSymbol().getLine(), node.getSymbol().getText(), index);
        }
    }


    protected static class BlockAnswerRecord {

        public final String exceptionName;
        public Label handlerLabel;

        public BlockAnswerRecord(String name) {
            this.exceptionName = name.replaceAll("\\.", "/");
            this.handlerLabel = new Label();
        }
    }

    protected static class KeywordRecord {

        public StringBuilder keyword = new StringBuilder();
        public StringBuilder firstArgument = new StringBuilder();

        public String toString() {
            return keyword + " - " + firstArgument;
        }
    }

    static {
        OPCODES.put("V1_1", 196653);
        OPCODES.put("V1_2", 46);
        OPCODES.put("V1_3", 47);
        OPCODES.put("V1_4", 48);
        OPCODES.put("V1_5", 49);
        OPCODES.put("V1_6", 50);
        OPCODES.put("V1_7", 51);
        OPCODES.put("V1_8", 52);
        OPCODES.put("ACC_PUBLIC", 1);
        OPCODES.put("ACC_PRIVATE", 2);
        OPCODES.put("ACC_PROTECTED", 4);
        OPCODES.put("ACC_STATIC", 8);
        OPCODES.put("ACC_FINAL", 16);
        OPCODES.put("ACC_SUPER", 32);
        OPCODES.put("ACC_SYNCHRONIZED", 32);
        OPCODES.put("ACC_VOLATILE", 64);
        OPCODES.put("ACC_BRIDGE", 64);
        OPCODES.put("ACC_VARARGS", 128);
        OPCODES.put("ACC_TRANSIENT", 128);
        OPCODES.put("ACC_NATIVE", 256);
        OPCODES.put("ACC_INTERFACE", 512);
        OPCODES.put("ACC_ABSTRACT", 1024);
        OPCODES.put("ACC_STRICT", 2048);
        OPCODES.put("ACC_SYNTHETIC", 4096);
        OPCODES.put("ACC_ANNOTATION", 8192);
        OPCODES.put("ACC_ENUM", 16384);
        OPCODES.put("ACC_DEPRECATED", 131072);
        OPCODES.put("T_BOOLEAN", 4);
        OPCODES.put("T_CHAR", 5);
        OPCODES.put("T_FLOAT", 6);
        OPCODES.put("T_DOUBLE", 7);
        OPCODES.put("T_BYTE", 8);
        OPCODES.put("T_SHORT", 9);
        OPCODES.put("T_INT", 10);
        OPCODES.put("T_LONG", 11);
        OPCODES.put("F_NEW", -1);
        OPCODES.put("F_FULL", 0);
        OPCODES.put("F_APPEND", 1);
        OPCODES.put("F_CHOP", 2);
        OPCODES.put("F_SAME", 3);
        OPCODES.put("F_SAME1", 4);
        OPCODES.put("TOP", TOP);
        OPCODES.put("INTEGER", INTEGER);
        OPCODES.put("FLOAT", FLOAT);
        OPCODES.put("DOUBLE", DOUBLE);
        OPCODES.put("LONG", LONG);
        OPCODES.put("NULL", NULL);
        OPCODES.put("UNINITIALIZED_THIS", UNINITIALIZED_THIS);
        OPCODES.put("NOP", 0);
        OPCODES.put("ACONST_NULL", 1);
        OPCODES.put("ICONST_M1", 2);
        OPCODES.put("ICONST_0", 3);
        OPCODES.put("ICONST_1", 4);
        OPCODES.put("ICONST_2", 5);
        OPCODES.put("ICONST_3", 6);
        OPCODES.put("ICONST_4", 7);
        OPCODES.put("ICONST_5", 8);
        OPCODES.put("LCONST_0", 9);
        OPCODES.put("LCONST_1", 10);
        OPCODES.put("FCONST_0", 11);
        OPCODES.put("FCONST_1", 12);
        OPCODES.put("FCONST_2", 13);
        OPCODES.put("DCONST_0", 14);
        OPCODES.put("DCONST_1", 15);
        OPCODES.put("BIPUSH", 16);
        OPCODES.put("SIPUSH", 17);
        OPCODES.put("LDC", 18);
        OPCODES.put("ILOAD", 21);
        OPCODES.put("LLOAD", 22);
        OPCODES.put("FLOAD", 23);
        OPCODES.put("DLOAD", 24);
        OPCODES.put("ALOAD", 25);
        OPCODES.put("IALOAD", 46);
        OPCODES.put("LALOAD", 47);
        OPCODES.put("FALOAD", 48);
        OPCODES.put("DALOAD", 49);
        OPCODES.put("AALOAD", 50);
        OPCODES.put("BALOAD", 51);
        OPCODES.put("CALOAD", 52);
        OPCODES.put("SALOAD", 53);
        OPCODES.put("ISTORE", 54);
        OPCODES.put("LSTORE", 55);
        OPCODES.put("FSTORE", 56);
        OPCODES.put("DSTORE", 57);
        OPCODES.put("ASTORE", 58);
        OPCODES.put("IASTORE", 79);
        OPCODES.put("LASTORE", 80);
        OPCODES.put("FASTORE", 81);
        OPCODES.put("DASTORE", 82);
        OPCODES.put("AASTORE", 83);
        OPCODES.put("BASTORE", 84);
        OPCODES.put("CASTORE", 85);
        OPCODES.put("SASTORE", 86);
        OPCODES.put("POP", 87);
        OPCODES.put("POP2", 88);
        OPCODES.put("DUP", 89);
        OPCODES.put("DUP_X1", 90);
        OPCODES.put("DUP_X2", 91);
        OPCODES.put("DUP2", 92);
        OPCODES.put("DUP2_X1", 93);
        OPCODES.put("DUP2_X2", 94);
        OPCODES.put("SWAP", 95);
        OPCODES.put("IADD", 96);
        OPCODES.put("LADD", 97);
        OPCODES.put("FADD", 98);
        OPCODES.put("DADD", 99);
        OPCODES.put("ISUB", 100);
        OPCODES.put("LSUB", 101);
        OPCODES.put("FSUB", 102);
        OPCODES.put("DSUB", 103);
        OPCODES.put("IMUL", 104);
        OPCODES.put("LMUL", 105);
        OPCODES.put("FMUL", 106);
        OPCODES.put("DMUL", 107);
        OPCODES.put("IDIV", 108);
        OPCODES.put("LDIV", 109);
        OPCODES.put("FDIV", 110);
        OPCODES.put("DDIV", 111);
        OPCODES.put("IREM", 112);
        OPCODES.put("LREM", 113);
        OPCODES.put("FREM", 114);
        OPCODES.put("DREM", 115);
        OPCODES.put("INEG", 116);
        OPCODES.put("LNEG", 117);
        OPCODES.put("FNEG", 118);
        OPCODES.put("DNEG", 119);
        OPCODES.put("ISHL", 120);
        OPCODES.put("LSHL", 121);
        OPCODES.put("ISHR", 122);
        OPCODES.put("LSHR", 123);
        OPCODES.put("IUSHR", 124);
        OPCODES.put("LUSHR", 125);
        OPCODES.put("IAND", 126);
        OPCODES.put("LAND", 127);
        OPCODES.put("IOR", 128);
        OPCODES.put("LOR", 129);
        OPCODES.put("IXOR", 130);
        OPCODES.put("LXOR", 131);
        OPCODES.put("IINC", 132);
        OPCODES.put("I2L", 133);
        OPCODES.put("I2F", 134);
        OPCODES.put("I2D", 135);
        OPCODES.put("L2I", 136);
        OPCODES.put("L2F", 137);
        OPCODES.put("L2D", 138);
        OPCODES.put("F2I", 139);
        OPCODES.put("F2L", 140);
        OPCODES.put("F2D", 141);
        OPCODES.put("D2I", 142);
        OPCODES.put("D2L", 143);
        OPCODES.put("D2F", 144);
        OPCODES.put("I2B", 145);
        OPCODES.put("I2C", 146);
        OPCODES.put("I2S", 147);
        OPCODES.put("LCMP", 148);
        OPCODES.put("FCMPL", 149);
        OPCODES.put("FCMPG", 150);
        OPCODES.put("DCMPL", 151);
        OPCODES.put("DCMPG", 152);
        OPCODES.put("IFEQ", 153);
        OPCODES.put("IFNE", 154);
        OPCODES.put("IFLT", 155);
        OPCODES.put("IFGE", 156);
        OPCODES.put("IFGT", 157);
        OPCODES.put("IFLE", 158);
        OPCODES.put("IF_ICMPEQ", 159);
        OPCODES.put("IF_ICMPNE", 160);
        OPCODES.put("IF_ICMPLT", 161);
        OPCODES.put("IF_ICMPGE", 162);
        OPCODES.put("IF_ICMPGT", 163);
        OPCODES.put("IF_ICMPLE", 164);
        OPCODES.put("IF_ACMPEQ", 165);
        OPCODES.put("IF_ACMPNE", 166);
        OPCODES.put("GOTO", 167);
        OPCODES.put("JSR", 168);
        OPCODES.put("RET", 169);
        OPCODES.put("TABLESWITCH", 170);
        OPCODES.put("LOOKUPSWITCH", 171);
        OPCODES.put("IRETURN", 172);
        OPCODES.put("LRETURN", 173);
        OPCODES.put("FRETURN", 174);
        OPCODES.put("DRETURN", 175);
        OPCODES.put("ARETURN", 176);
        OPCODES.put("RETURN", 177);
        OPCODES.put("GETSTATIC", 178);
        OPCODES.put("PUTSTATIC", 179);
        OPCODES.put("GETFIELD", 180);
        OPCODES.put("PUTFIELD", 181);
        OPCODES.put("INVOKEVIRTUAL", 182);
        OPCODES.put("INVOKESPECIAL", 183);
        OPCODES.put("INVOKESTATIC", 184);
        OPCODES.put("INVOKEINTERFACE", 185);
        OPCODES.put("INVOKEDYNAMIC", 186);
        OPCODES.put("NEW", 187);
        OPCODES.put("NEWARRAY", 188);
        OPCODES.put("ANEWARRAY", 189);
        OPCODES.put("ARRAYLENGTH", 190);
        OPCODES.put("ATHROW", 191);
        OPCODES.put("CHECKCAST", 192);
        OPCODES.put("INSTANCEOF", 193);
        OPCODES.put("MONITORENTER", 194);
        OPCODES.put("MONITOREXIT", 195);
        OPCODES.put("MULTIANEWARRAY", 197);
        OPCODES.put("IFNULL", 198);
        OPCODES.put("IFNONNULL", 199);
    }
}
