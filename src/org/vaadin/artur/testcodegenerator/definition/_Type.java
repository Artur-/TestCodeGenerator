package org.vaadin.artur.testcodegenerator.definition;

import java.util.Set;

public class _Type {

    private Class<?> realClass;
    private _Type[] parameterTypes;

    public _Type(Class<?> realClass) {
        this.realClass = realClass;
    }

    public _Type(Class<?> realClass, _Type... parameterTypes) {
        this(realClass);
        this.parameterTypes = parameterTypes;
    }

    public _Type(Class<?> realClass, Class<?>... parameterTypes) {
        this(realClass);
        this.parameterTypes = new _Type[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            this.parameterTypes[i] = new _Type(parameterTypes[i]);
        }
    }

    public void getSource(StringBuilder source) {
        // Object
        // HashSet<Bar>
        // HashMap<Object,Foo>
        if (realClass == Void.class) {
            source.append("void");
            return;
        }
        source.append(realClass.getSimpleName());
        if (parameterTypes != null) {
            source.append("<");
            for (int i = 0; i < parameterTypes.length; i++) {
                if (i != 0)
                    source.append(",");
                parameterTypes[i].getSource(source);
            }
            source.append(">");
        }
    }

    public void getReferencedClasses(Set<Class<?>> classes) {
        classes.add(realClass);
        if (parameterTypes != null)
            for (_Type t : parameterTypes)
                t.getReferencedClasses(classes);
    }
}
