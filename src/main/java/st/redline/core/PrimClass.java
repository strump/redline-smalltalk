/* Redline Smalltalk, Copyright (c) James C. Ladd. All rights reserved. See LICENSE in the root of this distribution. */
package st.redline.core;

import java.util.*;

import static st.redline.core.PrimSubclass.PRIM_SUBCLASS;

public class PrimClass extends PrimObject {

    private boolean meta;
    private String name;
    private PrimClass superclass;
    private Map<String, PrimObject> methods = new HashMap<>();

    public PrimClass() {
        this("", false);
    }

    public PrimClass(boolean isMeta) {
        this("", isMeta);
    }

    public PrimClass(String name) {
        this(name, false);
    }

    public PrimClass(String name, boolean isMeta) {
        this.meta = isMeta;
        this.name = name;
    }

    public String toString() {
        if (isMeta())
            return name + " class";
        return name;
    }

    public boolean isMeta() {
        return meta;
    }

    public boolean includesSelector(String selector) {
        if (selector.startsWith("subclass:"))
            return true;
        return methods.containsKey(selector);
    }

    public PrimObject methodFor(String selector) {
        if (selector.startsWith("subclass:"))
            return PRIM_SUBCLASS;

        return methods.get(selector);
    }

    public void superclass(PrimClass superclass) {
        this.superclass = superclass;
    }

    protected PrimClass superclass() {
        return superclass;
    }

    public void addMethod(String selector, PrimObject method) {
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
