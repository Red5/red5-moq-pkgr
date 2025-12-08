package org.red5.io.moq.hang.deserialize;

import org.red5.io.moq.hang.model.HangObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Deserializes Hang media objects from byte arrays.
 * <p>
 * The Hang container format is very simple per draft-lcurley-moq-hang-01:
 * <pre>
 * +------------------+------------------+
 * | Timestamp (var)  | Payload          |
 * +------------------+------------------+
 * </pre>
 * Where:
 * <ul>
 *   <li>Timestamp: QUIC variable-length integer (62-bit max) in microseconds</li>
 *   <li>Payload: Codec-specific raw data</li>
 * </ul>
 * </p>
 *
 * Reference: draft-lcurley-moq-hang-01
 */
public class HangDeserializer {

    private static final Logger logger = LoggerFactory.getLogger(HangDeserializer.class);

    /**
     * Deserialize a Hang object from bytes.
     *
     * @param data the serialized data (timestamp + payload)
     * @param mediaType the expected media type (VIDEO or AUDIO)
     * @return the deserialized HangObject
     * @throws IOException if deserialization fails
     */
    public HangObject deserialize(byte[] data, HangObject.MediaType mediaType) throws IOException {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Data cannot be null or empty");
        }

        logger.debug("Deserializing Hang object: {} bytes, mediaType={}", data.length, mediaType);

        ByteBuffer buffer = ByteBuffer.wrap(data);

        // Decode timestamp varint
        VarintResult timestampResult = decodeVarint(buffer);
        long timestampMicros = timestampResult.value;

        // Remaining bytes are the payload
        int payloadLength = buffer.remaining();
        byte[] payload = new byte[payloadLength];
        buffer.get(payload);

        HangObject obj = new HangObject(mediaType, timestampMicros, payload);

        logger.debug("Deserialized Hang object: timestamp={} us, payloadSize={}",
                timestampMicros, payload.length);

        return obj;
    }

    /**
     * Deserialize video data with codec hint.
     *
     * @param data the serialized data
     * @param codec the video codec
     * @param keyframe true if this is a keyframe
     * @return the deserialized HangObject
     * @throws IOException if deserialization fails
     */
    public HangObject deserializeVideo(byte[] data, HangObject.VideoCodec codec, boolean keyframe) throws IOException {
        HangObject obj = deserialize(data, HangObject.MediaType.VIDEO);
        obj.setVideoCodec(codec);
        obj.setKeyframe(keyframe);
        return obj;
    }

    /**
     * Deserialize audio data with codec hint.
     *
     * @param data the serialized data
     * @param codec the audio codec
     * @return the deserialized HangObject
     * @throws IOException if deserialization fails
     */
    public HangObject deserializeAudio(byte[] data, HangObject.AudioCodec codec) throws IOException {
        HangObject obj = deserialize(data, HangObject.MediaType.AUDIO);
        obj.setAudioCodec(codec);
        return obj;
    }

    /**
     * Extract just the timestamp from serialized data without fully deserializing.
     *
     * @param data the serialized data
     * @return timestamp in microseconds
     * @throws IOException if parsing fails
     */
    public long extractTimestamp(byte[] data) throws IOException {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Data cannot be null or empty");
        }
        ByteBuffer buffer = ByteBuffer.wrap(data);
        return decodeVarint(buffer).value;
    }

    /**
     * Extract just the payload from serialized data (skipping timestamp).
     *
     * @param data the serialized data
     * @return payload bytes
     * @throws IOException if parsing fails
     */
    public byte[] extractPayload(byte[] data) throws IOException {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Data cannot be null or empty");
        }
        ByteBuffer buffer = ByteBuffer.wrap(data);
        decodeVarint(buffer); // Skip timestamp
        byte[] payload = new byte[buffer.remaining()];
        buffer.get(payload);
        return payload;
    }

    /**
     * Decode a QUIC variable-length integer from a ByteBuffer.
     * <p>
     * QUIC varint encoding (RFC 9000 Section 16):
     * <ul>
     *   <li>1 byte: prefix 00, value in bits 0-5</li>
     *   <li>2 bytes: prefix 01, value in bits 0-13</li>
     *   <li>4 bytes: prefix 10, value in bits 0-29</li>
     *   <li>8 bytes: prefix 11, value in bits 0-61</li>
     * </ul>
     * </p>
     *
     * @param buffer the buffer to read from
     * @return the decoded value and number of bytes consumed
     * @throws IOException if not enough bytes available
     */
    public static VarintResult decodeVarint(ByteBuffer buffer) throws IOException {
        if (!buffer.hasRemaining()) {
            throw new IOException("Buffer underflow: no bytes available for varint");
        }

        int firstByte = buffer.get() & 0xFF;
        int prefix = firstByte >> 6;

        long value;
        int bytesConsumed;

        switch (prefix) {
            case 0: // 1 byte
                value = firstByte & 0x3F;
                bytesConsumed = 1;
                break;
            case 1: // 2 bytes
                if (buffer.remaining() < 1) {
                    throw new IOException("Buffer underflow: need 2 bytes for varint, only have " + (buffer.remaining() + 1));
                }
                value = ((long) (firstByte & 0x3F) << 8) | (buffer.get() & 0xFF);
                bytesConsumed = 2;
                break;
            case 2: // 4 bytes
                if (buffer.remaining() < 3) {
                    throw new IOException("Buffer underflow: need 4 bytes for varint, only have " + (buffer.remaining() + 1));
                }
                value = ((long) (firstByte & 0x3F) << 24) |
                        ((buffer.get() & 0xFFL) << 16) |
                        ((buffer.get() & 0xFFL) << 8) |
                        (buffer.get() & 0xFFL);
                bytesConsumed = 4;
                break;
            case 3: // 8 bytes
                if (buffer.remaining() < 7) {
                    throw new IOException("Buffer underflow: need 8 bytes for varint, only have " + (buffer.remaining() + 1));
                }
                value = ((long) (firstByte & 0x3F) << 56) |
                        ((buffer.get() & 0xFFL) << 48) |
                        ((buffer.get() & 0xFFL) << 40) |
                        ((buffer.get() & 0xFFL) << 32) |
                        ((buffer.get() & 0xFFL) << 24) |
                        ((buffer.get() & 0xFFL) << 16) |
                        ((buffer.get() & 0xFFL) << 8) |
                        (buffer.get() & 0xFFL);
                bytesConsumed = 8;
                break;
            default:
                throw new IOException("Invalid varint prefix: " + prefix);
        }

        return new VarintResult(value, bytesConsumed);
    }

    /**
     * Decode a QUIC variable-length integer from a byte array.
     *
     * @param data the data
     * @param offset starting offset
     * @return the decoded value and number of bytes consumed
     * @throws IOException if not enough bytes available
     */
    public static VarintResult decodeVarint(byte[] data, int offset) throws IOException {
        if (data == null || offset >= data.length) {
            throw new IOException("Invalid data or offset");
        }
        ByteBuffer buffer = ByteBuffer.wrap(data, offset, data.length - offset);
        return decodeVarint(buffer);
    }

    /**
     * Result of varint decoding containing the value and bytes consumed.
     */
    public static class VarintResult {
        public final long value;
        public final int bytesConsumed;

        public VarintResult(long value, int bytesConsumed) {
            this.value = value;
            this.bytesConsumed = bytesConsumed;
        }
    }
}