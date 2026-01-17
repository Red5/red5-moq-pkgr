package org.red5.io.moq.carp.timeline;

import java.util.List;

/**
 * CARP SAP timeline entry per draft-law-moq-carp.
 */
public class CarpSapTimelineEntry {
    private List<Long> location;

    private List<Long> data;

    public CarpSapTimelineEntry() {
    }

    public CarpSapTimelineEntry(long groupId, long objectId, int sapType, long earliestPresentationTimeMs) {
        this.location = List.of(groupId, objectId);
        this.data = List.of((long) sapType, earliestPresentationTimeMs);
    }

    public List<Long> getLocation() {
        return location;
    }

    public void setLocation(List<Long> location) {
        this.location = location;
    }

    public List<Long> getData() {
        return data;
    }

    public void setData(List<Long> data) {
        this.data = data;
    }

    public long getGroupId() {
        return location != null && location.size() > 0 ? location.get(0) : 0;
    }

    public long getObjectId() {
        return location != null && location.size() > 1 ? location.get(1) : 0;
    }

    public int getSapType() {
        return data != null && data.size() > 0 ? data.get(0).intValue() : 0;
    }

    public long getEarliestPresentationTimeMs() {
        return data != null && data.size() > 1 ? data.get(1) : 0;
    }
}
