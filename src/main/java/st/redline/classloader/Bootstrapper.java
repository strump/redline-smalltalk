/* Redline Smalltalk, Copyright (c) James C. Ladd. All rights reserved. See LICENSE in the root of this distribution. */
package st.redline.classloader;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import st.redline.core.*;

import static st.redline.core.PrimSubclassMethod.PRIM_SUBCLASS_METHOD;

public class Bootstrapper {
    private static final Logger log = LogManager.getLogger(Bootstrapper.class);

    public void bootstrap(SmalltalkClassLoader classLoader) {
        setContextClassLoader(classLoader);

        classLoader.beginBootstrapping();
        createKernelObjectsHierarchy(classLoader);
        classLoader.importAll("st.redline.kernel");
        loadKernelObjects(classLoader);
        classLoader.endBootstrapping();
    }

    private void createKernelObjectsHierarchy(SmalltalkClassLoader classLoader) {
        // Create Kernel Objects and Classes we need to start Runtime.
        PrimClass metaclass = createKernelClass("Metaclass", true);
        PrimClass metaclassClass = createKernelClass("Metaclass class", true);
        metaclassClass.selfClass(metaclass);
        metaclass.selfClass(metaclassClass);

        PrimClass objectClass = createKernelClass("Object class", true);
        objectClass.selfClass(metaclass);
        PrimClass object = createKernelClass("Object", false);
        object.selfClass(objectClass);

        PrimClass behavior = createKernelClass("Behavior", object);
        PrimClass classDescription = createKernelClass("ClassDescription", behavior);
        PrimClass klass = createKernelClass("Class", classDescription);

        objectClass.superclass(klass);
        metaclass.superclass(classDescription);
        metaclassClass.superclass(classDescription.selfClass());

        //Create util classes
        PrimClass undefinedObject = createKernelClass("UndefinedObject", object);
        PrimClass blockClosure = createKernelClass("BlockClosure", object);
        PrimClass compiledMethod = createKernelClass("CompiledMethod", object);
        PrimClass booleanObject = createKernelClass("Boolean", object);
        PrimClass trueObject = createKernelClass("True", booleanObject);
        PrimClass falseObject = createKernelClass("False", booleanObject);
        PrimClass collection = createKernelClass("Collection", object);
        PrimClass sequenceableCollection = createKernelClass("SequenceableCollection", collection);
        PrimClass arrayedCollection = createKernelClass("ArrayedCollection", sequenceableCollection);
        PrimClass string = createKernelClass("String", arrayedCollection);
        PrimClass symbol = createKernelClass("Symbol", string);
        PrimClass transcript = createKernelClass("Transcript", object);
        PrimClass magnitude = createKernelClass("Magnitude", object);
        PrimClass number = createKernelClass("Number", magnitude);
        PrimClass random = createKernelClass("Random", number);
        PrimClass _integer = createKernelClass("Integer", number);
        PrimClass _float = createKernelClass("Float", number);

        // Fix up bootstrapped Kernel Objects Metaclass instance.
        klass.selfClass().selfClass(metaclass);

        // Initialise special Smalltalk circular hierarchy.
        object.addMethod(PrimDoesNotUnderstand.doesNotUnderstand_SELECTOR, PrimDoesNotUnderstand.PRIM_DOES_NOT_UNDERSTAND);

        // Let subclass primitive know the Metaclass instance - used when subclassing.
        PRIM_SUBCLASS_METHOD.metaclass(metaclass);

        // Add basicAddSelector:withMethod: to Behaviour
        behavior.addMethod("basicAddSelector:withMethod:", new PrimAddMethod());
        addSumblassMethods(klass);

        // Create special instances, referred to with pseudo variables.
        PrimObject nil = new PrimObject();
        nil.selfClass(undefinedObject);
        classLoader.nilInstance(nil);

        PrimObject trueInstance = new PrimObject();
        trueInstance.selfClass(trueObject);
        classLoader.trueInstance(trueInstance);

        PrimObject falseInstance = new PrimObject();
        falseInstance.selfClass(falseObject);
        classLoader.falseInstance(falseInstance);

        PrimObject smalltalkImage = new PrimObject();
        smalltalkImage.selfClass(object);

        // Load the hierarchy which will attached their methods.
        classLoader.cacheObject("st.redline.kernel.Object", object);
        classLoader.cacheObject("st.redline.kernel.Behavior", behavior);
        classLoader.cacheObject("st.redline.kernel.ClassDescription", classDescription);
        classLoader.cacheObject("st.redline.kernel.Class", klass);
        classLoader.cacheObject("st.redline.kernel.Metaclass", metaclass);
        classLoader.cacheObject("st.redline.kernel.UndefinedObject", undefinedObject);
        classLoader.cacheObject("st.redline.kernel.BlockClosure", blockClosure);
        classLoader.cacheObject("st.redline.kernel.CompiledMethod", compiledMethod);
        classLoader.cacheObject("st.redline.kernel.Boolean", booleanObject);
        classLoader.cacheObject("st.redline.kernel.True", trueObject);
        classLoader.cacheObject("st.redline.kernel.False", falseObject);
        classLoader.cacheObject("st.redline.kernel.Collection", collection);
        classLoader.cacheObject("st.redline.kernel.SequenceableCollection", sequenceableCollection);
        classLoader.cacheObject("st.redline.kernel.ArrayedCollection", arrayedCollection);
        classLoader.cacheObject("st.redline.kernel.String", string);
        classLoader.cacheObject("st.redline.kernel.Symbol", symbol);
        classLoader.cacheObject("st.redline.kernel.Transcript", transcript);
        classLoader.cacheObject("st.redline.kernel.Magnitude", magnitude);
        classLoader.cacheObject("st.redline.kernel.Number", number);
        classLoader.cacheObject("st.redline.kernel.Random", random);
        classLoader.cacheObject("st.redline.kernel.Integer", _integer);
        classLoader.cacheObject("st.redline.kernel.Float", _float);
        classLoader.cacheObject("st.redline.kernel.Smalltalk", smalltalkImage);
    }

    private void addSumblassMethods(PrimClass klass) {
        klass.addMethod("subclass:", PRIM_SUBCLASS_METHOD);
        klass.addMethod("subclass:instanceVariableNames:", PRIM_SUBCLASS_METHOD);
        klass.addMethod("subclass:instanceVariableNames:category:", PRIM_SUBCLASS_METHOD);
        klass.addMethod("subclass:instanceVariableNames:classVariableNames:", PRIM_SUBCLASS_METHOD);
        klass.addMethod("subclass:instanceVariableNames:classVariableNames:category:", PRIM_SUBCLASS_METHOD);
        klass.addMethod("subclass:instanceVariableNames:classVariableNames:poolDictionaries:category:", PRIM_SUBCLASS_METHOD);
    }

    private PrimClass createKernelClass(String className, boolean isMeta) {
        return new PrimClass(className,isMeta);
    }

    private PrimClass createKernelClass(String name, PrimClass superclass) {
        PrimClass primMeta = new PrimClass(name + " class",true);
        primMeta.superclass(superclass.selfClass());

        PrimClass primClass = new PrimClass(name);
        primClass.superclass(superclass);
        primClass.selfClass(primMeta);
        return primClass;
    }

    private PrimClass createKernelClass(String name, PrimClass superclass, PrimClass metaclass) {
        PrimClass primClass = createKernelClass(name, superclass);
        primClass.selfClass().selfClass(metaclass);
        return primClass;
    }

    private void setContextClassLoader(SmalltalkClassLoader classLoader) {
        Thread.currentThread().setContextClassLoader(classLoader);
    }

    private void loadKernelObjects(SmalltalkClassLoader classLoader) {
        loadObject(classLoader, "st.redline.kernel.Object");
        loadObject(classLoader, "st.redline.kernel.Behavior");
        loadObject(classLoader, "st.redline.kernel.ClassDescription");
        loadObject(classLoader, "st.redline.kernel.Class");
        loadObject(classLoader, "st.redline.kernel.Metaclass");
        loadObject(classLoader, "st.redline.kernel.UndefinedObject");
        loadObject(classLoader, "st.redline.kernel.BlockClosure");
        loadObject(classLoader, "st.redline.kernel.CompiledMethod");
        loadObject(classLoader, "st.redline.kernel.Boolean");
        loadObject(classLoader, "st.redline.kernel.True");
        loadObject(classLoader, "st.redline.kernel.False");
        loadObject(classLoader, "st.redline.kernel.Collection");
        loadObject(classLoader, "st.redline.kernel.SequenceableCollection");
        loadObject(classLoader, "st.redline.kernel.ArrayedCollection");
        loadObject(classLoader, "st.redline.kernel.String");
        loadObject(classLoader, "st.redline.kernel.Symbol");
        loadObject(classLoader, "st.redline.kernel.Transcript");
        loadObject(classLoader, "st.redline.kernel.Magnitude");
        loadObject(classLoader, "st.redline.kernel.Number");
        loadObject(classLoader, "st.redline.kernel.Random");
        loadObject(classLoader, "st.redline.kernel.Integer");
        loadObject(classLoader, "st.redline.kernel.Float");
    }

    private void loadObject(SmalltalkClassLoader classLoader, String name) {
        try {
            // Loading and instantiating the class causes the 'sendMessages' java method
            // to be called which executes all the message sends of the Smalltalk source.
            final Class<?> aClass = classLoader.loadClass(name);
            aClass.getDeclaredConstructor().newInstance();

            /*final PrimObject cachedObject = classLoader.cachedObject(name);
            if (cachedObject instanceof PrimClass) {
                PrimClass cachedClass = (PrimClass) cachedObject;
                //cachedClass.addMethod;
                System.out.println("Class: "+ aClass.getName());
            }*/
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
