package org.vaadin.artur.testcodegenerator;

import com.vaadin.server.AbstractExtension;
import com.vaadin.ui.UI;

public class AbstractDebugConsoleExtension extends AbstractExtension {
    /**
     * Extend the given UI to add the debug console feature(s)
     * 
     * @param target
     */
    public void extend(UI target) {
        super.extend(target);
    }
}
