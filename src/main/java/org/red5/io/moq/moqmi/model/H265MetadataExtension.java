package org.red5.io.moq.moqmi.model;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Video H265 in AVCC metadata header extension (header extension type = 0x17).
 *
 * Provides video metadata useful to consume the H265 video carried in the payload.
 *
 * Fields (all varints):
 * - Seq ID: Monotonically increasing counter for this media track
 * - PTS Timestamp: Presentation timestamp in timebase units
 * - DTS Timestamp: Decode timestamp in timebase units (same as PTS if no B-frames)
 * - Timebase: Units used in PTS, DTS, and duration
 * - Duration: Frame duration in timebase units (0 if not set)
 * - Wallclock: EPOCH time in ms when frame started being captured (0 if not set)
 *
 * MUST be present in all objects where media type = Video H265 in HVCC (0x04).
 */
public class H265MetadataExtension extends MoqMIHeaderExtension {

    public static final int EXTENSION_ID = 0x17;

    private long seqId;
    private long ptsTimestamp;
    private long dtsTimestamp;
    private long timebase;
    private long duration;
    private long wallclock;

    public H265MetadataExtension() {
        super(EXTENSION_ID);
    }

    public H265MetadataExtension(long seqId, long ptsTimestamp, long dtsTimestamp,
                                  long timebase, long duration, long wallclock) {
        super(EXTENSION_ID);
        this.seqId = seqId;
        this.ptsTimestamp = ptsTimestamp;
        this.dtsTimestamp = dtsTimestamp;
        this.timebase = timebase;
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

    public long getDtsTimestamp() {
        return dtsTimestamp;
    }

    public void setDtsTimestamp(long dtsTimestamp) {
        this.dtsTimestamp = dtsTimestamp;
    }

    public long getTimebase() {
        return timebase;
    }

    public void setTimebase(long timebase) {
        this.timebase = timebase;
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
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writeVarint(baos, seqId);
        writeVarint(baos, ptsTimestamp);
        writeVarint(baos, dtsTimestamp);
        writeVarint(baos, timebase);
        writeVarint(baos, duration);
        writeVarint(baos, wallclock);
        return baos.toByteArray();
    }

    @Override
    public void deserializeValue(ByteBuffer buffer, int length) throws IOException {
        this.seqId = readVarint(buffer);
        this.ptsTimestamp = readVarint(buffer);
        this.dtsTimestamp = readVarint(buffer);
        this.timebase = readVarint(buffer);
        this.duration = readVarint(buffer);
        this.wallclock = readVarint(buffer);
    }

    @Override
    public String toString() {
        return "H265MetadataExtension{" +
                "seqId=" + seqId +
                ", ptsTimestamp=" + ptsTimestamp +
                ", dtsTimestamp=" + dtsTimestamp +
                ", timebase=" + timebase +
                ", duration=" + duration +
                ", wallclock=" + wallclock +
                '}';
    }
}
