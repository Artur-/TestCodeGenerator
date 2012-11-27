package org.vaadin.artur.testgenerator.client;

import org.vaadin.artur.testgenerator.TestGeneratorExtension;
import org.vaadin.artur.testgenerator.shared.TestGeneratorClientRpc;
import org.vaadin.artur.testgenerator.shared.TestGeneratorServerRpc;

import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.TextArea;
import com.vaadin.client.ApplicationConnection;
import com.vaadin.client.ServerConnector;
import com.vaadin.client.ui.VNotification;
import com.vaadin.client.ui.VWindow;
import com.vaadin.shared.Position;
import com.vaadin.shared.ui.Connect;

@Connect(TestGeneratorExtension.class)
public class TestGeneratorConnector extends
        AbstractDebugConsoleExtensionConnector {
    VNotification waitNotification = null;

    @Override
    protected void init() {
        super.init();
        registerRpc(TestGeneratorClientRpc.class, new TestGeneratorClientRpc() {

            @Override
            public void sendTest(String test) {
                if (waitNotification != null) {
                    waitNotification.hide();
                    waitNotification = null;
                }
                VWindow popup = new VWindow() {
                    // Workaround to avoid NPEs
                    @Override
                    public void onBlur(BlurEvent event) {
                    }

                    @Override
                    public void onFocus(FocusEvent event) {
                    }

                    @Override
                    public void onBrowserEvent(Event event) {
                        if (isClosable() && event.getTarget() == closeBox
                                && event.getTypeInt() == Event.ONCLICK) {
                            hide();
                            return;
                        }
                        super.onBrowserEvent(event);
                    }

                    @Override
                    protected ApplicationConnection getApplicationConnection() {
                        return getConnection();
                    }
                };
                popup.setCaption("Generated test");
                popup.setClosable(true);
                popup.setVaadinModality(true);
                TextArea textarea = new TextArea();
                textarea.setSize("1000px", "700px");
                textarea.setText(test);
                popup.contentPanel.setWidget(textarea);
                popup.show();
                popup.center();
            }
        });
    }

    @Override
    protected void extend(ServerConnector target) {
        addToDebugConsole(createTestGeneratorButton());
    }

    private Button createTestGeneratorButton() {
        Button debugConsoleButton = new Button("T");
        debugConsoleButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent arg0) {
                getRpcProxy(TestGeneratorServerRpc.class).generateTest();
                waitNotification = new VNotification() {
                    @Override
                    protected ApplicationConnection getApplicationConnection() {
                        return getConnection();
                    }
                };
                waitNotification.show("Generating test, please wait...",
                        Position.MIDDLE_CENTER, VNotification.STYLE_SYSTEM);
            }
        });
        return debugConsoleButton;
    }

}
