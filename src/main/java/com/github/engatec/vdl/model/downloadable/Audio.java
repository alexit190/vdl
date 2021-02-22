package com.github.engatec.vdl.model.downloadable;

import java.nio.file.Path;

import com.github.engatec.vdl.model.Format;
import org.apache.commons.lang3.ObjectUtils;

public class Audio implements Downloadable {

    private final String baseUrl;
    private Path downloadPath;

    private int trackNo;
    private final Format format;

    public Audio(String baseUrl, Format format) {
        this.baseUrl = baseUrl;
        this.format = format;
    }

    @Override
    public String getBaseUrl() {
        return baseUrl;
    }

    @Override
    public Path getDownloadPath() {
        return downloadPath;
    }

    @Override
    public void setDownloadPath(Path downloadPath) {
        this.downloadPath = downloadPath;
    }

    @Override
    public String getFormatId() {
        return getId();
    }

    public String getId() {
        return format.getId();
    }

    public String getExtension() {
        return format.getExtension();
    }

    public Double getBitrate() {
        return ObjectUtils.defaultIfNull(format.getAudioBitrate(), format.getTotalBitrate());
    }

    public Long getFilesize() {
        return format.getFilesize();
    }

    public String getCodec() {
        return format.getAcodec();
    }

    public int getTrackNo() {
        return trackNo;
    }

    public void setTrackNo(int trackNo) {
        this.trackNo = trackNo;
    }
}