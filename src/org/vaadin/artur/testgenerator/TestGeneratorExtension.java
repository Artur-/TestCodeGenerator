package org.vaadin.artur.testgenerator;

import org.vaadin.artur.testgenerator.shared.TestGeneratorClientRpc;
import org.vaadin.artur.testgenerator.shared.TestGeneratorServerRpc;

import com.vaadin.ui.UI;

public class TestGeneratorExtension extends AbstractDebugConsoleExtension {

    public TestGeneratorExtension() {
        registerRpc(new TestGeneratorServerRpc() {
            @Override
            public void generateTest() {
                Writer w = new Writer();
                UI ui = getUI();
                getRpcProxy(TestGeneratorClientRpc.class).sendTest(
                        w.createUIClass(ui));
            }
        });
    }

}
