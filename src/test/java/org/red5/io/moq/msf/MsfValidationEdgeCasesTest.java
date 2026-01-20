package org.red5.io.moq.msf;

import org.junit.jupiter.api.Test;
import org.red5.io.moq.msf.catalog.MsfCatalog;
import org.red5.io.moq.msf.catalog.MsfCatalogValidator;
import org.red5.io.moq.msf.catalog.MsfTrack;
import org.red5.io.moq.warp.catalog.WarpCatalog;
import org.red5.io.moq.warp.catalog.WarpTrack;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Edge case and comprehensive validation tests for MsfCatalogValidator.
 */
class MsfValidationEdgeCasesTest {

    // Null and empty input tests

    @Test
    void testValidateNullCatalog() {
        assertThrows(IllegalArgumentException.class,
            () -> MsfCatalogValidator.validateCatalog(null));
    }

    @Test
    void testValidateCatalogWithNullTracks() {
        WarpCatalog catalog = new WarpCatalog();
        catalog.setVersion(1);
        catalog.setTracks(null);

        assertThrows(IllegalArgumentException.class,
            () -> MsfCatalogValidator.validateCatalog(catalog));
    }

    @Test
    void testValidateCatalogWithEmptyTracks() {
        WarpCatalog catalog = new WarpCatalog();
        catalog.setVersion(1);
        catalog.setTracks(Collections.emptyList());

        // Empty tracks without isComplete should fail
        assertThrows(IllegalArgumentException.class,
            () -> MsfCatalogValidator.validateCatalog(catalog));
    }

    // Version validation

    @Test
    void testValidateMissingVersion() {
        WarpTrack track = new WarpTrack();
        track.setName("video");
        track.setPackaging("loc");
        track.setIsLive(true);

        WarpCatalog catalog = new WarpCatalog();
        catalog.setTracks(List.of(track));
        // Missing version

        assertThrows(IllegalArgumentException.class,
            () -> MsfCatalogValidator.validateCatalog(catalog));
    }

    // Track name validation

    @Test
    void testValidateTrackWithNullName() {
        WarpTrack track = new WarpTrack();
        track.setPackaging("loc");
        track.setIsLive(true);
        // Missing name

        WarpCatalog catalog = new WarpCatalog();
        catalog.setVersion(1);
        catalog.setTracks(List.of(track));

        assertThrows(IllegalArgumentException.class,
            () -> MsfCatalogValidator.validateCatalog(catalog));
    }

    @Test
    void testValidateTrackWithEmptyName() {
        WarpTrack track = new WarpTrack();
        track.setName("");
        track.setPackaging("loc");
        track.setIsLive(true);

        WarpCatalog catalog = new WarpCatalog();
        catalog.setVersion(1);
        catalog.setTracks(List.of(track));

        assertThrows(IllegalArgumentException.class,
            () -> MsfCatalogValidator.validateCatalog(catalog));
    }

    @Test
    void testValidateTrackWithWhitespaceName() {
        WarpTrack track = new WarpTrack();
        track.setName("   ");
        track.setPackaging("loc");
        track.setIsLive(true);

        WarpCatalog catalog = new WarpCatalog();
        catalog.setVersion(1);
        catalog.setTracks(List.of(track));

        assertThrows(IllegalArgumentException.class,
            () -> MsfCatalogValidator.validateCatalog(catalog));
    }

    // Packaging validation

    @Test
    void testValidateTrackWithNullPackaging() {
        WarpTrack track = new WarpTrack();
        track.setName("video");
        track.setIsLive(true);
        // Missing packaging

        WarpCatalog catalog = new WarpCatalog();
        catalog.setVersion(1);
        catalog.setTracks(List.of(track));

        assertThrows(IllegalArgumentException.class,
            () -> MsfCatalogValidator.validateCatalog(catalog));
    }

    @Test
    void testValidateTrackWithInvalidPackaging() {
        WarpTrack track = new WarpTrack();
        track.setName("video");
        track.setPackaging("invalid-packaging");
        track.setIsLive(true);

        WarpCatalog catalog = new WarpCatalog();
        catalog.setVersion(1);
        catalog.setTracks(List.of(track));

        assertThrows(IllegalArgumentException.class,
            () -> MsfCatalogValidator.validateCatalog(catalog));
    }

    @Test
    void testValidateWarpTimelinePackagingRejected() {
        // WARP "timeline" packaging is not valid for MSF
        WarpTrack track = new WarpTrack();
        track.setName("timeline");
        track.setPackaging("timeline");
        track.setIsLive(true);

        WarpCatalog catalog = new WarpCatalog();
        catalog.setVersion(1);
        catalog.setTracks(List.of(track));

        assertThrows(IllegalArgumentException.class,
            () -> MsfCatalogValidator.validateCatalog(catalog));
    }

    // isLive validation

    @Test
    void testValidateTrackWithNullIsLive() {
        WarpTrack track = new WarpTrack();
        track.setName("video");
        track.setPackaging("loc");
        // Missing isLive

        WarpCatalog catalog = new WarpCatalog();
        catalog.setVersion(1);
        catalog.setTracks(List.of(track));

        assertThrows(IllegalArgumentException.class,
            () -> MsfCatalogValidator.validateCatalog(catalog));
    }

    // targetLatency and trackDuration cross-validation

    @Test
    void testValidateLiveTrackWithTargetLatency() {
        MsfCatalog catalog = MsfCatalog.builder()
            .addTrack(MsfTrack.video("video").live().targetLatency(2000))
            .build();

        assertDoesNotThrow(() -> MsfCatalogValidator.validateCatalog(catalog));
    }

    @Test
    void testValidateVodTrackWithTargetLatencyFails() {
        WarpTrack track = new WarpTrack();
        track.setName("video");
        track.setPackaging("loc");
        track.setIsLive(false);
        track.setTargetLatency(2000L); // Invalid for VOD

        WarpCatalog catalog = new WarpCatalog();
        catalog.setVersion(1);
        catalog.setTracks(List.of(track));

        assertThrows(IllegalArgumentException.class,
            () -> MsfCatalogValidator.validateCatalog(catalog));
    }

    @Test
    void testValidateVodTrackWithTrackDuration() {
        MsfCatalog catalog = MsfCatalog.builder()
            .addTrack(MsfTrack.video("video").vod().trackDuration(3600000L))
            .build();

        assertDoesNotThrow(() -> MsfCatalogValidator.validateCatalog(catalog));
    }

    @Test
    void testValidateLiveTrackWithTrackDurationFails() {
        WarpTrack track = new WarpTrack();
        track.setName("video");
        track.setPackaging("loc");
        track.setIsLive(true);
        track.setTrackDuration(3600000L); // Invalid for live

        WarpCatalog catalog = new WarpCatalog();
        catalog.setVersion(1);
        catalog.setTracks(List.of(track));

        assertThrows(IllegalArgumentException.class,
            () -> MsfCatalogValidator.validateCatalog(catalog));
    }

    // Media timeline track validation

    @Test
    void testValidateMediaTimelineWithoutDepends() {
        WarpTrack track = new WarpTrack();
        track.setName("history");
        track.setPackaging("mediatimeline");
        track.setMimeType("application/json");
        track.setIsLive(true);
        // Missing depends

        WarpCatalog catalog = new WarpCatalog();
        catalog.setVersion(1);
        catalog.setTracks(List.of(track));

        assertThrows(IllegalArgumentException.class,
            () -> MsfCatalogValidator.validateCatalog(catalog));
    }

    @Test
    void testValidateMediaTimelineWithWrongMimeType() {
        WarpTrack track = new WarpTrack();
        track.setName("history");
        track.setPackaging("mediatimeline");
        track.setMimeType("text/csv"); // Wrong - should be application/json
        track.setDepends(List.of("video"));
        track.setIsLive(true);

        WarpCatalog catalog = new WarpCatalog();
        catalog.setVersion(1);
        catalog.setTracks(List.of(track));

        assertThrows(IllegalArgumentException.class,
            () -> MsfCatalogValidator.validateCatalog(catalog));
    }

    @Test
    void testValidateMediaTimelineWithNullMimeType() {
        WarpTrack track = new WarpTrack();
        track.setName("history");
        track.setPackaging("mediatimeline");
        track.setDepends(List.of("video"));
        track.setIsLive(true);
        // Missing mimeType

        WarpCatalog catalog = new WarpCatalog();
        catalog.setVersion(1);
        catalog.setTracks(List.of(track));

        assertThrows(IllegalArgumentException.class,
            () -> MsfCatalogValidator.validateCatalog(catalog));
    }

    // Event timeline track validation

    @Test
    void testValidateEventTimelineWithoutEventType() {
        WarpTrack track = new WarpTrack();
        track.setName("events");
        track.setPackaging("eventtimeline");
        track.setMimeType("application/json");
        track.setDepends(List.of("video"));
        track.setIsLive(true);
        // Missing eventType

        WarpCatalog catalog = new WarpCatalog();
        catalog.setVersion(1);
        catalog.setTracks(List.of(track));

        assertThrows(IllegalArgumentException.class,
            () -> MsfCatalogValidator.validateCatalog(catalog));
    }

    @Test
    void testValidateEventTimelineWithEmptyEventType() {
        WarpTrack track = new WarpTrack();
        track.setName("events");
        track.setPackaging("eventtimeline");
        track.setMimeType("application/json");
        track.setDepends(List.of("video"));
        track.setEventType(""); // Empty
        track.setIsLive(true);

        WarpCatalog catalog = new WarpCatalog();
        catalog.setVersion(1);
        catalog.setTracks(List.of(track));

        assertThrows(IllegalArgumentException.class,
            () -> MsfCatalogValidator.validateCatalog(catalog));
    }

    // Render group latency validation

    @Test
    void testValidateRenderGroupWithMixedLatencies() {
        WarpTrack video = new WarpTrack();
        video.setName("video");
        video.setPackaging("loc");
        video.setIsLive(true);
        video.setTargetLatency(2000L);
        video.setRenderGroup(1);

        WarpTrack audio = new WarpTrack();
        audio.setName("audio");
        audio.setPackaging("loc");
        audio.setIsLive(true);
        audio.setTargetLatency(3000L); // Different latency
        audio.setRenderGroup(1);

        WarpCatalog catalog = new WarpCatalog();
        catalog.setVersion(1);
        catalog.setTracks(List.of(video, audio));

        assertThrows(IllegalArgumentException.class,
            () -> MsfCatalogValidator.validateCatalog(catalog));
    }

    @Test
    void testValidateRenderGroupWithOneLatencyNull() {
        // If one track in render group has latency and other doesn't, should be valid
        WarpTrack video = new WarpTrack();
        video.setName("video");
        video.setPackaging("loc");
        video.setIsLive(true);
        video.setTargetLatency(2000L);
        video.setRenderGroup(1);

        WarpTrack audio = new WarpTrack();
        audio.setName("audio");
        audio.setPackaging("loc");
        audio.setIsLive(true);
        // No targetLatency
        audio.setRenderGroup(1);

        WarpCatalog catalog = new WarpCatalog();
        catalog.setVersion(1);
        catalog.setTracks(List.of(video, audio));

        // Should pass - only validates when both have latency
        assertDoesNotThrow(() -> MsfCatalogValidator.validateCatalog(catalog));
    }

    @Test
    void testValidateDifferentRenderGroupsAllowed() {
        WarpTrack video = new WarpTrack();
        video.setName("video");
        video.setPackaging("loc");
        video.setIsLive(true);
        video.setTargetLatency(2000L);
        video.setRenderGroup(1);

        WarpTrack audio = new WarpTrack();
        audio.setName("audio");
        audio.setPackaging("loc");
        audio.setIsLive(true);
        audio.setTargetLatency(3000L); // Different latency is OK in different group
        audio.setRenderGroup(2);

        WarpCatalog catalog = new WarpCatalog();
        catalog.setVersion(1);
        catalog.setTracks(List.of(video, audio));

        assertDoesNotThrow(() -> MsfCatalogValidator.validateCatalog(catalog));
    }

    // Alt group latency validation

    @Test
    void testValidateAltGroupWithMixedLatencies() {
        WarpTrack hd = new WarpTrack();
        hd.setName("hd");
        hd.setPackaging("loc");
        hd.setIsLive(true);
        hd.setTargetLatency(1500L);
        hd.setAltGroup(1);

        WarpTrack sd = new WarpTrack();
        sd.setName("sd");
        sd.setPackaging("loc");
        sd.setIsLive(true);
        sd.setTargetLatency(2500L); // Different latency
        sd.setAltGroup(1);

        WarpCatalog catalog = new WarpCatalog();
        catalog.setVersion(1);
        catalog.setTracks(List.of(hd, sd));

        assertThrows(IllegalArgumentException.class,
            () -> MsfCatalogValidator.validateCatalog(catalog));
    }

    // isComplete validation

    @Test
    void testValidateIsCompleteTrueWithEmptyTracks() {
        WarpCatalog catalog = new WarpCatalog();
        catalog.setVersion(1);
        catalog.setIsComplete(true);
        catalog.setTracks(Collections.emptyList());

        // This is valid for broadcast termination
        assertDoesNotThrow(() -> MsfCatalogValidator.validateCatalog(catalog));
    }

    @Test
    void testValidateIsCompleteTrueWithNullTracks() {
        WarpCatalog catalog = new WarpCatalog();
        catalog.setVersion(1);
        catalog.setIsComplete(true);
        catalog.setTracks(null);

        // This is valid for broadcast termination
        assertDoesNotThrow(() -> MsfCatalogValidator.validateCatalog(catalog));
    }

    @Test
    void testValidateTerminationCatalogRequiresVersion() {
        WarpCatalog catalog = new WarpCatalog();
        catalog.setIsComplete(true);
        catalog.setTracks(Collections.emptyList());
        // Missing version

        assertThrows(IllegalArgumentException.class,
            () -> MsfCatalogValidator.validateCatalog(catalog));
    }

    // Delta update validation

    @Test
    void testValidateDeltaUpdateWithAddTracks() {
        WarpTrack newTrack = new WarpTrack();
        newTrack.setName("audio");
        newTrack.setPackaging("loc");
        newTrack.setIsLive(true);

        WarpCatalog delta = new WarpCatalog();
        delta.setDeltaUpdate(true);
        delta.setAddTracks(List.of(newTrack));

        assertDoesNotThrow(() -> MsfCatalogValidator.validateCatalog(delta));
    }

    @Test
    void testValidateDeltaUpdateWithRemoveTracks() {
        WarpTrack removeTrack = new WarpTrack();
        removeTrack.setName("video");
        // Remove track should only have name

        WarpCatalog delta = new WarpCatalog();
        delta.setDeltaUpdate(true);
        delta.setRemoveTracks(List.of(removeTrack));

        assertDoesNotThrow(() -> MsfCatalogValidator.validateCatalog(delta));
    }

    @Test
    void testValidateDeltaUpdateWithVersion() {
        WarpTrack newTrack = new WarpTrack();
        newTrack.setName("audio");
        newTrack.setPackaging("loc");
        newTrack.setIsLive(true);

        WarpCatalog delta = new WarpCatalog();
        delta.setDeltaUpdate(true);
        delta.setVersion(1); // Invalid for delta
        delta.setAddTracks(List.of(newTrack));

        assertThrows(IllegalArgumentException.class,
            () -> MsfCatalogValidator.validateCatalog(delta));
    }

    @Test
    void testValidateDeltaUpdateWithTracks() {
        WarpTrack track = new WarpTrack();
        track.setName("video");
        track.setPackaging("loc");
        track.setIsLive(true);

        WarpCatalog delta = new WarpCatalog();
        delta.setDeltaUpdate(true);
        delta.setTracks(List.of(track)); // Invalid for delta

        assertThrows(IllegalArgumentException.class,
            () -> MsfCatalogValidator.validateCatalog(delta));
    }

    @Test
    void testValidateDeltaUpdateEmpty() {
        WarpCatalog delta = new WarpCatalog();
        delta.setDeltaUpdate(true);
        // No addTracks, removeTracks, or cloneTracks

        assertThrows(IllegalArgumentException.class,
            () -> MsfCatalogValidator.validateCatalog(delta));
    }

    // Duplicate track name validation

    @Test
    void testValidateDuplicateTrackNames() {
        WarpTrack track1 = new WarpTrack();
        track1.setName("video");
        track1.setPackaging("loc");
        track1.setIsLive(true);

        WarpTrack track2 = new WarpTrack();
        track2.setName("video"); // Duplicate
        track2.setPackaging("loc");
        track2.setIsLive(true);

        WarpCatalog catalog = new WarpCatalog();
        catalog.setVersion(1);
        catalog.setTracks(List.of(track1, track2));

        assertThrows(IllegalArgumentException.class,
            () -> MsfCatalogValidator.validateCatalog(catalog));
    }

    @Test
    void testValidateSameNameDifferentNamespace() {
        WarpTrack track1 = new WarpTrack();
        track1.setName("video");
        track1.setNamespace("example.com/stream1");
        track1.setPackaging("loc");
        track1.setIsLive(true);

        WarpTrack track2 = new WarpTrack();
        track2.setName("video"); // Same name
        track2.setNamespace("example.com/stream2"); // Different namespace
        track2.setPackaging("loc");
        track2.setIsLive(true);

        WarpCatalog catalog = new WarpCatalog();
        catalog.setVersion(1);
        catalog.setTracks(List.of(track1, track2));

        // Same name in different namespaces should be valid
        assertDoesNotThrow(() -> MsfCatalogValidator.validateCatalog(catalog));
    }

    // Complex valid catalog

    @Test
    void testValidateComplexValidCatalog() {
        MsfCatalog catalog = MsfCatalog.builder()
            .addTrack(MsfTrack.video("hd")
                .live()
                .targetLatency(2000)
                .renderGroup(1)
                .altGroup(1)
                .codec("av01"))
            .addTrack(MsfTrack.video("sd")
                .live()
                .targetLatency(2000)
                .renderGroup(1)
                .altGroup(1)
                .codec("av01"))
            .addTrack(MsfTrack.audio("audio")
                .live()
                .targetLatency(2000)
                .renderGroup(1)
                .codec("opus"))
            .addTrack(MsfTrack.mediaTimeline("history")
                .live()
                .dependsOn(List.of("hd", "sd", "audio")))
            .addTrack(MsfTrack.eventTimeline("events", "com.example/events")
                .live()
                .dependsOn("hd"))
            .addTrack(MsfTrack.caption("cc-en")
                .live()
                .language("en"))
            .build();

        assertDoesNotThrow(() -> MsfCatalogValidator.validateCatalog(catalog));
        assertEquals(6, catalog.getTracks().size());
    }
}
