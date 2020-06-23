package st.redline.compiler.visitor;

import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import st.redline.compiler.ClassGenerator;
import st.redline.compiler.generated.SmalltalkParser;

public class LiteralArrayVisitor extends ClassGeneratorVisitor {
    private static final Logger log = LogManager.getLogger(LiteralArrayVisitor.class);

    public LiteralArrayVisitor(ClassGenerator classGen, ClassWriter cw, MethodVisitor mv) {
        super(classGen, cw, mv);
    }


    /* Generate Smalltalk Array:
       <code>
       smalltalkArray(new Object[] {
           smalltalkSymbol("aaa"),
           smalltalkNumber(100),
           nil(),
           ...,
       })
       </code>
     */
    @Override
    public Void visitLiteralArrayRest(@NotNull SmalltalkParser.LiteralArrayRestContext ctx) {
        log.trace("  visitLiteralArrayRest");
        int arraySize = calcLiteralArraySize(ctx);

        pushReceiver(mv);
        pushNumber(mv, arraySize);
        mv.visitTypeInsn(ANEWARRAY, PRIM_OBJECT_CLASS);

        int index = 0;
        for (ParseTree child : ctx.children) {
            if ( !(child instanceof SmalltalkParser.ParsetimeLiteralContext) &&
                    !(child instanceof SmalltalkParser.BareLiteralArrayContext) &&
                    !(child instanceof SmalltalkParser.BareSymbolContext))
            {
                //Assuming child is a whitespace or a CLOSE_PAREN token
                continue;
            }

            mv.visitInsn(DUP);
            pushNumber(mv, index);
            if (child instanceof SmalltalkParser.ParsetimeLiteralContext) {
                this.visitParsetimeLiteral((SmalltalkParser.ParsetimeLiteralContext) child);
            }
            else if (child instanceof SmalltalkParser.BareLiteralArrayContext) {
                this.visitBareLiteralArray((SmalltalkParser.BareLiteralArrayContext) child);
            }
            else if (child instanceof SmalltalkParser.BareSymbolContext) {
                this.visitBareSymbol((SmalltalkParser.BareSymbolContext) child);
            }
            else {
                throw new UnsupportedOperationException("Unknown child of type: " + child.getClass() +
                        " at line "+ctx.start.getLine() + ". Text: '"+child.getText()+"'");
            }
            mv.visitInsn(AASTORE); // Put object to array at index `index`
            index ++;
        }

        mv.visitMethodInsn(INVOKEVIRTUAL, PRIM_OBJECT_CLASS, "smalltalkArray", "([Lst/redline/core/PrimObject;)Lst/redline/core/PrimObject;", false);
        return null;
    }

    //Count all parseTimeLiteral, bareLiteralArray and baseSymbol children
    private int calcLiteralArraySize(SmalltalkParser.LiteralArrayRestContext ctx) {
        int size = 0;
        for (ParseTree child : ctx.children) {
            if ( (child instanceof SmalltalkParser.ParsetimeLiteralContext) ||
                    (child instanceof SmalltalkParser.BareLiteralArrayContext) ||
                    (child instanceof SmalltalkParser.BareSymbolContext))
                size ++;
        }
        return size;
    }

    @Override
    public Void visitParsetimeLiteral(@NotNull SmalltalkParser.ParsetimeLiteralContext ctx) {
        log.trace("  visitParsetimeLiteral");
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
        log.trace("  visitPseudoVariable {}", ctx.RESERVED_WORD().getSymbol().getText());
        TerminalNode pseudoVariable = ctx.RESERVED_WORD();
        String name = pseudoVariable.getSymbol().getText();
        if ("self".equals(name))
            pushNewObject(mv, "smalltalkSymbol", "#self", pseudoVariable.getSymbol().getLine());
        else if ("nil".equals(name))
            pushNil(mv);
        else if ("true".equals(name))
            pushTrue(mv);
        else if ("false".equals(name))
            pushFalse(mv);
        else if ("super".equals(name))
            pushNewObject(mv, "smalltalkSymbol", "#super", pseudoVariable.getSymbol().getLine());
        else
            throw new RuntimeException("visitPseudoVariable unknown variable: " + name);
        return null;
    }

    @Override
    public Void visitBareSymbol(SmalltalkParser.BareSymbolContext ctx) {
        log.trace("  visitBareSymbol {}", ctx.getText());
        final String symbolText = ctx.getText();
        pushNewObject(mv, "smalltalkSymbol", symbolText, ctx.start.getLine());
        return null;
    }

    @Override
    public Void visitBareLiteralArray(SmalltalkParser.BareLiteralArrayContext ctx) {
        log.trace("  visitBareLiteralArray");
        LiteralArrayVisitor literalArrayVisitor = new LiteralArrayVisitor(classGen, cw, mv);

        classGen.pushCurrentVisitor(literalArrayVisitor);
        ctx.literalArrayRest().accept(literalArrayVisitor);
        classGen.popCurrentVisitor();

        return null;
    }
}
