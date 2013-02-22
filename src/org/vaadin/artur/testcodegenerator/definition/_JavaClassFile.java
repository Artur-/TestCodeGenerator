package org.vaadin.artur.testcodegenerator.definition;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.vaadin.artur.testcodegenerator.WriterUtil;

import com.vaadin.ui.Component;
import com.vaadin.ui.UI;

public class _JavaClassFile {
    int nextComponentId = 1;

    private HashSet<Class<?>> imports = new HashSet<Class<?>>();
    private int dataSourceIndex = 1;
    private Map<Component, String> componentToIdentifier = new HashMap<Component, String>();

    private String packageName;

    private _Class mainClass;

    public void addImport(Class<?> class1) {
        if (imports.contains(class1)) {
            return;
        }

        imports.add(class1);
    }

    public Object getNewDataSourceIndex() {
        return dataSourceIndex++;
    }

    public String getOrCreateComponentIdentifier(Component c) {
        if (c instanceof UI) {
            return "this";
        }

        if (!componentToIdentifier.containsKey(c)) {
            String cid = WriterUtil.lowerFirst(c.getClass().getSimpleName())
                    + nextComponentId++;
            componentToIdentifier.put(c, cid);
        }

        return componentToIdentifier.get(c);
    }

    // public boolean isImported(Class<? extends Object> cls) {
    // return imports.contains(cls);
    // }

    public String getSource() {
        StringBuilder source = new StringBuilder();

        source.append("package ");
        source.append(packageName);
        source.append(";");

        Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.addAll(imports);
        getReferencedClasses(classes);
        for (Class<?> cls : classes) {
            source.append("import " + cls.getCanonicalName() + ";");
        }
        mainClass.getSource(source);

        return source.toString();
    }

    private void getReferencedClasses(Set<Class<?>> classes) {
        mainClass.getReferencedClasses(classes);
        for (Component c : componentToIdentifier.keySet())
            classes.add(c.getClass());
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public _Class getMainClass() {
        return mainClass;
    }

    public void setMainClass(_Class mainClass) {
        this.mainClass = mainClass;

    }
}
