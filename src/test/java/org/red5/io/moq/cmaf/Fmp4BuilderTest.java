package org.red5.io.moq.cmaf;

import org.junit.jupiter.api.Test;
import org.red5.io.moq.cmaf.deserialize.CmafDeserializer;
import org.red5.io.moq.cmaf.model.CmafFragment;
import org.red5.io.moq.cmaf.model.MoofBox;
import org.red5.io.moq.cmaf.model.SampleFlags;
import org.red5.io.moq.cmaf.util.Fmp4FragmentBuilder;
import org.red5.io.moq.cmaf.util.Fmp4InitSegmentBuilder;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Fmp4BuilderTest {

    @Test
    void testInitSegmentBuildsFtypAndMoov() throws Exception {
        byte[] avcC = boxBytes("avcC", new byte[] { 1, 2, 3, 4 });
        byte[] esds = boxBytes("esds", new byte[] { 5, 6, 7, 8 });

        byte[] initSegment = new Fmp4InitSegmentBuilder()
            .addVideoTrack(new Fmp4InitSegmentBuilder.VideoTrackConfig(
                1, 90000, "avc1", avcC, 640, 360))
            .addAudioTrack(new Fmp4InitSegmentBuilder.AudioTrackConfig(
                2, 48000, "mp4a", esds, 2, 48000, 16))
            .build();

        assertNotNull(initSegment);
        assertTrue(findBox(initSegment, "ftyp"));
        assertTrue(findBox(initSegment, "moov"));
        assertTrue(findBox(initSegment, "avcC"));
        assertTrue(findBox(initSegment, "esds"));
    }

    @Test
    void testFragmentBuildsMoofMdatAndOffsets() throws Exception {
        byte[] mediaData = new byte[] { 9, 8, 7, 6, 5, 4 };

        Fmp4FragmentBuilder.FragmentConfig config = new Fmp4FragmentBuilder.FragmentConfig()
            .setSequenceNumber(1)
            .setTrackId(1)
            .setBaseDecodeTime(0)
            .setMediaData(mediaData)
            .setMediaType(CmafFragment.MediaType.VIDEO)
            .setSamples(List.of(
                new Fmp4FragmentBuilder.SampleData(3000, mediaData.length, SampleFlags.createSyncSampleFlags())
            ));

        Fmp4FragmentBuilder builder = new Fmp4FragmentBuilder();
        CmafFragment fragment = builder.buildFragment(config);
        byte[] serialized = fragment.serialize();

        assertTrue(findBox(serialized, "styp"));
        assertTrue(findBox(serialized, "moof"));
        assertTrue(findBox(serialized, "mdat"));

        CmafFragment decoded = new CmafDeserializer().deserialize(serialized);
        MoofBox.TrafBox traf = decoded.getMoof().getTrafs().get(0);
        MoofBox.TrunBox trun = traf.getTruns().get(0);
        int dataOffset = trun.getDataOffset();

        int moofSize = decoded.getMoof().serialize().length;
        assertEquals(moofSize + 8, dataOffset);
    }

    private static byte[] boxBytes(String type, byte[] payload) {
        byte[] typeBytes = type.getBytes(StandardCharsets.US_ASCII);
        int size = 8 + payload.length;
        ByteBuffer buffer = ByteBuffer.allocate(size);
        buffer.putInt(size);
        buffer.put(typeBytes, 0, 4);
        buffer.put(payload);
        return buffer.array();
    }

    private static boolean findBox(byte[] data, String type) {
        byte[] pattern = type.getBytes(StandardCharsets.US_ASCII);
        for (int i = 0; i <= data.length - pattern.length; i++) {
            boolean match = true;
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return true;
            }
        }
        return false;
    }
}
