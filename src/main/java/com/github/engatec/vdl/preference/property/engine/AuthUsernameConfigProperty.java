package com.github.engatec.vdl.preference.property.engine;

import com.github.engatec.vdl.preference.configitem.ConfigItem;
import com.github.engatec.vdl.preference.configitem.youtubedl.AuthUsernameConfigItem;
import com.github.engatec.vdl.preference.property.ConfigProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class AuthUsernameConfigProperty extends ConfigProperty<StringProperty, String> {

    private static final ConfigItem<String> CONFIG_ITEM = new AuthUsernameConfigItem();

    private final StringProperty property = new SimpleStringProperty();

    public AuthUsernameConfigProperty() {
        restore();
    }

    @Override
    protected ConfigItem<String> getConfigItem() {
        return CONFIG_ITEM;
    }

    @Override
    public StringProperty getProperty() {
        return property;
    }

    @Override
    public String getValue() {
        return property.get();
    }

    @Override
    public void setValue(String value) {
        property.set(value);
    }
}
