/* Redline Smalltalk, Copyright (c) James C. Ladd. All rights reserved. See LICENSE in the root of this distribution. */
package st.redline.core;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import st.redline.classloader.SmalltalkClassLoader;

public class PrimSubclassMethod extends PrimMethod {
    private static final Logger log = LogManager.getLogger(PrimSubclassMethod.class);

    public static final PrimSubclassMethod PRIM_SUBCLASS_METHOD = new PrimSubclassMethod();

    private PrimClass theMetaclass;

    protected PrimSubclassMethod() {
        super();
    }

    protected PrimSubclassMethod(LambdaBlock lambdaBlock) {
        super(lambdaBlock);
    }

    @Override
    protected PrimObject invoke(PrimObject receiver, PrimContext primContext) {
        log.info("PrimSubclassMethod invoke: {}", primContext.argumentJavaValueAt(0));
        assert receiver.equals(primContext.receiver());

        final ClassDefinition classDefinition = parseArguments(primContext.selector(), primContext.arguments());

        final String subclassName = classDefinition.subclassName;
        final PrimClass superclass = (PrimClass) primContext.receiver();
        PrimClass newClass;
        PrimClass newMeta;
        boolean bootstrapping = isBootstrapping();

        if (bootstrapping) {
            newClass = resolveClass(subclassName);
            if (newClass == null)
                throw new RuntimeException("Subclass "+subclassName+" is unexpectedly null.");
        } else {
            newClass = new PrimClass(subclassName, false, classDefinition.instanceVariableNames);
            newMeta = new PrimClass(subclassName, true, classDefinition.classVariableNames);
            newClass.selfClass(newMeta);
            newClass.superclass(superclass);
            newMeta.superclass(superclass.selfClass());
            newMeta.selfClass(theMetaclass);
        }

        // TODO - Add other definitions to appropriate objects.
        //log.warn("TODO - Add other definitions to appropriate objects.");

        if (!bootstrapping) {
            SmalltalkClassLoader classLoader = classLoader();
            String fullQualifiedName = makeFullyQualifiedName(classLoader, subclassName);
            classLoader.cacheObject(fullQualifiedName, newClass);
        }

        return newClass;
    }

    private ClassDefinition parseArguments(String selector, PrimObject[] arguments) {
        String[] keywords = selector.split(":");
        final int size = arguments.length;

        String subclassName = null;
        String[] instanceVariableNames = null;
        String[] classVariableNames = null;
        String poolDictionaries = null;
        String category = null;

        for (int i=0; i<size; i++) {
            final String keyword = keywords[i];
            if (keyword.length() == 0) continue;
            final PrimObject argument = arguments[i];

            switch (keyword) {
                case "subclass":
                    if (argument.javaValue() instanceof String) {
                        subclassName = (String) argument.javaValue();
                    } else {
                        String actualType = argument.javaValue().getClass().getCanonicalName();
                        throw new IllegalArgumentException("subclass: argument should have String type but " + actualType + " found");
                    }
                    break;
                case "instanceVariableNames":
                    if (argument.javaValue() instanceof String) {
                        final String strArgument = (String) argument.javaValue();
                        if (!strArgument.isEmpty()) {
                            instanceVariableNames = strArgument.split(" ");
                        }
                    } else {
                        String actualType = argument.javaValue().getClass().getCanonicalName();
                        throw new IllegalArgumentException("instanceVariableNames: argument should have String type but " + actualType + " found");
                    }
                    break;
                case "classVariableNames":
                    if (argument.javaValue() instanceof String) {
                        final String strArgument = (String) argument.javaValue();
                        if (!strArgument.isEmpty()) {
                            classVariableNames = strArgument.split(" ");
                        }
                    } else {
                        String actualType = argument.javaValue().getClass().getCanonicalName();
                        throw new IllegalArgumentException("classVariableNames: argument should have String type but " + actualType + " found");
                    }
                    break;
                case "poolDictionaries":
                    if (argument.javaValue() instanceof String) {
                        poolDictionaries = (String) argument.javaValue();
                        if (poolDictionaries.isEmpty()) poolDictionaries = null;
                    } else {
                        String actualType = argument.javaValue().getClass().getCanonicalName();
                        throw new IllegalArgumentException("poolDictionaries: argument should have String type but " + actualType + " found");
                    }
                    break;
                case "category":
                    if (argument.javaValue() instanceof String) {
                        category = (String) argument.javaValue();
                        if (category.isEmpty()) category = null;
                    } else {
                        String actualType = argument.javaValue().getClass().getCanonicalName();
                        throw new IllegalArgumentException("category: argument should have String type but " + actualType + " found");
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unknown subclass message keyword: '" + keyword + "'");
            }
        }

        return new ClassDefinition(subclassName, instanceVariableNames, classVariableNames, poolDictionaries, category);
    }

    private String makeFullyQualifiedName(SmalltalkClassLoader classLoader, String name) {
        return classLoader.peekExecutionPackage() + "." + name;
    }

    public void metaclass(PrimClass metaclass) {
        theMetaclass = metaclass;
    }

    private static class ClassDefinition {
        public final String subclassName;
        public final String[] instanceVariableNames;
        public final String[] classVariableNames;
        public final String poolDictionaries;
        public final String category;

        public ClassDefinition(String subclassName, String[] instanceVariableNames, String[] classVariableNames,
                               String poolDictionaries, String category) {
            this.subclassName = subclassName;
            this.instanceVariableNames = instanceVariableNames;
            this.classVariableNames = classVariableNames;
            this.poolDictionaries = poolDictionaries;
            this.category = category;
        }
    }
}
