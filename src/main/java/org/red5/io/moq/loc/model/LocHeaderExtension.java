package org.red5.io.moq.loc.model;

import org.red5.io.moq.model.IHeaderExtension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Base class for LOC (Low Overhead Media Container) Header Extensions.
 *
 * LOC Header Extensions carry optional metadata for the corresponding LOC Payload.
 * These are contained within MOQ Object Header Extensions and provide information
 * for subscribers, relays, and intermediaries without accessing the media payload.
 *
 * Reference: draft-ietf-moq-loc
 * https://datatracker.ietf.org/doc/html/draft-mzanaty-moq-loc-05
 */
public abstract class LocHeaderExtension implements IHeaderExtension {

    /**
     * Extension ID (varint).
     * Even IDs: Value is varint, Length is omitted
     * Odd IDs: Value is Length bytes, Length is varint
     */
    protected final int extensionId;

    public LocHeaderExtension(int extensionId) {
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
     * Write a varint to an output stream.
     */
    protected void writeVarint(ByteArrayOutputStream baos, long value) throws IOException {
        while (value >= 0x80) {
            baos.write((int) ((value & 0x7F) | 0x80));
            value >>>= 7;
        }
        baos.write((int) value);
    }

    /**
     * Read a varint from a ByteBuffer.
     */
    public static long readVarint(ByteBuffer buffer) throws IOException {
        long value = 0;
        int shift = 0;

        while (true) {
            if (!buffer.hasRemaining()) {
                throw new IOException("Unexpected end of buffer while reading varint");
            }

            byte b = buffer.get();
            value |= ((long) (b & 0x7F)) << shift;

            if ((b & 0x80) == 0) {
                break;
            }

            shift += 7;
            if (shift >= 64) {
                throw new IOException("Varint too long");
            }
        }

        return value;
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
