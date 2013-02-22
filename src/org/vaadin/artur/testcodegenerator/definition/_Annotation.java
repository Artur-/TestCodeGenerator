package org.vaadin.artur.testcodegenerator.definition;

import java.lang.annotation.Annotation;

public class _Annotation {
    private Class<? extends Annotation> annotationClass;
    private String[] parameters;

    public _Annotation(Class<? extends Annotation> annotationClass,
            String... parameters) {
        super();
        this.annotationClass = annotationClass;
        this.parameters = parameters;
    }

    public void getSource(StringBuilder code) {
        code.append("@");
        new _Type(annotationClass).getSource(code);
        if (parameters.length > 0) {
            code.append("(");
            for (int i = 0; i < parameters.length; i++) {
                if (i != 0)
                    code.append(",");
                code.append("\"");
                code.append(parameters[i]);
                code.append("\"");
            }
            code.append(")");
        }

    }

    public Class<?> getAnnotationClass() {
        return annotationClass;
    }

}
