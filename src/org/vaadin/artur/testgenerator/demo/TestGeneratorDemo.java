package org.vaadin.artur.testgenerator.demo;

import org.vaadin.artur.testgenerator.TestGeneratorExtension;

import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.NativeButton;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.ColumnGenerator;
import com.vaadin.ui.UI;

public class TestGeneratorDemo extends UI {
    public static class Person {
        private String first, last;
        private int age;
        private boolean alive;

        public Person(String first, String last, int age, boolean alive) {
            this.first = first;
            this.last = last;
            this.age = age;
            this.alive = alive;
        }

        public String getFirst() {
            return first;
        }

        public String getLast() {
            return last;
        }

        public String getFullName() {
            return getFirst() + " " + getLast();
        }

        public int getAge() {
            return age;
        }

        public boolean isAlive() {
            return alive;
        }
    }

    @Override
    public void init(VaadinRequest request) {
        VisualDemo vd = new VisualDemo();
        for (int i = 1; i <= 15; i++) {
            vd.getComboBox_1().addItem("Item " + i);
        }

        BeanItemContainer<Person> beanItemContainer = new BeanItemContainer<TestGeneratorDemo.Person>(
                Person.class);
        beanItemContainer.addBean(new Person("John", "Doe", 67, true));
        beanItemContainer.addBean(new Person("Homer", "Simpson", 44, true));
        beanItemContainer.addBean(new Person("Elvis", "Presley", 112, false));

        vd.getNativeSelect_1().setContainerDataSource(beanItemContainer);
        vd.getNativeSelect_1().setItemCaptionPropertyId("fullName");
        setContent(vd);

        vd.getListSelect_1().addItem("Project number 1");
        vd.getListSelect_1().addItem("Foo");
        vd.getListSelect_1().addItem("Bar");
        vd.getListSelect_1().setMultiSelect(true);

        vd.getTable().addContainerProperty("foo", String.class, "");
        vd.getTable().addContainerProperty(1, Integer.class, -1);
        vd.getTable().addGeneratedColumn("gen", new ColumnGenerator() {

            @Override
            public Object generateCell(Table source, Object itemId,
                    Object columnId) {
                return new NativeButton("Generated for " + itemId + "/"
                        + columnId);
            }
        });
        vd.getTable().addItem(new Object[] { "foo", 42 }, "aaa");
        vd.getTable().addItem(new Object[] { "bar", -3 }, "bbb");
        new TestGeneratorExtension().extend(this);
    }

}
