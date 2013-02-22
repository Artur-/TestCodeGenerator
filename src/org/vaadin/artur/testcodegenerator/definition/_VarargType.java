package org.vaadin.artur.testcodegenerator.definition;

public class _VarargType extends _Type {

    public _VarargType(Class<?> superClass) {
        super(superClass);
    }

    @Override
    public void getSource(StringBuilder source) {
        super.getSource(source);
        source.append("...");
    }

}
