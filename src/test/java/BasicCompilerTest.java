import org.junit.BeforeClass;
import org.junit.Test;
import st.redline.classloader.*;
import st.redline.core.PrimClass;
import st.redline.core.PrimContext;
import st.redline.core.PrimObject;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;

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
        final PrimObject result = runString("^ 'The test string'", "StringTest");
        final PrimObject classObject = result.selfClass();
        final PrimObject classObject2 = result.perform("class");
        assertEquals(classObject, classObject2);
        assertEquals(classObject, stClassLoader.findObject("st.redline.kernel.String"));

        //Check value inside result
        assertTrue(result.javaValue() instanceof String);
        assertEquals(result.javaValue(), "The test string");
    }

    @Test
    public void test_compiler_integer() throws Exception {
        final PrimObject result = runString("^ 1025", "IntegerTest");
        final PrimObject classObject = result.selfClass();
        final PrimObject classObject2 = result.perform("class");
        assertEquals(classObject, classObject2);
        assertEquals(classObject, stClassLoader.findObject("st.redline.kernel.Integer"));

        //Check value inside result
        assertTrue(result.javaValue() instanceof Integer);
        assertEquals(result.javaValue(), 1025);
    }

    @Test
    public void test_compiler_symbol() throws Exception {
        final PrimObject result = runString("^ #hello:world:", "SymbolTest");
        final PrimObject classObject = result.selfClass();
        final PrimObject classObject2 = result.perform("class");
        assertEquals(classObject, classObject2);
        assertEquals(classObject, stClassLoader.findObject("st.redline.kernel.Symbol"));

        //Check value in result
        assertTrue(result.javaValue() instanceof String);
        assertEquals(result.javaValue(), "hello:world:");
    }

    @Test
    public void test_compiler_literal_array() throws Exception {
        final PrimObject result = runString("^ #(first 1234 'hello' Symbol $; $4 $. 16r1F2A true nil)", "LiteralArrayTest");
        final PrimObject classObject = result.selfClass();
        assertEquals(classObject, stClassLoader.findObject("st.redline.kernel.Array"));

        //Check value in result
        assertTrue(result.javaValue() instanceof ArrayList);
        final ArrayList<PrimObject> arrayData = (ArrayList<PrimObject>) result.javaValue();
        assertEquals(arrayData.get(0).javaValue(), "first");
        assertEquals(arrayData.get(1).javaValue(), 1234);
        assertEquals(arrayData.get(2).javaValue(), "hello");
        assertEquals(arrayData.get(3).javaValue(), "Symbol");
        assertEquals(arrayData.get(4).javaValue(), (int)';');
        assertEquals(arrayData.get(5).javaValue(), (int)'4');
        assertEquals(arrayData.get(6).javaValue(), (int)'.');
        assertEquals(arrayData.get(7).javaValue(), 0x1F2A);
        assertEquals(arrayData.get(8), stClassLoader.trueInstance());
        assertEquals(arrayData.get(9), stClassLoader.nilInstance());
    }

    @Test
    public void test_compiler_boolean() throws Exception {
        final PrimObject result = runString("^ (false | true) ifTrue: ['Success'] ifFalse: ['Fail'] ", "BooleanTest");
        assertTrue(result.javaValue() instanceof String);
        assertEquals(result.javaValue(), "Success");
    }

    private static PrimObject runString(String sourceCode, String className) throws Exception {
        final Source src = sourceFromString(sourceCode, className);
        final Class<?> CompiledStClass = stClassLoader.compileToClass(src);
        assertEquals(CompiledStClass.getSuperclass(), PrimObject.class);

        final Object testInstance = CompiledStClass.getDeclaredConstructor().newInstance();
        final PrimContext context = new PrimContext((PrimObject) testInstance);
        final Method sendMessagesMethod = CompiledStClass.getDeclaredMethod("sendMessages", PrimObject.class, PrimContext.class);
        sendMessagesMethod.setAccessible(true);
        final Object result = sendMessagesMethod.invoke(testInstance, testInstance, context);
        assertEquals(result.getClass(), PrimObject.class);
        return (PrimObject) result;
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