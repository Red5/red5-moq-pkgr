package org.red5.io.moq.carp;

import org.junit.jupiter.api.Test;
import org.red5.io.moq.carp.timeline.CarpSapTimeline;
import org.red5.io.moq.carp.timeline.CarpSapTimelineEntry;
import org.red5.io.moq.warp.catalog.WarpCatalog;
import org.red5.io.moq.warp.catalog.WarpTrack;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CarpCatalogTest {

    @Test
    void testCarpCatalogValidation() {
        WarpTrack cmafTrack = new WarpTrack();
        cmafTrack.setName("video0");
        cmafTrack.setPackaging("cmaf");
        cmafTrack.setIsLive(true);
        cmafTrack.setMaxGrpSapStartingType(2);
        cmafTrack.setMaxObjSapStartingType(3);

        WarpTrack sapTimeline = new WarpTrack();
        sapTimeline.setName("sap-timeline");
        sapTimeline.setPackaging("eventtimeline");
        sapTimeline.setEventType(CarpCatalogValidator.SAP_EVENT_TYPE);
        sapTimeline.setIsLive(true);

        WarpCatalog catalog = new WarpCatalog();
        catalog.setVersion(1);
        catalog.setTracks(List.of(cmafTrack, sapTimeline));

        assertDoesNotThrow(() -> CarpCatalogValidator.validateCatalog(catalog));
    }

    @Test
    void testSapTimelineRequiresEventType() {
        WarpTrack sapTimeline = new WarpTrack();
        sapTimeline.setName("sap-timeline");
        sapTimeline.setPackaging("eventtimeline");
        sapTimeline.setIsLive(true);

        WarpCatalog catalog = new WarpCatalog();
        catalog.setVersion(1);
        catalog.setTracks(List.of(sapTimeline));

        assertThrows(IllegalArgumentException.class, () -> CarpCatalogValidator.validateCatalog(catalog));
    }

    @Test
    void testSapRangeValidation() {
        WarpTrack cmafTrack = new WarpTrack();
        cmafTrack.setName("video0");
        cmafTrack.setPackaging("cmaf");
        cmafTrack.setIsLive(true);
        cmafTrack.setMaxGrpSapStartingType(5);

        WarpCatalog catalog = new WarpCatalog();
        catalog.setVersion(1);
        catalog.setTracks(List.of(cmafTrack));

        assertThrows(IllegalArgumentException.class, () -> CarpCatalogValidator.validateCatalog(catalog));
    }

    @Test
    void testSapTimelineJsonRoundTrip() throws Exception {
        CarpSapTimeline timeline = new CarpSapTimeline();
        List<CarpSapTimelineEntry> entries = List.of(
            new CarpSapTimelineEntry(0, 0, 2, 0),
                new CarpSapTimelineEntry(0, 60, 3, 2100)
        );

        String json = timeline.toJson(entries);
        List<CarpSapTimelineEntry> parsed = timeline.fromJson(json);

        assertEquals(2, parsed.size());
        assertEquals(2, parsed.get(0).getSapType());
        assertEquals(2100, parsed.get(1).getEarliestPresentationTimeMs());
    }
}
