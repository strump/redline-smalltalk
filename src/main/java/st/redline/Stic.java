/* Redline Smalltalk, Copyright (c) James C. Ladd. All rights reserved. See LICENSE in the root of this distribution. */
package st.redline;

import org.apache.commons.cli.*;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import st.redline.classloader.*;
import st.redline.core.PrimSubclassMethod;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Stic {

    private final String scriptFilename;
    private static final String[] levelNames;

    static {
        final List<String> names = Arrays.stream(Level.values()).map(Level::name).collect(Collectors.toList());
        levelNames = new String[Level.values().length];
        names.toArray(levelNames);
    }

    public static void main(String[] args) throws Exception {
        CommandLineParser parser = new DefaultParser();

        Options options = new Options();
        options.addOption(Option.builder("l" )
                .longOpt("loglevel")
                .hasArg()
                .desc("logging level. Possible values: "+String.join(", ", levelNames))
                .optionalArg(true)
                .argName("LOGLEVEL")
                .build());

        final CommandLine cli = parser.parse(options, args);

        if (cli.getArgs().length == 0) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "redline [arguments] file [file ...]",
                    "Run smalltalk compiler and execute FILE from arguments",
                    options, "\nSee License for details");
        }
        else {
            //Apply cli options
            if (cli.hasOption("loglevel")) {
                final String loglevelStr = cli.getOptionValue("loglevel");
                final Level loglevel = Level.getLevel(loglevelStr);
                if (loglevel == null) {
                    throw new IllegalArgumentException("Invalid loglevel value \""+loglevelStr+"\"");
                }
                System.out.println(">> loglevel="+loglevelStr);

                Configurator.setLevel("st.redline", loglevel);
            }

            //Run all files from arguments
            for (String filename : cli.getArgs()) {
                new Stic(filename).run();
            }
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
