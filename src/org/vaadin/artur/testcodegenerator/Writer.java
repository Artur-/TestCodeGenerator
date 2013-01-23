package org.vaadin.artur.testcodegenerator;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.vaadin.data.Container;
import com.vaadin.data.Container.Indexed;
import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.data.util.sqlcontainer.ReadOnlyRowId;
import com.vaadin.data.util.sqlcontainer.RowId;
import com.vaadin.server.Sizeable;
import com.vaadin.server.Sizeable.Unit;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.AbsoluteLayout;
import com.vaadin.ui.AbsoluteLayout.ComponentPosition;
import com.vaadin.ui.AbstractSelect.DefaultNewItemHandler;
import com.vaadin.ui.AbstractSelect.NewItemHandler;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HasComponents;
import com.vaadin.ui.SingleComponentContainer;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TabSheet.Tab;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.ColumnGenerator;
import com.vaadin.ui.UI;
import com.vaadin.util.ReflectTools;

public class Writer {
    private static final Method SET_HEIGHT_METHOD = ReflectTools.findMethod(
            Sizeable.class, "setHeight", float.class, Unit.class);
    private static final Method SET_WIDTH_METHOD = ReflectTools.findMethod(
            Sizeable.class, "setWidth", float.class, Unit.class);
    private static final Method SET_WIDTH_STRING_METHOD = ReflectTools
            .findMethod(Sizeable.class, "setWidth", String.class);
    private static final Method SET_HEIGHT_STRING_METHOD = ReflectTools
            .findMethod(Sizeable.class, "setHeight", String.class);

    int nextComponentId = 1;
    private StringBuilder mainBuilder1 = new StringBuilder();
    private boolean hasInlineSet = false;
    private boolean hasCustomComponentClass = false;
    private StringBuilder methodBuilder1 = new StringBuilder();
    private HashSet<Class<?>> imports = new HashSet<Class<?>>();
    private StringBuilder headerBuilder1 = new StringBuilder();
    private int dataSourceIndex = 1;
    private Map<Component, String> componentToIdentifier = new HashMap<Component, String>();

    public Writer() {
    }

    public String createUIClass(UI ui) {
        writeHeader(ui);
        requireImport(String.class);
        requireImport(Integer.class);
        requireImport(Object.class);
        String uiId = writeComponent(mainBuilder1, ui.getContent());
        writeAttachChild(mainBuilder1, ui, ui.getContent(), uiId);

        // End of init
        mainBuilder1.append("}");

        return headerBuilder1.toString() + mainBuilder1.toString()
                + methodBuilder1.toString() + "}";

    }

    public void writeHeader(UI ui) {
        headerBuilder1.append("package " + ui.getClass().getPackage().getName()
                + ";");
        // builder.append("\n");
        requireImport(Component.class);
        requireImport(HasComponents.class);

        mainBuilder1.append("public class Test" + ui.getClass().getSimpleName()
                + " extends " + UI.class.getName() + " {");
        mainBuilder1.append("public void init(" + VaadinRequest.class.getName()
                + " request) {");

    }

    private void requireImport(Class<?> class1) {
        if (imports.contains(class1)) {
            return;
        }

        imports.add(class1);
        headerBuilder1.append("import " + class1.getCanonicalName() + ";");

    }

    public Class<? extends Component> getVaadinComponentClass(
            Class<? extends Component> component) {
        if (component.getPackage().getName().startsWith("com.vaadin")) {
            return component;
        } else {
            return getVaadinComponentClass((Class<? extends Component>) component
                    .getSuperclass());
        }
    }

    public boolean writeAttachChild(StringBuilder builder,
            HasComponents parent, Component child, String childId) {
        if (!isSupportedParentType(parent)) {
            getLogger().warning(
                    "Don't know how to attach component " + childId
                            + " to parent of type "
                            + parent.getClass().getName()
                            + ". Leaving out from test.");
            return false;
        }
        String parentReference = getOrCreateComponentIdentifier(parent);

        if (parent instanceof AbsoluteLayout) {
            ComponentPosition pos = ((AbsoluteLayout) parent)
                    .getPosition(child);
            builder.append(parentReference + ".addComponent(" + childId
                    + ", \"" + pos.getCSSString() + "\");");
        } else if (parent instanceof SingleComponentContainer) {
            requireImport(SingleComponentContainer.class);
            builder.append(parentReference + ".setContent(" + childId + ");");
            return true;
        } else if (parent instanceof CustomComponent) {
            requireImport(CustomComponent.class);
            builder.append(parentReference + ".setCompositionRoot(" + childId
                    + ");");
            return true;
        } else if (parent instanceof ComponentContainer) {
            requireImport(ComponentContainer.class);
            builder.append(parentReference + ".addComponent(" + childId + ");");
            return true;
        } else if (parent instanceof Table) {
            // Attached through data source
            return true;
        }
        return false;

    }

    private boolean isSupportedParentType(HasComponents parent) {
        if (parent instanceof SingleComponentContainer) {
            return true;
        }
        if (parent instanceof ComponentContainer) {
            return true;
        }
        if (parent instanceof CustomComponent) {
            return true;
        }
        if (parent instanceof Table) {
            return true;
        }

        return false;
    }

    public String writeComponent(StringBuilder builder, Component c) {
        Class<? extends Component> type = getVaadinComponentClass(c.getClass());
        String typeName = type.getName();
        if (c instanceof CustomComponent) {
            typeName = writeCustomComponentClass();
        }
        String cid = getOrCreateComponentIdentifier(c);

        builder.append(";");
        builder.append(typeName + " " + cid + " = new " + typeName + "();");
        writeComponentProperties(builder, c);
        writeComponentChildren(builder, c);
        writeComponentChildProperties(builder, c);
        return cid;

    }

    private String createDataSource(Table table, Container.Indexed dataSource) {
        String dataSourceName = "DataSource" + dataSourceIndex++;
        int maxItems = 50;

        // Properties
        requireImport(IndexedContainer.class);
        methodBuilder1.append("public IndexedContainer get" + dataSourceName
                + "() {");
        methodBuilder1.append("IndexedContainer ic = new IndexedContainer();");
        List<Object> visibleIds = new ArrayList<Object>();
        Set<Object> generatedColumns = new HashSet<Object>();

        if (table != null) {
            for (Object id : table.getVisibleColumns()) {
                visibleIds.add(id);
            }
            generatedColumns.addAll(getGeneratedColumnIds(table));
        } else {
            visibleIds.addAll(dataSource.getContainerPropertyIds());
        }

        for (Object propertyId : visibleIds) {
            addContainerProperty(methodBuilder1, table, dataSource, propertyId);
        }

        methodBuilder1.append("Item item;");
        requireImport(Item.class);
        int itemIndex = 0;
        for (Object itemId : dataSource.getItemIds()) {
            methodBuilder1.append("item = ic.addItem("
                    + formatObjectForJava(itemId) + ");");
            for (Object columnId : visibleIds) {
                Object value;
                if (generatedColumns.contains(columnId)) {
                    value = table.getColumnGenerator(columnId).generateCell(
                            table, itemId, columnId);
                    if (value instanceof Component) {
                        writeComponent(methodBuilder1, (Component) value);
                    }
                } else {
                    value = dataSource.getContainerProperty(itemId, columnId)
                            .getValue();
                }
                methodBuilder1.append("item.getItemProperty("
                        + formatObjectForJava(columnId) + ").setValue("
                        + formatObjectForJava(value) + ");");

            }

            if (itemIndex++ > maxItems) {
                break;
            }
        }
        methodBuilder1.append("return ic;");
        methodBuilder1.append("}");

        return "get" + dataSourceName + "()";
    }

    private Collection<? extends Object> getGeneratedColumnIds(Table table) {
        ArrayList<Object> ids = new ArrayList<Object>();
        for (Object id : table.getVisibleColumns()) {
            ColumnGenerator generator = table.getColumnGenerator(id);
            if (generator != null) {
                ids.add(id);
            }
        }
        return ids;

    }

    private void addContainerProperty(StringBuilder builder, Table table,
            Container.Indexed dataSource, Object propertyId) {
        builder.append("ic.addContainerProperty(");
        builder.append(formatObjectForJava(propertyId));
        builder.append(", ");
        Class<?> type = dataSource.getType(propertyId);
        if (type == null) {
            if (table != null) {
                ColumnGenerator colGen = table.getColumnGenerator(propertyId);
                if (colGen != null && table.firstItemId() != null) {
                    Object gen = colGen.generateCell(table,
                            table.firstItemId(), propertyId);
                    if (gen instanceof Component) {
                        type = Component.class;
                    }
                }
            }
        }
        if (type == null) {
            type = Object.class;
        }
        builder.append(formatObjectForJava(getBoxedType(type)));
        builder.append(", null);");

    }

    private Map<Class<?>, Class<?>> primitiveMap = new HashMap<Class<?>, Class<?>>();
    {
        primitiveMap.put(int.class, Integer.class);
        primitiveMap.put(long.class, Long.class);
        primitiveMap.put(double.class, Double.class);
        primitiveMap.put(float.class, Float.class);
        primitiveMap.put(boolean.class, Boolean.class);
        primitiveMap.put(char.class, Character.class);
        primitiveMap.put(byte.class, Byte.class);
        primitiveMap.put(void.class, Void.class);
        primitiveMap.put(short.class, Short.class);
    }

    private Class<?> getBoxedType(Class<?> type) {
        if (primitiveMap.containsKey(type)) {
            return primitiveMap.get(type);
        } else {
            return type;
        }
    }

    private String writeCustomComponentClass() {
        if (!hasCustomComponentClass) {
            hasCustomComponentClass = true;
            methodBuilder1
                    .append("private static class MyCustomComponent extends CustomComponent {");
            methodBuilder1
                    .append("public void setCompositionRoot(Component c) {super.setCompositionRoot(c);}");
            methodBuilder1.append("}");
        }
        return "MyCustomComponent";
    }

    private String lowerFirst(String str) {
        return str.substring(0, 1).toLowerCase() + str.substring(1);
    }

    private void writeComponentChildren(StringBuilder builder, Component parent) {
        if (!(parent instanceof HasComponents)) {
            return;
        }
        HasComponents hcParent = (HasComponents) parent;
        if (!isSupportedParentType(hcParent)) {
            getLogger().warning(
                    "Don't know how to handle children for "
                            + parent.getClass().getName()
                            + ". Leaving out from test.");
            return;
        }
        if (parent instanceof Table) {
            // Added through data source
            return;
        }
        for (Component child : hcParent) {
            String childId = writeComponent(builder, child);
            writeAttachChild(builder, hcParent, child, childId);
        }

    }

    private void writeComponentProperties(StringBuilder builder, Component c) {
        Class<? extends Component> refType = getVaadinComponentClass(c
                .getClass());
        try {
            Component ref = refType.newInstance();
            BeanInfo info = Introspector.getBeanInfo(refType);
            for (PropertyDescriptor propertyDescriptor : sort(c,
                    info.getPropertyDescriptors())) {
                Method readMethod = propertyDescriptor.getReadMethod();
                Method writeMethod = propertyDescriptor.getWriteMethod();
                if (readMethod == null || writeMethod == null) {
                    continue;
                }
                if (skipMethod(writeMethod)) {
                    continue;
                }
                // if (c instanceof Table) {
                // getLogger().warning(
                // "Checking " + propertyDescriptor.getDisplayName());
                // }
                Object refValue = readMethod.invoke(ref);
                Object value = readMethod.invoke(c);
                if (!equals(value, refValue)) {
                    writePropertySetter(builder, c, writeMethod, value);
                }
            }
            writeSizeProperties(builder, ref, c);
        } catch (Exception e) {
            getLogger().log(
                    Level.SEVERE,
                    "failed to create reference object of type "
                            + refType.getName(), e);
        }

    }

    private PropertyDescriptor[] sort(Component c,
            PropertyDescriptor[] propertyDescriptors) {
        if (c instanceof Table) {
            List<PropertyDescriptor> first = new ArrayList<PropertyDescriptor>();
            List<PropertyDescriptor> last = new ArrayList<PropertyDescriptor>();

            List<PropertyDescriptor> descs = Arrays.asList(propertyDescriptors);
            for (PropertyDescriptor pd : descs) {
                // System.out.println(pd.getName());
                if ("containerDataSource".equals(pd.getName())) {
                    first.add(0, pd);
                } else if ("visibleColumns".equals(pd.getName())) {
                    first.add(pd);
                } else {
                    last.add(pd);
                }
            }
            first.addAll(last);
            return first
                    .toArray(new PropertyDescriptor[propertyDescriptors.length]);
        }
        return propertyDescriptors;
    }

    private void writeComponentChildProperties(StringBuilder builder,
            Component p) {
        if (!(p instanceof HasComponents)) {
            return;
        }
        HasComponents parent = (HasComponents) p;

        for (Method setter : parent.getClass().getMethods()) {
            try {
                if (!setter.getName().startsWith("set")) {
                    continue;
                }
                if (setter.getName().equals("setPosition")
                        && p instanceof AbsoluteLayout) {
                    // Already set when added
                    continue;

                }
                if (setter.getParameterTypes().length != 1
                        && setter.getParameterTypes().length != 2) {
                    continue;
                }

                if (setter.getParameterTypes()[0] != Component.class) {
                    continue;
                }

                boolean twoParamSetter = (setter.getParameterTypes().length == 2);
                String getterName = setter.getName().replace("set", "get");
                Method getter;
                try {
                    if (twoParamSetter) {
                        getter = p.getClass().getMethod(getterName,
                                (Class<?>) Component.class);
                    } else {
                        getter = p.getClass().getMethod(getterName,
                                (Class<?>) null);
                    }
                } catch (NoSuchMethodException e) {
                    continue;
                }
                if (getter == null) {
                    continue;
                }

                for (Component child : parent) {
                    Object value;
                    if (twoParamSetter) {
                        value = getter.invoke(p, child);
                        writePropertySetter(builder, p, setter, child, value);
                    } else {
                        value = getter.invoke(p);
                        writePropertySetter(builder, p, setter, value);

                    }
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        writeTabMethods(builder, p);
    }

    private void writeTabMethods(StringBuilder builder, Component p) {
        if (!(p instanceof TabSheet)) {
            return;
        }
        TabSheet ts = (TabSheet) p;
        for (Component child : ts) {
            Tab tab = ts.getTab(child);
            BeanInfo info;
            try {
                Tab refTab = ts.new TabSheetTabImpl(child.getCaption(),
                        child.getIcon());
                info = Introspector.getBeanInfo(tab.getClass());

                for (PropertyDescriptor propertyDescriptor : info
                        .getPropertyDescriptors()) {
                    Method readMethod = propertyDescriptor.getReadMethod();
                    Method writeMethod = propertyDescriptor.getWriteMethod();
                    if (readMethod == null || writeMethod == null) {
                        continue;
                    }

                    Object value = readMethod.invoke(tab);
                    Object ref = readMethod.invoke(refTab);
                    if (!equals(value, ref)) {
                        builder.append(getOrCreateComponentIdentifier(p)
                                + ".getTab("
                                + getOrCreateComponentIdentifier(child) + ")."
                                + writeMethod.getName() + "("
                                + formatObjectForJava(value) + ");");
                    }

                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }

    private void writeSizeProperties(StringBuilder builder, Sizeable ref,
            Component c) {

        if (ref.getHeight() != c.getHeight()
                || ref.getHeightUnits() != c.getHeightUnits()) {
            if (c.getHeight() < 0) {
                writePropertySetter(builder, c, SET_HEIGHT_STRING_METHOD,
                        new Object[] { null });
            } else {
                writePropertySetter(builder, c, SET_HEIGHT_METHOD,
                        c.getHeight(), c.getHeightUnits());
            }
        }
        if (ref.getWidth() != c.getWidth()
                || ref.getWidthUnits() != c.getWidthUnits()) {
            if (c.getWidth() < 0) {
                writePropertySetter(builder, c, SET_WIDTH_STRING_METHOD,
                        new Object[] { null });
            } else {
                writePropertySetter(builder, c, SET_WIDTH_METHOD, c.getWidth(),
                        c.getWidthUnits());
            }
        }
    }

    private boolean skipMethod(Method writeMethod) {
        Set<String> alwaysSkip = new HashSet<String>();
        alwaysSkip.add("setCellStyleGenerator");
        alwaysSkip.add("setLocale");
        alwaysSkip.add("setMoreMenuItem");
        alwaysSkip.add("setConvertedValue");
        alwaysSkip.add("setSource");
        alwaysSkip.add("setReceiver");

        if (alwaysSkip.contains(writeMethod.getName())) {
            return true;
        }
        if (Component.class
                .isAssignableFrom(writeMethod.getParameterTypes()[0])) {
            return true;
        }
        return false;
    }

    private boolean equals(Object value, Object refValue) {
        if (value == null) {
            return refValue == null;
        }
        if (value instanceof NewItemHandler) {
            if (value.getClass() == DefaultNewItemHandler.class
                    && refValue.getClass() == DefaultNewItemHandler.class) {
                return true;
            }

        }

        return value.equals(refValue);
    }

    private void writePropertySetter(StringBuilder builder, Component c,
            Method writeMethod, Object... values) {
        String cid = getOrCreateComponentIdentifier(c);

        builder.append(cid + "." + writeMethod.getName() + "(");
        if (values == null) {
            builder.append("null");
        } else if (c instanceof Table
                && "setContainerDataSource".equals(writeMethod.getName())
                && ((Table) c).getContainerDataSource() instanceof Indexed) {
            Table t = (Table) c;
            builder.append(createDataSource(t,
                    (Indexed) t.getContainerDataSource()));
        } else {
            for (int i = 0; i < values.length; i++) {
                if (i != 0) {
                    builder.append(", ");
                }
                builder.append(formatObjectForJava(values[i]));
            }
        }
        builder.append(");");

    }

    private String formatObjectForJava(Object value) {
        if (value == null) {
            return "null";
        }
        if (value.getClass().isArray()) {
            return formatArrayForJava(value);

        }
        if (value instanceof Boolean) {
            return ((Boolean) value).toString();
        }
        if (value instanceof Date) {
            requireImport(Date.class);
            return "new Date(" + formatObjectForJava(((Date) value).getTime())
                    + ")";
        }
        if (value instanceof Long) {
            return value.toString() + "L";
        }
        if (value instanceof MarginInfo) {
            MarginInfo info = (MarginInfo) value;
            if (info.hasBottom() && info.hasTop() && info.hasLeft()
                    && info.hasRight()) {
                return "true";
            }
            if (!info.hasBottom() && !info.hasTop() && !info.hasLeft()
                    && !info.hasRight()) {
                return "false";
            }

            return "new MarginInfo(" + info.hasTop() + "," + info.hasRight()
                    + "," + info.hasBottom() + "," + info.hasLeft() + ")";
        }
        if (value instanceof Unit) {
            requireImport(Unit.class);
            return "Unit." + ((Unit) value).name();
        }
        if (value instanceof Enum) {
            Enum e = (Enum) value;
            return formatClassName(e.getClass()) + "." + e.name();
        }

        if (value instanceof Float) {
            return value.toString() + "f";
        }
        if (value instanceof Number) {
            return value.toString();
        }
        if (value instanceof Container.Indexed) {
            return createDataSource(null, (Indexed) value);
        }
        if (value instanceof Class) {
            return formatClassName((Class) value) + ".class";
        }
        if (value instanceof Set) {
            writeInlineSet();
            String ret = "new InlineSet(";
            boolean comma = false;
            for (Object o : ((Set) value)) {
                if (comma) {
                    ret += ", ";
                }
                comma = true;
                ret += formatObjectForJava(o);
            }
            ret += ")";
            return ret;
        }
        if (value instanceof Alignment) {
            return formatAlignment((Alignment) value);
        }
        if (value instanceof Component) {
            Component c = (Component) value;
            return getOrCreateComponentIdentifier(c);
        }
        if (value instanceof ReadOnlyRowId) {
            requireImport(ReadOnlyRowId.class);
            return "new ReadOnlyRowId(" + ((ReadOnlyRowId) value).getRowNum()
                    + ")";
        }
        if (value instanceof RowId) {
            requireImport(RowId.class);
            return "new RowId(" + formatObjectForJava(((RowId) value).getId())
                    + ")";
        }
        return "\"" + value.toString() + "\"";
    }

    private String formatAlignment(Alignment value) {
        requireImport(Alignment.class);
        String cls = formatClassName(value.getClass());
        if (Alignment.TOP_RIGHT.equals(value)) {
            return cls + ".TOP_RIGHT";
        }
        if (Alignment.TOP_LEFT.equals(value)) {
            return cls + ".TOP_LEFT";
        }
        if (Alignment.TOP_CENTER.equals(value)) {
            return cls + ".TOP_CENTER";
        }
        if (Alignment.MIDDLE_RIGHT.equals(value)) {
            return cls + ".MIDDLE_RIGHT";
        }
        if (Alignment.MIDDLE_LEFT.equals(value)) {
            return cls + ".MIDDLE_LEFT";
        }
        if (Alignment.MIDDLE_CENTER.equals(value)) {
            return cls + ".MIDDLE_CENTER";
        }
        if (Alignment.BOTTOM_RIGHT.equals(value)) {
            return cls + ".BOTTOM_RIGHT";
        }
        if (Alignment.BOTTOM_LEFT.equals(value)) {
            return cls + ".BOTTOM_LEFT";
        }
        if (Alignment.BOTTOM_CENTER.equals(value)) {
            return cls + ".BOTTOM_CENTER";
        }

        return "new " + cls + "(" + value.getBitMask() + ")";
    }

    private String formatClassName(Class cls) {
        if (isImported(cls)) {
            return cls.getSimpleName();
        } else if (isImported(cls.getEnclosingClass())) {
            return cls.getCanonicalName().replace(
                    cls.getEnclosingClass().getName(),
                    cls.getEnclosingClass().getSimpleName());
        }
        return cls.getCanonicalName();
    }

    private boolean isImported(Class<? extends Object> cls) {
        return imports.contains(cls);
    }

    Map<String, String> arrayTypes = new HashMap<String, String>();
    {
        arrayTypes.put("Z", "boolean");
        arrayTypes.put("B", "byte");
        arrayTypes.put("C", "char");
        arrayTypes.put("D", "double");
        arrayTypes.put("F", "float");
        arrayTypes.put("I", "int");
        arrayTypes.put("J", "long");
        arrayTypes.put("S", "short");
    }

    private String formatArrayForJava(Object array) {
        Class<?> arrayType = array.getClass().getComponentType();
        String ret = "new " + formatClassName(arrayType) + "[] {";
        for (int i = 0; i < Array.getLength(array); i++) {
            if (i != 0) {
                ret += ", ";
            }
            ret += formatObjectForJava(Array.get(array, i));
        }
        ret += "}";
        return ret;
    }

    private String getOrCreateComponentIdentifier(Component c) {
        if (c instanceof UI) {
            return "this";
        }

        if (!componentToIdentifier.containsKey(c)) {
            String cid = lowerFirst(c.getClass().getSimpleName())
                    + nextComponentId++;
            componentToIdentifier.put(c, cid);
        }

        return componentToIdentifier.get(c);
    }

    private void writeInlineSet() {
        if (hasInlineSet) {
            return;
        }
        requireImport(HashSet.class);
        hasInlineSet = true;
        methodBuilder1
                .append("public static class InlineSet extends HashSet<Object> {");
        methodBuilder1.append("public InlineSet(Object... contents) {");
        methodBuilder1.append("for (Object o : contents) {");
        methodBuilder1.append("add(o);");
        methodBuilder1.append("}");
        methodBuilder1.append("}");
        methodBuilder1.append("}");
    }

    private static Logger getLogger() {
        return Logger.getLogger(Writer.class.getName());
    }

}
