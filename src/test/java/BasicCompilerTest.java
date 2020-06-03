import org.junit.BeforeClass;
import org.junit.Test;
import st.redline.classloader.*;
import st.redline.core.PrimContext;
import st.redline.core.PrimObject;

import java.io.File;
import java.lang.reflect.Method;

import static org.junit.Assert.*;

public class BasicCompilerTest {
    public static final String SMALLTALK_FOLDER = "smalltalk";

    private static SmalltalkClassLoader stClassLoader = null;

    @BeforeClass
    public static void init() {
        final String[] classPaths = System.getProperty("java.class.path").split(File.pathSeparator);
        final SmalltalkSourceFinder sourceFinder = new SmalltalkSourceFinder(new SourceFactory(), classPaths);
        stClassLoader = new SmalltalkClassLoader(BasicCompilerTest.class.getClassLoader(), sourceFinder, new Bootstrapper());
    }

    @Test
    public void test_compiler() throws Exception {
        final Source src = sourceFromString("[1 < 0] whileTrue: [Transcript show: false]", "WhileTrueTest");
        final Class<?> WhileTrueTest = stClassLoader.compileToClass(src);
        assertEquals(WhileTrueTest.getSuperclass(), PrimObject.class);

        final Object testInstance = WhileTrueTest.getDeclaredConstructor().newInstance();
        final Method packageNameMethod = WhileTrueTest.getDeclaredMethod("packageName");
        final Object packageName = packageNameMethod.invoke(testInstance);
        assertEquals(packageName, "st.redline.test");
    }

    @Test
    public void test_compiler_string() throws Exception {
        final Source src = sourceFromString("^ 'The test string'", "StringTest");
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
        assertEquals(classObject, stClassLoader.findObject("st.redline.kernel.String"));

        //Check value inside stResult
        assertTrue(stResult.javaValue() instanceof String);
        assertEquals(stResult.javaValue(), "The test string");
    }

    @Test
    public void test_compiler_integer() throws Exception {
        final Source src = sourceFromString("^ 1025", "IntegerTest");
        final Class<?> IntegerTest = stClassLoader.compileToClass(src);
        assertEquals(IntegerTest.getSuperclass(), PrimObject.class);

        final Object testInstance = IntegerTest.getDeclaredConstructor().newInstance();
        final PrimContext context = new PrimContext((PrimObject) testInstance);
        final Method sendMessagesMethod = IntegerTest.getDeclaredMethod("sendMessages", PrimObject.class, PrimContext.class);
        sendMessagesMethod.setAccessible(true);
        final Object result = sendMessagesMethod.invoke(testInstance, testInstance, context);
        assertEquals(result.getClass(), PrimObject.class);
        //Check result is instance of Smalltalk class st.redline.kernel.Integer
        final PrimObject stResult = (PrimObject) result;
        final PrimObject classObject = stResult.selfClass();
        final PrimObject classObject2 = stResult.perform("class");
        assertEquals(classObject, classObject2);
        assertEquals(classObject, stClassLoader.findObject("st.redline.kernel.Integer"));

        //Check value inside stResult
        assertTrue(stResult.javaValue() instanceof Integer);
        assertEquals(stResult.javaValue(), 1025);
    }

    @Test
    public void test_compiler_symbol() throws Exception {
        final Source src = sourceFromString("^ #hello:world:", "SymbolTest");
        final Class<?> SymbolTest = stClassLoader.compileToClass(src);
        assertEquals(SymbolTest.getSuperclass(), PrimObject.class);

        final Object testInstance = SymbolTest.getDeclaredConstructor().newInstance();
        final PrimContext context = new PrimContext((PrimObject) testInstance);
        final Method sendMessagesMethod = SymbolTest.getDeclaredMethod("sendMessages", PrimObject.class, PrimContext.class);
        sendMessagesMethod.setAccessible(true);
        final Object result = sendMessagesMethod.invoke(testInstance, testInstance, context);
        assertEquals(result.getClass(), PrimObject.class);
        //Check result is instance of Smalltalk class st.redline.kernel.Integer
        final PrimObject stResult = (PrimObject) result;
        final PrimObject classObject = stResult.selfClass();
        final PrimObject classObject2 = stResult.perform("class");
        assertEquals(classObject, classObject2);
        assertEquals(classObject, stClassLoader.findObject("st.redline.kernel.Symbol"));

        //Check value in stResult
        assertTrue(stResult.javaValue() instanceof String);
        assertEquals(stResult.javaValue(), "hello:world:");
    }

    @Test
    public void test_compiler_literal_array() throws Exception {
        final Source src = sourceFromString("^ #(first 1234 $E 'hello' Symbol $$ $; $4 $. 16r1F2A)", "LiteralArrayTest");
        final Class<?> LiteralArrayTest = stClassLoader.compileToClass(src);
        assertEquals(LiteralArrayTest.getSuperclass(), PrimObject.class);

        final Object testInstance = LiteralArrayTest.getDeclaredConstructor().newInstance();
        final PrimContext context = new PrimContext((PrimObject) testInstance);
        final Method sendMessagesMethod = LiteralArrayTest.getDeclaredMethod("sendMessages", PrimObject.class, PrimContext.class);
        sendMessagesMethod.setAccessible(true);
        final Object result = sendMessagesMethod.invoke(testInstance, testInstance, context);
        assertEquals(result.getClass(), PrimObject.class);
        //Check result is instance of Smalltalk class st.redline.kernel.Integer
        final PrimObject stResult = (PrimObject) result;
        final PrimObject classObject = stResult.selfClass();
        assertEquals(classObject, stClassLoader.findObject("st.redline.kernel.Array"));

        //Check value in stResult
        assertTrue(stResult.javaValue() instanceof Object[]);
        //assertEquals(stResult.javaValue(), "hello:world:");
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