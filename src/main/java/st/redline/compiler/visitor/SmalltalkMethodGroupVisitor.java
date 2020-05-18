package st.redline.compiler.visitor;

public class SmalltalkMethodGroupVisitor extends SmalltalkGeneratingVisitor {
    private String className;
    private String methodGroupName;
    private boolean isClassMethod = false;

    public SmalltalkMethodGroupVisitor(String className, String methodGroupName, boolean isClassMethod) {
        super(null);
        this.className = className;
        this.methodGroupName = methodGroupName;
        this.isClassMethod = isClassMethod;
    }
}
