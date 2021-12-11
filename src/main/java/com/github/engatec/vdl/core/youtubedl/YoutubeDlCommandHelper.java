package com.github.engatec.vdl.core.youtubedl;

import java.nio.file.Files;
import java.nio.file.Path;

import com.github.engatec.vdl.core.ApplicationContext;
import com.github.engatec.vdl.core.preferences.ConfigRegistry;
import com.github.engatec.vdl.model.downloadable.Downloadable;
import com.github.engatec.vdl.model.preferences.wrapper.youtubedl.AuthPasswordPref;
import com.github.engatec.vdl.model.preferences.wrapper.youtubedl.AuthUsernamePref;
import com.github.engatec.vdl.model.preferences.wrapper.youtubedl.CookiesFileLocationPref;
import com.github.engatec.vdl.model.preferences.wrapper.youtubedl.ForceIpV4Pref;
import com.github.engatec.vdl.model.preferences.wrapper.youtubedl.ForceIpV6Pref;
import com.github.engatec.vdl.model.preferences.wrapper.youtubedl.MarkWatchedPref;
import com.github.engatec.vdl.model.preferences.wrapper.youtubedl.NetrcPref;
import com.github.engatec.vdl.model.preferences.wrapper.youtubedl.NoContinuePref;
import com.github.engatec.vdl.model.preferences.wrapper.youtubedl.NoMTimePref;
import com.github.engatec.vdl.model.preferences.wrapper.youtubedl.NoPartPref;
import com.github.engatec.vdl.model.preferences.wrapper.youtubedl.OutputTemplatePref;
import com.github.engatec.vdl.model.preferences.wrapper.youtubedl.ProxyUrlPref;
import com.github.engatec.vdl.model.preferences.wrapper.youtubedl.RateLimitPref;
import com.github.engatec.vdl.model.preferences.wrapper.youtubedl.ReadCookiesPref;
import com.github.engatec.vdl.model.preferences.wrapper.youtubedl.SocketTimeoutPref;
import com.github.engatec.vdl.model.preferences.wrapper.youtubedl.SourceAddressPref;
import com.github.engatec.vdl.model.preferences.wrapper.youtubedl.TwoFactorCodePref;
import com.github.engatec.vdl.model.preferences.wrapper.youtubedl.VideoPasswordPref;
import com.github.engatec.vdl.model.preferences.youtubedl.RateLimitConfigItem;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.strip;

public class YoutubeDlCommandHelper {

    private static final Logger LOGGER = LogManager.getLogger(YoutubeDlCommandHelper.class);

    private static final ConfigRegistry configRegistry = ApplicationContext.getInstance().getConfigRegistry();

    public static void setOutputPath(YoutubeDlCommandBuilder commandBuilder, Downloadable downloadable) {
        String outputTemplate = configRegistry.get(OutputTemplatePref.class).getValue();
        commandBuilder.outputPath(downloadable.getDownloadPath().resolve(outputTemplate).toString());
    }

    public static void setGeneralOptions(YoutubeDlCommandBuilder commandBuilder) {
        Boolean markWatched = configRegistry.get(MarkWatchedPref.class).getValue();
        if (markWatched) {
            commandBuilder.markWatched();
        }

        Boolean noContinue = configRegistry.get(NoContinuePref.class).getValue();
        if (noContinue) {
            commandBuilder.noContinue();
        }

        Boolean noPart = configRegistry.get(NoPartPref.class).getValue();
        if (noPart) {
            commandBuilder.noPart();
        }

        Boolean noMTime = configRegistry.get(NoMTimePref.class).getValue();
        if (noMTime) {
            commandBuilder.noMTime();
        }

        Boolean readCookies = configRegistry.get(ReadCookiesPref.class).getValue();
        if (readCookies) {
            String cookiesFileLocation = configRegistry.get(CookiesFileLocationPref.class).getValue();
            Path cookiesPath = Path.of(cookiesFileLocation);
            if (Files.exists(cookiesPath) && Files.isReadable(cookiesPath)) {
                commandBuilder.cookiesFile(Path.of(cookiesFileLocation));
            }
        }
    }

    public static void setDownloadOptions(YoutubeDlCommandBuilder commandBuilder) {
        String limit = StringUtils.defaultIfBlank(configRegistry.get(RateLimitPref.class).getValue(), RateLimitConfigItem.DEFAULT);
        if (!limit.equals(RateLimitConfigItem.DEFAULT)) {
            commandBuilder.rateLimit(limit);
        }
    }

    public static void setNetworkOptions(YoutubeDlCommandBuilder commandBuilder) {
        String proxyUrl = strip(configRegistry.get(ProxyUrlPref.class).getValue());
        if (isNotBlank(proxyUrl)) {
            commandBuilder.proxy(proxyUrl);
        }

        String socketTimeout = strip(configRegistry.get(SocketTimeoutPref.class).getValue());
        if (isNotBlank(socketTimeout)) {
            try {
                int timeout = Integer.parseInt(socketTimeout);
                commandBuilder.socketTimeout(timeout);
            } catch (NumberFormatException e) {
                LOGGER.warn(e.getMessage());
            }
        }

        String sourceAddress = strip(configRegistry.get(SourceAddressPref.class).getValue());
        if (isNotBlank(sourceAddress)) {
            commandBuilder.sourceAddress(sourceAddress);
        }

        Boolean forceIpV4 = configRegistry.get(ForceIpV4Pref.class).getValue();
        if (forceIpV4) {
            commandBuilder.forceIpV4();
        }

        Boolean forceIpV6 = configRegistry.get(ForceIpV6Pref.class).getValue();
        if (forceIpV6) {
            commandBuilder.forceIpV6();
        }

        if (forceIpV4 && forceIpV6) {
            LOGGER.warn("Both forceIpV4 and forceIpV6 settings enabled!");
        }
    }

    public static void setAuthenticationOptions(YoutubeDlCommandBuilder commandBuilder) {
        String username = strip(configRegistry.get(AuthUsernamePref.class).getValue());
        if (isNotBlank(username)) {
            commandBuilder.username(username);
        }

        String password = strip(configRegistry.get(AuthPasswordPref.class).getValue());
        if (isNotBlank(password)) {
            commandBuilder.password(password);
        }

        if (isNotBlank(username) && isNotBlank(password)) {
            String twoFactorCode = configRegistry.get(TwoFactorCodePref.class).getValue();
            if (isNotBlank(twoFactorCode)) {
                commandBuilder.twoFactor(twoFactorCode);
            }
        }

        Boolean netrc = configRegistry.get(NetrcPref.class).getValue();
        if (netrc) {
            commandBuilder.useNetrc();
        }

        String videoPassword = configRegistry.get(VideoPasswordPref.class).getValue();
        if (isNotBlank(videoPassword)) {
            commandBuilder.videoPassword(videoPassword);
        }
    }
}
