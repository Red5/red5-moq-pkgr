package org.red5.io.moq.msf;

import org.junit.jupiter.api.Test;
import org.red5.io.moq.warp.catalog.WarpCatalog;
import org.red5.io.moq.warp.catalog.WarpCatalogSerializer;
import org.red5.io.moq.warp.catalog.WarpTrack;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MSF-specific catalog fields (isComplete, targetLatency).
 */
class MsfCatalogFieldsTest {

    @Test
    void testIsCompleteFieldRoundTrip() throws Exception {
        WarpCatalog catalog = new WarpCatalog();
        catalog.setVersion(1);
        catalog.setIsComplete(true);
        catalog.setTracks(Collections.emptyList());

        WarpCatalogSerializer serializer = new WarpCatalogSerializer();
        String json = serializer.toJson(catalog);
        assertTrue(json.contains("\"isComplete\":true") || json.contains("\"isComplete\": true"));

        WarpCatalog parsed = serializer.fromJson(json);
        assertTrue(parsed.getIsComplete());
    }

    @Test
    void testTargetLatencyFieldRoundTrip() throws Exception {
        WarpTrack videoTrack = new WarpTrack();
        videoTrack.setName("video");
        videoTrack.setPackaging("loc");
        videoTrack.setIsLive(true);
        videoTrack.setTargetLatency(2000L);
        videoTrack.setRole("video");
        videoTrack.setCodec("av01.0.08M.10.0.110.09");

        WarpCatalog catalog = new WarpCatalog();
        catalog.setVersion(1);
        catalog.setTracks(List.of(videoTrack));

        WarpCatalogSerializer serializer = new WarpCatalogSerializer();
        String json = serializer.toJson(catalog);
        assertTrue(json.contains("\"targetLatency\":2000") || json.contains("\"targetLatency\": 2000"));

        WarpCatalog parsed = serializer.fromJson(json);
        assertEquals(2000L, parsed.getTracks().get(0).getTargetLatency());
    }

    @Test
    void testLiveBroadcastTerminationCatalog() throws Exception {
        // Per MSF section 5.3.9: Terminating a live broadcast
        WarpCatalog catalog = new WarpCatalog();
        catalog.setVersion(1);
        catalog.setGeneratedAt(System.currentTimeMillis());
        catalog.setIsComplete(true);
        catalog.setTracks(Collections.emptyList());

        WarpCatalogSerializer serializer = new WarpCatalogSerializer();
        String json = serializer.toJson(catalog);
        WarpCatalog parsed = serializer.fromJson(json);

        assertTrue(parsed.getIsComplete());
        assertTrue(parsed.getTracks().isEmpty());
    }

    @Test
    void testTimeAlignedTracksWithTargetLatency() throws Exception {
        // Per MSF section 5.3.1: Time-aligned Audio/Video Tracks
        WarpTrack videoTrack = new WarpTrack();
        videoTrack.setName("1080p-video");
        videoTrack.setNamespace("conference.example.com/conference123/alice");
        videoTrack.setPackaging("loc");
        videoTrack.setIsLive(true);
        videoTrack.setTargetLatency(2000L);
        videoTrack.setRole("video");
        videoTrack.setRenderGroup(1);
        videoTrack.setCodec("av01.0.08M.10.0.110.09");
        videoTrack.setWidth(1920);
        videoTrack.setHeight(1080);
        videoTrack.setFramerate(30);
        videoTrack.setBitrate(1500000);

        WarpTrack audioTrack = new WarpTrack();
        audioTrack.setName("audio");
        audioTrack.setNamespace("conference.example.com/conference123/alice");
        audioTrack.setPackaging("loc");
        audioTrack.setIsLive(true);
        audioTrack.setTargetLatency(2000L);
        audioTrack.setRole("audio");
        audioTrack.setRenderGroup(1);
        audioTrack.setCodec("opus");
        audioTrack.setSamplerate(48000);
        audioTrack.setChannelConfig("2");
        audioTrack.setBitrate(32000);

        WarpCatalog catalog = new WarpCatalog();
        catalog.setVersion(1);
        catalog.setGeneratedAt(System.currentTimeMillis());
        catalog.setTracks(List.of(videoTrack, audioTrack));

        WarpCatalogSerializer serializer = new WarpCatalogSerializer();
        String json = serializer.toJson(catalog);
        WarpCatalog parsed = serializer.fromJson(json);

        assertEquals(2, parsed.getTracks().size());
        // Both tracks in same render group should have same target latency
        assertEquals(parsed.getTracks().get(0).getTargetLatency(),
                     parsed.getTracks().get(1).getTargetLatency());
    }

    @Test
    void testVodCatalogWithTrackDuration() throws Exception {
        // Per MSF section 5.3.7: Time-aligned VOD Audio/Video Tracks
        WarpTrack videoTrack = new WarpTrack();
        videoTrack.setName("video");
        videoTrack.setNamespace("movies.example.com/assets/movie1");
        videoTrack.setPackaging("loc");
        videoTrack.setIsLive(false);
        videoTrack.setTrackDuration(8072340L);
        videoTrack.setRenderGroup(1);
        videoTrack.setCodec("av01.0.08M.10.0.110.09");

        WarpCatalog catalog = new WarpCatalog();
        catalog.setVersion(1);
        catalog.setTracks(List.of(videoTrack));

        WarpCatalogSerializer serializer = new WarpCatalogSerializer();
        String json = serializer.toJson(catalog);
        WarpCatalog parsed = serializer.fromJson(json);

        assertFalse(parsed.getTracks().get(0).getIsLive());
        assertEquals(8072340L, parsed.getTracks().get(0).getTrackDuration());
        // VOD tracks should not have targetLatency
        assertNull(parsed.getTracks().get(0).getTargetLatency());
    }
}
