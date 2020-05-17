import static org.junit.Assert.*;

import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import st.redline.classloader.Source;
import st.redline.compiler.Compiler;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class ParserTest {
    @Test
    public void test_Add_class_method() throws IOException {
        final Compiler compiler = new Compiler(loadTestSource("Add_method.st"));
        final ParseTree parseTree = compiler.parsedSourceContents();
        assertNotNull(parseTree);
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
