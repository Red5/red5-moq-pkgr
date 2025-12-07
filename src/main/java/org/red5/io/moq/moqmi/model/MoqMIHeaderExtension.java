package org.red5.io.moq.moqmi.model;

import org.red5.io.moq.model.IHeaderExtension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Base class for MoQ Media Interop Header Extensions.
 *
 * MoQ-MI header extensions provide metadata for audio, video, and text content
 * transmitted over MoQ Transport. These extensions use varint encoding for
 * efficient wire representation.
 *
 * Extension ID conventions:
 * - Even IDs: Value is varint, Length is omitted
 * - Odd IDs: Value is byte array, Length is varint
 *
 * Reference: draft-cenzano-moq-media-interop-03
 */
public abstract class MoqMIHeaderExtension implements IHeaderExtension {

    /**
     * Extension ID (varint).
     */
    protected final int extensionId;

    public MoqMIHeaderExtension(int extensionId) {
        this.extensionId = extensionId;
    }

    public int getExtensionId() {
        return extensionId;
    }

    /**
     * Check if this extension uses varint encoding for the value.
     * Even IDs use varint, odd IDs use byte array.
     */
    public boolean isVarintValue() {
        return (extensionId % 2) == 0;
    }

    /**
     * Serialize the extension to bytes.
     * Format: [ID][Length (if odd ID)][Value]
     */
    public byte[] serialize() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Write extension ID as varint
        writeVarint(baos, extensionId);

        // Get value bytes
        byte[] valueBytes = serializeValue();

        if (!isVarintValue()) {
            // Odd ID: write length as varint
            writeVarint(baos, valueBytes.length);
        }

        // Write value
        baos.write(valueBytes);

        return baos.toByteArray();
    }

    /**
     * Serialize the extension value (subclass-specific).
     */
    protected abstract byte[] serializeValue() throws IOException;

    /**
     * Deserialize the extension value from a buffer.
     */
    public abstract void deserializeValue(ByteBuffer buffer, int length) throws IOException;

    /**
     * Write a QUIC varint to an output stream.
     * QUIC varint format: first 2 bits indicate length (00=1, 01=2, 10=4, 11=8 bytes).
     */
    protected void writeVarint(ByteArrayOutputStream baos, long value) throws IOException {
        if (value <= 63) {
            // 1 byte: 00xxxxxx
            baos.write((int) value);
        } else if (value <= 16383) {
            // 2 bytes: 01xxxxxx xxxxxxxx
            baos.write((int) ((value >> 8) | 0x40));
            baos.write((int) (value & 0xFF));
        } else if (value <= 1073741823) {
            // 4 bytes: 10xxxxxx xxxxxxxx xxxxxxxx xxxxxxxx
            baos.write((int) ((value >> 24) | 0x80));
            baos.write((int) ((value >> 16) & 0xFF));
            baos.write((int) ((value >> 8) & 0xFF));
            baos.write((int) (value & 0xFF));
        } else {
            // 8 bytes: 11xxxxxx xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx
            baos.write((int) ((value >> 56) | 0xC0));
            baos.write((int) ((value >> 48) & 0xFF));
            baos.write((int) ((value >> 40) & 0xFF));
            baos.write((int) ((value >> 32) & 0xFF));
            baos.write((int) ((value >> 24) & 0xFF));
            baos.write((int) ((value >> 16) & 0xFF));
            baos.write((int) ((value >> 8) & 0xFF));
            baos.write((int) (value & 0xFF));
        }
    }

    /**
     * Read a QUIC varint from a ByteBuffer.
     * QUIC varint format: first 2 bits indicate length (00=1, 01=2, 10=4, 11=8 bytes).
     */
    public static long readVarint(ByteBuffer buffer) throws IOException {
        if (!buffer.hasRemaining()) {
            throw new IOException("Unexpected end of buffer while reading varint");
        }

        byte firstByte = buffer.get();
        int lengthType = (firstByte & 0xC0) >> 6;

        switch (lengthType) {
            case 0:
                // 1 byte: 00xxxxxx (6-bit value)
                return firstByte & 0x3F;
            case 1:
                // 2 bytes: 01xxxxxx xxxxxxxx (14-bit value)
                if (!buffer.hasRemaining()) {
                    throw new IOException("Unexpected end of buffer while reading 2-byte varint");
                }
                return ((firstByte & 0x3F) << 8) | (buffer.get() & 0xFF);
            case 2:
                // 4 bytes: 10xxxxxx xxxxxxxx xxxxxxxx xxxxxxxx (30-bit value)
                if (buffer.remaining() < 3) {
                    throw new IOException("Unexpected end of buffer while reading 4-byte varint");
                }
                return ((long) (firstByte & 0x3F) << 24) |
                       ((buffer.get() & 0xFF) << 16) |
                       ((buffer.get() & 0xFF) << 8) |
                       (buffer.get() & 0xFF);
            case 3:
                // 8 bytes: 11xxxxxx xxxxxxxx... (62-bit value)
                if (buffer.remaining() < 7) {
                    throw new IOException("Unexpected end of buffer while reading 8-byte varint");
                }
                return ((long) (firstByte & 0x3F) << 56) |
                       ((long) (buffer.get() & 0xFF) << 48) |
                       ((long) (buffer.get() & 0xFF) << 40) |
                       ((long) (buffer.get() & 0xFF) << 32) |
                       ((long) (buffer.get() & 0xFF) << 24) |
                       ((buffer.get() & 0xFF) << 16) |
                       ((buffer.get() & 0xFF) << 8) |
                       (buffer.get() & 0xFF);
            default:
                throw new IOException("Invalid varint length type: " + lengthType);
        }
    }

    /**
     * Serialize a varint value to bytes.
     */
    protected byte[] serializeVarint(long value) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writeVarint(baos, value);
        return baos.toByteArray();
    }

}
