package com.github.engatec.vdl.ui.component.subscriptions;

import com.github.engatec.vdl.ui.component.AppComponent;
import com.github.engatec.vdl.ui.controller.component.subscriptions.SubscriptionsComponentController;
import javafx.stage.Stage;

public class SubscriptionsComponent extends AppComponent<SubscriptionsComponentController> {

    public SubscriptionsComponent(Stage stage) {
        super(stage);
    }

    @Override
    protected String getFxmlPath() {
        return "/fxml/subscriptions/subscriptions.fxml";
    }

    @Override
    protected SubscriptionsComponentController getController() {
        return new SubscriptionsComponentController(stage);
    }
}
