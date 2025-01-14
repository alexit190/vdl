package com.github.engatec.vdl.ui.component.controller.search;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.imageio.ImageIO;

import com.github.engatec.vdl.core.ApplicationContext;
import com.github.engatec.vdl.core.QueueManager;
import com.github.engatec.vdl.core.youtubedl.YoutubeDlAttr;
import com.github.engatec.vdl.exception.ServiceStubException;
import com.github.engatec.vdl.handler.ComboBoxMouseScrollHandler;
import com.github.engatec.vdl.model.AudioFormat;
import com.github.engatec.vdl.model.BitrateType;
import com.github.engatec.vdl.model.Format;
import com.github.engatec.vdl.model.QueueItem;
import com.github.engatec.vdl.model.Resolution;
import com.github.engatec.vdl.model.VideoInfo;
import com.github.engatec.vdl.model.downloadable.BaseDownloadable;
import com.github.engatec.vdl.model.downloadable.Downloadable;
import com.github.engatec.vdl.model.postprocessing.ExtractAudioPostprocessing;
import com.github.engatec.vdl.preference.ConfigRegistry;
import com.github.engatec.vdl.preference.property.general.AudioExtractionBitrateConfigProperty;
import com.github.engatec.vdl.preference.property.general.AudioExtractionBitrateTypeConfigProperty;
import com.github.engatec.vdl.preference.property.general.AudioExtractionFormatConfigProperty;
import com.github.engatec.vdl.preference.property.general.AudioExtractionQualityConfigProperty;
import com.github.engatec.vdl.preference.property.general.AutoSelectFormatConfigProperty;
import com.github.engatec.vdl.preference.property.general.LoadThumbnailsConfigProperty;
import com.github.engatec.vdl.preference.property.misc.RecentDownloadPathConfigProperty;
import com.github.engatec.vdl.service.DownloadableSearchService;
import com.github.engatec.vdl.ui.Icon;
import com.github.engatec.vdl.ui.data.ComboBoxValueHolder;
import com.github.engatec.vdl.ui.helper.Dialogs;
import com.github.engatec.vdl.ui.helper.Tooltips;
import com.github.engatec.vdl.ui.stage.core.FormatsStage;
import com.github.engatec.vdl.ui.stage.core.SubtitlesStage;
import com.github.engatec.vdl.util.AppUtils;
import com.github.engatec.vdl.util.Thumbnails;
import com.github.engatec.vdl.util.YouDlUtils;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DownloadableItemComponentController extends HBox {

    private static final Logger LOGGER = LogManager.getLogger(DownloadableItemComponentController.class);

    private static final String CUSTOM_FORMAT_LABEL = "Custom format";
    private static final String N_A_FORMAT_LABEL = "N/A";

    private final ApplicationContext ctx = ApplicationContext.getInstance();
    private final QueueManager queueManager = ctx.getManager(QueueManager.class);

    private final Stage stage;
    private VideoInfo videoInfo;
    private String customFormat;

    @FXML private HBox rootHBox;

    @FXML private Label titleLabel;
    @FXML private Label durationLabel;
    @FXML private ComboBox<ComboBoxValueHolder<String>> formatsComboBox;
    @FXML private Button allFormatsButton;
    @FXML private Button audioButton;
    @FXML private Button subtitlesButton;
    @FXML private CheckBox itemSelectedCheckBox;

    @FXML private ImageView thumbnailImageView;
    @FXML private StackPane thumbnailWrapperPane;

    public DownloadableItemComponentController(Stage stage, VideoInfo videoInfo) {
        this.stage = stage;
        this.videoInfo = videoInfo;
    }

    @FXML
    public void initialize() {
        initThumbnail();
        initLabels();
        initFormatsDropdown();
        initAllAvailableFormatsButton();
        initAudioDownloadButton();
        initSubtitlesButton();

        adjustUiControls();
    }

    private void adjustUiControls() {
        rootHBox.setOnMouseClicked(event -> itemSelectedCheckBox.setSelected(!itemSelectedCheckBox.isSelected()));

        formatsComboBox.prefHeightProperty().bind(allFormatsButton.heightProperty());
        formatsComboBox.setOnScroll(new ComboBoxMouseScrollHandler());
    }

    private void initThumbnail() {
        if (!ctx.getConfigRegistry().get(LoadThumbnailsConfigProperty.class).getValue()) {
            thumbnailWrapperPane.setVisible(false);
            thumbnailWrapperPane.setManaged(false);
            return;
        }

        String thumbnailUrl = Thumbnails.normalizeThumbnailUrl(videoInfo, Thumbnails.Quality.MEDIUM);
        if (StringUtils.isBlank(thumbnailUrl)) {
            return;
        }

        Image image;
        try {
            image = new Image(thumbnailUrl);
        } catch (Exception e) {
            // In case image constructor threw an exception, show no thumbnail
            LOGGER.warn(e.getMessage(), e);
            return;
        }

        if (image.getException() == null) {
            thumbnailImageView.setCursor(Cursor.HAND);
            thumbnailImageView.setImage(image);
            thumbnailImageView.setOnMouseClicked(event -> {
                FileChooser fileChooser = new FileChooser();
                File recentDownloadPath = Path.of(ctx.getConfigRegistry().get(RecentDownloadPathConfigProperty.class).getValue()).toFile();
                if (recentDownloadPath.isDirectory()) {
                    fileChooser.setInitialDirectory(recentDownloadPath);
                }
                fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("*.jpg", "*.jpg"));
                fileChooser.setInitialFileName("thumbnail_img");
                File downloadPath = fileChooser.showSaveDialog(stage);
                if (downloadPath != null) {
                    boolean saved = saveImage(videoInfo, downloadPath);
                    if (!saved) {
                        Dialogs.error("image.save.error");
                    }
                }
                event.consume();
            });
        }
    }

    private boolean saveImage(VideoInfo videoInfo, File downloadPath) {
        BufferedImage origImg = null;
        for (Thumbnails.Quality quality : List.of(Thumbnails.Quality.MAX, Thumbnails.Quality.HIGH, Thumbnails.Quality.MEDIUM)) {
            try {
                origImg = ImageIO.read(new URL(Thumbnails.normalizeThumbnailUrl(videoInfo, quality)));
                break;
            } catch (IOException e) {
                LOGGER.warn(e.getMessage(), e);
            }
        }

        boolean saved = false;
        if (origImg != null) {
            final BufferedImage convertedImg = new BufferedImage(origImg.getWidth(), origImg.getHeight(), BufferedImage.TYPE_INT_RGB);
            convertedImg.createGraphics().drawImage(origImg, 0, 0, Color.WHITE, null);

            try {
                saved = ImageIO.write(convertedImg, "jpg", downloadPath);
            } catch (IOException e) {
                LOGGER.warn(e.getMessage(), e);
            }
        }

        return saved;
    }

    private void initLabels() {
        titleLabel.setText(videoInfo.title());

        int durationSeconds = Objects.requireNonNullElse(videoInfo.duration(), 0);
        String formattedDuration = durationSeconds <= 0 ? StringUtils.EMPTY : DurationFormatUtils.formatDuration(durationSeconds * 1000L, "HH:mm:ss");
        durationLabel.setText(formattedDuration);
    }

    private void initFormatsDropdown() {
        final String noCodec = YoutubeDlAttr.NO_CODEC.getValue();
        Predicate<Format> anyCodecDefined = it -> !noCodec.equals(it.getVcodec()) || !noCodec.equals(it.getAcodec());

        List<Integer> commonAvailableHeights = ListUtils.emptyIfNull(videoInfo.formats()).stream()
                .filter(anyCodecDefined)
                .map(Format::getHeight)
                .filter(Objects::nonNull)
                .filter(it -> it > 0) // Should never happen, just a sanity check in case there's a bug in yt-dlp
                .distinct()
                .sorted(Comparator.reverseOrder())
                .toList();

        if (CollectionUtils.isEmpty(commonAvailableHeights)) {
            commonAvailableHeights = Stream.of(Resolution.values()).map(Resolution::getHeight).toList();
        }

        ObservableList<ComboBoxValueHolder<String>> comboBoxItems = formatsComboBox.getItems();
        comboBoxItems.clear();
        Integer autoSelectFormat = ctx.getConfigRegistry().get(AutoSelectFormatConfigProperty.class).getValue();
        ComboBoxValueHolder<String> selectedItem = null;
        for (Integer height : commonAvailableHeights) {
            ComboBoxValueHolder<String> item = new ComboBoxValueHolder<>(height + "p " + Resolution.getDescriptionByHeight(height), YouDlUtils.createFormatByHeight(height));
            comboBoxItems.add(item);

            if (selectedItem == null && height <= autoSelectFormat) {
                selectedItem = item;
            }
        }

        if (comboBoxItems.isEmpty()) {
            comboBoxItems.add(new ComboBoxValueHolder<>(N_A_FORMAT_LABEL, YouDlUtils.createFormatByHeight(null)));
        }

        if (selectedItem == null) {
            formatsComboBox.getSelectionModel().selectFirst();
        } else {
            formatsComboBox.getSelectionModel().select(selectedItem);
        }

        adjustWidth();
    }

    /**
     * A small hack to make comboboxes the same width no matter when they get rendered
     */
    private void adjustWidth() {
        ComboBoxValueHolder<String> dummyItem = new ComboBoxValueHolder<>("99999p 8K Ultra HD", StringUtils.EMPTY);
        formatsComboBox.getItems().add(dummyItem);
        formatsComboBox.setOnKeyPressed(e -> {
            formatsComboBox.getItems().remove(dummyItem);
            formatsComboBox.setOnKeyPressed(null);
            formatsComboBox.setOnMouseEntered(null);
        });
        formatsComboBox.setOnMouseEntered(e -> {
            formatsComboBox.getItems().remove(dummyItem);
            formatsComboBox.setOnMouseEntered(null);
            formatsComboBox.setOnKeyPressed(null);
        });
    }

    private void initAllAvailableFormatsButton() {
        allFormatsButton.setGraphic(new ImageView(Icon.FILTER_LIST_SMALL.getImage()));
        allFormatsButton.setTooltip(Tooltips.create("format.all"));
        allFormatsButton.setOnAction(e -> {
            if (CollectionUtils.isEmpty(videoInfo.formats())) {
                DownloadableSearchService searchService = new DownloadableSearchService();
                searchService.setUrls(List.of(videoInfo.baseUrl()));
                searchService.setOnSucceeded(it ->
                        ListUtils.emptyIfNull((List<VideoInfo>) it.getSource().getValue()).stream()
                                .findFirst()
                                .ifPresentOrElse(
                                        vi -> {
                                            videoInfo = vi;
                                            updateControls(vi);

                                            if (CollectionUtils.isEmpty(videoInfo.formats())) {
                                                Dialogs.info("formats.notfound");
                                            } else {
                                                showFormatsStage();
                                            }
                                        },
                                        () -> Dialogs.info("formats.notfound")
                                )
                );
                searchService.setOnFailed(event -> {
                    Throwable exception = Objects.requireNonNullElseGet(event.getSource().getException(), () -> new ServiceStubException(searchService.getClass()));
                    LOGGER.warn(exception.getMessage(), exception);
                    Dialogs.error("formats.error");
                });

                Dialogs.progress("format.searching", stage, searchService);
            } else {
                showFormatsStage();
            }

            e.consume();
        });
    }

    private void showFormatsStage() {
        new FormatsStage(videoInfo, customFormat, formatId -> {
            customFormat = formatId;
            ObservableList<ComboBoxValueHolder<String>> comboBoxItems = formatsComboBox.getItems();
            comboBoxItems.stream().filter(it -> it.key().equals(CUSTOM_FORMAT_LABEL)).findFirst().ifPresent(comboBoxItems::remove);
            ComboBoxValueHolder<String> valueHolder = new ComboBoxValueHolder<>(CUSTOM_FORMAT_LABEL, customFormat);
            comboBoxItems.add(valueHolder);
            formatsComboBox.getSelectionModel().select(valueHolder);
        }).modal(stage).show();
    }

    private void initAudioDownloadButton() {
        audioButton.setGraphic(new ImageView(Icon.AUDIOTRACK_SMALL.getImage()));
        audioButton.setTooltip(Tooltips.create("download.audio"));
        audioButton.setOnAction(e -> {
            AppUtils.resolveDownloadPath(stage).ifPresent(this::downloadAudio);
            e.consume();
        });
    }

    private void initSubtitlesButton() {
        subtitlesButton.setGraphic(new ImageView(Icon.SUBTITLES_SMALL.getImage()));
        subtitlesButton.setTooltip(Tooltips.create("subtitles"));
        subtitlesButton.setOnAction(e -> {
            // No formats means that video info hasn't been fully loaded, thus no subtitles either
            if (CollectionUtils.isEmpty(videoInfo.formats())) {
                DownloadableSearchService searchService = new DownloadableSearchService();
                searchService.setUrls(List.of(videoInfo.baseUrl()));
                searchService.setOnSucceeded(it ->
                        ListUtils.emptyIfNull((List<VideoInfo>) it.getSource().getValue()).stream()
                                .findFirst()
                                .ifPresentOrElse(
                                        vi -> {
                                            videoInfo = vi;
                                            updateControls(vi);

                                            if (CollectionUtils.isEmpty(videoInfo.subtitles())) {
                                                Dialogs.info("subtitles.notfound");
                                            } else {
                                                showSubtitlesStage();
                                            }
                                        },
                                        () -> Dialogs.info("subtitles.notfound")
                                )
                );
                searchService.setOnFailed(event -> {
                    Throwable exception = Objects.requireNonNullElseGet(event.getSource().getException(), () -> new ServiceStubException(searchService.getClass()));
                    LOGGER.warn(exception.getMessage(), exception);
                    Dialogs.error("subtitles.error");
                });

                Dialogs.progress("subtitles.searching", stage, searchService);
            } else if (CollectionUtils.isEmpty(videoInfo.subtitles())) {
                Dialogs.info("subtitles.notfound");
            } else {
                showSubtitlesStage();
            }
            e.consume();
        });
    }

    private void showSubtitlesStage() {
        new SubtitlesStage(videoInfo).modal(stage).show();
    }

    private void updateControls(VideoInfo vi) {
        if (CollectionUtils.isNotEmpty(vi.formats())) {
            initFormatsDropdown();
        }
    }

    public void setSelectable(boolean selectable) {
        itemSelectedCheckBox.setVisible(selectable);
        itemSelectedCheckBox.setManaged(selectable);
    }

    public boolean isSelected() {
        return itemSelectedCheckBox.isSelected();
    }

    public void setSelected(boolean selected) {
        itemSelectedCheckBox.setSelected(selected);
    }

    public CheckBox getItemSelectedCheckBox() {
        return itemSelectedCheckBox;
    }

    public Downloadable getDownloadable() {
        BaseDownloadable downloadable = new BaseDownloadable();
        downloadable.setFormatId(formatsComboBox.getSelectionModel().getSelectedItem().value());
        downloadable.setTitle(titleLabel.getText());
        downloadable.setBaseUrl(videoInfo.baseUrl());
        return downloadable;
    }

    public void download(Path path) {
        Downloadable downloadable = getDownloadable();
        downloadable.setDownloadPath(path);
        queueManager.addItem(new QueueItem(downloadable));
    }

    public void downloadAudio(Path path) {
        ConfigRegistry configRegistry = ctx.getConfigRegistry();

        AudioFormat format = AudioFormat.fromString(configRegistry.get(AudioExtractionFormatConfigProperty.class).getValue());
        BitrateType bitrateType = BitrateType.fromString(configRegistry.get(AudioExtractionBitrateTypeConfigProperty.class).getValue());
        String quality;
        if (format == AudioFormat.MP3 && bitrateType == BitrateType.CBR) {
            quality = configRegistry.get(AudioExtractionBitrateConfigProperty.class).getValue() + "K";
        } else {
            // Yt-dlp quality goes from 9 (worst) to 0 (best), thus needs adjusting to VDLs 0 (worst) - 9 (best)
            quality = String.valueOf(Math.abs(configRegistry.get(AudioExtractionQualityConfigProperty.class).getValue() - AudioFormat.BEST_QUALITY));
        }

        Downloadable downloadable = getDownloadable();
        downloadable.setDownloadPath(path);
        // No need to download video if user only wants to extract audio. However if formats are empty chances are this is a music only service,
        // then it might not have "bestaudio" format and "best" must be used
        downloadable.setFormatId("bestaudio" + (CollectionUtils.isEmpty(videoInfo.formats()) ? "/best" : ""));
        downloadable.setPostprocessingSteps(List.of(new ExtractAudioPostprocessing(format, quality)));
        queueManager.addItem(new QueueItem(downloadable));
    }
}
