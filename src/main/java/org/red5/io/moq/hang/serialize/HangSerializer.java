package org.red5.io.moq.hang.serialize;

import org.red5.io.moq.hang.model.HangObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Serializes Hang media objects to byte arrays.
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
public class HangSerializer {

    private static final Logger logger = LoggerFactory.getLogger(HangSerializer.class);

    /**
     * Serialize a Hang object to bytes (timestamp + payload).
     *
     * @param hangObject the Hang object to serialize
     * @return serialized bytes
     * @throws IOException if serialization fails
     */
    public byte[] serialize(HangObject hangObject) throws IOException {
        if (hangObject == null) {
            throw new IllegalArgumentException("HangObject cannot be null");
        }

        logger.debug("Serializing Hang object: mediaType={}, timestamp={} us, payloadSize={}",
                hangObject.getMediaType(), hangObject.getTimestampMicros(),
                hangObject.getPayload() != null ? hangObject.getPayload().length : 0);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Write timestamp as QUIC variable-length integer
        byte[] timestampBytes = encodeVarint(hangObject.getTimestampMicros());
        baos.write(timestampBytes);

        // Write payload
        byte[] payload = hangObject.getPayload();
        if (payload != null && payload.length > 0) {
            baos.write(payload);
        }

        byte[] result = baos.toByteArray();
        logger.debug("Serialized Hang object: {} total bytes (timestamp: {} bytes, payload: {} bytes)",
                result.length, timestampBytes.length, payload != null ? payload.length : 0);

        return result;
    }

    /**
     * Get just the payload portion (for cases where timestamp is sent separately).
     *
     * @param hangObject the Hang object
     * @return payload bytes
     */
    public byte[] getPayload(HangObject hangObject) {
        if (hangObject == null) {
            throw new IllegalArgumentException("HangObject cannot be null");
        }
        return hangObject.getPayload();
    }

    /**
     * Encode timestamp to QUIC variable-length integer bytes.
     *
     * @param hangObject the Hang object
     * @return encoded timestamp bytes
     */
    public byte[] encodeTimestamp(HangObject hangObject) {
        if (hangObject == null) {
            throw new IllegalArgumentException("HangObject cannot be null");
        }
        return encodeVarint(hangObject.getTimestampMicros());
    }

    /**
     * Create and serialize a video Hang object.
     *
     * @param payload video data
     * @param timestampMicros timestamp in microseconds
     * @param keyframe true if keyframe
     * @param codec video codec
     * @return serialized bytes
     * @throws IOException if serialization fails
     */
    public byte[] serializeVideo(byte[] payload, long timestampMicros, boolean keyframe,
                                  HangObject.VideoCodec codec) throws IOException {
        HangObject obj = HangObject.createVideo(timestampMicros, payload, keyframe, codec);
        return serialize(obj);
    }

    /**
     * Create and serialize an audio Hang object.
     *
     * @param payload audio data
     * @param timestampMicros timestamp in microseconds
     * @param codec audio codec
     * @return serialized bytes
     * @throws IOException if serialization fails
     */
    public byte[] serializeAudio(byte[] payload, long timestampMicros,
                                  HangObject.AudioCodec codec) throws IOException {
        HangObject obj = HangObject.createAudio(timestampMicros, payload, codec);
        return serialize(obj);
    }

    /**
     * Encode a 62-bit value as a QUIC variable-length integer.
     * <p>
     * QUIC varint encoding (RFC 9000 Section 16):
     * <ul>
     *   <li>1 byte: 0-63 (2^6-1), prefix 00</li>
     *   <li>2 bytes: 0-16383 (2^14-1), prefix 01</li>
     *   <li>4 bytes: 0-1073741823 (2^30-1), prefix 10</li>
     *   <li>8 bytes: 0-4611686018427387903 (2^62-1), prefix 11</li>
     * </ul>
     * </p>
     *
     * @param value the value to encode (must be non-negative, max 62 bits)
     * @return encoded bytes
     */
    public static byte[] encodeVarint(long value) {
        if (value < 0) {
            throw new IllegalArgumentException("Value must be non-negative: " + value);
        }

        if (value <= 63) {
            // 1 byte, prefix 00
            return new byte[] { (byte) value };
        } else if (value <= 16383) {
            // 2 bytes, prefix 01
            return new byte[] {
                (byte) (0x40 | (value >> 8)),
                (byte) (value & 0xFF)
            };
        } else if (value <= 1073741823L) {
            // 4 bytes, prefix 10
            return new byte[] {
                (byte) (0x80 | (value >> 24)),
                (byte) ((value >> 16) & 0xFF),
                (byte) ((value >> 8) & 0xFF),
                (byte) (value & 0xFF)
            };
        } else if (value <= 4611686018427387903L) {
            // 8 bytes, prefix 11
            return new byte[] {
                (byte) (0xC0 | (value >> 56)),
                (byte) ((value >> 48) & 0xFF),
                (byte) ((value >> 40) & 0xFF),
                (byte) ((value >> 32) & 0xFF),
                (byte) ((value >> 24) & 0xFF),
                (byte) ((value >> 16) & 0xFF),
                (byte) ((value >> 8) & 0xFF),
                (byte) (value & 0xFF)
            };
        } else {
            throw new IllegalArgumentException("Value exceeds 62-bit maximum: " + value);
        }
    }

    /**
     * Calculate the encoded size of a varint without actually encoding it.
     *
     * @param value the value
     * @return number of bytes needed
     */
    public static int varintSize(long value) {
        if (value < 0) {
            throw new IllegalArgumentException("Value must be non-negative: " + value);
        }
        if (value <= 63) return 1;
        if (value <= 16383) return 2;
        if (value <= 1073741823L) return 4;
        if (value <= 4611686018427387903L) return 8;
        throw new IllegalArgumentException("Value exceeds 62-bit maximum: " + value);
    }
}