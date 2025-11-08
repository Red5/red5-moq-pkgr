package org.red5.io.moq.moqmi.serialize;

import org.red5.io.moq.moqmi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Serializes MoQ Media Interop objects to byte arrays.
 *
 * The serialized format follows draft-cenzano-moq-media-interop-03:
 * - MoQ-MI Header Extensions (metadata including media type)
 * - Payload (encoded media data in codec-specific format)
 *
 * Reference: draft-cenzano-moq-media-interop-03
 */
public class MoqMISerializer {

    private static final Logger logger = LoggerFactory.getLogger(MoqMISerializer.class);

    /**
     * Serialize a MoQ-MI object to a byte array containing header extensions and payload.
     *
     * @param moqMIObject the MoQ-MI object to serialize
     * @return byte array with serialized data
     * @throws IOException if serialization fails
     */
    public byte[] serialize(MoqMIObject moqMIObject) throws IOException {
        if (moqMIObject == null) {
            throw new IllegalArgumentException("MoqMIObject cannot be null");
        }

        logger.debug("Serializing MoQ-MI object: groupId={}, objectId={}, mediaType={}",
                moqMIObject.getGroupId(), moqMIObject.getObjectId(), moqMIObject.getMediaType());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Serialize header extensions
        byte[] headerExtensions = moqMIObject.serializeHeaderExtensions();
        if (headerExtensions.length > 0) {
            logger.debug("Serializing {} header extension bytes", headerExtensions.length);
            baos.write(headerExtensions);
        }

        // Serialize payload
        byte[] payload = moqMIObject.getPayload();
        if (payload != null && payload.length > 0) {
            logger.debug("Serializing {} payload bytes", payload.length);
            baos.write(payload);
        }

        byte[] result = baos.toByteArray();
        logger.debug("Serialized MoQ-MI object: {} total bytes", result.length);

        return result;
    }

    /**
     * Serialize only the header extensions portion.
     *
     * @param moqMIObject the MoQ-MI object
     * @return byte array with serialized header extensions
     * @throws IOException if serialization fails
     */
    public byte[] serializeHeaderExtensions(MoqMIObject moqMIObject) throws IOException {
        if (moqMIObject == null) {
            throw new IllegalArgumentException("MoqMIObject cannot be null");
        }

        return moqMIObject.serializeHeaderExtensions();
    }

    /**
     * Get the payload portion of a MoQ-MI object.
     *
     * @param moqMIObject the MoQ-MI object
     * @return payload byte array
     */
    public byte[] getPayload(MoqMIObject moqMIObject) {
        if (moqMIObject == null) {
            throw new IllegalArgumentException("MoqMIObject cannot be null");
        }

        return moqMIObject.getPayload();
    }

    /**
     * Create a minimal H264 video object.
     *
     * @param payload the H264 video data in AVCC format
     * @param seqId sequence ID
     * @param ptsTimestamp PTS in timebase units
     * @param dtsTimestamp DTS in timebase units
     * @param timebase timebase denominator
     * @return a new MoQ-MI object
     */
    public static MoqMIObject createH264Object(byte[] payload, long seqId, long ptsTimestamp,
                                                long dtsTimestamp, long timebase) {
        MoqMIObject obj = new MoqMIObject(MoqMIObject.MediaType.VIDEO_H264_AVCC, payload);

        // Add required media type extension
        obj.addHeaderExtension(new MediaTypeExtension(MoqMIObject.MediaType.VIDEO_H264_AVCC));

        // Add H264 metadata extension
        H264MetadataExtension metadata = new H264MetadataExtension(
                seqId, ptsTimestamp, dtsTimestamp, timebase, 0, 0);
        obj.addHeaderExtension(metadata);

        return obj;
    }

    /**
     * Create a minimal H264 video object with extradata (for IDR frames).
     *
     * @param payload the H264 video data in AVCC format
     * @param seqId sequence ID
     * @param ptsTimestamp PTS in timebase units
     * @param dtsTimestamp DTS in timebase units
     * @param timebase timebase denominator
     * @param extradata AVCDecoderConfigurationRecord
     * @return a new MoQ-MI object
     */
    public static MoqMIObject createH264ObjectWithExtradata(byte[] payload, long seqId,
                                                             long ptsTimestamp, long dtsTimestamp,
                                                             long timebase, byte[] extradata) {
        MoqMIObject obj = createH264Object(payload, seqId, ptsTimestamp, dtsTimestamp, timebase);

        // Add H264 extradata extension (for IDR frames)
        obj.addHeaderExtension(new H264ExtradataExtension(extradata));

        return obj;
    }

    /**
     * Create a minimal Opus audio object.
     *
     * @param payload the Opus audio data
     * @param seqId sequence ID
     * @param ptsTimestamp PTS in timebase units
     * @param timebase timebase denominator
     * @param sampleFreq sample frequency
     * @param numChannels number of channels
     * @return a new MoQ-MI object
     */
    public static MoqMIObject createOpusObject(byte[] payload, long seqId, long ptsTimestamp,
                                                long timebase, long sampleFreq, long numChannels) {
        MoqMIObject obj = new MoqMIObject(MoqMIObject.MediaType.AUDIO_OPUS, payload);

        // Add required media type extension
        obj.addHeaderExtension(new MediaTypeExtension(MoqMIObject.MediaType.AUDIO_OPUS));

        // Add Opus data extension
        OpusDataExtension opusData = new OpusDataExtension(
                seqId, ptsTimestamp, timebase, sampleFreq, numChannels, 0, 0);
        obj.addHeaderExtension(opusData);

        return obj;
    }

    /**
     * Create a minimal AAC-LC audio object.
     *
     * @param payload the AAC-LC audio data
     * @param seqId sequence ID
     * @param ptsTimestamp PTS in timebase units
     * @param timebase timebase denominator
     * @param sampleFreq sample frequency
     * @param numChannels number of channels
     * @return a new MoQ-MI object
     */
    public static MoqMIObject createAacLcObject(byte[] payload, long seqId, long ptsTimestamp,
                                                 long timebase, long sampleFreq, long numChannels) {
        MoqMIObject obj = new MoqMIObject(MoqMIObject.MediaType.AUDIO_AAC_LC, payload);

        // Add required media type extension
        obj.addHeaderExtension(new MediaTypeExtension(MoqMIObject.MediaType.AUDIO_AAC_LC));

        // Add AAC-LC data extension
        AacLcDataExtension aacData = new AacLcDataExtension(
                seqId, ptsTimestamp, timebase, sampleFreq, numChannels, 0, 0);
        obj.addHeaderExtension(aacData);

        return obj;
    }

    /**
     * Create a minimal UTF-8 text object.
     *
     * @param payload the UTF-8 text data
     * @param seqId sequence ID
     * @return a new MoQ-MI object
     */
    public static MoqMIObject createUtf8TextObject(byte[] payload, long seqId) {
        MoqMIObject obj = new MoqMIObject(MoqMIObject.MediaType.TEXT_UTF8, payload);

        // Add required media type extension
        obj.addHeaderExtension(new MediaTypeExtension(MoqMIObject.MediaType.TEXT_UTF8));

        // Add UTF-8 text extension
        obj.addHeaderExtension(new Utf8TextExtension(seqId));

        return obj;
    }
}
