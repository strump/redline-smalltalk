/* Redline Smalltalk, Copyright (c) James C. Ladd. All rights reserved. See LICENSE in the root of this distribution. */
package st.redline.core;

public class PrimAddMethod extends PrimMethod {

    @Override
    protected PrimObject invoke(PrimObject receiver, PrimContext context) {
        String selector = selector(context);
        PrimMethod method = method(context);
        ((PrimClass) receiver).addMethod(selector, method);
        return receiver;
    }

    private String selector(PrimContext context) {
        return String.valueOf(context.argumentJavaValueAt(0));
    }

    private PrimMethod method(PrimContext context) {
        PrimObject method = context.argumentAt(1);
        if (!(method instanceof PrimMethod)) {
            PrimMethod newMethod = new PrimMethod((LambdaBlock) method.javaValue());
            newMethod.selfClass(method.selfClass());
            return newMethod;
        }
        else {
            return (PrimMethod) method;
        }
    }
}
