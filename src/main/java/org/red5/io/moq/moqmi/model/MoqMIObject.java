package org.red5.io.moq.moqmi.model;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a MoQ Media Interop Object for MoQ Transport.
 *
 * MoQ-MI builds on top of LOC (Low Overhead Media Container) and defines specific
 * header extensions for media interoperability across different implementations.
 *
 * A MoQ-MI Object consists of:
 * - MoQ-MI Header Extensions (metadata including media type and codec-specific data)
 * - Payload (encoded audio/video/text data in codec-specific format)
 *
 * Reference: draft-cenzano-moq-media-interop-03
 * https://datatracker.ietf.org/doc/html/draft-cenzano-moq-media-interop-03
 */
public class MoqMIObject {

    /**
     * Media type enumeration as defined in the spec.
     */
    public enum MediaType {
        VIDEO_H264_AVCC(0x00),
        AUDIO_OPUS(0x01),
        TEXT_UTF8(0x02),
        AUDIO_AAC_LC(0x03),
        VIDEO_H265_HVCC(0x04);

        private final int value;

        MediaType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static MediaType fromValue(int value) {
            for (MediaType type : values()) {
                if (type.value == value) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown media type: " + value);
        }
    }

    private MediaType mediaType;
    private byte[] payload;
    private List<MoqMIHeaderExtension> headerExtensions;

    // MOQ Transport identifiers
    private long groupId;
    private long objectId;
    private long subgroupId;

    public MoqMIObject() {
        this.headerExtensions = new ArrayList<>();
    }

    public MoqMIObject(MediaType mediaType, byte[] payload) {
        this();
        this.mediaType = mediaType;
        this.payload = payload;
    }

    // Getters and Setters

    public MediaType getMediaType() {
        return mediaType;
    }

    public void setMediaType(MediaType mediaType) {
        this.mediaType = mediaType;
    }

    public byte[] getPayload() {
        return payload;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    public List<MoqMIHeaderExtension> getHeaderExtensions() {
        return headerExtensions;
    }

    public void setHeaderExtensions(List<MoqMIHeaderExtension> headerExtensions) {
        this.headerExtensions = headerExtensions;
    }

    public void addHeaderExtension(MoqMIHeaderExtension extension) {
        this.headerExtensions.add(extension);
    }

    public long getGroupId() {
        return groupId;
    }

    public void setGroupId(long groupId) {
        this.groupId = groupId;
    }

    public long getObjectId() {
        return objectId;
    }

    public void setObjectId(long objectId) {
        this.objectId = objectId;
    }

    public long getSubgroupId() {
        return subgroupId;
    }

    public void setSubgroupId(long subgroupId) {
        this.subgroupId = subgroupId;
    }

    /**
     * Serialize header extensions to bytes.
     */
    public byte[] serializeHeaderExtensions() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        for (MoqMIHeaderExtension extension : headerExtensions) {
            byte[] extBytes = extension.serialize();
            baos.write(extBytes);
        }

        return baos.toByteArray();
    }

    /**
     * Get a specific header extension by type.
     */
    @SuppressWarnings("unchecked")
    public <T extends MoqMIHeaderExtension> T getHeaderExtension(Class<T> extensionClass) {
        for (MoqMIHeaderExtension extension : headerExtensions) {
            if (extensionClass.isInstance(extension)) {
                return (T) extension;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "MoqMIObject{" +
                "mediaType=" + mediaType +
                ", groupId=" + groupId +
                ", objectId=" + objectId +
                ", subgroupId=" + subgroupId +
                ", headerExtensions=" + headerExtensions.size() +
                ", payloadSize=" + (payload != null ? payload.length : 0) +
                '}';
    }
}
