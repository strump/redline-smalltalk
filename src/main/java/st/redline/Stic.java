/* Redline Smalltalk, Copyright (c) James C. Ladd. All rights reserved. See LICENSE in the root of this distribution. */
package st.redline;

import st.redline.classloader.*;
import java.io.File;

public class Stic {

    private final String scriptFilename;
    public final static String USAGE = "Usage:\n" +
            " > redline FILE [FILE ...]\n" +
            "Run smalltalk compiler and execite FILE from arguments";

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println(USAGE);
            System.exit(0);
        }

        for(String filename: args) {
            new Stic(filename).run();
        }
    }

    public Stic(String scriptFilename) {
        this.scriptFilename = scriptFilename;
    }

    private void run() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        run(loadScript(scriptFilename));
    }

    private void run(Class<?> cls) throws IllegalAccessException, InstantiationException {
        cls.newInstance();
    }

    private Class<?> loadScript(String filename) throws ClassNotFoundException {
        final Source src = sourceFinder().sourceFile(filename);
        return classLoader().compileToClass(src);
    }

    private SmalltalkClassLoader classLoader() {
        return new SmalltalkClassLoader(currentClassLoader(), sourceFinder(), bootstrapper());
    }

    private Bootstrapper bootstrapper() {
        return new Bootstrapper();
    }

    private SourceFinder sourceFinder() {
        return new SmalltalkSourceFinder(sourceFactory(), classPaths());
    }

    private SourceFactory sourceFactory() {
        return new SourceFactory();
    }

    public String[] classPaths() {
        return classPath().split(File.pathSeparator);
    }

    private String classPath() {
        return System.getProperty("java.class.path");
    }

    private ClassLoader currentClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }
}
