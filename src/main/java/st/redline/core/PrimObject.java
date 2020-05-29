/* Redline Smalltalk, Copyright (c) James C. Ladd. All rights reserved. See LICENSE in the root of this distribution. */
package st.redline.core;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import st.redline.classloader.SmalltalkClassLoader;

import java.util.ArrayList;
import java.util.Collections;

import static st.redline.compiler.visitor.SmalltalkGeneratingVisitor.DEFAULT_IMPORTED_PACKAGE;
import static st.redline.core.PrimDoesNotUnderstand.PRIM_DOES_NOT_UNDERSTAND;
import static st.redline.core.PrimSubclass.PRIM_SUBCLASS;

public class PrimObject {
    private static final Logger log = LogManager.getLogger(PrimObject.class);

    private PrimObject selfClass;
    private Object javaValue;

    public String toString() {
        if (javaValue != null)
            return javaValue.toString();
        if (selfClass != null && selfClass != this)
            return selfClass.toString();
        return super.toString();
    }

    public void javaValue(Object object) {
        javaValue = object;
    }

    public Object javaValue() {
        return javaValue;
    }

    public void selfClass(PrimObject primObject) {
        selfClass = primObject;
    }

    public PrimObject selfClass() {
        return selfClass;
    }

    public PrimObject referenceNil() {
        //System.out.println("** referenceNil");
        return classLoader().nilInstance();
    }

    public PrimObject referenceTrue() {
        //System.out.println("** referenceTrue");
        return classLoader().trueInstance();
    }

    public PrimObject referenceFalse() {
        //System.out.println("** referenceFalse");
        return classLoader().falseInstance();
    }

    public PrimObject reference(String name) {
//        System.out.println("***** reference " + name);
        return resolveObject(name);
    }

    public PrimObject resolveObject(String name) {
        //System.out.println("** resolveObject " + name + " " + importFor(name));
        return findObject(importFor(name));
    }

    public PrimObject smalltalkBlock(Object value, PrimContext homeContext) {
//        System.out.println("** smalltalkBlock " + value);
        return instanceOfWith("BlockClosure", new Object[] { value, homeContext });
    }

    public PrimObject smalltalkBlockAnswer(Object value, PrimContext homeContext, String answerClassName) {
//        System.out.println("** smalltalkBlockAnswer " + value + " from " + answerClassName);
        return instanceOfWith("BlockClosure", new Object[] { throwingAnswer(value, answerClassName), homeContext });
    }

    private LambdaBlock throwingAnswer(Object value, String answerClassName) {
        return (self, receiver, context) -> {
            PrimObject answer = ((LambdaBlock) value).apply(self, receiver, context);
            PrimBlockAnswer blockAnswer;
            try {
                blockAnswer = (PrimBlockAnswer)
                        classLoader()
                                .loadClass(answerClassName)
                                .getConstructor(PrimObject.class)
                                .newInstance(answer);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            throw blockAnswer;
        };
    }

    public PrimObject smalltalkCharacter(Object value) {
        return instanceOfWith("Character", value);
    }

    public PrimObject smalltalkArray(Object ignored) {
        return instanceOfWith("Array", new ArrayList<PrimObject>());
    }

    public PrimObject smalltalkArray(PrimObject items[]) {
        final ArrayList<PrimObject> value = new ArrayList<>();
        Collections.addAll(value, items);
        return instanceOfWith("Array", value);
    }

    public PrimObject smalltalkInteger(Object value) {
        return instanceOfWith("Integer", Integer.valueOf(String.valueOf(value)));
    }

    public PrimObject smalltalkMethod(Object value) {
        //System.out.println("** smalltalkMethod " + value);
        final PrimMethod method = new PrimMethod((LambdaBlock) value);
        method.selfClass(resolveObject("CompiledMethod"));
        return method;
    }

    public PrimObject smalltalkString(Object value) {
        return instanceOfWith("String", value);
    }

    public PrimObject smalltalkSymbol(Object value) {
        return instanceOfWith("Symbol", value);

//        String symbol = (String) javaValue;
//        SmalltalkClassLoader smalltalkClassLoader = classLoader();
//        if (smalltalkClassLoader.isInternedSymbol(symbol))
//            return smalltalkClassLoader.internedSymbolAt(symbol);
//        PrimObject symbolObject = instanceOfWith("Symbol", symbol);
//        smalltalkClassLoader.internSymbolAtPut(symbol, symbolObject);
//        return symbolObject;
    }

    protected PrimObject instanceOfWith(String type, Object value) {
        PrimObject instance = instanceOf(type);
        instance.javaValue(value);
        return instance;
    }

    protected PrimObject instanceOf(String type) {
        return isBootstrapping() ? new PrimObject() : resolveObject(type).perform("new");
    }

    protected boolean isBootstrapping() {
        return classLoader().isBootstrapping();
    }

    protected PrimObject findObject(String name) {
        return classLoader().findObject(name);
    }

    protected String importFor(String name) {
        // Subclass should override.
        return packageName() + "." + name;
    }

    protected void importAll(String packageName) {
        classLoader().importAll(packageName);
    }

    protected SmalltalkClassLoader classLoader() {
        return (SmalltalkClassLoader) Thread.currentThread().getContextClassLoader();
    }

    protected PrimObject sendMessages(PrimObject receiver, PrimContext context) {
        // Subclass should override.
        return receiver;
    }

    protected String packageName() {
        // Subclass should override.
        return DEFAULT_IMPORTED_PACKAGE;
    }

    public PrimObject perform(String selector) {
//        System.out.println("** perform(" + selector + ") " + this);
        return perform0(selector);
    }

    public PrimObject superPerform(String selector) {
//        System.out.println("** superPerform(" + selector + ") " + this);
        return perform0s(selector);
    }

    public PrimObject perform(PrimObject arg1, String selector) {
//        System.out.println("** perform(" + arg1 + "," + selector + ") " + this);
        return perform0(selector, arg1);
    }

    public PrimObject superPerform(PrimObject arg1, String selector) {
//        System.out.println("** superPerform(" + arg1 + "," + selector + ") " + this);
        return perform0s(selector, arg1);
    }

    public PrimObject perform(PrimObject arg1, PrimObject arg2, String selector) {
        //System.out.println("** perform(" + arg1 + "," + arg2 + "," + selector + ") " + this);
        return perform0(selector, arg1, arg2);
    }

    public PrimObject superPerform(PrimObject arg1, PrimObject arg2, String selector) {
        //System.out.println("** superPerform(" + arg1 + "," + arg2 + "," + selector + ") " + this);
        return perform0s(selector, arg1, arg2);
    }

    public PrimObject perform(PrimObject arg1, PrimObject arg2, PrimObject arg3, String selector) {
//        System.out.println("** perform(" + arg1 + "," + arg2 + "," + arg3 + "," + selector + ") " + this);
        return perform0(selector, arg1, arg2);
    }

    public PrimObject superPerform(PrimObject arg1, PrimObject arg2, PrimObject arg3, String selector) {
//        System.out.println("** superPerform(" + arg1 + "," + arg2 + "," + arg3 + "," + selector + ") " + this);
        return perform0s(selector, arg1, arg2, arg3);
    }

    public PrimObject perform(PrimObject arg1, PrimObject arg2, PrimObject arg3, PrimObject arg4, String selector) {
//        System.out.println("** perform(" + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + "," + selector + ") " + this);
        return perform0(selector, arg1, arg2, arg3, arg4);
    }

    public PrimObject superPerform(PrimObject arg1, PrimObject arg2, PrimObject arg3, PrimObject arg4, String selector) {
//        System.out.println("** superPerform(" + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + "," + selector + ") " + this);
        return perform0s(selector, arg1, arg2, arg3, arg4);
    }

    public PrimObject perform(PrimObject arg1, PrimObject arg2, PrimObject arg3, PrimObject arg4, PrimObject arg5, String selector) {
//        System.out.println("** perform(" + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + "," + arg5 + "," + selector + ") " + this);
        return perform0(selector, arg1, arg2, arg3, arg4, arg5);
    }

    public PrimObject superPerform(PrimObject arg1, PrimObject arg2, PrimObject arg3, PrimObject arg4, PrimObject arg5, String selector) {
//        System.out.println("** superPerform(" + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + "," + arg5 + "," + selector + ") " + this);
        return perform0s(selector, arg1, arg2, arg3, arg4, arg5);
    }

    public PrimObject perform(PrimObject arg1, PrimObject arg2, PrimObject arg3, PrimObject arg4, PrimObject arg5, PrimObject arg6, String selector) {
//        System.out.println("** perform(" + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + "," + arg5 + "," + arg6 + "," + selector + ") " + this);
        return perform0(selector, arg1, arg2, arg3, arg4, arg5, arg6);
    }

    public PrimObject superPerform(PrimObject arg1, PrimObject arg2, PrimObject arg3, PrimObject arg4, PrimObject arg5, PrimObject arg6, String selector) {
//        System.out.println("** superPerform(" + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + "," + arg5 + "," + arg6 + "," + selector + ") " + this);
        return perform0s(selector, arg1, arg2, arg3, arg4, arg5, arg6);
    }

    public PrimObject perform(PrimObject arg1, PrimObject arg2, PrimObject arg3, PrimObject arg4, PrimObject arg5, PrimObject arg6, PrimObject arg7, String selector) {
//        System.out.println("** perform(" + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + "," + arg5 + "," + arg6 + "," + arg7 + "," + selector + ") " + this);
        return perform0(selector, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
    }

    public PrimObject superPerform(PrimObject arg1, PrimObject arg2, PrimObject arg3, PrimObject arg4, PrimObject arg5, PrimObject arg6, PrimObject arg7, String selector) {
//        System.out.println("** superPerform(" + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + "," + arg5 + "," + arg6 + "," + arg7 + "," + selector + ") " + this);
        return perform0s(selector, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
    }

    public PrimObject perform(PrimObject arg1, PrimObject arg2, PrimObject arg3, PrimObject arg4, PrimObject arg5, PrimObject arg6, PrimObject arg7, PrimObject arg8, String selector) {
//        System.out.println("** perform(" + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + "," + arg5 + "," + arg6 + "," + arg7 + "," + arg8 + "," + selector + ") " + this);
        return perform0(selector, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);
    }

    public PrimObject superPerform(PrimObject arg1, PrimObject arg2, PrimObject arg3, PrimObject arg4, PrimObject arg5, PrimObject arg6, PrimObject arg7, PrimObject arg8, String selector) {
//        System.out.println("** superPerform(" + arg1 + "," + arg2 + "," + arg3 + "," + arg4 + "," + arg5 + "," + arg6 + "," + arg7 + "," + arg8 + "," + selector + ") " + this);
        return perform0s(selector, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);
    }

    public PrimObject perform(PrimObject[] args, String selector) {
        return perform0(selector, args);
    }

    public PrimObject superPerform(PrimObject[] args, String selector) {
        return perform0s(selector, args);
    }

    protected PrimObject perform0(String selector, PrimObject ... arguments) {
        return perform0(selfClass, selector, arguments);
    }

    protected PrimObject perform0s(String selector, PrimObject ... arguments) {
        return perform0(selfClass.superclass(), selector, arguments);
    }

    protected PrimObject perform0(PrimObject foundInClass, String selector, PrimObject ... arguments) {
        PrimObject cls = foundInClass;
        while (!cls.includesSelector(selector))
            cls = cls.superclass();
        return apply(cls.methodFor(selector), cls, selector, arguments);
    }

    protected PrimObject apply(PrimObject method, PrimObject foundInClass, String selector, PrimObject ... arguments) {
        log.info("** apply: #{} found in {} to {}", selector, foundInClass, this);
        PrimObject result = method.invoke(this, new PrimContext(this, foundInClass, selector, arguments));
        log.info("** result: {}", String.valueOf(result));
        return result;
    }

    protected PrimObject invoke(PrimObject receiver, PrimContext context) {
        return this;
    }

    protected PrimObject methodFor(String selector) {
        // This only happens for Bootstrapped instance after that PrimClass takes over.
        if (selector.startsWith("subclass:"))
            return PRIM_SUBCLASS;
        return PRIM_DOES_NOT_UNDERSTAND;
    }

    protected PrimObject superclass() {
        throw new IllegalStateException("This receiver should not have received this message.");
    }

    protected boolean includesSelector(String selector) {
        return true;
    }

    public boolean isMeta() {
        return false;
    }

    public PrimObject primitiveSubclass(PrimContext primContext) {
//        System.out.println("primitiveSubclass: " + primContext.argumentJavaValueAt(0));
        return PRIM_SUBCLASS.invoke(this, primContext);
    }

    public PrimObject primitiveNew() {
        PrimObject object = new PrimObject();
        object.selfClass(this);
        return object;
    }

    public PrimObject primitiveEval(PrimContext context) {
//        System.out.println("primitiveEval: " + context + " " + ((Object[]) javaValue())[1]);
        Object[] values = (Object[]) javaValue();
        // Context is invocation context while values[11 is context of the creator of the block.
        context.setupCallContext((PrimContext) values[1]);
        return ((LambdaBlock) values[0]).apply(this, this, context);
    }

    public PrimObject primitive110(PrimContext context) {
//        System.out.println("primitive110: " + context);
        if (this.equals(context.argumentAt(0)))
            return classLoader().trueInstance();
        else
            return classLoader().falseInstance();
    }

    public PrimObject primitive111(PrimContext context) {
//        System.out.println("primitive111: " + context);
        return this.selfClass();
    }

    public PrimObject primitive306(PrimContext context) {
        // ^ superclass
        return this.superclass();
    }

    public PrimObject primitive307(PrimContext context) {
//        (aClass == nil or: [aClass isKindOf: Behavior])
//        ifTrue: [ superclass := aClass]
//        ifFalse: [self error: 'superclass must be a class-describing object'].
        PrimObject aSuperclass = context.argumentAt(0);
        PrimClass theClass = (PrimClass) context.receiver();
        theClass.superclass(aSuperclass);
        return theClass;
    }
}
