package org.vaadin.artur.testcodegenerator;

public class WriterUtil {
    public static String lowerFirst(String str) {
        if (str == null || str.isEmpty())
            return str;

        return str.substring(0, 1).toLowerCase() + str.substring(1);
    }

}
