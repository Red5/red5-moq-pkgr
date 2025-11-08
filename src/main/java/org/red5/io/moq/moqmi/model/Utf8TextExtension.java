package org.red5.io.moq.moqmi.model;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * UTF-8 Text header extension (header extension type = 0x11).
 *
 * Provides metadata for UTF-8 text objects.
 *
 * Fields:
 * - Seq ID: Monotonically increasing counter for this track
 *
 * Reference: draft-cenzano-moq-media-interop-03 Section 2.4.5
 */
public class Utf8TextExtension extends MoqMIHeaderExtension {

    public static final int EXTENSION_ID = 0x11;

    private long seqId;

    public Utf8TextExtension() {
        super(EXTENSION_ID);
    }

    public Utf8TextExtension(long seqId) {
        super(EXTENSION_ID);
        this.seqId = seqId;
    }

    public long getSeqId() {
        return seqId;
    }

    public void setSeqId(long seqId) {
        this.seqId = seqId;
    }

    @Override
    protected byte[] serializeValue() throws IOException {
        // Odd ID, so value is a byte array containing a varint
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writeVarint(baos, seqId);
        return baos.toByteArray();
    }

    @Override
    public void deserializeValue(ByteBuffer buffer, int length) throws IOException {
        // Odd ID, so read a varint from the buffer
        this.seqId = readVarint(buffer);
    }

    @Override
    public String toString() {
        return "Utf8TextExtension{" +
                "seqId=" + seqId +
                '}';
    }
}
