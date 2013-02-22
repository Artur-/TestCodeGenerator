package org.vaadin.artur.testcodegenerator.definition;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.vaadin.artur.testcodegenerator.WriterUtil;

public class _Method {

    private String name;
    private String[] modifiers;
    private List<_Type> parameterTypes = new ArrayList<_Type>();
    private List<String> parameterNames = new ArrayList<String>();
    private StringBuilder codeBuilder = new StringBuilder();
    private _Type returnType;
    private _Class parentClass;
    private List<_Annotation> annotations = new ArrayList<_Annotation>();

    public _Method(String name) {
        this.name = name;
    }

    public void setModifiers(String... modifiers) {
        this.modifiers = modifiers;
    }

    public String getSource(StringBuilder code) {
        // public static void
        for (_Annotation annotation : annotations) {
            annotation.getSource(code);
        }
        WriterUtil.writeModifiers(code, modifiers);

        if (returnType != null) {
            returnType.getSource(code);
            code.append(" ");
        }
        code.append(name);
        code.append("(");
        for (int i = 0; i < parameterNames.size(); i++) {
            if (i != 0)
                code.append(",");

            parameterTypes.get(i).getSource(code);
            code.append(" ");
            code.append(parameterNames.get(i));
        }
        code.append(") {");
        code.append(codeBuilder);
        code.append("}");

        return code.toString();
    }

    public void addParameter(Class<?> cls, String name) {
        addParameter(new _Type(cls), name);
    }

    public void addParameter(_Type type, String name) {
        parameterTypes.add(type);
        parameterNames.add(name);
    }

    public void addCode(String code) {
        codeBuilder.append(code);
    }

    public void setReturnType(_Type type) {
        this.returnType = type;
    }

    public void setReturnType(Class<?> cls) {
        setReturnType(new _Type(cls));
    }

    public String getName() {
        return name;
    }

    public void getReferencedClasses(Set<Class<?>> classes) {
        if (returnType != null)
            returnType.getReferencedClasses(classes);

        for (_Type t : parameterTypes)
            t.getReferencedClasses(classes);
        for (_Annotation a : annotations)
            classes.add(a.getAnnotationClass());
    }

    public void setContainingClass(_Class parentClass) {
        this.parentClass = parentClass;
    }

    public _Class getContainingClass() {
        return parentClass;
    }

    public void addAnnotation(_Annotation annotation) {
        this.annotations.add(annotation);

    }

}
