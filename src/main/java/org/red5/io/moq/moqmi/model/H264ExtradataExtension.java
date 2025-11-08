package org.red5.io.moq.moqmi.model;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Video H264 in AVCC extradata header extension (header extension type = 0x0D).
 *
 * Provides extradata (decoder configuration) needed to start decoding the video stream.
 *
 * The extradata MUST be AVCDecoderConfigurationRecord as described in
 * ISO14496-15:2019 section 5.3.3.1, with lengthSizeMinusOne = 3 (length = 4).
 *
 * MUST be present in object 0 (start of group) where media type = Video H264 in AVCC (0x0)
 * AND there has been an update on the encoding parameters (or at the start of the stream).
 *
 * Reference: draft-cenzano-moq-media-interop-03 Section 2.4.3
 */
public class H264ExtradataExtension extends MoqMIHeaderExtension {

    public static final int EXTENSION_ID = 0x0D;

    private byte[] extradata;

    public H264ExtradataExtension() {
        super(EXTENSION_ID);
    }

    public H264ExtradataExtension(byte[] extradata) {
        super(EXTENSION_ID);
        this.extradata = extradata;
    }

    public byte[] getExtradata() {
        return extradata;
    }

    public void setExtradata(byte[] extradata) {
        this.extradata = extradata;
    }

    @Override
    protected byte[] serializeValue() throws IOException {
        // Odd ID, so value is the raw byte array
        return extradata != null ? extradata : new byte[0];
    }

    @Override
    public void deserializeValue(ByteBuffer buffer, int length) throws IOException {
        // Odd ID, so read 'length' bytes
        this.extradata = new byte[length];
        buffer.get(this.extradata);
    }

    @Override
    public String toString() {
        return "H264ExtradataExtension{" +
                "extradataLength=" + (extradata != null ? extradata.length : 0) +
                '}';
    }
}
