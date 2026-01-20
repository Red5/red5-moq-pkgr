package org.red5.io.moq.msf.timeline;

import com.google.gson.JsonElement;

/**
 * MSF Event Timeline entry per draft-ietf-moq-msf section 8.
 * Each entry contains an index reference and associated data.
 * <p>
 * Index reference types:
 * <ul>
 *   <li>'t' - wallclock time (milliseconds since Unix epoch)</li>
 *   <li>'l' - MOQT location [groupId, objectId]</li>
 *   <li>'m' - media presentation timestamp (milliseconds)</li>
 * </ul>
 */
public class MsfEventTimelineEntry {

    /** Index type constants */
    public static final String INDEX_WALLCLOCK = "t";
    public static final String INDEX_LOCATION = "l";
    public static final String INDEX_MEDIA_PTS = "m";

    private final String indexType;
    private final Long wallclockMillis;
    private final Long groupId;
    private final Long objectId;
    private final Long mediaPtsMillis;
    private final JsonElement data;

    private MsfEventTimelineEntry(String indexType, Long wallclockMillis, Long groupId,
                                   Long objectId, Long mediaPtsMillis, JsonElement data) {
        this.indexType = indexType;
        this.wallclockMillis = wallclockMillis;
        this.groupId = groupId;
        this.objectId = objectId;
        this.mediaPtsMillis = mediaPtsMillis;
        this.data = data;
    }

    /**
     * Create an entry indexed by wallclock time.
     */
    public static MsfEventTimelineEntry withWallclock(long wallclockMillis, JsonElement data) {
        return new MsfEventTimelineEntry(INDEX_WALLCLOCK, wallclockMillis, null, null, null, data);
    }

    /**
     * Create an entry indexed by MOQT location.
     */
    public static MsfEventTimelineEntry withLocation(long groupId, long objectId, JsonElement data) {
        return new MsfEventTimelineEntry(INDEX_LOCATION, null, groupId, objectId, null, data);
    }

    /**
     * Create an entry indexed by media presentation timestamp.
     */
    public static MsfEventTimelineEntry withMediaPts(long mediaPtsMillis, JsonElement data) {
        return new MsfEventTimelineEntry(INDEX_MEDIA_PTS, null, null, null, mediaPtsMillis, data);
    }

    /**
     * Get the index type ('t', 'l', or 'm').
     */
    public String getIndexType() {
        return indexType;
    }

    /**
     * Get wallclock time (only valid if indexType is 't').
     */
    public Long getWallclockMillis() {
        return wallclockMillis;
    }

    /**
     * Get MOQT Group ID (only valid if indexType is 'l').
     */
    public Long getGroupId() {
        return groupId;
    }

    /**
     * Get MOQT Object ID (only valid if indexType is 'l').
     */
    public Long getObjectId() {
        return objectId;
    }

    /**
     * Get media presentation timestamp (only valid if indexType is 'm').
     */
    public Long getMediaPtsMillis() {
        return mediaPtsMillis;
    }

    /**
     * Get the event data. Structure is defined by the eventType declared in the catalog.
     */
    public JsonElement getData() {
        return data;
    }

    /**
     * Check if this entry is indexed by wallclock time.
     */
    public boolean isWallclockIndexed() {
        return INDEX_WALLCLOCK.equals(indexType);
    }

    /**
     * Check if this entry is indexed by MOQT location.
     */
    public boolean isLocationIndexed() {
        return INDEX_LOCATION.equals(indexType);
    }

    /**
     * Check if this entry is indexed by media PTS.
     */
    public boolean isMediaPtsIndexed() {
        return INDEX_MEDIA_PTS.equals(indexType);
    }
}
