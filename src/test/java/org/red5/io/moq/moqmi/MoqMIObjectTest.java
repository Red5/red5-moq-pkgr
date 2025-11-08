package org.red5.io.moq.moqmi;

import org.junit.jupiter.api.Test;
import org.red5.io.moq.moqmi.deserialize.MoqMIDeserializer;
import org.red5.io.moq.moqmi.model.*;
import org.red5.io.moq.moqmi.serialize.MoqMISerializer;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for MoQ Media Interop (MoqMI) implementation.
 *
 * Tests serialization, deserialization, and all header extension types
 * according to draft-cenzano-moq-media-interop-03.
 */
class MoqMIObjectTest {

    @Test
    void testH264VideoWithoutExtradata() throws Exception {
        // Create H264 video object
        byte[] videoPayload = new byte[]{0x00, 0x00, 0x00, 0x01, 0x67, 0x42}; // Mock H264 NALU
        long seqId = 1;
        long pts = 0;
        long dts = 0;
        long timebase = 30; // 30 fps

        MoqMIObject obj = MoqMISerializer.createH264Object(videoPayload, seqId, pts, dts, timebase);
        obj.setGroupId(1);
        obj.setObjectId(1);

        // Verify object structure
        assertEquals(MoqMIObject.MediaType.VIDEO_H264_AVCC, obj.getMediaType());
        assertEquals(2, obj.getHeaderExtensions().size()); // MediaType + H264Metadata

        // Verify media type extension
        MediaTypeExtension mediaTypeExt = obj.getHeaderExtension(MediaTypeExtension.class);
        assertNotNull(mediaTypeExt);
        assertEquals(MoqMIObject.MediaType.VIDEO_H264_AVCC, mediaTypeExt.getMediaType());

        // Verify H264 metadata extension
        H264MetadataExtension metadata = obj.getHeaderExtension(H264MetadataExtension.class);
        assertNotNull(metadata);
        assertEquals(seqId, metadata.getSeqId());
        assertEquals(pts, metadata.getPtsTimestamp());
        assertEquals(dts, metadata.getDtsTimestamp());
        assertEquals(timebase, metadata.getTimebase());

        // Serialize
        MoqMISerializer serializer = new MoqMISerializer();
        byte[] headerExtensions = serializer.serializeHeaderExtensions(obj);
        byte[] payload = serializer.getPayload(obj);

        assertNotNull(headerExtensions);
        assertTrue(headerExtensions.length > 0);
        assertArrayEquals(videoPayload, payload);

        // Deserialize
        MoqMIDeserializer deserializer = new MoqMIDeserializer();
        MoqMIObject deserialized = deserializer.deserialize(headerExtensions, payload);

        // Verify deserialized object
        assertEquals(MoqMIObject.MediaType.VIDEO_H264_AVCC, deserialized.getMediaType());
        assertArrayEquals(videoPayload, deserialized.getPayload());

        MediaTypeExtension deserializedMediaType = deserialized.getHeaderExtension(MediaTypeExtension.class);
        assertNotNull(deserializedMediaType);
        assertEquals(MoqMIObject.MediaType.VIDEO_H264_AVCC, deserializedMediaType.getMediaType());

        H264MetadataExtension deserializedMetadata = deserialized.getHeaderExtension(H264MetadataExtension.class);
        assertNotNull(deserializedMetadata);
        assertEquals(seqId, deserializedMetadata.getSeqId());
        assertEquals(pts, deserializedMetadata.getPtsTimestamp());
        assertEquals(dts, deserializedMetadata.getDtsTimestamp());
        assertEquals(timebase, deserializedMetadata.getTimebase());
    }

    @Test
    void testH264VideoWithExtradata() throws Exception {
        // Create H264 IDR frame with extradata
        byte[] videoPayload = new byte[]{0x00, 0x00, 0x00, 0x01, 0x65}; // IDR NALU
        byte[] extradata = new byte[]{0x01, 0x42, (byte) 0xC0, 0x1E}; // Mock AVCDecoderConfigurationRecord
        long seqId = 0; // First frame
        long pts = 0;
        long dts = 0;
        long timebase = 30;

        MoqMIObject obj = MoqMISerializer.createH264ObjectWithExtradata(
                videoPayload, seqId, pts, dts, timebase, extradata);
        obj.setGroupId(0);
        obj.setObjectId(0);

        // Verify object structure
        assertEquals(3, obj.getHeaderExtensions().size()); // MediaType + H264Metadata + H264Extradata

        // Verify extradata extension
        H264ExtradataExtension extradataExt = obj.getHeaderExtension(H264ExtradataExtension.class);
        assertNotNull(extradataExt);
        assertArrayEquals(extradata, extradataExt.getExtradata());

        // Serialize
        MoqMISerializer serializer = new MoqMISerializer();
        byte[] headerExtensions = serializer.serializeHeaderExtensions(obj);
        byte[] payload = serializer.getPayload(obj);

        // Deserialize
        MoqMIDeserializer deserializer = new MoqMIDeserializer();
        MoqMIObject deserialized = deserializer.deserialize(headerExtensions, payload);

        // Verify deserialized extradata
        H264ExtradataExtension deserializedExtradata = deserialized.getHeaderExtension(H264ExtradataExtension.class);
        assertNotNull(deserializedExtradata);
        assertArrayEquals(extradata, deserializedExtradata.getExtradata());
    }

    @Test
    void testOpusAudio() throws Exception {
        // Create Opus audio object
        byte[] audioPayload = new byte[480]; // 10ms Opus frame at 48kHz
        long seqId = 5;
        long pts = 150;
        long timebase = 48000; // 48 kHz
        long sampleFreq = 48000;
        long numChannels = 2; // Stereo

        MoqMIObject obj = MoqMISerializer.createOpusObject(
                audioPayload, seqId, pts, timebase, sampleFreq, numChannels);
        obj.setGroupId(5);
        obj.setObjectId(0);

        // Verify object structure
        assertEquals(MoqMIObject.MediaType.AUDIO_OPUS, obj.getMediaType());
        assertEquals(2, obj.getHeaderExtensions().size()); // MediaType + OpusData

        // Verify Opus data extension
        OpusDataExtension opusData = obj.getHeaderExtension(OpusDataExtension.class);
        assertNotNull(opusData);
        assertEquals(seqId, opusData.getSeqId());
        assertEquals(pts, opusData.getPtsTimestamp());
        assertEquals(timebase, opusData.getTimebase());
        assertEquals(sampleFreq, opusData.getSampleFreq());
        assertEquals(numChannels, opusData.getNumChannels());

        // Serialize and deserialize
        MoqMISerializer serializer = new MoqMISerializer();
        byte[] headerExtensions = serializer.serializeHeaderExtensions(obj);
        byte[] payload = serializer.getPayload(obj);

        MoqMIDeserializer deserializer = new MoqMIDeserializer();
        MoqMIObject deserialized = deserializer.deserialize(headerExtensions, payload);

        // Verify deserialized object
        assertEquals(MoqMIObject.MediaType.AUDIO_OPUS, deserialized.getMediaType());
        assertArrayEquals(audioPayload, deserialized.getPayload());

        OpusDataExtension deserializedOpus = deserialized.getHeaderExtension(OpusDataExtension.class);
        assertNotNull(deserializedOpus);
        assertEquals(seqId, deserializedOpus.getSeqId());
        assertEquals(pts, deserializedOpus.getPtsTimestamp());
        assertEquals(timebase, deserializedOpus.getTimebase());
        assertEquals(sampleFreq, deserializedOpus.getSampleFreq());
        assertEquals(numChannels, deserializedOpus.getNumChannels());
    }

    @Test
    void testAacLcAudio() throws Exception {
        // Create AAC-LC audio object
        byte[] audioPayload = new byte[1024]; // Mock AAC frame
        long seqId = 10;
        long pts = 0;
        long timebase = 48000; // 48 kHz
        long sampleFreq = 48000;
        long numChannels = 2; // Stereo

        MoqMIObject obj = MoqMISerializer.createAacLcObject(
                audioPayload, seqId, pts, timebase, sampleFreq, numChannels);
        obj.setGroupId(10);
        obj.setObjectId(0);

        // Verify object structure
        assertEquals(MoqMIObject.MediaType.AUDIO_AAC_LC, obj.getMediaType());
        assertEquals(2, obj.getHeaderExtensions().size()); // MediaType + AacLcData

        // Verify AAC-LC data extension
        AacLcDataExtension aacData = obj.getHeaderExtension(AacLcDataExtension.class);
        assertNotNull(aacData);
        assertEquals(seqId, aacData.getSeqId());
        assertEquals(pts, aacData.getPtsTimestamp());
        assertEquals(timebase, aacData.getTimebase());
        assertEquals(sampleFreq, aacData.getSampleFreq());
        assertEquals(numChannels, aacData.getNumChannels());

        // Serialize and deserialize
        MoqMISerializer serializer = new MoqMISerializer();
        byte[] headerExtensions = serializer.serializeHeaderExtensions(obj);
        byte[] payload = serializer.getPayload(obj);

        MoqMIDeserializer deserializer = new MoqMIDeserializer();
        MoqMIObject deserialized = deserializer.deserialize(headerExtensions, payload);

        // Verify deserialized object
        assertEquals(MoqMIObject.MediaType.AUDIO_AAC_LC, deserialized.getMediaType());
        assertArrayEquals(audioPayload, deserialized.getPayload());

        AacLcDataExtension deserializedAac = deserialized.getHeaderExtension(AacLcDataExtension.class);
        assertNotNull(deserializedAac);
        assertEquals(seqId, deserializedAac.getSeqId());
        assertEquals(pts, deserializedAac.getPtsTimestamp());
        assertEquals(timebase, deserializedAac.getTimebase());
        assertEquals(sampleFreq, deserializedAac.getSampleFreq());
        assertEquals(numChannels, deserializedAac.getNumChannels());
    }

    @Test
    void testUtf8Text() throws Exception {
        // Create UTF-8 text object
        String textContent = "Hello MoQ Media Interop!";
        byte[] textPayload = textContent.getBytes(StandardCharsets.UTF_8);
        long seqId = 1;

        MoqMIObject obj = MoqMISerializer.createUtf8TextObject(textPayload, seqId);
        obj.setGroupId(1);
        obj.setObjectId(0);

        // Verify object structure
        assertEquals(MoqMIObject.MediaType.TEXT_UTF8, obj.getMediaType());
        assertEquals(2, obj.getHeaderExtensions().size()); // MediaType + Utf8Text

        // Verify UTF-8 text extension
        Utf8TextExtension textExt = obj.getHeaderExtension(Utf8TextExtension.class);
        assertNotNull(textExt);
        assertEquals(seqId, textExt.getSeqId());

        // Serialize and deserialize
        MoqMISerializer serializer = new MoqMISerializer();
        byte[] headerExtensions = serializer.serializeHeaderExtensions(obj);
        byte[] payload = serializer.getPayload(obj);

        MoqMIDeserializer deserializer = new MoqMIDeserializer();
        MoqMIObject deserialized = deserializer.deserialize(headerExtensions, payload);

        // Verify deserialized object
        assertEquals(MoqMIObject.MediaType.TEXT_UTF8, deserialized.getMediaType());
        assertArrayEquals(textPayload, deserialized.getPayload());

        // Verify text content
        String deserializedText = new String(deserialized.getPayload(), StandardCharsets.UTF_8);
        assertEquals(textContent, deserializedText);

        Utf8TextExtension deserializedTextExt = deserialized.getHeaderExtension(Utf8TextExtension.class);
        assertNotNull(deserializedTextExt);
        assertEquals(seqId, deserializedTextExt.getSeqId());
    }

    @Test
    void testH264WithTimestampAndDuration() throws Exception {
        // Create H264 object with specific timestamps and duration
        byte[] videoPayload = new byte[2048];
        long seqId = 100;
        long pts = 3000;
        long dts = 3000;
        long timebase = 90000; // Common for video (90 kHz)
        long duration = 3000; // 1/30th second at 90kHz
        long wallclock = System.currentTimeMillis();

        MoqMIObject obj = new MoqMIObject(MoqMIObject.MediaType.VIDEO_H264_AVCC, videoPayload);
        obj.addHeaderExtension(new MediaTypeExtension(MoqMIObject.MediaType.VIDEO_H264_AVCC));

        H264MetadataExtension metadata = new H264MetadataExtension(
                seqId, pts, dts, timebase, duration, wallclock);
        obj.addHeaderExtension(metadata);

        // Serialize and deserialize
        MoqMISerializer serializer = new MoqMISerializer();
        byte[] headerExtensions = serializer.serializeHeaderExtensions(obj);
        byte[] payload = serializer.getPayload(obj);

        MoqMIDeserializer deserializer = new MoqMIDeserializer();
        MoqMIObject deserialized = deserializer.deserialize(headerExtensions, payload);

        // Verify all timestamp fields
        H264MetadataExtension deserializedMetadata = deserialized.getHeaderExtension(H264MetadataExtension.class);
        assertNotNull(deserializedMetadata);
        assertEquals(seqId, deserializedMetadata.getSeqId());
        assertEquals(pts, deserializedMetadata.getPtsTimestamp());
        assertEquals(dts, deserializedMetadata.getDtsTimestamp());
        assertEquals(timebase, deserializedMetadata.getTimebase());
        assertEquals(duration, deserializedMetadata.getDuration());
        assertEquals(wallclock, deserializedMetadata.getWallclock());
    }

    @Test
    void testMediaTypeEnum() {
        // Test media type enum values
        assertEquals(0x00, MoqMIObject.MediaType.VIDEO_H264_AVCC.getValue());
        assertEquals(0x01, MoqMIObject.MediaType.AUDIO_OPUS.getValue());
        assertEquals(0x02, MoqMIObject.MediaType.TEXT_UTF8.getValue());
        assertEquals(0x03, MoqMIObject.MediaType.AUDIO_AAC_LC.getValue());

        // Test fromValue
        assertEquals(MoqMIObject.MediaType.VIDEO_H264_AVCC, MoqMIObject.MediaType.fromValue(0x00));
        assertEquals(MoqMIObject.MediaType.AUDIO_OPUS, MoqMIObject.MediaType.fromValue(0x01));
        assertEquals(MoqMIObject.MediaType.TEXT_UTF8, MoqMIObject.MediaType.fromValue(0x02));
        assertEquals(MoqMIObject.MediaType.AUDIO_AAC_LC, MoqMIObject.MediaType.fromValue(0x03));

        // Test invalid value
        assertThrows(IllegalArgumentException.class, () -> MoqMIObject.MediaType.fromValue(0x99));
    }

    @Test
    void testExtensionIdValues() {
        // Verify extension IDs match the spec
        assertEquals(0x0A, MediaTypeExtension.EXTENSION_ID);
        assertEquals(0x0D, H264ExtradataExtension.EXTENSION_ID);
        assertEquals(0x0F, OpusDataExtension.EXTENSION_ID);
        assertEquals(0x11, Utf8TextExtension.EXTENSION_ID);
        assertEquals(0x13, AacLcDataExtension.EXTENSION_ID);
        assertEquals(0x15, H264MetadataExtension.EXTENSION_ID);
    }

    @Test
    void testVarintValueCheck() {
        // MediaTypeExtension has even ID (0x0A), so should use varint
        MediaTypeExtension mediaType = new MediaTypeExtension();
        assertTrue(mediaType.isVarintValue());

        // H264ExtradataExtension has odd ID (0x0D), so should use byte array
        H264ExtradataExtension extradata = new H264ExtradataExtension();
        assertFalse(extradata.isVarintValue());

        // OpusDataExtension has odd ID (0x0F), so should use byte array
        OpusDataExtension opus = new OpusDataExtension();
        assertFalse(opus.isVarintValue());

        // Utf8TextExtension has odd ID (0x11), so should use byte array
        Utf8TextExtension text = new Utf8TextExtension();
        assertFalse(text.isVarintValue());

        // AacLcDataExtension has odd ID (0x13), so should use byte array
        AacLcDataExtension aac = new AacLcDataExtension();
        assertFalse(aac.isVarintValue());

        // H264MetadataExtension has odd ID (0x15), so should use byte array
        H264MetadataExtension metadata = new H264MetadataExtension();
        assertFalse(metadata.isVarintValue());
    }

    @Test
    void testLargeVarintValues() throws Exception {
        // Test with large varint values (edge case)
        byte[] payload = new byte[1024];
        long largeSeqId = 0x7FFFFFFFL; // Large sequence number
        long largePts = 0xFFFFFFFFL;
        long largeTimebase = 1000000;

        MoqMIObject obj = MoqMISerializer.createH264Object(
                payload, largeSeqId, largePts, largePts, largeTimebase);

        // Serialize and deserialize
        MoqMISerializer serializer = new MoqMISerializer();
        byte[] headerExtensions = serializer.serializeHeaderExtensions(obj);
        byte[] payloadBytes = serializer.getPayload(obj);

        MoqMIDeserializer deserializer = new MoqMIDeserializer();
        MoqMIObject deserialized = deserializer.deserialize(headerExtensions, payloadBytes);

        // Verify large values preserved
        H264MetadataExtension metadata = deserialized.getHeaderExtension(H264MetadataExtension.class);
        assertNotNull(metadata);
        assertEquals(largeSeqId, metadata.getSeqId());
        assertEquals(largePts, metadata.getPtsTimestamp());
        assertEquals(largeTimebase, metadata.getTimebase());
    }

    @Test
    void testEmptyPayload() throws Exception {
        // Test with empty payload (edge case)
        byte[] emptyPayload = new byte[0];
        long seqId = 0;

        MoqMIObject obj = MoqMISerializer.createUtf8TextObject(emptyPayload, seqId);

        // Serialize and deserialize
        MoqMISerializer serializer = new MoqMISerializer();
        byte[] headerExtensions = serializer.serializeHeaderExtensions(obj);
        byte[] payload = serializer.getPayload(obj);

        MoqMIDeserializer deserializer = new MoqMIDeserializer();
        MoqMIObject deserialized = deserializer.deserialize(headerExtensions, payload);

        // Verify empty payload preserved
        assertNotNull(deserialized.getPayload());
        assertEquals(0, deserialized.getPayload().length);
    }

    @Test
    void testToStringMethods() {
        // Test toString methods for debugging
        MoqMIObject obj = new MoqMIObject(MoqMIObject.MediaType.VIDEO_H264_AVCC, new byte[100]);
        obj.setGroupId(1);
        obj.setObjectId(2);

        assertNotNull(obj.toString());
        assertTrue(obj.toString().contains("VIDEO_H264_AVCC"));
        assertTrue(obj.toString().contains("groupId=1"));
        assertTrue(obj.toString().contains("objectId=2"));

        MediaTypeExtension mediaType = new MediaTypeExtension(MoqMIObject.MediaType.AUDIO_OPUS);
        assertNotNull(mediaType.toString());
        assertTrue(mediaType.toString().contains("AUDIO_OPUS"));

        H264MetadataExtension metadata = new H264MetadataExtension(1, 0, 0, 30, 0, 0);
        assertNotNull(metadata.toString());
        assertTrue(metadata.toString().contains("seqId=1"));
        assertTrue(metadata.toString().contains("timebase=30"));
    }
}
