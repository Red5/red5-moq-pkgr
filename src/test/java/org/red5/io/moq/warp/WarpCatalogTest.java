package org.red5.io.moq.warp;

import org.junit.jupiter.api.Test;
import org.red5.io.moq.warp.catalog.WarpCatalog;
import org.red5.io.moq.warp.catalog.WarpCatalogSerializer;
import org.red5.io.moq.warp.catalog.WarpCatalogValidator;
import org.red5.io.moq.warp.catalog.WarpTrack;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WarpCatalogTest {

    @Test
    void testCatalogRoundTrip() throws Exception {
        WarpTrack videoTrack = new WarpTrack();
        videoTrack.setName("video0");
        videoTrack.setPackaging("loc");
        videoTrack.setIsLive(true);
        videoTrack.setMimeType("video/h264");

        WarpCatalog catalog = new WarpCatalog();
        catalog.setVersion(1);
        catalog.setTracks(List.of(videoTrack));

        WarpCatalogSerializer serializer = new WarpCatalogSerializer();
        String json = serializer.toJson(catalog);
        WarpCatalog parsed = serializer.fromJson(json);

        assertEquals(1, parsed.getVersion());
        assertEquals(1, parsed.getTracks().size());
        assertDoesNotThrow(() -> WarpCatalogValidator.validateCatalog(parsed));
    }

    @Test
    void testTimelineTrackValidation() {
        WarpTrack timeline = new WarpTrack();
        timeline.setName("timeline0");
        timeline.setPackaging("timeline");
        timeline.setType("timeline");
        timeline.setDepends(List.of("video0"));
        timeline.setMimeType("text/csv");
        timeline.setIsLive(true);

        WarpCatalog catalog = new WarpCatalog();
        catalog.setVersion(1);
        catalog.setTracks(List.of(timeline));

        assertDoesNotThrow(() -> WarpCatalogValidator.validateCatalog(catalog));
    }

    @Test
    void testDeltaUpdateValidation() {
        WarpTrack newTrack = new WarpTrack();
        newTrack.setName("audio0");
        newTrack.setPackaging("loc");
        newTrack.setIsLive(true);

        WarpCatalog delta = new WarpCatalog();
        delta.setDeltaUpdate(true);
        delta.setAddTracks(List.of(newTrack));

        assertDoesNotThrow(() -> WarpCatalogValidator.validateCatalog(delta));
    }

    @Test
    void testRemoveTrackMustOnlyContainName() {
        WarpTrack remove = new WarpTrack();
        remove.setName("video0");
        remove.setPackaging("loc");

        WarpCatalog delta = new WarpCatalog();
        delta.setDeltaUpdate(true);
        delta.setRemoveTracks(List.of(remove));

        assertThrows(IllegalArgumentException.class, () -> WarpCatalogValidator.validateCatalog(delta));
    }

    @Test
    void testCloneTrackRequiresParentName() {
        WarpTrack clone = new WarpTrack();
        clone.setName("video1");
        clone.setPackaging("loc");
        clone.setIsLive(true);

        WarpCatalog delta = new WarpCatalog();
        delta.setDeltaUpdate(true);
        delta.setCloneTracks(List.of(clone));

        assertThrows(IllegalArgumentException.class, () -> WarpCatalogValidator.validateCatalog(delta));
    }

    @Test
    void testInvalidPackagingRejected() {
        WarpTrack track = new WarpTrack();
        track.setName("bad");
        track.setPackaging("cmaf");
        track.setIsLive(true);

        WarpCatalog catalog = new WarpCatalog();
        catalog.setVersion(1);
        catalog.setTracks(List.of(track));

        assertThrows(IllegalArgumentException.class, () -> WarpCatalogValidator.validateCatalog(catalog));
    }
}
