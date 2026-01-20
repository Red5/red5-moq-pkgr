package org.red5.io.moq.msf.timeline;

/**
 * MSF Media Timeline record per draft-ietf-moq-msf section 7.1.
 * Each record maps media presentation time to MOQT location and wallclock time.
 */
public class MsfMediaTimelineRecord {
    private final long mediaPtsMillis;
    private final long groupId;
    private final long objectId;
    private final long wallclockMillis;

    public MsfMediaTimelineRecord(long mediaPtsMillis, long groupId, long objectId, long wallclockMillis) {
        this.mediaPtsMillis = mediaPtsMillis;
        this.groupId = groupId;
        this.objectId = objectId;
        this.wallclockMillis = wallclockMillis;
    }

    /**
     * Media presentation timestamp in milliseconds.
     */
    public long getMediaPtsMillis() {
        return mediaPtsMillis;
    }

    /**
     * MOQT Group ID.
     */
    public long getGroupId() {
        return groupId;
    }

    /**
     * MOQT Object ID.
     */
    public long getObjectId() {
        return objectId;
    }

    /**
     * Wallclock time in milliseconds since Unix epoch.
     * For VOD assets or unknown wallclock, this value should be 0.
     */
    public long getWallclockMillis() {
        return wallclockMillis;
    }
}
