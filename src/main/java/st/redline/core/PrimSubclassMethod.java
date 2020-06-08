/* Redline Smalltalk, Copyright (c) James C. Ladd. All rights reserved. See LICENSE in the root of this distribution. */
package st.redline.core;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import st.redline.classloader.SmalltalkClassLoader;

public class PrimSubclassMethod extends PrimMethod {
    private static final Logger log = LogManager.getLogger(PrimSubclassMethod.class);

    public static final PrimSubclassMethod PRIM_SUBCLASS_METHOD = new PrimSubclassMethod();

    private PrimClass theMetaclass;

    @Override
    protected PrimObject invoke(PrimObject receiver, PrimContext primContext) {
        log.info("PrimSubclassMethod invoke: {}", primContext.argumentJavaValueAt(0));
        assert receiver.equals(primContext.receiver());

        String subclassName = String.valueOf(primContext.argumentJavaValueAt(0));
        PrimClass superclass = (PrimClass) primContext.receiver();
        PrimClass newClass;
        PrimClass newMeta;
        boolean bootstrapping = isBootstrapping();

        if (bootstrapping) {
            newClass = resolveClass(subclassName);
            if (newClass == null)
                throw new RuntimeException("Subclass "+subclassName+" is unexpectedly null.");
        } else {
            newClass = new PrimClass(subclassName);
            newMeta = new PrimClass(subclassName, true);
            newClass.selfClass(newMeta);
            newClass.superclass(superclass);
            newMeta.superclass(superclass.selfClass());
            newMeta.selfClass(theMetaclass);
        }

        // TODO - Add other definitions to appropriate objects.
        //log.warn("TODO - Add other definitions to appropriate objects.");

        if (!bootstrapping) {
            SmalltalkClassLoader classLoader = classLoader();
            String fullQualifiedName = makeFullyQualifiedName(superclass, subclassName);
            classLoader.cacheObject(fullQualifiedName, newClass);
        }

        return newClass;
    }

    private String makeFullyQualifiedName(SmalltalkClassLoader classLoader, String name) {
        String instantiationName = classLoader.peekInstantiationName();
        if (instantiationName != null && instantiationName.endsWith(name))
            return instantiationName;
        throw new RuntimeException("Current instantiating class name not found.");
    }

    private String makeFullyQualifiedName(PrimClass superClass, String name) {
        return superClass.packageName() + "." + name;
    }

    public void metaclass(PrimClass metaclass) {
        theMetaclass = metaclass;
    }
}
