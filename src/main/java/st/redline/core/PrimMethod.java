/* Redline Smalltalk, Copyright (c) James C. Ladd. All rights reserved. See LICENSE in the root of this distribution. */
package st.redline.core;

public class PrimMethod extends PrimObject {
    public PrimMethod() {
        super();
    }

    public PrimMethod(LambdaBlock lambdaBlock) {
        this.javaValue(lambdaBlock);
    }

    @Override
    public String toString() {
        final Object value = javaValue();
        return "(PrimMethod) " + (value!=null ? value.toString() : "null");
    }

    protected PrimObject invoke(PrimObject receiver, PrimContext context) {
        // We send receiver as first _and_ second argument as LambdaBlock's are static
        // and we need receiver in argument slot 1
        return ((LambdaBlock) javaValue()).apply(receiver, receiver, context);
    }
}
