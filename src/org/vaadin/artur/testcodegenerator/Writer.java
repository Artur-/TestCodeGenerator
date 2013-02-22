package org.vaadin.artur.testcodegenerator;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
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

import org.vaadin.artur.testcodegenerator.definition._Annotation;
import org.vaadin.artur.testcodegenerator.definition._Class;
import org.vaadin.artur.testcodegenerator.definition._Field;
import org.vaadin.artur.testcodegenerator.definition._JavaClassFile;
import org.vaadin.artur.testcodegenerator.definition._Method;
import org.vaadin.artur.testcodegenerator.definition._Type;
import org.vaadin.artur.testcodegenerator.definition._VarargType;

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
import com.vaadin.ui.CustomField;
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

    private _JavaClassFile javaClassFile;

    public Writer() {
    }

    public String createUIClass(UI ui) {
        javaClassFile = new _JavaClassFile();
        javaClassFile.setPackageName(ui.getClass().getPackage().getName());

        _Class testClass = new _Class("Test" + ui.getClass().getSimpleName(),
                UI.class);
        testClass.setModifiers("public");
        _Method initMethod = new _Method("init");
        initMethod.setModifiers("protected");
        initMethod.setReturnType(Void.class);
        initMethod.addParameter(VaadinRequest.class, "request");
        testClass.addMethod(initMethod);

        javaClassFile.setMainClass(testClass);

        String uiId = writeComponent(initMethod, ui.getContent());
        writeAttachChild(initMethod, ui, ui.getContent(), uiId);

        return javaClassFile.getSource();
    }

    public static Class<? extends Component> getVaadinComponentClass(
            Class<? extends Component> component) {
        if (component.getPackage().getName().startsWith("com.vaadin")) {
            return component;
        } else {
            return getVaadinComponentClass((Class<? extends Component>) component
                    .getSuperclass());
        }
    }

    public boolean writeAttachChild(_Method m, HasComponents parent,
            Component child, String childId) {
        if (!isSupportedParentType(parent)) {
            getLogger().warning(
                    "Don't know how to attach component " + childId
                            + " to parent of type "
                            + parent.getClass().getName()
                            + ". Leaving out from test.");
            return false;
        }
        String parentReference = javaClassFile
                .getOrCreateComponentIdentifier(parent);

        if (parent instanceof AbsoluteLayout) {
            ComponentPosition pos = ((AbsoluteLayout) parent)
                    .getPosition(child);
            m.addCode(parentReference + ".addComponent(" + childId + ", \""
                    + pos.getCSSString() + "\");");
        } else if (parent instanceof SingleComponentContainer) {
            m.addCode(parentReference + ".setContent(" + childId + ");");
            return true;
        } else if (parent instanceof CustomComponent) {
            m.addCode(parentReference + ".setCompositionRoot(" + childId + ");");
            return true;
        } else if (parent instanceof CustomField) {
            m.addCode(parentReference + ".setContent(" + childId + ");");
            return true;
        } else if (parent instanceof ComponentContainer) {
            m.addCode(parentReference + ".addComponent(" + childId + ");");
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
        if (parent instanceof CustomField) {
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

    public String writeComponent(_Method m, Component c) {
        Class<? extends Component> type = getVaadinComponentClass(c.getClass());
        if (!canBeInstantiated(c)) {
            return null;
        }
        String typeName = formatClassName(type);
        if (c instanceof CustomComponent) {
            typeName = writeCustomComponentClass();
        } else if (c instanceof CustomField) {
            typeName = writeCustomFieldClass();
        }
        String cid = javaClassFile.getOrCreateComponentIdentifier(c);

        m.addCode(";");
        m.addCode(typeName + " " + cid + " = new " + typeName + "();");
        writeComponentProperties(m, c);
        writeComponentChildren(m, c);
        writeComponentChildProperties(m, c);
        return cid;

    }

    private boolean canBeInstantiated(Component c) {
        if (c instanceof CustomComponent || c instanceof CustomField)
            return true;

        Class<? extends Component> type = c.getClass();
        try {
            Constructor<? extends Component> constructor = type
                    .getConstructor(null);
            constructor.newInstance(null);
        } catch (Exception e) {
            getLogger()
                    .warning(
                            "Class "
                                    + type.getName()
                                    + " does not have a public no-arg constructor or cannot be instatiated using its constructor. This component will be excluded.");
            return false;
        }
        return true;

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
        if (!javaClassFile.getMainClass().hasClass(customComponentClassName)) {
            _Class c = new _Class(customComponentClassName,
                    CustomComponent.class);
            c.setModifiers("private", "static");
            _Method constructor = new _Method(customComponentClassName);
            constructor.setModifiers("public");
            _Method m = new _Method("setCompositionRoot");
            m.addParameter(Component.class, "c");
            m.addCode("super.setCompositionRoot(c);");
            m.setReturnType(Void.class);
            m.setModifiers("public");

            c.addMethod(constructor);
            c.addMethod(m);
            javaClassFile.getMainClass().addClass(c);
        }
        return customComponentClassName;
    }

    private static final String customComponentClassName = "MyCustomComponent";
    private static final String customFieldClassName = "MyCustomField";

    private String writeCustomFieldClass() {
        if (!javaClassFile.getMainClass().hasClass(customFieldClassName)) {
            _Class c = new _Class(customFieldClassName, CustomField.class);
            _Field contentField = new _Field(Component.class, "content");
            contentField.setModifiers("private");

            _Field typeField = new _Field(Class.class, "type");
            typeField.setModifiers("private");

            c.addField(contentField);
            c.addField(typeField);
            c.setModifiers("private", "static");
            _Method constructor = new _Method(customFieldClassName);
            constructor.setModifiers("public");

            _Method setContent = new _Method("setContent");
            setContent.setModifiers("public");
            setContent.addParameter(Component.class, "content");
            setContent.setReturnType(Void.class);
            setContent.addCode("this.content = content;");

            _Method initContent = new _Method("initContent");
            initContent.setModifiers("protected");
            initContent.setReturnType(Component.class);
            initContent.addCode("return this.content;");

            _Method setType = new _Method("setType");
            setType.setModifiers("public");
            setType.setReturnType(Void.class);
            setType.addParameter(Class.class, "type");
            setType.addCode("this.type = type;");

            _Method getType = new _Method("getType");
            getType.setModifiers("public");
            getType.setReturnType(Class.class);
            getType.addCode("return this.type;");

            c.addMethod(constructor);
            c.addMethod(setType);
            c.addMethod(getType);
            c.addMethod(setContent);
            c.addMethod(initContent);
            javaClassFile.getMainClass().addClass(c);
        }
        return customFieldClassName;
    }

    private void writeComponentChildren(_Method m, Component parent) {
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
            String childId = writeComponent(m, child);
            if (childId != null)
                writeAttachChild(m, hcParent, child, childId);
        }

    }

    private void writeComponentProperties(_Method m, Component c) {
        Class<? extends Component> refType = getRefType(c);
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
                    writePropertySetter(m, c, writeMethod, value);
                }
            }
            writeSizeProperties(m, ref, c);
        } catch (Exception e) {
            getLogger().log(
                    Level.SEVERE,
                    "failed to create reference object of type "
                            + refType.getName(), e);
        }

    }

    public static class ConcreteCustomField extends CustomField {

        @Override
        protected Component initContent() {
            return null;
        }

        @Override
        public Class getType() {
            return null;
        }

    }

    private static Class<? extends Component> getRefType(Component c) {
        if (c instanceof CustomField) {
            return ConcreteCustomField.class;
        }
        return getVaadinComponentClass(c.getClass());
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

    private void writeComponentChildProperties(_Method m, Component p) {
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
                        writePropertySetter(m, p, setter, child, value);
                    } else {
                        value = getter.invoke(p);
                        writePropertySetter(m, p, setter, value);

                    }
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        writeTabMethods(m, p);
    }

    private void writeTabMethods(_Method m, Component p) {
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
                        m.addCode(javaClassFile
                                .getOrCreateComponentIdentifier(p)
                                + ".getTab("
                                + javaClassFile
                                        .getOrCreateComponentIdentifier(child)
                                + ")."
                                + writeMethod.getName()
                                + "("
                                + formatObjectForJava(value) + ");");
                    }

                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }

    private void writeSizeProperties(_Method m, Sizeable ref, Component c) {

        if (ref.getHeight() != c.getHeight()
                || ref.getHeightUnits() != c.getHeightUnits()) {
            if (c.getHeight() < 0) {
                writePropertySetter(m, c, SET_HEIGHT_STRING_METHOD,
                        new Object[] { null });
            } else {
                writePropertySetter(m, c, SET_HEIGHT_METHOD, c.getHeight(),
                        c.getHeightUnits());
            }
        }
        if (ref.getWidth() != c.getWidth()
                || ref.getWidthUnits() != c.getWidthUnits()) {
            if (c.getWidth() < 0) {
                writePropertySetter(m, c, SET_WIDTH_STRING_METHOD,
                        new Object[] { null });
            } else {
                writePropertySetter(m, c, SET_WIDTH_METHOD, c.getWidth(),
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

    private void writePropertySetter(_Method m, Component c,
            Method writeMethod, Object... values) {
        String cid = javaClassFile.getOrCreateComponentIdentifier(c);

        m.addCode(cid + "." + writeMethod.getName() + "(");
        if (values == null) {
            m.addCode("null");
        } else if (c instanceof Table
                && "setContainerDataSource".equals(writeMethod.getName())
                && ((Table) c).getContainerDataSource() instanceof Indexed) {
            Table t = (Table) c;
            _Method dataSourceMethod = createDataSource(t,
                    (Indexed) t.getContainerDataSource());
            m.addCode(dataSourceMethod.getName() + "()");
        } else {
            for (int i = 0; i < values.length; i++) {
                if (i != 0) {
                    m.addCode(", ");
                }
                m.addCode(formatObjectForJava(values[i]));
            }
        }
        m.addCode(");");
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
            return createDataSource(null, (Indexed) value).getName() + "()";

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
            return javaClassFile.getOrCreateComponentIdentifier(c);
        }
        if (value instanceof ReadOnlyRowId) {
            return "new ReadOnlyRowId(" + ((ReadOnlyRowId) value).getRowNum()
                    + ")";
        }
        if (value instanceof RowId) {
            return "new RowId(" + formatObjectForJava(((RowId) value).getId())
                    + ")";
        }
        return "\"" + value.toString() + "\"";
    }

    private String formatAlignment(Alignment value) {
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
        javaClassFile.addImport(cls);
        // if (javaClassFile.isImported(cls)) {
        return cls.getSimpleName();
        // } else if (javaClassFile.isImported(cls.getEnclosingClass())) {
        // return cls.getCanonicalName().replace(
        // cls.getEnclosingClass().getName(),
        // cls.getEnclosingClass().getSimpleName());
        // }
        // return cls.getCanonicalName();
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

    private void writeInlineSet() {
        if (javaClassFile.getMainClass().hasClass("InlineSet")) {
            return;
        }
        // javaClassFile.addImport(HashSet.class);

        _Class inlineSet = new _Class("InlineSet", new _Type(HashSet.class,
                Object.class));
        inlineSet.setModifiers("public", "static");

        _Method m = new _Method("InlineSet");
        m.setModifiers("public");
        m.setReturnType((_Type) null);
        m.addParameter(new _VarargType(Object.class), "contents");
        m.addCode("for (Object o : contents) {");
        m.addCode("add(o);");
        m.addCode("}");

        inlineSet.addMethod(m);
        javaClassFile.getMainClass().addClass(inlineSet);
    }

    private static Logger getLogger() {
        return Logger.getLogger(Writer.class.getName());
    }

    private _Method createDataSource(Table table, Container.Indexed dataSource) {
        String dataSourceName = "DataSource"
                + javaClassFile.getNewDataSourceIndex();
        int maxItems = 50;

        // Properties

        // javaClassFile.addImport(IndexedContainer.class);

        _Method getDataSourceMethod = new _Method("get" + dataSourceName);
        _Annotation a = new _Annotation(SuppressWarnings.class, "unchecked");
        getDataSourceMethod.addAnnotation(a);
        getDataSourceMethod.setReturnType(IndexedContainer.class);
        getDataSourceMethod
                .addCode("IndexedContainer ic = new IndexedContainer();");

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
            addContainerProperty(getDataSourceMethod, table, dataSource,
                    propertyId);
        }

        getDataSourceMethod.addCode("Item item;");
        javaClassFile.addImport(Item.class);
        int itemIndex = 0;
        for (Object itemId : dataSource.getItemIds()) {
            getDataSourceMethod.addCode("item = ic.addItem("
                    + formatObjectForJava(itemId) + ");");
            for (Object columnId : visibleIds) {
                Object value;
                if (generatedColumns.contains(columnId)) {
                    value = table.getColumnGenerator(columnId).generateCell(
                            table, itemId, columnId);
                    if (value instanceof Component) {
                        writeComponent(getDataSourceMethod, (Component) value);
                    }
                } else {
                    value = dataSource.getContainerProperty(itemId, columnId)
                            .getValue();
                }
                getDataSourceMethod.addCode("item.getItemProperty("
                        + formatObjectForJava(columnId) + ").setValue("
                        + formatObjectForJava(value) + ");");

            }

            if (itemIndex++ > maxItems) {
                break;
            }
        }
        getDataSourceMethod.addCode("return ic;");
        javaClassFile.getMainClass().addMethod(getDataSourceMethod);
        return getDataSourceMethod;
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

    private void addContainerProperty(_Method m, Table table,
            Container.Indexed dataSource, Object propertyId) {
        m.addCode("ic.addContainerProperty(");
        m.addCode(formatObjectForJava(propertyId));
        m.addCode(", ");
        Class<?> type = null;
        try {
            type = dataSource.getType(propertyId);
        } catch (Exception e) {
            getLogger()
                    .warning(
                            "Broken container implementation threw exception for getType. Ignoring.");
        }
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
        m.addCode(formatObjectForJava(getBoxedType(type)));
        m.addCode(", null);");
    }

}
