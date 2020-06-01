/* Redline Smalltalk, Copyright (c) James C. Ladd. All rights reserved. See LICENSE in the root of this distribution. */
package st.redline.classloader;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import st.redline.core.*;

import static st.redline.core.PrimSubclass.PRIM_SUBCLASS;

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
        PrimClass metaclass = createKernelObject("Metaclass", true);
        PrimClass metaclassClass = createKernelObject("Metaclass class", true);
        metaclassClass.selfClass(metaclass);
        metaclass.selfClass(metaclassClass);

        PrimClass objectClass = createKernelObject("Object class", true);
        objectClass.selfClass(metaclass);
        PrimClass object = createKernelObject("Object", false);
        object.selfClass(objectClass);

        PrimClass behavior = createKernelObject("Behavior", object);
        PrimClass classDescription = createKernelObject("ClassDescription", behavior);
        PrimClass klass = createKernelObject("Class", classDescription);

        objectClass.superclass(klass);
        metaclass.superclass(classDescription);
        metaclassClass.superclass(classDescription.selfClass());

        //Create util classes
        PrimClass undefinedObject = createKernelObject("UndefinedObject", object);
        PrimClass blockClosure = createKernelObject("BlockClosure", object);
        PrimClass compiledMethod = createKernelObject("CompiledMethod", object);
        PrimClass booleanObject = createKernelObject("Boolean", object);
        PrimClass trueObject = createKernelObject("True", booleanObject);
        PrimClass falseObject = createKernelObject("False", booleanObject);
        PrimClass collection = createKernelObject("Collection", object);
        PrimClass sequenceableCollection = createKernelObject("SequenceableCollection", collection);
        PrimClass arrayedCollection = createKernelObject("ArrayedCollection", sequenceableCollection);
        PrimClass string = createKernelObject("String", arrayedCollection);
        PrimClass symbol = createKernelObject("Symbol", string);
        PrimClass transcript = createKernelObject("Transcript", object);
        PrimClass magnitude = createKernelObject("Magnitude", object);
        PrimClass number = createKernelObject("Number", magnitude);
        PrimClass random = createKernelObject("Random", number);
        PrimClass _integer = createKernelObject("Integer", number);
        PrimClass _float = createKernelObject("Float", number);

        // Fix up bootstrapped Kernel Objects Metaclass instance.
        klass.selfClass().selfClass(metaclass);
        //classDescription.selfClass().selfClass(metaclass);
        //behavior.selfClass().selfClass(metaclass);
        //object.selfClass().selfClass(metaclass);

        // Initialise special Smalltalk circular hierarchy.
        //((PrimClass) object.selfClass()).superclass(klass);
        object.addMethod(PrimDoesNotUnderstand.doesNotUnderstand_SELECTOR, PrimDoesNotUnderstand.PRIM_DOES_NOT_UNDERSTAND);

        // Let subclass primitive know the Metaclass instance - used when subclassing.
        ((PrimSubclass) PRIM_SUBCLASS).metaclass(metaclass);

        // Add basicAddSelector:withMethod: to Behaviour
        behavior.addMethod("basicAddSelector:withMethod:", new PrimAddMethod());

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

    private PrimClass createKernelObject(String className, boolean isMeta) {
        return new PrimClass(className,isMeta);
    }

    private PrimClass createKernelObject(String name, PrimClass superclass) {
        PrimClass primMeta = new PrimClass(name + " class",true);
        primMeta.superclass(superclass.selfClass());

        PrimClass primClass = new PrimClass(name);
        primClass.superclass(superclass);
        primClass.selfClass(primMeta);
        return primClass;
    }

    private PrimClass createKernelObject(String name, PrimClass superclass, PrimClass metaclass) {
        PrimClass primClass = createKernelObject(name, superclass);
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
