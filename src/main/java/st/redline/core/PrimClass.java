/* Redline Smalltalk, Copyright (c) James C. Ladd. All rights reserved. See LICENSE in the root of this distribution. */
package st.redline.core;

import java.util.*;

import static st.redline.core.PrimSubclassMethod.PRIM_SUBCLASS_METHOD;

public class PrimClass extends PrimObject {

    private final boolean meta;
    private final String name;
    private PrimClass superclass;
    private final Map<String, PrimMethod> methods = new HashMap<>();
    private Set<String> instanceVariableNames;
    private String category;

    public PrimClass() {
        this("", false);
    }

    public PrimClass(String name) {
        this(name, false);
    }

    public PrimClass(String name, boolean isMeta) {
        this(name, isMeta, null, null);
    }

    public PrimClass(String name, boolean isMeta, String[] instanceVariableNames) {
        this(name, isMeta, instanceVariableNames, null);
    }

    public PrimClass(String name, boolean isMeta, String[] instanceVariableNames, String category) {
        this.meta = isMeta;
        this.name = name;
        this.instanceVariableNames = instanceVariableNames!=null ? toSet(instanceVariableNames) : Collections.emptySet();
        this.category = category!=null ? category : "Unclassified";
    }

    private static Set<String> toSet(String[] strings) {
        final HashSet<String> result = new HashSet<>();
        Collections.addAll(result, strings);
        return result;
    }

    @Override
    public String toString() {
        if (isMeta())
            return name + " class";
        return name;
    }

    public boolean isMeta() {
        return meta;
    }

    public String name() {
        return name;
    }

    public boolean includesSelector(String selector) {
        return methods.containsKey(selector);
    }

    public void superclass(PrimClass superclass) {
        this.superclass = superclass;
    }

    public PrimMethod methodFor(String selector) {
        return methods.get(selector);
    }

    public PrimClass superclass() {
        return superclass;
    }

    public void addMethod(String selector, PrimMethod method) {
        methods.put(selector, method);
    }

    public PrimObject primitiveNew() {
        PrimObject object = new PrimObject();
        object.selfClass(this);
        return object;
    }

    public PrimObject primitiveNew(PrimObject indexableVariables) {
        //Answer an instance of the receiver (which is a class) with the number of indexable variables
        PrimObject object = new PrimObject();
        object.selfClass(this);
        return object;
    }
}
