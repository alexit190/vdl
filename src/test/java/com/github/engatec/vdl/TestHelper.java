package com.github.engatec.vdl;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.github.engatec.vdl.core.ApplicationContext;
import com.github.engatec.vdl.core.HistoryManager;
import com.github.engatec.vdl.core.QueueManager;
import com.github.engatec.vdl.core.SubscriptionsManager;
import com.github.engatec.vdl.db.DbManager;
import com.github.engatec.vdl.preference.ConfigRegistry;
import com.github.engatec.vdl.preference.property.general.DownloadThreadsConfigProperty;
import com.github.engatec.vdl.preference.property.general.LanguageConfigProperty;
import org.apache.commons.lang3.StringUtils;
import org.mockito.Mockito;

public class TestHelper {

    private static final Path APP_BINARIES_PATH = Path.of(StringUtils.EMPTY);
    private static final Path APP_DATA_PATH = Path.of(StringUtils.EMPTY);
    private static final String DB_NAME = "test.db";

    static {
        try {
            Files.deleteIfExists(APP_DATA_PATH.resolve(DB_NAME));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void initTestApplicationContext() {
        ConfigRegistry configRegistryMock = Mockito.mock(ConfigRegistry.class);
        Mockito.when(configRegistryMock.get(LanguageConfigProperty.class)).thenReturn(Mockito.mock(LanguageConfigProperty.class));
        Mockito.when(configRegistryMock.get(LanguageConfigProperty.class).getValue()).thenReturn("en");

        Mockito.when(configRegistryMock.get(DownloadThreadsConfigProperty.class)).thenReturn(Mockito.mock(DownloadThreadsConfigProperty.class));
        Mockito.when(configRegistryMock.get(DownloadThreadsConfigProperty.class).getValue()).thenReturn(3);

        ApplicationContext.create(
                APP_BINARIES_PATH,
                APP_DATA_PATH,
                configRegistryMock,
                List.of(
                        new DbManager("jdbc:sqlite:" + APP_DATA_PATH.resolve(DB_NAME)),
                        new QueueManager(),
                        new HistoryManager(),
                        new SubscriptionsManager()
                )
        );
    }
}
