package org.red5.io.moq.warp.timeline;

/**
 * Single WARP timeline record.
 */
public class WarpTimelineRecord {
    private final long mediaPtsMillis;
    private final Long groupId;
    private final Long objectId;
    private final long wallclockMillis;
    private final String metadata;

    public WarpTimelineRecord(long mediaPtsMillis, Long groupId, Long objectId, long wallclockMillis, String metadata) {
        this.mediaPtsMillis = mediaPtsMillis;
        this.groupId = groupId;
        this.objectId = objectId;
        this.wallclockMillis = wallclockMillis;
        this.metadata = metadata;
    }

    public long getMediaPtsMillis() {
        return mediaPtsMillis;
    }

    public Long getGroupId() {
        return groupId;
    }

    public Long getObjectId() {
        return objectId;
    }

    public long getWallclockMillis() {
        return wallclockMillis;
    }

    public String getMetadata() {
        return metadata;
    }
}
