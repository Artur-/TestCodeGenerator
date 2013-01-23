package org.vaadin.artur.testcodegenerator;

import org.vaadin.artur.testcodegenerator.shared.TestCodeGeneratorClientRpc;
import org.vaadin.artur.testcodegenerator.shared.TestCodeGeneratorServerRpc;

import com.vaadin.ui.UI;

public class TestCodeGenerator extends AbstractDebugConsoleExtension {

    public TestCodeGenerator() {
        registerRpc(new TestCodeGeneratorServerRpc() {
            @Override
            public void generateTest() {
                Writer w = new Writer();
                UI ui = getUI();
                getRpcProxy(TestCodeGeneratorClientRpc.class).sendTest(
                        w.createUIClass(ui));
            }
        });
    }

}
