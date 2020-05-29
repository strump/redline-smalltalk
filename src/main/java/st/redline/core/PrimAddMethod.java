/* Redline Smalltalk, Copyright (c) James C. Ladd. All rights reserved. See LICENSE in the root of this distribution. */
package st.redline.core;

public class PrimAddMethod extends PrimMethod {

    @Override
    protected PrimObject invoke(PrimObject receiver, PrimContext context) {
        String selector = selector(context);
        PrimObject method = method(context);
        if (!(method instanceof PrimMethod)) {
            PrimMethod newMethod = new PrimMethod((LambdaBlock) method.javaValue());
            newMethod.selfClass(method.selfClass());
            method = newMethod;
        }
        ((PrimClass) receiver).addMethod(selector, method);
        return receiver;
    }

    private String selector(PrimContext context) {
        return String.valueOf(context.argumentJavaValueAt(0));
    }

    private PrimObject method(PrimContext context) {
        return context.argumentAt(1);
    }
}
