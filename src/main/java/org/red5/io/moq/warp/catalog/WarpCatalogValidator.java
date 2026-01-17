package org.red5.io.moq.warp.catalog;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * WARP catalog validation rules per draft-ietf-moq-warp.
 */
public class WarpCatalogValidator {
    private static final Set<String> WARP_PACKAGING = Set.of("loc", "timeline");

    public static void validateCatalog(WarpCatalog catalog) {
        validateCatalog(catalog, WARP_PACKAGING);
    }

    public static void validateCatalog(WarpCatalog catalog, Set<String> allowedPackaging) {
        if (catalog == null) {
            throw new IllegalArgumentException("Catalog cannot be null");
        }

        boolean isDelta = Boolean.TRUE.equals(catalog.getDeltaUpdate());

        if (isDelta) {
            if (catalog.getVersion() != null) {
                throw new IllegalArgumentException("Delta update must not include version");
            }
            if (catalog.getTracks() != null && !catalog.getTracks().isEmpty()) {
                throw new IllegalArgumentException("Delta update must not include tracks");
            }
            if (isEmpty(catalog.getAddTracks())
                    && isEmpty(catalog.getRemoveTracks())
                    && isEmpty(catalog.getCloneTracks())) {
                throw new IllegalArgumentException("Delta update must include addTracks, removeTracks, or cloneTracks");
            }

            validateDeltaTracks(catalog, allowedPackaging);
            return;
        }

        if (catalog.getVersion() == null) {
            throw new IllegalArgumentException("Catalog version is required");
        }
        if (isEmpty(catalog.getTracks())) {
            throw new IllegalArgumentException("Catalog tracks are required");
        }

        validateTrackSet(catalog.getTracks(), allowedPackaging, true);
        ensureUniqueNames(catalog.getTracks());
    }

    private static void validateDeltaTracks(WarpCatalog catalog, Set<String> allowedPackaging) {
        if (!isEmpty(catalog.getAddTracks())) {
            validateTrackSet(catalog.getAddTracks(), allowedPackaging, true);
        }
        if (!isEmpty(catalog.getRemoveTracks())) {
            for (WarpTrack track : catalog.getRemoveTracks()) {
                validateRemoveTrack(track);
            }
        }
        if (!isEmpty(catalog.getCloneTracks())) {
            for (WarpTrack track : catalog.getCloneTracks()) {
                validateTrack(track, allowedPackaging, true);
                if (isBlank(track.getParentName())) {
                    throw new IllegalArgumentException("Clone track must include parentName");
                }
            }
        }
    }

    private static void validateTrackSet(List<WarpTrack> tracks, Set<String> allowedPackaging, boolean requireIsLive) {
        for (WarpTrack track : tracks) {
            validateTrack(track, allowedPackaging, requireIsLive);
        }
    }

    private static void validateTrack(WarpTrack track, Set<String> allowedPackaging, boolean requireIsLive) {
        if (track == null) {
            throw new IllegalArgumentException("Track cannot be null");
        }
        if (isBlank(track.getName())) {
            throw new IllegalArgumentException("Track name is required");
        }
        if (isBlank(track.getPackaging())) {
            throw new IllegalArgumentException("Track packaging is required");
        }
        if (!allowedPackaging.contains(track.getPackaging())) {
            throw new IllegalArgumentException("Unsupported packaging: " + track.getPackaging());
        }
        if (requireIsLive && track.getIsLive() == null) {
            throw new IllegalArgumentException("Track isLive is required");
        }
        if (Boolean.FALSE.equals(track.getIsLive()) && track.getTrackDuration() != null) {
            throw new IllegalArgumentException("trackDuration must not be set when isLive is false");
        }
        if ("timeline".equals(track.getPackaging())) {
            if (!"timeline".equals(track.getType())) {
                throw new IllegalArgumentException("Timeline tracks must set type=timeline");
            }
            if (isEmpty(track.getDepends())) {
                throw new IllegalArgumentException("Timeline tracks must declare depends");
            }
            if (!"text/csv".equals(track.getMimeType())) {
                throw new IllegalArgumentException("Timeline tracks must use mimeType=text/csv");
            }
        }
    }

    private static void validateRemoveTrack(WarpTrack track) {
        if (track == null) {
            throw new IllegalArgumentException("Remove track cannot be null");
        }
        if (isBlank(track.getName())) {
            throw new IllegalArgumentException("Remove track must include name");
        }
        Map<String, Object> extraFields = new HashMap<>();
        if (track.getPackaging() != null) {
            extraFields.put("packaging", track.getPackaging());
        }
        if (track.getRole() != null) {
            extraFields.put("role", track.getRole());
        }
        if (track.getLabel() != null) {
            extraFields.put("label", track.getLabel());
        }
        if (track.getRenderGroup() != null) {
            extraFields.put("renderGroup", track.getRenderGroup());
        }
        if (track.getAltGroup() != null) {
            extraFields.put("altGroup", track.getAltGroup());
        }
        if (track.getInitData() != null) {
            extraFields.put("initData", track.getInitData());
        }
        if (track.getDepends() != null) {
            extraFields.put("depends", track.getDepends());
        }
        if (track.getTemporalId() != null) {
            extraFields.put("temporalId", track.getTemporalId());
        }
        if (track.getSpatialId() != null) {
            extraFields.put("spatialId", track.getSpatialId());
        }
        if (track.getCodec() != null) {
            extraFields.put("codec", track.getCodec());
        }
        if (track.getMimeType() != null) {
            extraFields.put("mimeType", track.getMimeType());
        }
        if (track.getFramerate() != null) {
            extraFields.put("framerate", track.getFramerate());
        }
        if (track.getTimescale() != null) {
            extraFields.put("timescale", track.getTimescale());
        }
        if (track.getBitrate() != null) {
            extraFields.put("bitrate", track.getBitrate());
        }
        if (track.getWidth() != null) {
            extraFields.put("width", track.getWidth());
        }
        if (track.getHeight() != null) {
            extraFields.put("height", track.getHeight());
        }
        if (track.getSamplerate() != null) {
            extraFields.put("samplerate", track.getSamplerate());
        }
        if (track.getChannelConfig() != null) {
            extraFields.put("channelConfig", track.getChannelConfig());
        }
        if (track.getDisplayWidth() != null) {
            extraFields.put("displayWidth", track.getDisplayWidth());
        }
        if (track.getDisplayHeight() != null) {
            extraFields.put("displayHeight", track.getDisplayHeight());
        }
        if (track.getLang() != null) {
            extraFields.put("lang", track.getLang());
        }
        if (track.getParentName() != null) {
            extraFields.put("parentName", track.getParentName());
        }
        if (track.getTrackDuration() != null) {
            extraFields.put("trackDuration", track.getTrackDuration());
        }
        if (track.getIsLive() != null) {
            extraFields.put("isLive", track.getIsLive());
        }
        if (track.getType() != null) {
            extraFields.put("type", track.getType());
        }
        if (track.getEventType() != null) {
            extraFields.put("eventType", track.getEventType());
        }
        if (track.getMaxGrpSapStartingType() != null) {
            extraFields.put("maxGrpSapStartingType", track.getMaxGrpSapStartingType());
        }
        if (track.getMaxObjSapStartingType() != null) {
            extraFields.put("maxObjSapStartingType", track.getMaxObjSapStartingType());
        }
        if (!extraFields.isEmpty()) {
            throw new IllegalArgumentException("Remove tracks must not include extra fields: " + extraFields.keySet());
        }
    }

    private static void ensureUniqueNames(List<WarpTrack> tracks) {
        Set<String> seen = new HashSet<>();
        for (WarpTrack track : tracks) {
            String key = (track.getNamespace() == null ? "" : track.getNamespace()) + "::" + track.getName();
            if (!seen.add(key)) {
                throw new IllegalArgumentException("Duplicate track name in namespace: " + key);
            }
        }
    }

    private static boolean isEmpty(List<?> list) {
        return list == null || list.isEmpty();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
