/* Redline Smalltalk, Copyright (c) James C. Ladd. All rights reserved. See LICENSE in the root of this distribution. */
package st.redline.classloader;

import java.io.File;
import java.util.*;

public interface SourceFinder {

    Source find(String name);
    List<Source> findIn(String packageName);

    Source sourceFile(String filename, File file, String classpath);

    Source sourceFile(String filename);
}
