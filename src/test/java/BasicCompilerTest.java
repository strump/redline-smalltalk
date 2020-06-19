import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import st.redline.classloader.*;
import st.redline.core.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
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
        assertEquals(WhileTrueTest.getSuperclass(), PrimModule.class);

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
    public void test_compiler_boolean_true() throws Exception {
        final PrimObject result = runString("^ true", "BooleanTrueTest");
        assertEquals(result, stClassLoader.trueInstance());
    }

    @Test
    public void test_compiler_boolean_false() throws Exception {
        final PrimObject result = runString("^ false", "BooleanFalseTest");
        assertEquals(result, stClassLoader.falseInstance());
    }

    @Test
    public void test_compiler_boolean_exp() throws Exception {
        final PrimObject result = runString("^ (false | true) & (false not)", "BooleanExpTest");
        assertEquals(result, stClassLoader.trueInstance());
    }

    @Test
    public void test_compiler_boolean_exp2() throws Exception {
        final PrimObject result = runString("^ (false | true) ifTrue: ['Success'] ifFalse: ['Fail'] ", "BooleanExp2Test");
        assertTrue(result.javaValue() instanceof String);
        assertEquals(result.javaValue(), "Success");
    }

    @Test
    public void test_compiler_class() throws Exception {
        final PrimObject result = runScript("smalltalk/compiler/Class_test.st", "ClassTest");
        assertTrue(result.selfClass().isMeta());
        assertTrue(result instanceof PrimClass);
        final PrimClass testClass = (PrimClass) result;
        assertEquals(testClass.name(), "ClassCompilerTest");

        final PrimClass superClass = testClass.superclass();
        assertEquals(superClass.name(), "Object");
    }

    @Test
    public void test_compiler_class_withMethods() throws Exception {
        final PrimObject result = runScript("smalltalk/compiler/ClassMethod_test.st", "ClassMethodTest");
        assertTrue(result.selfClass().isMeta());
        assertTrue(result instanceof PrimClass);
        final PrimClass testClass = (PrimClass) result;
        assertEquals(testClass.name(), "ClassMethodCompilerTest");

        final PrimClass superClass = testClass.superclass();
        assertEquals(superClass.name(), "Object");

        assertTrue(testClass.includesSelector("answerPlease"));
        assertTrue(testClass.selfClass().includesSelector("concat:and:"));

        final PrimObject val1 = testClass.smalltalkString("4");
        final PrimObject val2 = testClass.smalltalkString("2");
        final PrimObject answer = testClass.perform(val1, val2, "concat:and:");
        assertTrue(answer.javaValue() instanceof String);
        assertEquals(answer.javaValue(), "42");

        final PrimObject instance = testClass.primitiveNew();
        final PrimObject answer2 = instance.perform("answerPlease");
        assertEquals(answer2.javaValue(), "the answer");

        final PrimObject answer3 = instance.perform(instance, "singleArg:");
        assertEquals(answer3, instance);
    }

    @Test
    public void test_compiler_class_withFields() throws Exception {
        final PrimObject result = runScript("smalltalk/compiler/ClassFields_test.st", "ClassFields_test");
        assertTrue(result.selfClass().isMeta());
        assertTrue(result instanceof PrimClass);
        final PrimClass testClass = (PrimClass) result;
        assertEquals(testClass.name(), "ClassFieldsCompilerTest");

        final PrimObject instance = testClass.primitiveNew();

        PrimObject fieldA = instance.perform("fieldA");
        assertEquals(fieldA, stClassLoader.nilInstance());

        instance.perform(instance.smalltalkInteger(101), "fieldA:");
        fieldA = instance.perform("fieldA");
        assertEquals(fieldA.javaValue(), 101);

        instance.perform(instance.smalltalkString("field B value"), "fieldB:");
        final PrimObject fieldB = instance.perform("fieldB");
        assertEquals(fieldB.javaValue(), "field B value");

        try {
            //Call method to read non-existing field
            final PrimObject fieldC = instance.perform("fieldC");
            fail("Exception FieldNotFoundException should be thrown");
        }
        catch (FieldNotFoundException e) {
            //ok
        }
    }

    @Test
    public void test_compiler_classFields() throws Exception {
        final PrimObject result = runScript("smalltalk/compiler/ClassVariable_test.st", "ClassVariableTest");
        assertTrue(result.selfClass().isMeta());
        assertTrue(result instanceof PrimClass);
        final PrimClass testClass = (PrimClass) result;
        assertEquals(testClass.name(), "ClassVariableCompilerTest");

        testClass.perform(testClass.smalltalkInteger(101), "fieldA:");
        PrimObject fieldA = testClass.perform("fieldA");
        assertEquals(fieldA.javaValue(), 101);

        testClass.perform(testClass.smalltalkString("42"), "fieldB:");
        final PrimObject fieldB = testClass.perform("fieldB");
        assertEquals(fieldB.javaValue(), "42");

        try {
            //Call method to read non-existing field
            final PrimObject instance = testClass.primitiveNew();
            final PrimObject fieldC = instance.perform("tryReadClassField");
            fail("Exception FieldNotFoundException should be thrown");
        }
        catch (FieldNotFoundException e) {
            //ok
        }
    }

    /* Compile Smalltalk code and execute.
     * Returns result of execution as Smalltalk object. */
    private static PrimObject runString(String sourceCode, String className) throws Exception {
        final Source src = sourceFromString(sourceCode, className);
        return compileSource(src);
    }

    private static PrimObject compileSource(Source src) throws Exception {
        final Class<?> CompiledStClass = stClassLoader.compileToClass(src);
        assertTrue(PrimObject.class.isAssignableFrom(CompiledStClass.getSuperclass()));

        final Object testInstance = CompiledStClass.getDeclaredConstructor().newInstance();
        final PrimContext context = new PrimContext((PrimObject) testInstance);
        final Method moduleAnswerMethod = CompiledStClass.getMethod("moduleAnswer");

        final Object result = moduleAnswerMethod.invoke(testInstance);

        assertTrue(result instanceof PrimObject);
        return (PrimObject) result;
    }

    private PrimObject runScript(String filename, String className) throws Exception {
        final InputStream stream = getClass().getClassLoader().getResourceAsStream(filename);
        if (stream == null) {
            throw new FileNotFoundException("File not found: "+filename);
        }
        final String sourceCode = IOUtils.toString(stream);
        return runString(sourceCode, className);
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