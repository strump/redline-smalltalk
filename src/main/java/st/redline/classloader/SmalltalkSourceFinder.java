/* Redline Smalltalk, Copyright (c) James C. Ladd. All rights reserved. See LICENSE in the root of this distribution. */
package st.redline.classloader;

import java.io.*;
import java.util.*;
import java.util.jar.*;

import static st.redline.classloader.SmalltalkSourceFile.*;

public class SmalltalkSourceFinder implements SourceFinder {

    private final SourceFactory sourceFactory;
    private final String[] classPaths;

    public SmalltalkSourceFinder(SourceFactory sourceFactory, String[] classPaths) {
        this.sourceFactory = sourceFactory;
        this.classPaths = classPaths;
    }

    public Source find(String className) {
        //System.out.println(">>> find: " + className);
        String filename = toFilename(className);
        File file = new File(filename);
        if (file.exists())
            return sourceFile(filename, file, "");
        return new SourceNotFound(className);
    }

    public List<Source> findIn(String packageName) {
        //System.out.println("** findIn: " + packageName);
        return findInPath(packageName);
    }

    private List<Source> findInPath(String path) {
        String packagePath = path.replace('.', CLASS_SEPARATOR);
        List<Source> sources = new ArrayList<>();
        for (String classPath : classPaths)
            sources.addAll(findInPath(packagePath, classPath));
        return sources;
    }

    public List<Source> findInPath(String packagePath, String classPath) {
        if (isJar(classPath)) {
            return findSourceInInJar(packagePath, classPath);
        } else
            return findSourceInFile(packagePath, classPath);
    }

    @SuppressWarnings("unchecked")
    private List<Source> findSourceInFile(String packagePath, String classPath) {
        File folder = new File(classPath + CLASS_SEPARATOR + packagePath);
        if (!folder.isDirectory())
            return Collections.EMPTY_LIST;
        List<Source> sources = new ArrayList<>();
        File[] files = folder.listFiles();
        if (files != null)
            for (File file : files)
                if (file.isFile() && file.getName().endsWith(SOURCE_EXTENSION))
                    sources.add(sourceFile(packagePath + CLASS_SEPARATOR + file.getName(), file, classPath));
        return sources;
    }

    private List<Source> findSourceInInJar(String packagePath, String classPath) {
        List<Source> sources = new ArrayList<>();
        JarFile jarFile = tryCreateJarFile(classPath);
        for (Enumeration em1 = jarFile.entries(); em1.hasMoreElements();) {
            String entry = em1.nextElement().toString();
            int lastSlash = entry.lastIndexOf('/');
            int pathLength = packagePath.length();
            if (entry.startsWith(packagePath) && pathLength == lastSlash && entry.endsWith(".st"))
                sources.add(sourceFactory.createFromJar(entry, classPath));
        }
        return sources;
    }

    private JarFile tryCreateJarFile(String classPath) {
        try {
            return createJarFile(classPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public JarFile createJarFile(String classPath) throws IOException {
        return new JarFile(classPath);
    }

    private boolean isJar(String classPath) {
        return classPath.endsWith(".jar") || classPath.endsWith(".JAR");
    }

    @Override
    public Source sourceFile(String filename, File file, String classpath) {
        return sourceFactory.createFromFile(filename, file, classpath);
    }

    @Override
    public Source sourceFile(String filename) {
        final File file = new File(filename);
        return sourceFile(filename, file, "");
    }

    private String toFilename(String name) {
        return name.replace('.', File.separatorChar) + SOURCE_EXTENSION;
    }

    public class SourceNotFound implements Source {

        private final String name;

        public SourceNotFound(String name) {
            this.name = name;
        }

        public boolean exists() {
            return false;
        }

        public boolean hasContent() {
            return false;
        }

        public String contents() {
            return "";
        }

        public String className() {
            int index = name.lastIndexOf(".");
            if (index == -1)
                return name;
            return name.substring(index + 1);
        }

        public String fullClassName() {
            return name;
        }

        public String fileExtension() {
            return "";
        }

        public String packageName() {
            int index = name.lastIndexOf(".");
            if (index == -1)
                return "";
            return name.substring(0, index);
        }

        public String classpath() {
            return "";
        }
    }
}
