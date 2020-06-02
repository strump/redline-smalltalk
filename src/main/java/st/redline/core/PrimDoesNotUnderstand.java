/* Redline Smalltalk, Copyright (c) James C. Ladd. All rights reserved. See LICENSE in the root of this distribution. */
package st.redline.core;

import static st.redline.core.PrimNil.PRIM_NIL;

public class PrimDoesNotUnderstand extends PrimMethod {

    public static final String doesNotUnderstand_SELECTOR = "doesNotUnderstand:";
    public static final PrimMethod PRIM_DOES_NOT_UNDERSTAND = new PrimDoesNotUnderstand();

    protected PrimObject invoke(PrimObject receiver, PrimContext context) {
        if (!doesNotUnderstand_SELECTOR.equals(context.selector()))
            return receiver.perform0(doesNotUnderstand_SELECTOR, context.selectorAndArguments());
        outputDoesNotUnderstandError(receiver, context);
        return PRIM_NIL;
    }

    private void outputDoesNotUnderstandError(PrimObject receiver, PrimContext context) {
        StringBuilder message = new StringBuilder();
        message.append("Object '")
                .append(receiver)
                .append("' of class '")
                .append(receiver.selfClass())
                .append("' does not understand #")
                .append(context.arguments()[0]);
//        if (context.arguments.length > 1) {
//            message.append(" with arguments:\n");
//            for (int i = 1; i < context.arguments.length; i++)
//                message.append(i).append("\t").append(context.arguments[i]).append(" ")
//                        .append(context.arguments[i].javaValue()).append("\n");
//        } else
//            message.append(".");
//        System.out.println();
//        System.out.println();
//        System.out.println("Receiver       " + receiver);
//        if (receiver instanceof ProtoClass)
//            System.out.println("Methods " + ((ProtoClass) receiver).methods);
        throw new DoesNotUnderstandException(message.toString());
    }
}
