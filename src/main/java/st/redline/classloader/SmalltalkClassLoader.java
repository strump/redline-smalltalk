/* Redline Smalltalk, Copyright (c) James C. Ladd. All rights reserved. See LICENSE in the root of this distribution. */
package st.redline.classloader;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import st.redline.compiler.Compiler;
import st.redline.core.PrimClass;
import st.redline.core.PrimObject;

import java.io.*;
import java.util.*;

import static st.redline.compiler.visitor.SmalltalkGeneratingVisitor.DEFAULT_IMPORTED_PACKAGE;

public class SmalltalkClassLoader extends ClassLoader {
    private static final Logger log = LogManager.getLogger(SmalltalkClassLoader.class);

    // Special Object instance values set during bootstrapping.
    private static PrimObject NIL;
    private static PrimObject TRUE;
    private static PrimObject FALSE;

    private final SourceFinder sourceFinder;
    private final Map<String, Class<?>> classCache;
    private final Map<String, PrimObject> objectCache;
    private final Map<String, Map<String, Source>> packageCache;
    private final Stack<String> executionPackageNames;
    private boolean bootstrapping;

    public SmalltalkClassLoader(ClassLoader classLoader, SourceFinder sourceFinder, Bootstrapper bootstrapper) {
        super(classLoader);
        this.sourceFinder = sourceFinder;
        this.classCache = new HashMap<>();
        this.objectCache = new HashMap<>();
        this.packageCache = new HashMap<>();
        this.executionPackageNames = new Stack<>();

        // initialize Object cache with bootstrapped objects.
        bootstrapper.bootstrap(this);
    }

    public PrimObject findObject(String name) {
        log.trace("** findObject {}", name);
        PrimObject cls = cachedObject(name);
        if (cls != null)
            return cls;
        try {
            boolean requiresInstantiation = !isCachedClass(name);
            Class<?> messageSendingClass = findClass(name);
            if (requiresInstantiation)
                messageSendingClass.newInstance();
            cls = cachedObject(name);
            if (cls != null)
                return cls;
        } catch (Exception e) {
            e.printStackTrace();
        }
        throw new ObjectNotFoundException("Object '" + name + "' was not found.");
    }

    public PrimClass findPrimClass(String name) {
        log.trace("** findPrimClass {}", name);
        PrimObject cls = findObject(name);
        if (cls instanceof PrimClass)
            return (PrimClass) cls;
        else
            throw new StClassNotFoundException("Object '" + name + "' was not found.");
    }

    protected PrimObject cachedObject(String name) {
        log.trace("** cachedObject {}", name);
        return objectCache.get(name);
    }

    public void cacheObject(String name, PrimObject object) {
        log.trace("** cacheObject {} as {}", object, name);
        objectCache.put(name, object);
    }

    public boolean isCachedObject(String name) {
        return objectCache.containsKey(name);
    }

    public Class<?> findClass(String name) throws ClassNotFoundException {
        log.trace("** findClass {}", name);
        Class<?> cls = cachedClass(name);
        if (cls != null)
            return cls;
        byte[] classData = loadClassData(name);
        if (classData == null)
            return super.findClass(name);
        cls = defineClass(null, classData, 0, classData.length);
        saveClass(classData, name);
        cacheClass(cls, name);
        return cls;
    }

    public Class<?> compileToClass(Source stSource) {
        final byte[] classData = compile(stSource);
        saveClass(classData, stSource.className());
        final Class<?> cls = defineClass(null, classData, 0, classData.length);
        return cls;
    }

    private void saveClass(byte[] classData, String name) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream("D:\\tmp\\redline\\" + name + ".class");
            fos.write(classData);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void cacheClass(Class cls, String name) {
        log.trace("** cacheClass {} as {}", cls, name);
        classCache.put(name, cls);
    }

    private boolean isCachedClass(String name) {
        return classCache.containsKey(name);
    }

    private Class cachedClass(String name) {
        return classCache.get(name);
    }

    private byte[] loadClassData(String name) {
        return compile(findSource(name));
    }

    public Class defineClass(byte[] bytes) {
        return defineClass(null, bytes, 0, bytes.length);
    }

    @SuppressWarnings("unchecked")
    private Source findSource(String name) {
        Source source = sourceFinder.find(name);
        if (source.exists())
            return source;
        Map<String, Source> imports = packageCache.getOrDefault(source.packageName(), Collections.EMPTY_MAP);
        return imports.getOrDefault(source.className(), source);
    }

    private byte[] compile(Source source) {
        return compiler(source).compile();
    }

    private Compiler compiler(Source source) {
        return new Compiler(source);
    }

    public void beginBootstrapping() {
        bootstrapping = true;
    }

    public void endBootstrapping() {
        bootstrapping = false;
    }

    public boolean isBootstrapping() {
        return bootstrapping;
    }

    public void nilInstance(PrimObject nil) {
        NIL = nil;
    }

    public PrimObject nilInstance() {
        return NIL;
    }

    public void falseInstance(PrimObject instance) {
        FALSE = instance;
    }

    public PrimObject falseInstance() {
        return FALSE;
    }

    public void trueInstance(PrimObject instance) {
        TRUE = instance;
    }

    public PrimObject trueInstance() {
        return TRUE;
    }

    public Class loadScript(String name) throws ClassNotFoundException {
        importAll(packageName(name));
        return loadClass(name);
    }

    private String packageName(String name) {
        int index = name.lastIndexOf(".");
        if (index == -1)
            return name;
        return name.substring(0, index);
    }

    @SuppressWarnings("unchecked")
    public String importForBy(String name, String packageName) {
        log.trace("** importFor: {} in {}", name, packageName);

        final String fullClassName = packageName + "." + name;
        if (isCachedObject(fullClassName)) {
            return fullClassName;
        }

        Map<String, Source> imports = packageCache.getOrDefault(packageName, Collections.EMPTY_MAP);
        Source source = imports.get(name);
        if (source != null)
            return dotted(source.fullClassName());
        if (!DEFAULT_IMPORTED_PACKAGE.equals(packageName))
            return importForBy(name, DEFAULT_IMPORTED_PACKAGE);
        return name;
    }

    private String dotted(String name) {
        return name.replace(SmalltalkSourceFile.CLASS_SEPARATOR, '.');
    }

    public void importAll(String packageName) {
        log.trace("** importAll: {} {}", packageName, packageCache.containsKey(packageName));
        if (!packageCache.containsKey(packageName))
            for (Source source : sourceFinder.findIn(packageName))
                addImport(packageName, source);
    }

    private void addImport(String packageName, Source source) {
        log.trace("** addImport: {} {}:{}", packageName, source.className(), source.fullClassName());
        Map<String, Source> objects = packageCache.getOrDefault(packageName, new HashMap<>());
        objects.put(source.className(), source);
        packageCache.put(packageName, objects);
    }

    public void pushExecutionPackage(String packageName) {
        executionPackageNames.push(packageName);
    }

    public void popExecutionPackage() {
        executionPackageNames.pop();
    }

    public String peekExecutionPackage() {
        return executionPackageNames.peek();
    }
}
