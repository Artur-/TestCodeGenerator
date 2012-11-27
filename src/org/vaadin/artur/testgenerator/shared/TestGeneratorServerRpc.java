package org.vaadin.artur.testgenerator.shared;

import com.vaadin.shared.communication.ServerRpc;

public interface TestGeneratorServerRpc extends ServerRpc {
    public void generateTest();
}
