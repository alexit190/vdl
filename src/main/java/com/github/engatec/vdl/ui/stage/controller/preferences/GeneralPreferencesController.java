package com.github.engatec.vdl.ui.stage.controller.preferences;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.github.engatec.fxcontrols.FxDirectoryChooser;
import com.github.engatec.vdl.core.ApplicationContext;
import com.github.engatec.vdl.handler.ComboBoxMouseScrollHandler;
import com.github.engatec.vdl.handler.SliderMouseScrollHandler;
import com.github.engatec.vdl.model.AudioFormat;
import com.github.engatec.vdl.model.BitrateType;
import com.github.engatec.vdl.model.Language;
import com.github.engatec.vdl.model.Resolution;
import com.github.engatec.vdl.preference.ConfigRegistry;
import com.github.engatec.vdl.preference.configitem.general.AutoSelectFormatConfigItem;
import com.github.engatec.vdl.preference.property.general.AlwaysAskDownloadPathConfigProperty;
import com.github.engatec.vdl.preference.property.general.AudioExtractionAddMetadataConfigProperty;
import com.github.engatec.vdl.preference.property.general.AudioExtractionBitrateConfigProperty;
import com.github.engatec.vdl.preference.property.general.AudioExtractionBitrateTypeConfigProperty;
import com.github.engatec.vdl.preference.property.general.AudioExtractionEmbedThumbnailConfigProperty;
import com.github.engatec.vdl.preference.property.general.AudioExtractionFormatConfigProperty;
import com.github.engatec.vdl.preference.property.general.AudioExtractionQualityConfigProperty;
import com.github.engatec.vdl.preference.property.general.AutoSearchFromClipboardConfigProperty;
import com.github.engatec.vdl.preference.property.general.AutoSelectFormatConfigProperty;
import com.github.engatec.vdl.preference.property.general.DownloadPathConfigProperty;
import com.github.engatec.vdl.preference.property.general.DownloadThreadsConfigProperty;
import com.github.engatec.vdl.preference.property.general.LanguageConfigProperty;
import com.github.engatec.vdl.preference.property.general.LoadThumbnailsConfigProperty;
import com.github.engatec.vdl.ui.data.ComboBoxValueHolder;
import com.github.engatec.vdl.ui.helper.Dialogs;
import com.github.engatec.vdl.ui.validation.InputForm;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.lang3.StringUtils;

public class GeneralPreferencesController extends ScrollPane implements InputForm {

    private final ApplicationContext ctx = ApplicationContext.getInstance();
    private final ConfigRegistry configRegistry = ctx.getConfigRegistry();

    private static final BidiMap<Integer, Integer> MP3_BITRATE_SLIDER_BIDI_MAP = new DualHashBidiMap<>(Map.of(
            0, 64,
            1, 96,
            2, 128,
            3, 160,
            4, 192,
            5, 256,
            6, 320
    ));

    @FXML private ComboBox<ComboBoxValueHolder<Language>> languageComboBox;

    private final ToggleGroup downloadPathRadioGroup = new ToggleGroup();
    @FXML private RadioButton downloadPathRadioBtn;
    @FXML private RadioButton askPathRadioBtn;
    @FXML private FxDirectoryChooser downloadPathDirectoryChooser;

    @FXML private ComboBox<Integer> downloadThreadsComboBox;
    @FXML private CheckBox autoSearchFromClipboardCheckBox;
    @FXML private CheckBox loadThumbnailsCheckbox;
    @FXML private ComboBox<Integer> autoSelectFormatComboBox;

    @FXML private ComboBox<String> audioFormatComboBox;
    @FXML private ComboBox<String> audioBitrateTypeComboBox;

    @FXML private HBox audioExtractionQualityControls;
    @FXML private Slider audioExtractionQualitySlider;
    @FXML private Label audioExtractionQualityValueLabel;

    @FXML private HBox audioExtractionBitrateControls;
    @FXML private Slider audioExtractionBitrateSlider;
    @FXML private Label audioExtractionBitrateValueLabel;

    @FXML private CheckBox audioExtractionAddMetadataCheckbox;
    @FXML private CheckBox audioExtractionEmbedThumbnailCheckbox;

    @FXML
    public void initialize() {
        initLanguageSettings();
        initDownloadPathSettings();
        initAutoSelectFormatSettings();
        initAudioExtractionSettings();
        initDownloadThreadsSettings();

        bindPropertyHolder();
    }

    private void initLanguageSettings() {
        List<ComboBoxValueHolder<Language>> languages = Stream.of(Language.values())
                .map(it -> new ComboBoxValueHolder<>(it.getLocalizedName(), it))
                .toList();
        languageComboBox.getItems().addAll(languages);

        Language currentLanguage = Language.getByLocaleCode(configRegistry.get(LanguageConfigProperty.class).getValue());
        languages.stream()
                .filter(it -> it.value() == currentLanguage)
                .findFirst()
                .ifPresent(it -> languageComboBox.getSelectionModel().select(it));

        languageComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                Language newLanguage = newValue.value();
                LanguageConfigProperty languageConfigProperty = configRegistry.get(LanguageConfigProperty.class);
                languageConfigProperty.setValue(newLanguage.getLocaleCode());
                Dialogs.info("preferences.general.language.restartrequired", newLanguage);
            }
        });
        languageComboBox.setOnScroll(new ComboBoxMouseScrollHandler());
    }

    private void bindPropertyHolder() {
        askPathRadioBtn.selectedProperty().bindBidirectional(configRegistry.get(AlwaysAskDownloadPathConfigProperty.class).getProperty());
        downloadPathDirectoryChooser.pathProperty().bindBidirectional(configRegistry.get(DownloadPathConfigProperty.class).getProperty());
        autoSearchFromClipboardCheckBox.selectedProperty().bindBidirectional(configRegistry.get(AutoSearchFromClipboardConfigProperty.class).getProperty());
        autoSelectFormatComboBox.valueProperty().bindBidirectional(configRegistry.get(AutoSelectFormatConfigProperty.class).getProperty());
        audioFormatComboBox.valueProperty().bindBidirectional(configRegistry.get(AudioExtractionFormatConfigProperty.class).getProperty());
        audioBitrateTypeComboBox.valueProperty().bindBidirectional(configRegistry.get(AudioExtractionBitrateTypeConfigProperty.class).getProperty());
        audioExtractionAddMetadataCheckbox.selectedProperty().bindBidirectional(configRegistry.get(AudioExtractionAddMetadataConfigProperty.class).getProperty());
        audioExtractionEmbedThumbnailCheckbox.selectedProperty().bindBidirectional(configRegistry.get(AudioExtractionEmbedThumbnailConfigProperty.class).getProperty());
        audioExtractionQualitySlider.valueProperty().bindBidirectional(configRegistry.get(AudioExtractionQualityConfigProperty.class).getProperty());
        loadThumbnailsCheckbox.selectedProperty().bindBidirectional(configRegistry.get(LoadThumbnailsConfigProperty.class).getProperty());
    }

    private void initDownloadPathSettings() {
        downloadPathRadioBtn.setToggleGroup(downloadPathRadioGroup);
        askPathRadioBtn.setToggleGroup(downloadPathRadioGroup);
        downloadPathDirectoryChooser.setButtonText(ctx.getLocalizedString("button.directorychoose"));
        downloadPathDirectoryChooser.disableProperty().bind(downloadPathRadioBtn.selectedProperty().not());
        downloadPathRadioBtn.setSelected(true); // Set default value to trigger ToggleGroup. It will be overriden during PropertyHolder binding
    }

    private void initAutoSelectFormatSettings() {
        ObservableList<Integer> comboBoxItems = autoSelectFormatComboBox.getItems();
        comboBoxItems.add(AutoSelectFormatConfigItem.DEFAULT);
        for (Resolution res : Resolution.values()) {
            comboBoxItems.add(res.getHeight());
        }

        final String BEST_FORMAT = ctx.getLocalizedString("preferences.general.autoselectformat.best");

        autoSelectFormatComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Integer object) {
                if (object == null) {
                    return null;
                }

                return object.equals(AutoSelectFormatConfigItem.DEFAULT) ? BEST_FORMAT : object + "p " + Resolution.getDescriptionByHeight(object);
            }

            @Override
            public Integer fromString(String string) {
                return BEST_FORMAT.equals(string) ? AutoSelectFormatConfigItem.DEFAULT : Integer.parseInt(StringUtils.substringBefore(string, "p"));
            }
        });

        autoSelectFormatComboBox.setOnScroll(new ComboBoxMouseScrollHandler());
    }

    private void initAudioExtractionSettings() {
        List<String> audioFormats = Stream.of(AudioFormat.values())
                .map(AudioFormat::toString)
                .toList();
        audioFormatComboBox.setItems(FXCollections.observableArrayList(audioFormats));
        audioFormatComboBox.setOnScroll(new ComboBoxMouseScrollHandler());

        List<String> bitrateTypes = Stream.of(BitrateType.values())
                .map(BitrateType::toString)
                .toList();
        audioBitrateTypeComboBox.setItems(FXCollections.observableArrayList(bitrateTypes));
        audioBitrateTypeComboBox.setOnScroll(new ComboBoxMouseScrollHandler());

        var bitrateTypeProperty = configRegistry.get(AudioExtractionBitrateTypeConfigProperty.class).getProperty();
        var cbrBitrateSelectedBinding = Bindings.createBooleanBinding(() -> BitrateType.fromString(bitrateTypeProperty.get()) == BitrateType.CBR, bitrateTypeProperty);
        var mp3SelectedBinding = Bindings.createBooleanBinding(() -> AudioFormat.fromString(audioFormatComboBox.getValue()) == AudioFormat.MP3, audioFormatComboBox.valueProperty());
        audioBitrateTypeComboBox.visibleProperty().bind(mp3SelectedBinding);
        audioBitrateTypeComboBox.managedProperty().bind(audioBitrateTypeComboBox.visibleProperty());
        audioExtractionBitrateControls.visibleProperty().bind(mp3SelectedBinding.and(cbrBitrateSelectedBinding));
        audioExtractionBitrateControls.managedProperty().bind(audioExtractionBitrateControls.visibleProperty());
        audioExtractionQualityControls.visibleProperty().bind(audioExtractionBitrateControls.visibleProperty().not());
        audioExtractionQualityControls.managedProperty().bind(audioExtractionQualityControls.visibleProperty());

        audioExtractionQualitySlider.setOnScroll(new SliderMouseScrollHandler());
        audioExtractionQualityValueLabel.textProperty().bind(Bindings.createStringBinding(() -> String.valueOf((int) audioExtractionQualitySlider.getValue()), audioExtractionQualitySlider.valueProperty()));

        var audioExtractionBitrateConfigProperty = configRegistry.get(AudioExtractionBitrateConfigProperty.class).getProperty();
        audioExtractionBitrateSlider.setValue(MP3_BITRATE_SLIDER_BIDI_MAP.getKey(audioExtractionBitrateConfigProperty.getValue()));
        audioExtractionBitrateValueLabel.textProperty().bind(Bindings.createStringBinding(() -> audioExtractionBitrateConfigProperty.getValue() + "kbps", audioExtractionBitrateConfigProperty));
        audioExtractionBitrateSlider.setOnScroll(new SliderMouseScrollHandler());
        audioExtractionBitrateConfigProperty.bind(Bindings.createIntegerBinding(() ->
                        MP3_BITRATE_SLIDER_BIDI_MAP.get(audioExtractionBitrateSlider.valueProperty().intValue()),
                audioExtractionBitrateSlider.valueProperty())
        );
    }

    private void initDownloadThreadsSettings() {
        downloadThreadsComboBox.setItems(FXCollections.observableArrayList(
                IntStream.rangeClosed(1, 10).boxed().toList()
        ));

        DownloadThreadsConfigProperty pref = configRegistry.get(DownloadThreadsConfigProperty.class);
        downloadThreadsComboBox.getSelectionModel().select(pref.getValue());
        downloadThreadsComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                pref.setValue(newValue);
                Dialogs.info("preferences.general.download.threads.restartrequired");
            }
        });
        downloadThreadsComboBox.setOnScroll(new ComboBoxMouseScrollHandler());
    }

    @Override
    public boolean hasErrors() {
        return false;
    }
}
