package org.red5.io.moq.cmaf.model;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Field/Frame Information Box (fiel) - QuickTime File Format / Apple TN2162.
 * Optional video sample description extension that describes interlaced field ordering.
 *
 * <p>Structure:
 * <ul>
 *   <li>fields (1 byte): 1=progressive, 2=interlaced</li>
 *   <li>detail (1 byte): field ordering/storage mode</li>
 * </ul>
 *
 * <p>Detail values per Apple Technical Note TN2162:
 * <ul>
 *   <li>1 = Separated fields, TFF (Top Field First)</li>
 *   <li>6 = Separated fields, BFF (Bottom Field First)</li>
 *   <li>9 = Interleaved fields, TFF</li>
 *   <li>14 = Interleaved fields, BFF</li>
 * </ul>
 *
 * <p>This box appears inside video sample descriptions (avc1, hvc1, av01, etc.)
 * within the stsd box hierarchy: moov/trak/mdia/minf/stbl/stsd/[codec]/fiel
 */
public class FielBox extends Box {

    /** Progressive scan - single field/frame per sample */
    public static final byte FIELDS_PROGRESSIVE = 1;

    /** Interlaced - two fields per sample */
    public static final byte FIELDS_INTERLACED = 2;

    /** Separated fields, Top Field First */
    public static final byte DETAIL_SEPARATED_TFF = 1;

    /** Separated fields, Bottom Field First */
    public static final byte DETAIL_SEPARATED_BFF = 6;

    /** Interleaved fields, Top Field First */
    public static final byte DETAIL_INTERLEAVED_TFF = 9;

    /** Interleaved fields, Bottom Field First */
    public static final byte DETAIL_INTERLEAVED_BFF = 14;

    private byte fieldsPerSample;
    private byte detail;

    public FielBox() {
        super("fiel");
    }

    /**
     * Creates a FielBox with the specified field configuration.
     *
     * @param fieldsPerSample 1 for progressive, 2 for interlaced
     * @param detail field ordering (1, 6, 9, or 14 per TN2162)
     */
    public FielBox(byte fieldsPerSample, byte detail) {
        super("fiel");
        this.fieldsPerSample = fieldsPerSample;
        this.detail = detail;
        this.size = calculateSize();
    }

    /**
     * Creates a FielBox for progressive content.
     *
     * @return FielBox configured for progressive scan
     */
    public static FielBox progressive() {
        return new FielBox(FIELDS_PROGRESSIVE, (byte) 0);
    }

    /**
     * Creates a FielBox for interlaced content with top field first, interleaved storage.
     *
     * @return FielBox configured for TFF interlaced
     */
    public static FielBox interlacedTFF() {
        return new FielBox(FIELDS_INTERLACED, DETAIL_INTERLEAVED_TFF);
    }

    /**
     * Creates a FielBox for interlaced content with bottom field first, interleaved storage.
     *
     * @return FielBox configured for BFF interlaced
     */
    public static FielBox interlacedBFF() {
        return new FielBox(FIELDS_INTERLACED, DETAIL_INTERLEAVED_BFF);
    }

    /**
     * Creates a FielBox for interlaced content with separated fields, top field first.
     *
     * @return FielBox configured for separated TFF
     */
    public static FielBox separatedTFF() {
        return new FielBox(FIELDS_INTERLACED, DETAIL_SEPARATED_TFF);
    }

    /**
     * Creates a FielBox for interlaced content with separated fields, bottom field first.
     *
     * @return FielBox configured for separated BFF
     */
    public static FielBox separatedBFF() {
        return new FielBox(FIELDS_INTERLACED, DETAIL_SEPARATED_BFF);
    }

    public byte getFieldsPerSample() {
        return fieldsPerSample;
    }

    public void setFieldsPerSample(byte fieldsPerSample) {
        this.fieldsPerSample = fieldsPerSample;
    }

    public byte getDetail() {
        return detail;
    }

    public void setDetail(byte detail) {
        this.detail = detail;
    }

    /**
     * @return true if this is progressive (non-interlaced) content
     */
    public boolean isProgressive() {
        return fieldsPerSample == FIELDS_PROGRESSIVE;
    }

    /**
     * @return true if this is interlaced content
     */
    public boolean isInterlaced() {
        return fieldsPerSample == FIELDS_INTERLACED;
    }

    /**
     * @return true if top field is displayed/coded first
     */
    public boolean isTopFieldFirst() {
        return detail == DETAIL_SEPARATED_TFF || detail == DETAIL_INTERLEAVED_TFF;
    }

    /**
     * @return true if bottom field is displayed/coded first
     */
    public boolean isBottomFieldFirst() {
        return detail == DETAIL_SEPARATED_BFF || detail == DETAIL_INTERLEAVED_BFF;
    }

    /**
     * @return true if fields are stored interleaved
     */
    public boolean isInterleaved() {
        return detail == DETAIL_INTERLEAVED_TFF || detail == DETAIL_INTERLEAVED_BFF;
    }

    /**
     * @return true if fields are stored separated
     */
    public boolean isSeparated() {
        return detail == DETAIL_SEPARATED_TFF || detail == DETAIL_SEPARATED_BFF;
    }

    @Override
    protected long calculateSize() {
        // 8 (header) + 1 (fields) + 1 (detail)
        return 10;
    }

    @Override
    public byte[] serialize() throws IOException {
        this.size = calculateSize();
        ByteBuffer buffer = ByteBuffer.allocate((int) size);

        // Write header
        writeHeader(buffer);

        // Write fields per sample
        buffer.put(fieldsPerSample);

        // Write detail (field ordering)
        buffer.put(detail);

        return buffer.array();
    }

    @Override
    public void deserialize(ByteBuffer buffer) throws IOException {
        // Read header
        readHeader(buffer);

        // Read fields per sample
        this.fieldsPerSample = buffer.get();

        // Read detail (field ordering)
        this.detail = buffer.get();
    }

    @Override
    public String toString() {
        String fieldDesc = isProgressive() ? "progressive" : "interlaced";
        String orderDesc = "";
        if (isInterlaced()) {
            orderDesc = ", " + (isTopFieldFirst() ? "TFF" : "BFF") +
                       ", " + (isInterleaved() ? "interleaved" : "separated");
        }
        return "FielBox{" +
                "type='" + type + "', " +
                "size=" + size + ", " +
                "fields=" + fieldsPerSample + " (" + fieldDesc + "), " +
                "detail=" + detail + orderDesc +
                '}';
    }
}
