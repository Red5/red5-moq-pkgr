package org.red5.io.moq.moqmi.model;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Audio Opus bitstream data header extension (header extension type = 0x0F).
 *
 * Provides audio metadata useful to consume the Opus audio carried in the payload.
 *
 * Fields (all varints):
 * - Seq ID: Monotonically increasing counter for this media track
 * - PTS Timestamp: Presentation timestamp in timebase units
 * - Timebase: Units used in PTS and duration
 * - Sample Freq: Sample frequency of original signal (before encoding)
 * - Num Channels: Number of channels in original signal (before encoding)
 * - Duration: Frame duration in timebase units (0 if not set)
 * - Wallclock: EPOCH time in ms when frame started being captured (0 if not set)
 *
 * MUST be present in all objects where media type = Audio Opus bitstream (0x1).
 *
 * Reference: draft-cenzano-moq-media-interop-03 Section 2.4.4
 */
public class OpusDataExtension extends MoqMIHeaderExtension {

    public static final int EXTENSION_ID = 0x0F;

    private long seqId;
    private long ptsTimestamp;
    private long timebase;
    private long sampleFreq;
    private long numChannels;
    private long duration;
    private long wallclock;

    public OpusDataExtension() {
        super(EXTENSION_ID);
    }

    public OpusDataExtension(long seqId, long ptsTimestamp, long timebase,
                             long sampleFreq, long numChannels, long duration, long wallclock) {
        super(EXTENSION_ID);
        this.seqId = seqId;
        this.ptsTimestamp = ptsTimestamp;
        this.timebase = timebase;
        this.sampleFreq = sampleFreq;
        this.numChannels = numChannels;
        this.duration = duration;
        this.wallclock = wallclock;
    }

    public long getSeqId() {
        return seqId;
    }

    public void setSeqId(long seqId) {
        this.seqId = seqId;
    }

    public long getPtsTimestamp() {
        return ptsTimestamp;
    }

    public void setPtsTimestamp(long ptsTimestamp) {
        this.ptsTimestamp = ptsTimestamp;
    }

    public long getTimebase() {
        return timebase;
    }

    public void setTimebase(long timebase) {
        this.timebase = timebase;
    }

    public long getSampleFreq() {
        return sampleFreq;
    }

    public void setSampleFreq(long sampleFreq) {
        this.sampleFreq = sampleFreq;
    }

    public long getNumChannels() {
        return numChannels;
    }

    public void setNumChannels(long numChannels) {
        this.numChannels = numChannels;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public long getWallclock() {
        return wallclock;
    }

    public void setWallclock(long wallclock) {
        this.wallclock = wallclock;
    }

    @Override
    protected byte[] serializeValue() throws IOException {
        // Odd ID, so value is a byte array containing multiple varints
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writeVarint(baos, seqId);
        writeVarint(baos, ptsTimestamp);
        writeVarint(baos, timebase);
        writeVarint(baos, sampleFreq);
        writeVarint(baos, numChannels);
        writeVarint(baos, duration);
        writeVarint(baos, wallclock);
        return baos.toByteArray();
    }

    @Override
    public void deserializeValue(ByteBuffer buffer, int length) throws IOException {
        // Odd ID, so read multiple varints from the buffer
        this.seqId = readVarint(buffer);
        this.ptsTimestamp = readVarint(buffer);
        this.timebase = readVarint(buffer);
        this.sampleFreq = readVarint(buffer);
        this.numChannels = readVarint(buffer);
        this.duration = readVarint(buffer);
        this.wallclock = readVarint(buffer);
    }

    @Override
    public String toString() {
        return "OpusDataExtension{" +
                "seqId=" + seqId +
                ", ptsTimestamp=" + ptsTimestamp +
                ", timebase=" + timebase +
                ", sampleFreq=" + sampleFreq +
                ", numChannels=" + numChannels +
                ", duration=" + duration +
                ", wallclock=" + wallclock +
                '}';
    }
}
