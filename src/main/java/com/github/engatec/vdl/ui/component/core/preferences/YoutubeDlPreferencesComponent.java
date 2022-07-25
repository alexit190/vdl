package com.github.engatec.vdl.ui.component.core.preferences;

import com.github.engatec.vdl.ui.component.core.AppComponent;
import com.github.engatec.vdl.ui.stage.controller.preferences.YoutubedlPreferencesController;
import javafx.stage.Stage;

public class YoutubeDlPreferencesComponent extends AppComponent<YoutubedlPreferencesController> {

    public YoutubeDlPreferencesComponent(Stage stage) {
        super(stage);
    }

    @Override
    protected String getFxmlPath() {
        return "/fxml/preferences/preferences-youtubedl.fxml";
    }

    @Override
    protected YoutubedlPreferencesController getController() {
        return new YoutubedlPreferencesController();
    }
}