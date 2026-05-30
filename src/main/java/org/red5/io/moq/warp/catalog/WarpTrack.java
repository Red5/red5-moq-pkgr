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

    /** MSFTS 6.2: source-packet size in octets, MUST be 188 or 192. */
    private Integer m2tsPacketSize;

    /** MSFTS 6.3: usual number of source packets per media Object (advisory). */
    private Integer m2tsPacketsPerObject;

    /** MSFTS 6.4: MPEG-2 program number carried by this track. */
    private Integer m2tsProgramNumber;

    /** MSFTS 6.5: PID carrying the Program Map Table for the program (advisory). */
    private Integer m2tsPmtPid;

    /** MSFTS 6.6: PID carrying the Program Clock Reference for the program (advisory). */
    private Integer m2tsPcrPid;

    /** MSFTS 6.7: maximum PAT/PMT repetition interval in milliseconds. */
    private Integer m2tsPsiInterval;

    /** MSFTS 6.8: when true, the first Object in every Group begins at a random access point. */
    private Boolean m2tsRandomAccess;

    /** MSFTS 6.9: interpretation of the 4-octet timestamp prefix ("arrival-time" or "opaque"). */
    private String m2tsTimestampMode;

    /** MSFTS 6.10: PID carrying SCTE-35 splice_info_section() messages (advisory). */
    private Integer m2tsScte35Pid;

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

    public Integer getM2tsPacketSize() {
        return m2tsPacketSize;
    }

    public void setM2tsPacketSize(Integer m2tsPacketSize) {
        this.m2tsPacketSize = m2tsPacketSize;
    }

    public Integer getM2tsPacketsPerObject() {
        return m2tsPacketsPerObject;
    }

    public void setM2tsPacketsPerObject(Integer m2tsPacketsPerObject) {
        this.m2tsPacketsPerObject = m2tsPacketsPerObject;
    }

    public Integer getM2tsProgramNumber() {
        return m2tsProgramNumber;
    }

    public void setM2tsProgramNumber(Integer m2tsProgramNumber) {
        this.m2tsProgramNumber = m2tsProgramNumber;
    }

    public Integer getM2tsPmtPid() {
        return m2tsPmtPid;
    }

    public void setM2tsPmtPid(Integer m2tsPmtPid) {
        this.m2tsPmtPid = m2tsPmtPid;
    }

    public Integer getM2tsPcrPid() {
        return m2tsPcrPid;
    }

    public void setM2tsPcrPid(Integer m2tsPcrPid) {
        this.m2tsPcrPid = m2tsPcrPid;
    }

    public Integer getM2tsPsiInterval() {
        return m2tsPsiInterval;
    }

    public void setM2tsPsiInterval(Integer m2tsPsiInterval) {
        this.m2tsPsiInterval = m2tsPsiInterval;
    }

    public Boolean getM2tsRandomAccess() {
        return m2tsRandomAccess;
    }

    public void setM2tsRandomAccess(Boolean m2tsRandomAccess) {
        this.m2tsRandomAccess = m2tsRandomAccess;
    }

    public String getM2tsTimestampMode() {
        return m2tsTimestampMode;
    }

    public void setM2tsTimestampMode(String m2tsTimestampMode) {
        this.m2tsTimestampMode = m2tsTimestampMode;
    }

    public Integer getM2tsScte35Pid() {
        return m2tsScte35Pid;
    }

    public void setM2tsScte35Pid(Integer m2tsScte35Pid) {
        this.m2tsScte35Pid = m2tsScte35Pid;
    }
}
