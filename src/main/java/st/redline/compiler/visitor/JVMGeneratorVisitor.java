package st.redline.compiler.visitor;

import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import st.redline.compiler.ClassGenerator;
import st.redline.compiler.generated.SmalltalkParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JVMGeneratorVisitor extends ClassGeneratorVisitor {
    private static final Logger log = LoggerFactory.getLogger(JVMGeneratorVisitor.class);

    private List<Object> arguments = new ArrayList<Object>();

    public JVMGeneratorVisitor(ClassGenerator classGenerator, ClassWriter cw, MethodVisitor mv) {
        super(classGenerator, cw, mv);
    }

    @Override
    public Void visitMessage(@NotNull SmalltalkParser.MessageContext ctx) {
        log.info("  visitMessage");
        SmalltalkParser.KeywordMessageContext keywordMessage = ctx.keywordMessage();
        if (keywordMessage != null)
            return keywordMessage.accept(currentVisitor());
        throw new RuntimeException("visitMessage no alternative found.");
    }

    @Override
    public Void visitKeywordMessage(@NotNull SmalltalkParser.KeywordMessageContext ctx) {
        log.info("  visitKeywordMessage");
        initializeKeyword();
        for (SmalltalkParser.KeywordPairContext keywordPair : ctx.keywordPair())
            keywordPair.accept(currentVisitor());
        visitLine(mv, ctx.keywordPair().get(0).KEYWORD().getSymbol().getLine());
        String keyword = removeKeyword();
        JVMWriter jvmWriter = JVM_WRITERS.get(keyword);
        if (jvmWriter == null)
            throw new RuntimeException("JVM keyword not recognized.");
        jvmWriter.write(mv, arguments);
        return null;
    }

    @Override
    protected void initializeKeyword() {
        super.initializeKeyword();
        arguments = new ArrayList<>();
    }

    @Override
    public Void visitKeywordPair(@NotNull SmalltalkParser.KeywordPairContext ctx) {
        log.info("  visitKeywordPair {}", ctx.KEYWORD().getSymbol().getText());
        TerminalNode keyword = ctx.KEYWORD();
        String part = keyword.getSymbol().getText();
        visitLine(mv, keyword.getSymbol().getLine());
        addToKeyword(part);
        SmalltalkParser.BinarySendContext binarySend = ctx.binarySend();
        arguments.add(argumentFrom(binarySend));
        return null;
    }

    private Object argumentFrom(SmalltalkParser.BinarySendContext binarySend) {
        SmalltalkParser.UnarySendContext unarySend = binarySend.unarySend();
        if (unarySend != null) {
            SmalltalkParser.OperandContext operand = unarySend.operand();
            if (operand != null) {
                SmalltalkParser.LiteralContext literal = operand.literal();
                if (literal != null) {
                    SmalltalkParser.ParsetimeLiteralContext parsetimeLiteral = literal.parsetimeLiteral();
                    if (parsetimeLiteral != null) {
                        SmalltalkParser.StringContext string = parsetimeLiteral.string();
                        if (string != null) {
                            String raw = string.STRING().getSymbol().getText();
                            return raw.substring(1, raw.length() - 1);
                        }
                        SmalltalkParser.NumberContext number = parsetimeLiteral.number();
                        if (number != null) {
                            return number.getText();
                        }
                        SmalltalkParser.SymbolContext symbol = parsetimeLiteral.symbol();
//                            if (symbol != null) {
//                                SmalltalkParser.BareSymbolContext bareSymbol = symbol.bareSymbol();
//                                List<TerminalNode> keyword = bareSymbol.KEYWORD();
//                                if (keyword != null) {
//                                    System.out.println("here");
//                                }
//                            }
                        throw new RuntimeException("Unhandled JVM keyword argument.");
                    }
                }
            }
        }
        throw new RuntimeException("JVM keyword argument expected.");
    }


    protected interface JVMWriter {
        void write(MethodVisitor mv, List<Object> arguments);
    }

    private static final Map<String, JVMWriter> JVM_WRITERS = new HashMap<String, JVMWriter>();
    static {
        JVM_WRITERS.put("aload:", new JVMWriter() {
            public void write(MethodVisitor mv, List<Object> arguments) {
                mv.visitVarInsn(ALOAD, Integer.valueOf(String.valueOf(arguments.get(0))));
            }
        });
        JVM_WRITERS.put("invokeVirtual:method:matching:", new JVMWriter() {
            public void write(MethodVisitor mv, List<Object> arguments) {
                mv.visitMethodInsn(INVOKEVIRTUAL, String.valueOf(arguments.get(0)), String.valueOf(arguments.get(1)), String.valueOf(arguments.get(2)), false);
            }
        });
        JVM_WRITERS.put("invokeStatic:method:matching:", new JVMWriter() {
            public void write(MethodVisitor mv, List<Object> arguments) {
                mv.visitMethodInsn(INVOKESTATIC, String.valueOf(arguments.get(0)), String.valueOf(arguments.get(1)), String.valueOf(arguments.get(2)), false);
            }
        });
        JVM_WRITERS.put("getStatic:named:as:", new JVMWriter() {
            public void write(MethodVisitor mv, List<Object> arguments) {
                mv.visitFieldInsn(GETSTATIC, String.valueOf(arguments.get(0)), String.valueOf(arguments.get(1)), String.valueOf(arguments.get(2)));
            }
        });
        // ## Redline Additions - these are not true JVM instructions but helpers.
        JVM_WRITERS.put("argLoad:", new JVMWriter() {
            public void write(MethodVisitor mv, List<Object> arguments) {
                mv.visitVarInsn(ALOAD, 2);  // Load Context
                pushNumber(mv, Integer.valueOf(String.valueOf(arguments.get(0))));  // Load index of argument we want to get.
                mv.visitMethodInsn(INVOKEVIRTUAL, "st/redline/core/PrimContext", "argumentAt", "(I)Lst/redline/core/PrimObject;", false);
            }
        });
        JVM_WRITERS.put("primitive:", new JVMWriter() {
            public void write(MethodVisitor mv, List<Object> arguments) {
                mv.visitVarInsn(ALOAD, 1);  // Load Receiver
                mv.visitVarInsn(ALOAD, 2);  // Load Context
                String methodName = "primitive" + String.valueOf(arguments.get(0));
                mv.visitMethodInsn(INVOKEVIRTUAL, "st/redline/core/PrimObject", methodName, "(Lst/redline/core/PrimContext;)Lst/redline/core/PrimObject;", false);
            }
        });
    }
}
