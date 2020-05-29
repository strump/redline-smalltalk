import org.junit.BeforeClass;
import org.junit.Test;
import st.redline.classloader.*;
import st.redline.core.PrimContext;
import st.redline.core.PrimObject;

import java.io.File;
import java.lang.reflect.Method;

import static org.junit.Assert.*;

public class PrimCompilerTest {
    public static final String SMALLTALK_FOLDER = "smalltalk";

    private static SmalltalkClassLoader stClassLoader = null;

    @BeforeClass
    public static void init() {
        final String[] classPaths = System.getProperty("java.class.path").split(File.pathSeparator);
        final SmalltalkSourceFinder sourceFinder = new SmalltalkSourceFinder(new SourceFactory(), classPaths);
        stClassLoader = new SmalltalkClassLoader(PrimCompilerTest.class.getClassLoader(), sourceFinder, new Bootstrapper());
    }

    @Test
    public void test_compiler() throws Exception {
        final Source src = sourceFromString("[1 < 0] whileTrue: [Transcript show: false]", "StringTest");
        final Class<?> StringReturn = stClassLoader.compileToClass(src);
        assertEquals(StringReturn.getSuperclass(), PrimObject.class);

        final Object testInstance = StringReturn.getDeclaredConstructor().newInstance();
        final Method packageNameMethod = StringReturn.getDeclaredMethod("packageName");
        final Object packageName = packageNameMethod.invoke(testInstance);
        assertEquals(packageName, "st.redline.test");
    }

    //@Test
    public void test_compiler_string() throws Exception {
        final Source src = sourceFromString("^ 'test'", "StringTest2");
        final Class<?> StringReturn = stClassLoader.compileToClass(src);
        assertEquals(StringReturn.getSuperclass(), PrimObject.class);

        final Object testInstance = StringReturn.getDeclaredConstructor().newInstance();
        final PrimContext context = new PrimContext((PrimObject) testInstance);
        final Method sendMessagesMethod = StringReturn.getDeclaredMethod("sendMessages", PrimObject.class, PrimContext.class);
        sendMessagesMethod.setAccessible(true);
        final Object result = sendMessagesMethod.invoke(testInstance, testInstance, context);
        assertEquals(result.getClass(), PrimObject.class);
        final PrimObject stResult = (PrimObject) result;
        final PrimObject classObject = stResult.selfClass();
        final PrimObject classObject2 = stResult.perform("class");
        assertEquals(classObject, classObject2);
        assertEquals(classObject, stClassLoader.findObject("String"));
    }

    private static Source sourceFromString(String smalltalkCode, String className) {
        return new StringSource(smalltalkCode, className, "st.redline.test");
    }

    private static class StringSource implements Source {
        private final String content;
        private final String className;
        private final String packageName;

        public StringSource(String content, String className, String packageName) {
            this.content = content;
            this.className = className;
            this.packageName = packageName;
        }

        @Override
        public boolean hasContent() {
            return true;
        }

        @Override
        public boolean exists() {
            return true;
        }

        @Override
        public String contents() {
            return content;
        }

        @Override
        public String className() {
            return className;
        }

        @Override
        public String fullClassName() {
            return packageName().replace('.', '/') + "/"+className;
        }

        @Override
        public String fileExtension() {
            return ".st";
        }

        @Override
        public String packageName() {
            return packageName;
        }

        @Override
        public String classpath() {
            return ".";
        }
    }
}