package org.red5.io.moq.moqmi.deserialize;

import org.red5.io.moq.moqmi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Deserializes MoQ Media Interop objects from byte arrays.
 *
 * The input format follows draft-cenzano-moq-media-interop-03:
 * - MoQ-MI Header Extensions (metadata including media type)
 * - Payload (encoded media data in codec-specific format)
 *
 * Reference: draft-cenzano-moq-media-interop-03
 */
public class MoqMIDeserializer {

    private static final Logger logger = LoggerFactory.getLogger(MoqMIDeserializer.class);

    /**
     * Deserialize a MoQ-MI object from a byte array.
     *
     * Note: In practice, the MOQ transport layer would provide separate buffers for
     * header extensions and payload.
     *
     * @param headerExtensionBytes the header extension bytes
     * @param payloadBytes the payload bytes
     * @return deserialized MoQ-MI object
     * @throws IOException if deserialization fails
     */
    public MoqMIObject deserialize(byte[] headerExtensionBytes, byte[] payloadBytes) throws IOException {
        logger.debug("Deserializing MoQ-MI object: headerExtensionBytes={}, payloadBytes={}",
                headerExtensionBytes != null ? headerExtensionBytes.length : 0,
                payloadBytes != null ? payloadBytes.length : 0);

        MoqMIObject obj = new MoqMIObject();

        // Deserialize header extensions
        if (headerExtensionBytes != null && headerExtensionBytes.length > 0) {
            deserializeHeaderExtensions(headerExtensionBytes, obj);
        }

        // Determine media type from extensions
        MediaTypeExtension mediaTypeExt = obj.getHeaderExtension(MediaTypeExtension.class);
        if (mediaTypeExt != null) {
            obj.setMediaType(mediaTypeExt.getMediaType());
        } else {
            logger.warn("No media type extension found in MoQ-MI object");
        }

        // Set payload
        obj.setPayload(payloadBytes);

        logger.debug("Deserialized MoQ-MI object with {} header extensions, mediaType={}",
                obj.getHeaderExtensions().size(), obj.getMediaType());

        return obj;
    }

    /**
     * Deserialize header extensions from a byte array.
     *
     * @param data the header extension bytes
     * @param obj the MoQ-MI object to populate
     * @throws IOException if deserialization fails
     */
    private void deserializeHeaderExtensions(byte[] data, MoqMIObject obj) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(data);

        while (buffer.hasRemaining()) {
            // Read extension ID
            long extensionId = MoqMIHeaderExtension.readVarint(buffer);

            logger.debug("Reading extension ID: 0x{}", Long.toHexString(extensionId));

            // Determine if this is a varint value (even ID) or byte array (odd ID)
            boolean isVarintValue = (extensionId % 2) == 0;

            int length = 0;
            if (!isVarintValue) {
                // Odd ID: read length
                length = (int) MoqMIHeaderExtension.readVarint(buffer);
                logger.debug("Extension length: {}", length);
            }

            // Create and deserialize the appropriate extension
            MoqMIHeaderExtension extension = createExtension((int) extensionId);
            if (extension != null) {
                extension.deserializeValue(buffer, length);
                obj.addHeaderExtension(extension);
                logger.debug("Deserialized extension: {}", extension);
            } else {
                // Unknown extension, skip it
                logger.warn("Unknown extension ID: 0x{}, skipping", Long.toHexString(extensionId));
                if (!isVarintValue) {
                    // Skip the value bytes
                    if (buffer.remaining() >= length) {
                        buffer.position(buffer.position() + length);
                    } else {
                        throw new IOException("Not enough bytes to skip unknown extension");
                    }
                } else {
                    // Skip the varint value
                    MoqMIHeaderExtension.readVarint(buffer);
                }
            }
        }
    }

    /**
     * Create an extension instance based on the extension ID.
     *
     * @param extensionId the extension ID
     * @return new extension instance, or null if unknown
     */
    private MoqMIHeaderExtension createExtension(int extensionId) {
        return switch (extensionId) {
            case MediaTypeExtension.EXTENSION_ID -> new MediaTypeExtension();
            case H264MetadataExtension.EXTENSION_ID -> new H264MetadataExtension();
            case H264ExtradataExtension.EXTENSION_ID -> new H264ExtradataExtension();
            case OpusDataExtension.EXTENSION_ID -> new OpusDataExtension();
            case Utf8TextExtension.EXTENSION_ID -> new Utf8TextExtension();
            case AacLcDataExtension.EXTENSION_ID -> new AacLcDataExtension();
            default -> null;
        };
    }
}
