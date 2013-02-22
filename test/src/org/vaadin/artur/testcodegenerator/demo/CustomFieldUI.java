package org.vaadin.artur.testcodegenerator.demo;

import org.vaadin.artur.testcodegenerator.TestCodeGenerator;

import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

public class CustomFieldUI extends UI {

    @Override
    protected void init(VaadinRequest request) {
        MyCustomField cf = new MyCustomField();
        cf.setValue("Foo");
        setContent(new HorizontalLayout(cf));
        new TestCodeGenerator().extend(this);
    }

    class MyCustomField extends CustomField<String> {

        @Override
        protected Component initContent() {
            VerticalLayout layout = new VerticalLayout();
            Label l = new Label("This is the custom field");
            final TextField tf = new TextField();
            tf.setValue(getInternalValue());
            tf.addValueChangeListener(new ValueChangeListener() {

                @Override
                public void valueChange(
                        com.vaadin.data.Property.ValueChangeEvent event) {
                    setInternalValue(tf.getValue());
                }
            });
            layout.addComponents(l, tf);
            return layout;
        }

        @Override
        public Class<? extends String> getType() {
            return String.class;
        }

    }

}
