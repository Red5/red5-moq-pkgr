package org.red5.io.moq.warp.catalog;

import java.util.List;

/**
 * WARP catalog root object per draft-ietf-moq-warp and draft-ietf-moq-msf.
 */
public class WarpCatalog {
    private Integer version;

    private Boolean deltaUpdate;

    /** MSF 5.1.7: Indicates a previously live broadcast is complete with no new content. */
    private Boolean isComplete;

    private List<WarpTrack> addTracks;

    private List<WarpTrack> removeTracks;

    private List<WarpTrack> cloneTracks;

    private Long generatedAt;

    private List<WarpTrack> tracks;

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Boolean getDeltaUpdate() {
        return deltaUpdate;
    }

    public void setDeltaUpdate(Boolean deltaUpdate) {
        this.deltaUpdate = deltaUpdate;
    }

    public Boolean getIsComplete() {
        return isComplete;
    }

    public void setIsComplete(Boolean isComplete) {
        this.isComplete = isComplete;
    }

    public List<WarpTrack> getAddTracks() {
        return addTracks;
    }

    public void setAddTracks(List<WarpTrack> addTracks) {
        this.addTracks = addTracks;
    }

    public List<WarpTrack> getRemoveTracks() {
        return removeTracks;
    }

    public void setRemoveTracks(List<WarpTrack> removeTracks) {
        this.removeTracks = removeTracks;
    }

    public List<WarpTrack> getCloneTracks() {
        return cloneTracks;
    }

    public void setCloneTracks(List<WarpTrack> cloneTracks) {
        this.cloneTracks = cloneTracks;
    }

    public Long getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Long generatedAt) {
        this.generatedAt = generatedAt;
    }

    public List<WarpTrack> getTracks() {
        return tracks;
    }

    public void setTracks(List<WarpTrack> tracks) {
        this.tracks = tracks;
    }
}
