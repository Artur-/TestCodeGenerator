package org.vaadin.artur.testcodegenerator.definition;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.vaadin.artur.testcodegenerator.WriterUtil;

public class _Class {

    private String name;
    private _Type superType;
    private String[] modifiers;
    private List<_Field> fields = new ArrayList<_Field>();
    private List<_Method> methods = new ArrayList<_Method>();
    private List<_Class> classes = new ArrayList<_Class>();

    public _Class(String name, Class<?> superClass) {
        this.name = name;
        this.superType = new _Type(superClass);
    }

    public _Class(String name, _Type superType) {
        this.name = name;
        this.superType = superType;
    }

    public void setModifiers(String... modifiers) {
        this.modifiers = modifiers;
    }

    public void getSource(StringBuilder source) {
        WriterUtil.writeModifiers(source, modifiers);

        source.append("class ");
        source.append(name);
        source.append(" ");
        if (superType != null) {
            source.append("extends ");
            superType.getSource(source);
            source.append(" ");
        }
        source.append("{");

        for (_Field f : fields) {
            f.getSource(source);
        }

        for (_Method m : methods) {
            m.getSource(source);
        }

        for (_Class cls : classes) {
            cls.getSource(source);
        }
        source.append("}");

    }

    public void addMethod(_Method m) {
        methods.add(m);
        m.setContainingClass(this);
    }

    public void addClass(_Class cls) {
        classes.add(cls);
    }

    public String getName() {
        return name;
    }

    public boolean hasClass(String className) {
        for (_Class cls : classes) {
            if (cls.getName().equals(className))
                return true;
        }
        return false;
    }

    public void getReferencedClasses(Set<Class<?>> classes) {
        superType.getReferencedClasses(classes);
        for (_Class c : this.classes) {
            c.getReferencedClasses(classes);
        }

        for (_Method m : this.methods) {
            m.getReferencedClasses(classes);
        }
        for (_Field f : this.fields) {
            f.getReferencedClasses(classes);
        }

    }

    public void addField(_Field field) {
        fields.add(field);
    }

}
