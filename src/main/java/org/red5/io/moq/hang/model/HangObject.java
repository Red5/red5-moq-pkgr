package org.red5.io.moq.hang.model;

/**
 * Represents a Hang media object as defined in draft-lcurley-moq-hang-01.
 * <p>
 * Hang is a real-time conferencing protocol built on top of moq-lite.
 * The container format is very simple:
 * <ul>
 *   <li>Timestamp: QUIC variable-length integer (62-bit max) in microseconds</li>
 *   <li>Payload: Codec-specific data (Annex-B for H.264 without description, AVCC with description)</li>
 * </ul>
 * </p>
 * <p>
 * Group structure per spec:
 * <ul>
 *   <li>Each group MUST start with a keyframe</li>
 *   <li>Video: one keyframe + zero or more delta frames per group</li>
 *   <li>Audio (no delta frames): group MAY contain multiple keyframes</li>
 * </ul>
 * </p>
 *
 * Reference: draft-lcurley-moq-hang-01
 * https://github.com/kixelated/moq-drafts
 */
public class HangObject {

    /**
     * Media type for the Hang object.
     */
    public enum MediaType {
        VIDEO,
        AUDIO
    }

    /**
     * Codec type for video.
     */
    public enum VideoCodec {
        H264_ANNEXB,   // Annex-B encoding (no description field in catalog)
        H264_AVCC,     // AVCC encoding (with description field in catalog)
        VP8,
        VP9,
        AV1
    }

    /**
     * Codec type for audio.
     */
    public enum AudioCodec {
        OPUS,
        AAC
    }

    private MediaType mediaType;
    private long timestampMicros;  // Timestamp in microseconds
    private byte[] payload;        // Codec-specific payload

    // MOQ Transport identifiers
    private long groupId;
    private long objectId;

    // Video-specific
    private VideoCodec videoCodec;
    private boolean keyframe;

    // Audio-specific
    private AudioCodec audioCodec;

    public HangObject() {
    }

    public HangObject(MediaType mediaType, long timestampMicros, byte[] payload) {
        this.mediaType = mediaType;
        this.timestampMicros = timestampMicros;
        this.payload = payload;
    }

    // Factory methods for convenience

    /**
     * Create a video Hang object.
     *
     * @param timestampMicros timestamp in microseconds
     * @param payload video data (Annex-B or AVCC depending on codec setting)
     * @param keyframe true if this is a keyframe
     * @param codec the video codec
     * @return a new HangObject for video
     */
    public static HangObject createVideo(long timestampMicros, byte[] payload, boolean keyframe, VideoCodec codec) {
        HangObject obj = new HangObject(MediaType.VIDEO, timestampMicros, payload);
        obj.keyframe = keyframe;
        obj.videoCodec = codec;
        return obj;
    }

    /**
     * Create an audio Hang object.
     *
     * @param timestampMicros timestamp in microseconds
     * @param payload audio data
     * @param codec the audio codec
     * @return a new HangObject for audio
     */
    public static HangObject createAudio(long timestampMicros, byte[] payload, AudioCodec codec) {
        HangObject obj = new HangObject(MediaType.AUDIO, timestampMicros, payload);
        obj.audioCodec = codec;
        return obj;
    }

    // Getters and Setters

    public MediaType getMediaType() {
        return mediaType;
    }

    public void setMediaType(MediaType mediaType) {
        this.mediaType = mediaType;
    }

    public long getTimestampMicros() {
        return timestampMicros;
    }

    public void setTimestampMicros(long timestampMicros) {
        this.timestampMicros = timestampMicros;
    }

    /**
     * Get timestamp in milliseconds.
     */
    public long getTimestampMillis() {
        return timestampMicros / 1000;
    }

    /**
     * Set timestamp from milliseconds.
     */
    public void setTimestampMillis(long timestampMillis) {
        this.timestampMicros = timestampMillis * 1000;
    }

    public byte[] getPayload() {
        return payload;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
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

    public VideoCodec getVideoCodec() {
        return videoCodec;
    }

    public void setVideoCodec(VideoCodec videoCodec) {
        this.videoCodec = videoCodec;
    }

    public boolean isKeyframe() {
        return keyframe;
    }

    public void setKeyframe(boolean keyframe) {
        this.keyframe = keyframe;
    }

    public AudioCodec getAudioCodec() {
        return audioCodec;
    }

    public void setAudioCodec(AudioCodec audioCodec) {
        this.audioCodec = audioCodec;
    }

    @Override
    public String toString() {
        return "HangObject{" +
                "mediaType=" + mediaType +
                ", timestampMicros=" + timestampMicros +
                ", payloadSize=" + (payload != null ? payload.length : 0) +
                ", groupId=" + groupId +
                ", objectId=" + objectId +
                (mediaType == MediaType.VIDEO ? ", videoCodec=" + videoCodec + ", keyframe=" + keyframe : "") +
                (mediaType == MediaType.AUDIO ? ", audioCodec=" + audioCodec : "") +
                '}';
    }
}