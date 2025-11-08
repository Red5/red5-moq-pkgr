package org.red5.io.moq.moqmi.model;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Media Type header extension (header extension type = 0x0A).
 *
 * This extension defines the media type inside the object payload and
 * MUST be present in all MoQ-MI objects.
 *
 * Reference: draft-cenzano-moq-media-interop-03 Section 2.4.1
 */
public class MediaTypeExtension extends MoqMIHeaderExtension {

    public static final int EXTENSION_ID = 0x0A;

    private MoqMIObject.MediaType mediaType;

    public MediaTypeExtension() {
        super(EXTENSION_ID);
    }

    public MediaTypeExtension(MoqMIObject.MediaType mediaType) {
        super(EXTENSION_ID);
        this.mediaType = mediaType;
    }

    public MoqMIObject.MediaType getMediaType() {
        return mediaType;
    }

    public void setMediaType(MoqMIObject.MediaType mediaType) {
        this.mediaType = mediaType;
    }

    @Override
    protected byte[] serializeValue() throws IOException {
        // Even ID, so value is a varint
        return serializeVarint(mediaType.getValue());
    }

    @Override
    public void deserializeValue(ByteBuffer buffer, int length) throws IOException {
        // Even ID, so read a varint
        long value = readVarint(buffer);
        this.mediaType = MoqMIObject.MediaType.fromValue((int) value);
    }

    @Override
    public String toString() {
        return "MediaTypeExtension{" +
                "mediaType=" + mediaType +
                '}';
    }
}
