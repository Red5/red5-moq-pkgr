package org.red5.io.moq.catalog;

import java.util.List;

/**
 * Represents a track in the MoQ Catalog.
 *
 * A track object describes a single media track with its properties,
 * selection parameters, and relationships to other tracks.
 */
public class CatalogTrack {

    /** Track namespace (optional, inherits from catalog if not specified) */
    private String namespace;

    /** Track name (required) */
    private String name;

    /** Packaging type ("cmaf" or "loc") */
    private String packaging;

    /** Forwarding preference ("datagram", "track", "group", "object") */
    private String forwardingPreference;

    /** Human-readable label */
    private String label;

    /** Render group ID - tracks with same ID should be rendered together */
    private Integer renderGroup;

    /** Alternate group ID - tracks with same ID are alternates of each other */
    private Integer altGroup;

    /** Base64-encoded initialization data */
    private String initData;

    /** Track name of another track holding initialization data */
    private String initTrack;

    /** Selection parameters for track selection */
    private SelectionParameters selectionParams;

    /** Array of track names this track depends on */
    private List<String> depends;

    /** Temporal layer ID (for SVC) */
    private Integer temporalId;

    /** Spatial layer ID (for SVC) */
    private Integer spatialId;

    /** Track role such as video, audio, mediatimeline, eventtimeline. */
    private String role;

    /** Parent track name for clone-style tracks. */
    private String parentName;

    /** Track duration in milliseconds. */
    private Long trackDuration;

    /** Whether the track is live. */
    private Boolean isLive;

    /** Target latency in milliseconds for real-time playback. */
    private Long targetLatency;

    /** Format-specific type field, such as timeline. */
    private String type;

    /** Event timeline type. */
    private String eventType;

    /** Maximum SAP type for the first object in each group. */
    private Integer maxGrpSapStartingType;

    /** Maximum SAP type for object starts. */
    private Integer maxObjSapStartingType;

    /** MSFTS 6.2: source-packet size in octets (188 or 192). */
    private Integer m2tsPacketSize;

    /** MSFTS 6.3: usual number of source packets per media Object. */
    private Integer m2tsPacketsPerObject;

    /** MSFTS 6.4: MPEG-2 program number carried by this track. */
    private Integer m2tsProgramNumber;

    /** MSFTS 6.5: PID carrying the Program Map Table for the program. */
    private Integer m2tsPmtPid;

    /** MSFTS 6.6: PID carrying the Program Clock Reference for the program. */
    private Integer m2tsPcrPid;

    /** MSFTS 6.7: maximum PAT/PMT repetition interval in milliseconds. */
    private Integer m2tsPsiInterval;

    /** MSFTS 6.8: when true, the first Object in every Group begins at a random access point. */
    private Boolean m2tsRandomAccess;

    /** MSFTS 6.9: interpretation of the 4-octet timestamp prefix ("arrival-time" or "opaque"). */
    private String m2tsTimestampMode;

    /** MSFTS 6.10: PID carrying SCTE-35 splice_info_section() messages. */
    private Integer m2tsScte35Pid;

    public CatalogTrack() {
    }

    public CatalogTrack(String name, String packaging) {
        this.name = name;
        this.packaging = packaging;
    }

    /**
     * Validates the track according to spec requirements.
     *
     * @throws IllegalStateException if track is invalid
     */
    public void validate() {
        validate(null);
    }

    public void validate(CommonTrackFields commonTrackFields) {
        if (name == null || name.isEmpty()) {
            throw new IllegalStateException("Track name is required");
        }

        String effectivePackaging = packaging != null ? packaging
            : commonTrackFields != null ? commonTrackFields.getPackaging() : null;
        if (effectivePackaging == null || effectivePackaging.isEmpty()) {
            throw new IllegalStateException("Track packaging is required");
        }

        if (!"cmaf".equals(effectivePackaging)
            && !"loc".equals(effectivePackaging)
            && !"timeline".equals(effectivePackaging)
            && !"mediatimeline".equals(effectivePackaging)
            && !"eventtimeline".equals(effectivePackaging)
            && !"m2ts".equals(effectivePackaging)) {
            throw new IllegalStateException("Invalid packaging type: " + effectivePackaging);
        }
    }

    // Getters and setters

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

    public String getForwardingPreference() {
        return forwardingPreference;
    }

    public void setForwardingPreference(String forwardingPreference) {
        this.forwardingPreference = forwardingPreference;
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

    public String getInitTrack() {
        return initTrack;
    }

    public void setInitTrack(String initTrack) {
        this.initTrack = initTrack;
    }

    public SelectionParameters getSelectionParams() {
        return selectionParams;
    }

    public void setSelectionParams(SelectionParameters selectionParams) {
        this.selectionParams = selectionParams;
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
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

    @Override
    public String toString() {
        return "CatalogTrack{" +
                "namespace='" + namespace + '\'' +
                ", name='" + name + '\'' +
                ", packaging='" + packaging + '\'' +
                ", label='" + label + '\'' +
                ", renderGroup=" + renderGroup +
                ", altGroup=" + altGroup +
                '}';
    }
}
