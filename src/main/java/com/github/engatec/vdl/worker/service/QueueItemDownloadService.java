package com.github.engatec.vdl.worker.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.engatec.vdl.core.AppExecutors;
import com.github.engatec.vdl.core.ApplicationContext;
import com.github.engatec.vdl.core.QueueManager;
import com.github.engatec.vdl.core.YoutubeDlManager;
import com.github.engatec.vdl.exception.YoutubeDlProcessException;
import com.github.engatec.vdl.model.DownloadStatus;
import com.github.engatec.vdl.model.QueueItem;
import com.github.engatec.vdl.worker.data.QueueItemDownloadProgressData;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class QueueItemDownloadService extends Service<QueueItemDownloadProgressData> {

    private static final Logger LOGGER = LogManager.getLogger(QueueItemDownloadService.class);

    private static final String SIZE_SEPARATOR = " / ";
    private static final String FORMAT_SEPARATOR = "/";

    private static final String ERROR_PREFIX = "ERROR:";

    private static final String GROUP_PROGRESS = "progress";
    private static final String GROUP_SIZE = "size";
    private static final String GROUP_THROUGHPUT = "throughput";

    private static final Pattern DOWNLOAD_PROGRESS_PATTERN = Pattern.compile(
            "\\s*\\[download]\\s+" +
                    "(?<progress>\\d+\\.?\\d*)%\\s+" +
                    "of\\s+(?<size>.+)\\s+" +
                    "at\\s+(?<throughput>.+?)\\s+" +
                    ".*"
    );

    private static final Pattern DOWNLOAD_NEW_DESTINATION_PATTERN = Pattern.compile("\\s*\\[download] Destination:.*");

    private final QueueItem queueItem;
    private static final int MAX_PROGRESS_PER_ITEM = 100;
    private final int maxOverallProgress;

    public QueueItemDownloadService(QueueItem queueItem) {
        super();
        this.queueItem = queueItem;
        maxOverallProgress = MAX_PROGRESS_PER_ITEM * (StringUtils.countMatches(StringUtils.substringBefore(queueItem.getFormatId(), FORMAT_SEPARATOR), '+') + 1);
        setExecutor(AppExecutors.QUEUE_EXECUTOR);

        queueItem.progressProperty().bind(progressProperty());
        valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                updateQueueItem(null, newValue.getSize(), newValue.getThroughput());
            }
        });
    }

    @Override
    public void start() {
        DownloadStatus status = queueItem.getStatus();
        if (status != DownloadStatus.READY) {
            String msg = "Queue item must be in state " + DownloadStatus.READY + ". Was in state " + status;
            LOGGER.error(msg);
            throw new IllegalStateException(msg);
        }
        queueItem.setStatus(DownloadStatus.SCHEDULED);

        super.start();
    }

    @Override
    public void restart() {
        DownloadStatus status = queueItem.getStatus();
        if (status != DownloadStatus.CANCELLED) {
            String msg = "Queue item must be in state " + DownloadStatus.CANCELLED + ". Was in state " + status;
            LOGGER.error(msg);
            throw new IllegalStateException(msg);
        }
        queueItem.setStatus(DownloadStatus.READY);

        super.restart();
    }

    @Override
    protected void running() {
        queueItem.setStatus(DownloadStatus.IN_PROGRESS);
    }

    @Override
    protected void succeeded() {
        updateQueueItem(DownloadStatus.FINISHED, null, StringUtils.EMPTY);
        updateProgress(1);
        QueueManager.INSTANCE.removeItem(queueItem);
    }

    @Override
    protected void cancelled() {
        updateQueueItem(DownloadStatus.CANCELLED, StringUtils.EMPTY, StringUtils.EMPTY);
    }

    @Override
    protected void failed() {
        updateQueueItem(DownloadStatus.FAILED, null, StringUtils.EMPTY);
        updateProgress(0);
        LOGGER.error(getException().getMessage());
    }

    private void updateQueueItem(DownloadStatus status, String size, String throughput) {
        if (status != null) {
            queueItem.setStatus(status);
        }

        if (size != null) {
            queueItem.setSize(size);
        }

        queueItem.setThroughput(throughput);
    }

    private void updateProgress(double value) {
        queueItem.progressProperty().unbind();
        queueItem.setProgress(value);
    }

    @Override
    protected Task<QueueItemDownloadProgressData> createTask() {
        return new Task<>() {
            @Override
            protected QueueItemDownloadProgressData call() throws Exception {
                var progressData = new QueueItemDownloadProgressData();
                AtomicInteger progressModificator = new AtomicInteger(0);
                Process process = YoutubeDlManager.INSTANCE.download(queueItem);
                try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream(), ApplicationContext.INSTANCE.getSystemCharset()))) {
                    reader.lines().filter(StringUtils::isNotBlank).forEach(it -> {
                        if (Thread.interrupted()) {
                            cancel();
                        }

                        if (isCancelled()) {
                            process.destroy();
                            return;
                        }

                        Matcher matcher = DOWNLOAD_PROGRESS_PATTERN.matcher(it);
                        if (matcher.matches()) {
                            double currentProgress = Double.parseDouble(matcher.group(GROUP_PROGRESS));

                            progressData.setProgress(currentProgress);
                            progressData.setThroughput(matcher.group(GROUP_THROUGHPUT));
                            progressData.setSize(calculateSizeString(progressData.getSize(), matcher.group(GROUP_SIZE)));

                            updateProgress(currentProgress + progressModificator.get(), maxOverallProgress);
                            updateValue(new QueueItemDownloadProgressData(progressData.getProgress(), progressData.getSize(), progressData.getThroughput()));
                        } else if (DOWNLOAD_NEW_DESTINATION_PATTERN.matcher(it).matches() && StringUtils.isNotBlank(progressData.getSize())) {
                            progressModificator.addAndGet(MAX_PROGRESS_PER_ITEM);
                            progressData.setProgress(0);
                            progressData.setSize(progressData.getSize() + SIZE_SEPARATOR);
                        }
                    });
                }

                checkErrors(process.getErrorStream());

                return null;
            }

            /**
             * Method updates latest not finished item size
             */
            private String calculateSizeString(String currentlyDisplayed, String youtubeDlParsed) {
                String finishedItemsSize = StringUtils.substringBeforeLast(currentlyDisplayed, SIZE_SEPARATOR);
                if (Objects.equals(currentlyDisplayed, finishedItemsSize)) { // There's no yet finished item
                    return youtubeDlParsed;
                } else {
                    return finishedItemsSize + SIZE_SEPARATOR + youtubeDlParsed;
                }
            }

            private void checkErrors(InputStream errorStream) throws IOException {
                List<String> errLines = IOUtils.readLines(errorStream, ApplicationContext.INSTANCE.getSystemCharset());
                if (CollectionUtils.isNotEmpty(errLines)) {
                    String msg = String.join(System.lineSeparator(), errLines);
                    // ErrorStream aggregates not only errors, but warnings as well. Don't care much about warnings so they will be just logged and ignored, but errors are showstoppers
                    boolean hasErrors = errLines.stream()
                            .filter(StringUtils::isNotBlank)
                            .map(StringUtils::strip)
                            .anyMatch(it -> it.startsWith(ERROR_PREFIX));
                    if (hasErrors) {
                        throw new YoutubeDlProcessException(msg);
                    } else {
                        LOGGER.warn(msg);
                    }
                }
            }
        };
    }
}
