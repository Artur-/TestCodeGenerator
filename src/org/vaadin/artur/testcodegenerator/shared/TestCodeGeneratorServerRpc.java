package org.vaadin.artur.testcodegenerator.shared;

import com.vaadin.shared.communication.ServerRpc;

public interface TestCodeGeneratorServerRpc extends ServerRpc {
    public void generateTest();
}
