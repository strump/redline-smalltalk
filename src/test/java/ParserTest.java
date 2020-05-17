import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import st.redline.classloader.Source;
import st.redline.compiler.Compiler;
import st.redline.compiler.SmalltalkParserException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class ParserTest {
    public static final String SMALLTALK_FOLDER = "smalltalk";

    private List<String> parserTestFiles;

    @Before
    public void init() throws IOException {
        parserTestFiles = new LinkedList<>();
        final InputStream smalltalkStream = getClass().getClassLoader().getResourceAsStream(SMALLTALK_FOLDER);
        BufferedReader br = new BufferedReader(new InputStreamReader(smalltalkStream));

        String resource;
        while ((resource = br.readLine()) != null) {
            parserTestFiles.add(resource);
        }
    }

    @Test
    public void test_Add_method() throws IOException {
        final Compiler compiler = new Compiler(loadTestSource(SMALLTALK_FOLDER+"/Add_method.st"));
        final ParseTree parseTree = compiler.parsedSourceContents();
        assertNotNull(parseTree);
    }

    @Test
    public void testSmalltalkParser() throws IOException {
        for(String filename : parserTestFiles) {
            try {
                final Compiler compiler = new Compiler(loadTestSource(SMALLTALK_FOLDER + "/" + filename));
                final ParseTree parseTree = compiler.parsedSourceContents();
                assertNotNull(parseTree);
            }
            catch (SmalltalkParserException ex) {
                fail("Failed to parse file "+SMALLTALK_FOLDER + "/" + filename + "\n\t" + ex.getMessage());
            }
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
