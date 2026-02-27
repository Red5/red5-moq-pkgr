package org.red5.io.moq.moqmi.model;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Video H265 in AVCC extradata header extension (header extension type = 0x19).
 *
 * Provides extradata (decoder configuration) needed to start decoding the video stream.
 *
 * The extradata MUST be HEVCDecoderConfigurationRecord as described in
 * ISO14496-15:2019 section 8.3.3.1.2, with lengthSizeMinusOne = 3 (length = 4).
 *
 * MUST be present in object 0 (start of group) where media type = Video H265 in HVCC (0x04)
 * AND there has been an update on the encoding parameters (or at the start of the stream).
 */
public class H265ExtradataExtension extends MoqMIHeaderExtension {

    public static final int EXTENSION_ID = 0x19;

    private byte[] extradata;

    public H265ExtradataExtension() {
        super(EXTENSION_ID);
    }

    public H265ExtradataExtension(byte[] extradata) {
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
        return extradata != null ? extradata : new byte[0];
    }

    @Override
    public void deserializeValue(ByteBuffer buffer, int length) throws IOException {
        this.extradata = new byte[length];
        buffer.get(this.extradata);
    }

    @Override
    public String toString() {
        return "H265ExtradataExtension{" +
                "extradataLength=" + (extradata != null ? extradata.length : 0) +
                '}';
    }
}
