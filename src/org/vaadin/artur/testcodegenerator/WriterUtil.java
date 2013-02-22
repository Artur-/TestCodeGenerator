package org.vaadin.artur.testcodegenerator;

public class WriterUtil {
    public static String lowerFirst(String str) {
        if (str == null || str.isEmpty())
            return str;

        return str.substring(0, 1).toLowerCase() + str.substring(1);
    }

    public static void writeModifiers(StringBuilder code, String[] modifiers) {
        if (modifiers != null) {
            for (int i = 0; i < modifiers.length; i++) {
                if (i != 0)
                    code.append(" ");

                code.append(modifiers[i]);
            }
            code.append(" ");
        }
        
    }

}
