import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import st.redline.classloader.Source;
import st.redline.compiler.Compiler;
import st.redline.compiler.SmalltalkParserException;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class ParserTest {
    public static final String SMALLTALK_FOLDER = "smalltalk/parser";

    private static List<String> parserTestFiles;

    @BeforeClass
    public static void init() {
        parserTestFiles = new LinkedList<>();
        final URL smalltalkStream = ParserTest.class.getClassLoader().getResource(SMALLTALK_FOLDER);
        final File[] files = new File(smalltalkStream.getPath()).listFiles();

        for(File resourceFile : files) {
            parserTestFiles.add(resourceFile.getName());
        }
    }

    @Test
    public void test_Add_method() throws IOException {
        final Compiler compiler = new Compiler(loadTestSource(SMALLTALK_FOLDER+"/Number.st"));
        final ParseTree parseTree = compiler.parsedSourceContents();
        assertNotNull(parseTree);
    }

    @Test
    public void testSmalltalkParser() throws IOException {
        int totalTestFiles = parserTestFiles.size();
        int counter = 1;
        boolean hasFailures = false;

        System.out.println("Parsing " + totalTestFiles + " test files:");
        for(String filename : parserTestFiles) {
            try {
                final Compiler compiler = new Compiler(loadTestSource(SMALLTALK_FOLDER + "/" + filename));
                final ParseTree parseTree = compiler.parsedSourceContents();
                assertNotNull(parseTree);
                System.out.println(counter+"/"+totalTestFiles+". "+SMALLTALK_FOLDER + "/" + filename + " - OK");
            }
            catch (SmalltalkParserException ex) {
                System.err.println(counter+"/"+totalTestFiles+". "+SMALLTALK_FOLDER + "/" + filename + " - failed\n\t" + ex.getMessage());
                hasFailures = true;
            }
            counter ++;
        }

        if (hasFailures) {
            fail("Some test Smalltalk files are not parsed successfully");
        }
    }

    /* Load "*.st" file from str/test/resources and wrap it with Source class */
    private Source loadTestSource(String filename) throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        final InputStream sourceStream = classLoader.getResourceAsStream(filename);
        assertNotNull(sourceStream);

        String sourceCode = IOUtils.toString(sourceStream, StandardCharsets.UTF_8.name());

        String className = filename.replaceAll("/", ".");
        if (filename.endsWith(".st")) {
            className = filename.substring(0, filename.length()-3);
        }
        return new TestSource(sourceCode, className);
    }

    private static class TestSource implements Source {
        final private String contents;
        final private String className;
        final private String fullClassName;
        final private String packageName;

        public TestSource(String contents, String fullClassName) {
            final int lastDotPos = fullClassName.lastIndexOf('.');

            this.contents = contents;
            this.className = lastDotPos!=-1 ? fullClassName.substring(lastDotPos+1) : fullClassName;
            this.fullClassName = fullClassName;
            this.packageName = lastDotPos!=-1 ? fullClassName.substring(0, lastDotPos) : "root";
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
            return contents;
        }

        @Override
        public String className() {
            return className;
        }

        @Override
        public String fullClassName() {
            return fullClassName;
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
