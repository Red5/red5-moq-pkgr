package org.red5.io.moq.carp;

import org.red5.io.moq.warp.catalog.WarpCatalog;
import org.red5.io.moq.warp.catalog.WarpCatalogValidator;
import org.red5.io.moq.warp.catalog.WarpTrack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * CARP catalog validation rules per draft-law-moq-carp.
 */
public class CarpCatalogValidator {
    public static final String SAP_EVENT_TYPE = "org.ietf.moq.carp.sap";
    private static final Set<String> CARP_PACKAGING = Set.of("loc", "timeline", "cmaf", "eventtimeline");

    public static void validateCatalog(WarpCatalog catalog) {
        WarpCatalogValidator.validateCatalog(catalog, CARP_PACKAGING);
        for (WarpTrack track : collectTracks(catalog)) {
            validateCarpTrack(track);
        }
    }

    private static void validateCarpTrack(WarpTrack track) {
        if (track == null) {
            return;
        }
        if (track.getMaxGrpSapStartingType() != null) {
            ensureSapRange(track.getMaxGrpSapStartingType(), "maxGrpSapStartingType");
        }
        if (track.getMaxObjSapStartingType() != null) {
            ensureSapRange(track.getMaxObjSapStartingType(), "maxObjSapStartingType");
        }
        if ("eventtimeline".equals(track.getPackaging())) {
            if (!SAP_EVENT_TYPE.equals(track.getEventType())) {
                throw new IllegalArgumentException("eventtimeline tracks must set eventType=" + SAP_EVENT_TYPE);
            }
        }
    }

    private static void ensureSapRange(int value, String fieldName) {
        if (value < 0 || value > 3) {
            throw new IllegalArgumentException(fieldName + " must be between 0 and 3");
        }
    }

    private static List<WarpTrack> collectTracks(WarpCatalog catalog) {
        List<WarpTrack> tracks = new ArrayList<>();
        if (catalog.getTracks() != null) {
            tracks.addAll(catalog.getTracks());
        }
        if (catalog.getAddTracks() != null) {
            tracks.addAll(catalog.getAddTracks());
        }
        if (catalog.getCloneTracks() != null) {
            tracks.addAll(catalog.getCloneTracks());
        }
        return tracks;
    }
}
