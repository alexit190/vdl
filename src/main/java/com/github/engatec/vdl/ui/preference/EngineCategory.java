package com.github.engatec.vdl.ui.preference;

import com.github.engatec.vdl.ui.component.core.preferences.EnginePreferencesComponent;
import com.github.engatec.vdl.ui.stage.controller.preferences.EnginePreferencesController;
import javafx.scene.Node;
import javafx.stage.Stage;

public class EngineCategory extends Category {

    public EngineCategory(String title) {
        super(title);
    }

    @Override
    public Node buildCategoryUi(Stage stage) {
        if (super.node == null) {
            super.node = new EnginePreferencesComponent(stage).load();
        }
        return node;
    }

    @Override
    public boolean hasErrors() {
        if (super.node == null) {
            return false;
        }

        return ((EnginePreferencesController) node).hasErrors();
    }
}
