package org.red5.io.moq.msf;

import org.junit.jupiter.api.Test;
import org.red5.io.moq.msf.catalog.MsfTrack;
import org.red5.io.moq.msf.catalog.PackagingType;
import org.red5.io.moq.msf.catalog.TrackRole;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for MsfTrack builder covering all setters and edge cases.
 */
class MsfTrackBuilderTest {

    // Factory method tests

    @Test
    void testVideoFactoryMethod() {
        MsfTrack track = MsfTrack.video("test-video").build();

        assertEquals("test-video", track.getName());
        assertEquals(PackagingType.LOC.getValue(), track.getPackaging());
        assertEquals(TrackRole.VIDEO.getValue(), track.getRole());
    }

    @Test
    void testAudioFactoryMethod() {
        MsfTrack track = MsfTrack.audio("test-audio").build();

        assertEquals("test-audio", track.getName());
        assertEquals(PackagingType.LOC.getValue(), track.getPackaging());
        assertEquals(TrackRole.AUDIO.getValue(), track.getRole());
    }

    @Test
    void testMediaTimelineFactoryMethod() {
        MsfTrack track = MsfTrack.mediaTimeline("history").build();

        assertEquals("history", track.getName());
        assertEquals(PackagingType.MEDIA_TIMELINE.getValue(), track.getPackaging());
        assertEquals(TrackRole.MEDIA_TIMELINE.getValue(), track.getRole());
        assertEquals("application/json", track.getMimeType());
    }

    @Test
    void testEventTimelineFactoryMethod() {
        MsfTrack track = MsfTrack.eventTimeline("events", "com.example/type").build();

        assertEquals("events", track.getName());
        assertEquals(PackagingType.EVENT_TIMELINE.getValue(), track.getPackaging());
        assertEquals(TrackRole.EVENT_TIMELINE.getValue(), track.getRole());
        assertEquals("application/json", track.getMimeType());
        assertEquals("com.example/type", track.getEventType());
    }

    @Test
    void testCaptionFactoryMethod() {
        MsfTrack track = MsfTrack.caption("cc").build();

        assertEquals("cc", track.getName());
        assertEquals(PackagingType.LOC.getValue(), track.getPackaging());
        assertEquals(TrackRole.CAPTION.getValue(), track.getRole());
    }

    @Test
    void testSubtitleFactoryMethod() {
        MsfTrack track = MsfTrack.subtitle("subs").build();

        assertEquals("subs", track.getName());
        assertEquals(PackagingType.LOC.getValue(), track.getPackaging());
        assertEquals(TrackRole.SUBTITLE.getValue(), track.getRole());
    }

    // All setter tests

    @Test
    void testNameSetter() {
        MsfTrack track = MsfTrack.builder().name("custom-name").build();
        assertEquals("custom-name", track.getName());
    }

    @Test
    void testNamespaceSetter() {
        MsfTrack track = MsfTrack.builder()
            .name("test")
            .namespace("example.com/stream/123")
            .build();
        assertEquals("example.com/stream/123", track.getNamespace());
    }

    @Test
    void testPackagingWithEnum() {
        MsfTrack track = MsfTrack.builder()
            .name("test")
            .packaging(PackagingType.LOC)
            .build();
        assertEquals("loc", track.getPackaging());
    }

    @Test
    void testPackagingWithString() {
        MsfTrack track = MsfTrack.builder()
            .name("test")
            .packaging("custom-packaging")
            .build();
        assertEquals("custom-packaging", track.getPackaging());
    }

    @Test
    void testRoleWithEnum() {
        MsfTrack track = MsfTrack.builder()
            .name("test")
            .role(TrackRole.SIGN_LANGUAGE)
            .build();
        assertEquals("signlanguage", track.getRole());
    }

    @Test
    void testRoleWithString() {
        MsfTrack track = MsfTrack.builder()
            .name("test")
            .role("custom-role")
            .build();
        assertEquals("custom-role", track.getRole());
    }

    @Test
    void testLabelSetter() {
        MsfTrack track = MsfTrack.builder()
            .name("test")
            .label("Main Camera - Stadium View")
            .build();
        assertEquals("Main Camera - Stadium View", track.getLabel());
    }

    @Test
    void testLiveSetter() {
        MsfTrack live = MsfTrack.builder().name("test").live(true).build();
        MsfTrack vod = MsfTrack.builder().name("test").live(false).build();

        assertTrue(live.getIsLive());
        assertFalse(vod.getIsLive());
    }

    @Test
    void testLiveConvenienceMethod() {
        MsfTrack track = MsfTrack.builder().name("test").live().build();
        assertTrue(track.getIsLive());
    }

    @Test
    void testVodConvenienceMethod() {
        MsfTrack track = MsfTrack.builder().name("test").vod().build();
        assertFalse(track.getIsLive());
    }

    @Test
    void testTargetLatencySetter() {
        MsfTrack track = MsfTrack.builder()
            .name("test")
            .targetLatency(1500)
            .build();
        assertEquals(1500L, track.getTargetLatency());
    }

    @Test
    void testTrackDurationSetter() {
        MsfTrack track = MsfTrack.builder()
            .name("test")
            .trackDuration(3600000L)
            .build();
        assertEquals(3600000L, track.getTrackDuration());
    }

    @Test
    void testRenderGroupSetter() {
        MsfTrack track = MsfTrack.builder()
            .name("test")
            .renderGroup(5)
            .build();
        assertEquals(5, track.getRenderGroup());
    }

    @Test
    void testAltGroupSetter() {
        MsfTrack track = MsfTrack.builder()
            .name("test")
            .altGroup(3)
            .build();
        assertEquals(3, track.getAltGroup());
    }

    @Test
    void testCodecSetter() {
        MsfTrack track = MsfTrack.builder()
            .name("test")
            .codec("av01.0.08M.10.0.110.09")
            .build();
        assertEquals("av01.0.08M.10.0.110.09", track.getCodec());
    }

    @Test
    void testMimeTypeSetter() {
        MsfTrack track = MsfTrack.builder()
            .name("test")
            .mimeType("video/mp4")
            .build();
        assertEquals("video/mp4", track.getMimeType());
    }

    @Test
    void testEventTypeSetter() {
        MsfTrack track = MsfTrack.builder()
            .name("test")
            .eventType("com.sports/live-scores")
            .build();
        assertEquals("com.sports/live-scores", track.getEventType());
    }

    @Test
    void testInitDataSetter() {
        MsfTrack track = MsfTrack.builder()
            .name("test")
            .initData("SGVsbG8gV29ybGQ=")
            .build();
        assertEquals("SGVsbG8gV29ybGQ=", track.getInitData());
    }

    @Test
    void testDependsOnSingleTrack() {
        MsfTrack track = MsfTrack.builder()
            .name("test")
            .dependsOn("base-track")
            .build();
        assertEquals(List.of("base-track"), track.getDepends());
    }

    @Test
    void testDependsOnMultipleTracks() {
        MsfTrack track = MsfTrack.builder()
            .name("test")
            .dependsOn("track1")
            .dependsOn("track2")
            .dependsOn("track3")
            .build();
        assertEquals(List.of("track1", "track2", "track3"), track.getDepends());
    }

    @Test
    void testDependsOnList() {
        MsfTrack track = MsfTrack.builder()
            .name("test")
            .dependsOn(List.of("a", "b", "c"))
            .build();
        assertEquals(List.of("a", "b", "c"), track.getDepends());
    }

    @Test
    void testTemporalIdSetter() {
        MsfTrack track = MsfTrack.builder()
            .name("test")
            .temporalId(2)
            .build();
        assertEquals(2, track.getTemporalId());
    }

    @Test
    void testSpatialIdSetter() {
        MsfTrack track = MsfTrack.builder()
            .name("test")
            .spatialId(1)
            .build();
        assertEquals(1, track.getSpatialId());
    }

    @Test
    void testFramerateSetter() {
        MsfTrack track = MsfTrack.builder()
            .name("test")
            .framerate(60)
            .build();
        assertEquals(60, track.getFramerate());
    }

    @Test
    void testTimescaleSetter() {
        MsfTrack track = MsfTrack.builder()
            .name("test")
            .timescale(90000)
            .build();
        assertEquals(90000, track.getTimescale());
    }

    @Test
    void testBitrateSetter() {
        MsfTrack track = MsfTrack.builder()
            .name("test")
            .bitrate(5000000)
            .build();
        assertEquals(5000000, track.getBitrate());
    }

    @Test
    void testResolutionSetter() {
        MsfTrack track = MsfTrack.builder()
            .name("test")
            .resolution(3840, 2160)
            .build();
        assertEquals(3840, track.getWidth());
        assertEquals(2160, track.getHeight());
    }

    @Test
    void testDisplayResolutionSetter() {
        MsfTrack track = MsfTrack.builder()
            .name("test")
            .displayResolution(1920, 1080)
            .build();
        assertEquals(1920, track.getDisplayWidth());
        assertEquals(1080, track.getDisplayHeight());
    }

    @Test
    void testSampleRateSetter() {
        MsfTrack track = MsfTrack.builder()
            .name("test")
            .sampleRate(44100)
            .build();
        assertEquals(44100, track.getSamplerate());
    }

    @Test
    void testChannelConfigSetter() {
        MsfTrack track = MsfTrack.builder()
            .name("test")
            .channelConfig("5.1")
            .build();
        assertEquals("5.1", track.getChannelConfig());
    }

    @Test
    void testLanguageSetter() {
        MsfTrack track = MsfTrack.builder()
            .name("test")
            .language("de-DE")
            .build();
        assertEquals("de-DE", track.getLang());
    }

    // Complex builder chains

    @Test
    void testFullVideoTrackBuilder() {
        MsfTrack track = MsfTrack.video("4k-hdr")
            .namespace("broadcast.example.com/event/123")
            .live()
            .targetLatency(500)
            .renderGroup(1)
            .altGroup(1)
            .codec("av01.0.12M.10.0.110.09")
            .resolution(3840, 2160)
            .displayResolution(3840, 2160)
            .framerate(60)
            .timescale(90000)
            .bitrate(25000000)
            .label("4K HDR Main Feed")
            .language("en")
            .temporalId(0)
            .spatialId(2)
            .initData("QVZDQw==")
            .build();

        assertEquals("4k-hdr", track.getName());
        assertEquals("broadcast.example.com/event/123", track.getNamespace());
        assertTrue(track.getIsLive());
        assertEquals(500L, track.getTargetLatency());
        assertEquals(1, track.getRenderGroup());
        assertEquals(1, track.getAltGroup());
        assertEquals("av01.0.12M.10.0.110.09", track.getCodec());
        assertEquals(3840, track.getWidth());
        assertEquals(2160, track.getHeight());
        assertEquals(60, track.getFramerate());
        assertEquals(25000000, track.getBitrate());
        assertEquals("4K HDR Main Feed", track.getLabel());
        assertEquals("en", track.getLang());
        assertEquals(0, track.getTemporalId());
        assertEquals(2, track.getSpatialId());
    }

    @Test
    void testFullAudioTrackBuilder() {
        MsfTrack track = MsfTrack.audio("surround")
            .live()
            .targetLatency(500)
            .renderGroup(1)
            .codec("ec-3")
            .sampleRate(48000)
            .channelConfig("5.1")
            .bitrate(640000)
            .label("Dolby Digital Plus 5.1")
            .language("en")
            .build();

        assertEquals("surround", track.getName());
        assertEquals("audio", track.getRole());
        assertEquals("ec-3", track.getCodec());
        assertEquals(48000, track.getSamplerate());
        assertEquals("5.1", track.getChannelConfig());
        assertEquals(640000, track.getBitrate());
    }

    // Edge cases

    @Test
    void testEmptyDependencies() {
        MsfTrack track = MsfTrack.builder()
            .name("test")
            .build();
        assertNull(track.getDepends());
    }

    @Test
    void testZeroValues() {
        MsfTrack track = MsfTrack.builder()
            .name("test")
            .temporalId(0)
            .spatialId(0)
            .renderGroup(0)
            .altGroup(0)
            .build();

        assertEquals(0, track.getTemporalId());
        assertEquals(0, track.getSpatialId());
        assertEquals(0, track.getRenderGroup());
        assertEquals(0, track.getAltGroup());
    }

    @Test
    void testLargeValues() {
        MsfTrack track = MsfTrack.builder()
            .name("test")
            .targetLatency(Long.MAX_VALUE)
            .trackDuration(Long.MAX_VALUE)
            .bitrate(Integer.MAX_VALUE)
            .build();

        assertEquals(Long.MAX_VALUE, track.getTargetLatency());
        assertEquals(Long.MAX_VALUE, track.getTrackDuration());
        assertEquals(Integer.MAX_VALUE, track.getBitrate());
    }

    @Test
    void testUnicodeLabel() {
        MsfTrack track = MsfTrack.builder()
            .name("test")
            .label("日本語コメンタリー")
            .language("ja")
            .build();

        assertEquals("日本語コメンタリー", track.getLabel());
        assertEquals("ja", track.getLang());
    }

    @Test
    void testSpecialCharactersInNamespace() {
        MsfTrack track = MsfTrack.builder()
            .name("test")
            .namespace("example.com/stream/user@domain/video-1")
            .build();

        assertEquals("example.com/stream/user@domain/video-1", track.getNamespace());
    }
}
