package st.redline.compiler;

import st.redline.classloader.Source;

public class SmalltalkMethodGroupVisitor extends SmalltalkGeneratingVisitor {
    private String className;
    private String methodGroupName;
    private boolean isClassMethod = false;

    public SmalltalkMethodGroupVisitor(Source source) {
        super(source);
    }

    public SmalltalkMethodGroupVisitor(String className, String methodGroupName, boolean isClassMethod) {
        super(null);
        this.className = className;
        this.methodGroupName = methodGroupName;
        this.isClassMethod = isClassMethod;
    }
}
