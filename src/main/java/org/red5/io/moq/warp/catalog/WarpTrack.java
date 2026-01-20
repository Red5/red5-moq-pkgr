package org.red5.io.moq.warp.catalog;

import java.util.List;

/**
 * WARP track object per draft-ietf-moq-warp.
 */
public class WarpTrack {
    private String namespace;

    private String name;

    private String packaging;

    private String role;

    private String label;

    private Integer renderGroup;

    private Integer altGroup;

    private String initData;

    private List<String> depends;

    private Integer temporalId;

    private Integer spatialId;

    private String codec;

    private String mimeType;

    private Integer framerate;

    private Integer timescale;

    private Integer bitrate;

    private Integer width;

    private Integer height;

    private Integer samplerate;

    private String channelConfig;

    private Integer displayWidth;

    private Integer displayHeight;

    private String lang;

    private String parentName;

    private Long trackDuration;

    private Boolean isLive;

    /** MSF 5.1.16: Target latency in milliseconds for real-time playback. */
    private Long targetLatency;

    private String type;

    private String eventType;

    private Integer maxGrpSapStartingType;

    private Integer maxObjSapStartingType;

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPackaging() {
        return packaging;
    }

    public void setPackaging(String packaging) {
        this.packaging = packaging;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Integer getRenderGroup() {
        return renderGroup;
    }

    public void setRenderGroup(Integer renderGroup) {
        this.renderGroup = renderGroup;
    }

    public Integer getAltGroup() {
        return altGroup;
    }

    public void setAltGroup(Integer altGroup) {
        this.altGroup = altGroup;
    }

    public String getInitData() {
        return initData;
    }

    public void setInitData(String initData) {
        this.initData = initData;
    }

    public List<String> getDepends() {
        return depends;
    }

    public void setDepends(List<String> depends) {
        this.depends = depends;
    }

    public Integer getTemporalId() {
        return temporalId;
    }

    public void setTemporalId(Integer temporalId) {
        this.temporalId = temporalId;
    }

    public Integer getSpatialId() {
        return spatialId;
    }

    public void setSpatialId(Integer spatialId) {
        this.spatialId = spatialId;
    }

    public String getCodec() {
        return codec;
    }

    public void setCodec(String codec) {
        this.codec = codec;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Integer getFramerate() {
        return framerate;
    }

    public void setFramerate(Integer framerate) {
        this.framerate = framerate;
    }

    public Integer getTimescale() {
        return timescale;
    }

    public void setTimescale(Integer timescale) {
        this.timescale = timescale;
    }

    public Integer getBitrate() {
        return bitrate;
    }

    public void setBitrate(Integer bitrate) {
        this.bitrate = bitrate;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Integer getSamplerate() {
        return samplerate;
    }

    public void setSamplerate(Integer samplerate) {
        this.samplerate = samplerate;
    }

    public String getChannelConfig() {
        return channelConfig;
    }

    public void setChannelConfig(String channelConfig) {
        this.channelConfig = channelConfig;
    }

    public Integer getDisplayWidth() {
        return displayWidth;
    }

    public void setDisplayWidth(Integer displayWidth) {
        this.displayWidth = displayWidth;
    }

    public Integer getDisplayHeight() {
        return displayHeight;
    }

    public void setDisplayHeight(Integer displayHeight) {
        this.displayHeight = displayHeight;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String getParentName() {
        return parentName;
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
    }

    public Long getTrackDuration() {
        return trackDuration;
    }

    public void setTrackDuration(Long trackDuration) {
        this.trackDuration = trackDuration;
    }

    public Boolean getIsLive() {
        return isLive;
    }

    public void setIsLive(Boolean isLive) {
        this.isLive = isLive;
    }

    public Long getTargetLatency() {
        return targetLatency;
    }

    public void setTargetLatency(Long targetLatency) {
        this.targetLatency = targetLatency;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Integer getMaxGrpSapStartingType() {
        return maxGrpSapStartingType;
    }

    public void setMaxGrpSapStartingType(Integer maxGrpSapStartingType) {
        this.maxGrpSapStartingType = maxGrpSapStartingType;
    }

    public Integer getMaxObjSapStartingType() {
        return maxObjSapStartingType;
    }

    public void setMaxObjSapStartingType(Integer maxObjSapStartingType) {
        this.maxObjSapStartingType = maxObjSapStartingType;
    }
}
