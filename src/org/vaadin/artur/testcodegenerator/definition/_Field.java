package org.vaadin.artur.testcodegenerator.definition;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.vaadin.artur.testcodegenerator.WriterUtil;

import com.vaadin.ui.Component;

public class _Field {

    private String name;
    private String[] modifiers;
    private _Type type;
    private List<_Annotation> annotations = new ArrayList<_Annotation>();

    public _Field(Class<?> type, String name) {
        this.name = name;
        setType(type);
    }

    public void setModifiers(String... modifiers) {
        this.modifiers = modifiers;
    }

    public void getSource(StringBuilder code) {
        // public static void
        for (_Annotation annotation : annotations) {
            annotation.getSource(code);
        }
        WriterUtil.writeModifiers(code, modifiers);

        type.getSource(code);
        code.append(" ");

        code.append(name);
        code.append(";");
    }

    public void setType(_Type type) {
        this.type = type;
    }

    public void setType(Class<?> cls) {
        setType(new _Type(cls));
    }

    public String getName() {
        return name;
    }

    public void getReferencedClasses(Set<Class<?>> classes) {
        type.getReferencedClasses(classes);

        for (_Annotation a : annotations)
            classes.add(a.getAnnotationClass());
    }

    public void addAnnotation(_Annotation annotation) {
        this.annotations.add(annotation);

    }

}
