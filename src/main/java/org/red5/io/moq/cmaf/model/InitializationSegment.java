package org.red5.io.moq.cmaf.model;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * ISO BMFF boxes for CMAF Initialization Segment.
 *
 * An initialization segment contains:
 * - ftyp (File Type Box)
 * - moov (Movie Box)
 *   - mvhd (Movie Header Box)
 *   - trak (Track Box) - one or more
 *     - tkhd (Track Header Box)
 *     - mdia (Media Box)
 *       - mdhd (Media Header Box)
 *       - hdlr (Handler Reference Box)
 *       - minf (Media Information Box)
 *         - vmhd/smhd (Video/Sound Media Header Box)
 *         - dinf (Data Information Box)
 *         - stbl (Sample Table Box)
 *           - stsd (Sample Description Box)
 *           - stts, stsc, stsz, stco (placeholder boxes)
 *   - mvex (Movie Extends Box) - required for fragmented MP4
 *     - trex (Track Extends Box) - one per track, default sample values
 */
public class InitializationSegment {

    /**
     * File Type Box (ftyp) - ISO/IEC 14496-12 Section 4.3
     * Similar to styp but used for initialization segments.
     */
    public static class FtypBox extends Box {
        private String majorBrand;
        private long minorVersion;
        private List<String> compatibleBrands;

        public FtypBox() {
            super("ftyp");
            this.compatibleBrands = new ArrayList<>();
        }

        public FtypBox(String majorBrand, long minorVersion, List<String> compatibleBrands) {
            super("ftyp");
            this.majorBrand = majorBrand;
            this.minorVersion = minorVersion;
            this.compatibleBrands = compatibleBrands != null ? compatibleBrands : new ArrayList<>();
        }

        public String getMajorBrand() {
            return majorBrand;
        }

        public void setMajorBrand(String majorBrand) {
            this.majorBrand = majorBrand;
        }

        public long getMinorVersion() {
            return minorVersion;
        }

        public void setMinorVersion(long minorVersion) {
            this.minorVersion = minorVersion;
        }

        public List<String> getCompatibleBrands() {
            return compatibleBrands;
        }

        public void setCompatibleBrands(List<String> compatibleBrands) {
            this.compatibleBrands = compatibleBrands;
        }

        @Override
        protected long calculateSize() {
            return 8 + 4 + 4 + (compatibleBrands.size() * 4L);
        }

        @Override
        public byte[] serialize() throws IOException {
            this.size = calculateSize();
            ByteBuffer buffer = ByteBuffer.allocate((int) size);
            writeHeader(buffer);
            buffer.put(majorBrand.getBytes(StandardCharsets.UTF_8), 0, 4);
            buffer.putInt((int) minorVersion);
            for (String brand : compatibleBrands) {
                buffer.put(brand.getBytes(StandardCharsets.UTF_8), 0, 4);
            }
            return buffer.array();
        }

        @Override
        public void deserialize(ByteBuffer buffer) throws IOException {
            long boxSize = readHeader(buffer);
            byte[] brandBytes = new byte[4];
            buffer.get(brandBytes);
            this.majorBrand = new String(brandBytes, StandardCharsets.UTF_8);
            this.minorVersion = Integer.toUnsignedLong(buffer.getInt());

            compatibleBrands = new ArrayList<>();
            int remaining = (int) (boxSize - 16); // boxSize - header - major_brand - minor_version
            while (remaining >= 4) {
                buffer.get(brandBytes);
                compatibleBrands.add(new String(brandBytes, StandardCharsets.UTF_8));
                remaining -= 4;
            }
        }

        @Override
        public String toString() {
            return String.format("FtypBox{majorBrand='%s', minorVersion=%d, compatibleBrands=%s}",
                    majorBrand, minorVersion, compatibleBrands);
        }
    }

    /**
     * Movie Header Box (mvhd) - ISO/IEC 14496-12 Section 8.2.2
     */
    public static class MvhdBox extends Box {
        private int version;
        private int flags;
        private long creationTime;
        private long modificationTime;
        private long timescale;
        private long duration;
        private int rate = 0x00010000; // 1.0
        private int volume = 0x0100; // 1.0
        private int[] matrix = new int[9]; // transformation matrix
        private long nextTrackId;

        public MvhdBox() {
            super("mvhd");
            // Default identity matrix
            matrix[0] = 0x00010000; // a
            matrix[4] = 0x00010000; // e
            matrix[8] = 0x40000000; // i
        }

        public int getVersion() {
            return version;
        }

        public void setVersion(int version) {
            this.version = version;
        }

        public long getTimescale() {
            return timescale;
        }

        public void setTimescale(long timescale) {
            this.timescale = timescale;
        }

        public long getDuration() {
            return duration;
        }

        public void setDuration(long duration) {
            this.duration = duration;
        }

        public long getNextTrackId() {
            return nextTrackId;
        }

        public void setNextTrackId(long nextTrackId) {
            this.nextTrackId = nextTrackId;
        }

        @Override
        protected long calculateSize() {
            return version == 1 ? 120 : 108;
        }

        @Override
        public byte[] serialize() throws IOException {
            this.size = calculateSize();
            ByteBuffer buffer = ByteBuffer.allocate((int) size);
            writeHeader(buffer);

            int versionFlags = (version << 24) | (flags & 0xFFFFFF);
            buffer.putInt(versionFlags);

            if (version == 1) {
                buffer.putLong(creationTime);
                buffer.putLong(modificationTime);
                buffer.putInt((int) timescale);
                buffer.putLong(duration);
            } else {
                buffer.putInt((int) creationTime);
                buffer.putInt((int) modificationTime);
                buffer.putInt((int) timescale);
                buffer.putInt((int) duration);
            }

            buffer.putInt(rate);
            buffer.putShort((short) volume);
            buffer.putShort((short) 0); // reserved
            buffer.putInt(0); // reserved
            buffer.putInt(0); // reserved

            for (int m : matrix) {
                buffer.putInt(m);
            }

            // pre_defined[6]
            for (int i = 0; i < 6; i++) {
                buffer.putInt(0);
            }

            buffer.putInt((int) nextTrackId);

            return buffer.array();
        }

        @Override
        public void deserialize(ByteBuffer buffer) throws IOException {
            readHeader(buffer);

            int versionFlags = buffer.getInt();
            this.version = (versionFlags >> 24) & 0xFF;
            this.flags = versionFlags & 0xFFFFFF;

            if (version == 1) {
                this.creationTime = buffer.getLong();
                this.modificationTime = buffer.getLong();
                this.timescale = Integer.toUnsignedLong(buffer.getInt());
                this.duration = buffer.getLong();
            } else {
                this.creationTime = Integer.toUnsignedLong(buffer.getInt());
                this.modificationTime = Integer.toUnsignedLong(buffer.getInt());
                this.timescale = Integer.toUnsignedLong(buffer.getInt());
                this.duration = Integer.toUnsignedLong(buffer.getInt());
            }

            this.rate = buffer.getInt();
            this.volume = buffer.getShort() & 0xFFFF;
            buffer.getShort(); // reserved
            buffer.getInt(); // reserved
            buffer.getInt(); // reserved

            for (int i = 0; i < 9; i++) {
                matrix[i] = buffer.getInt();
            }

            // pre_defined[6]
            for (int i = 0; i < 6; i++) {
                buffer.getInt();
            }

            this.nextTrackId = Integer.toUnsignedLong(buffer.getInt());
        }

        @Override
        public String toString() {
            return String.format("MvhdBox{timescale=%d, duration=%d, nextTrackId=%d}",
                    timescale, duration, nextTrackId);
        }
    }

    /**
     * Handler Reference Box (hdlr) - ISO/IEC 14496-12 Section 8.4.3
     */
    public static class HdlrBox extends Box {
        private int version;
        private int flags;
        private String handlerType;
        private String name;

        public HdlrBox() {
            super("hdlr");
        }

        public HdlrBox(String handlerType, String name) {
            super("hdlr");
            this.handlerType = handlerType;
            this.name = name;
        }

        public String getHandlerType() {
            return handlerType;
        }

        public void setHandlerType(String handlerType) {
            this.handlerType = handlerType;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        protected long calculateSize() {
            int nameLength = (name != null ? name.getBytes(StandardCharsets.UTF_8).length : 0) + 1; // null-terminated
            return 8 + 4 + 4 + 4 + 4 + 4 + 4 + nameLength; // header + version/flags + pre_defined + handlerType + 3x reserved + name
        }

        @Override
        public byte[] serialize() throws IOException {
            this.size = calculateSize();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            ByteBuffer header = ByteBuffer.allocate(32);
            writeHeader(header);
            header.putInt((version << 24) | (flags & 0xFFFFFF));
            header.putInt(0); // pre_defined
            header.put(handlerType.getBytes(StandardCharsets.UTF_8), 0, 4);
            header.putInt(0); // reserved
            header.putInt(0); // reserved
            header.putInt(0); // reserved
            baos.write(header.array(), 0, header.position());

            if (name != null) {
                baos.write(name.getBytes(StandardCharsets.UTF_8));
            }
            baos.write(0); // null terminator

            return baos.toByteArray();
        }

        @Override
        public void deserialize(ByteBuffer buffer) throws IOException {
            long boxSize = readHeader(buffer);

            int versionFlags = buffer.getInt();
            this.version = (versionFlags >> 24) & 0xFF;
            this.flags = versionFlags & 0xFFFFFF;

            buffer.getInt(); // pre_defined

            byte[] handlerBytes = new byte[4];
            buffer.get(handlerBytes);
            this.handlerType = new String(handlerBytes, StandardCharsets.UTF_8);

            buffer.getInt(); // reserved
            buffer.getInt(); // reserved
            buffer.getInt(); // reserved

            // Read name (null-terminated string)
            int nameLength = (int) (boxSize - 32);
            if (nameLength > 0) {
                byte[] nameBytes = new byte[nameLength];
                buffer.get(nameBytes);
                // Remove null terminator if present
                int len = nameLength;
                if (nameBytes[nameLength - 1] == 0) {
                    len--;
                }
                if (len > 0) {
                    this.name = new String(nameBytes, 0, len, StandardCharsets.UTF_8);
                }
            }
        }

        @Override
        public String toString() {
            return String.format("HdlrBox{handlerType='%s', name='%s'}", handlerType, name);
        }
    }

    /**
     * Media Header Box (mdhd) - ISO/IEC 14496-12 Section 8.4.2
     */
    public static class MdhdBox extends Box {
        private int version;
        private int flags;
        private long creationTime;
        private long modificationTime;
        private long timescale;
        private long duration;
        private int language; // ISO-639-2/T language code

        public MdhdBox() {
            super("mdhd");
            this.language = 0x55C4; // 'und' (undefined)
        }

        public int getVersion() {
            return version;
        }

        public void setVersion(int version) {
            this.version = version;
        }

        public long getTimescale() {
            return timescale;
        }

        public void setTimescale(long timescale) {
            this.timescale = timescale;
        }

        public long getDuration() {
            return duration;
        }

        public void setDuration(long duration) {
            this.duration = duration;
        }

        public int getLanguage() {
            return language;
        }

        public void setLanguage(int language) {
            this.language = language;
        }

        @Override
        protected long calculateSize() {
            return version == 1 ? 44 : 32;
        }

        @Override
        public byte[] serialize() throws IOException {
            this.size = calculateSize();
            ByteBuffer buffer = ByteBuffer.allocate((int) size);
            writeHeader(buffer);

            int versionFlags = (version << 24) | (flags & 0xFFFFFF);
            buffer.putInt(versionFlags);

            if (version == 1) {
                buffer.putLong(creationTime);
                buffer.putLong(modificationTime);
                buffer.putInt((int) timescale);
                buffer.putLong(duration);
            } else {
                buffer.putInt((int) creationTime);
                buffer.putInt((int) modificationTime);
                buffer.putInt((int) timescale);
                buffer.putInt((int) duration);
            }

            buffer.putShort((short) language);
            buffer.putShort((short) 0); // pre_defined

            return buffer.array();
        }

        @Override
        public void deserialize(ByteBuffer buffer) throws IOException {
            readHeader(buffer);

            int versionFlags = buffer.getInt();
            this.version = (versionFlags >> 24) & 0xFF;
            this.flags = versionFlags & 0xFFFFFF;

            if (version == 1) {
                this.creationTime = buffer.getLong();
                this.modificationTime = buffer.getLong();
                this.timescale = Integer.toUnsignedLong(buffer.getInt());
                this.duration = buffer.getLong();
            } else {
                this.creationTime = Integer.toUnsignedLong(buffer.getInt());
                this.modificationTime = Integer.toUnsignedLong(buffer.getInt());
                this.timescale = Integer.toUnsignedLong(buffer.getInt());
                this.duration = Integer.toUnsignedLong(buffer.getInt());
            }

            this.language = buffer.getShort() & 0xFFFF;
            buffer.getShort(); // pre_defined
        }

        @Override
        public String toString() {
            return String.format("MdhdBox{timescale=%d, duration=%d}", timescale, duration);
        }
    }

    /**
     * Video Media Header Box (vmhd) - ISO/IEC 14496-12 Section 12.1.2
     */
    public static class VmhdBox extends Box {
        private int version;
        private int flags = 1; // Must be 1

        public VmhdBox() {
            super("vmhd");
        }

        @Override
        protected long calculateSize() {
            return 20; // 8 + 4 + 2 + 2 + 2 + 2
        }

        @Override
        public byte[] serialize() throws IOException {
            this.size = calculateSize();
            ByteBuffer buffer = ByteBuffer.allocate((int) size);
            writeHeader(buffer);
            buffer.putInt((version << 24) | 1); // flags must be 1
            buffer.putShort((short) 0); // graphicsmode
            buffer.putShort((short) 0); // opcolor[0]
            buffer.putShort((short) 0); // opcolor[1]
            buffer.putShort((short) 0); // opcolor[2]
            return buffer.array();
        }

        @Override
        public void deserialize(ByteBuffer buffer) throws IOException {
            readHeader(buffer);
            int versionFlags = buffer.getInt();
            this.version = (versionFlags >> 24) & 0xFF;
            this.flags = versionFlags & 0xFFFFFF;
            buffer.getShort(); // graphicsmode
            buffer.getShort(); // opcolor[0]
            buffer.getShort(); // opcolor[1]
            buffer.getShort(); // opcolor[2]
        }
    }

    /**
     * Sound Media Header Box (smhd) - ISO/IEC 14496-12 Section 12.2.2
     */
    public static class SmhdBox extends Box {
        private int version;
        private int flags;
        private int balance; // Fixed point 8.8

        public SmhdBox() {
            super("smhd");
        }

        public int getBalance() {
            return balance;
        }

        public void setBalance(int balance) {
            this.balance = balance;
        }

        @Override
        protected long calculateSize() {
            return 16; // 8 + 4 + 2 + 2
        }

        @Override
        public byte[] serialize() throws IOException {
            this.size = calculateSize();
            ByteBuffer buffer = ByteBuffer.allocate((int) size);
            writeHeader(buffer);
            buffer.putInt((version << 24) | (flags & 0xFFFFFF));
            buffer.putShort((short) balance);
            buffer.putShort((short) 0); // reserved
            return buffer.array();
        }

        @Override
        public void deserialize(ByteBuffer buffer) throws IOException {
            readHeader(buffer);
            int versionFlags = buffer.getInt();
            this.version = (versionFlags >> 24) & 0xFF;
            this.flags = versionFlags & 0xFFFFFF;
            this.balance = buffer.getShort();
            buffer.getShort(); // reserved
        }
    }

    /**
     * Data Reference Box (dref) - ISO/IEC 14496-12 Section 8.7.2
     * Contains URL or URN entries. For self-contained files, uses 'url ' with flag=1.
     */
    public static class DrefBox extends Box {
        private int version;
        private int flags;
        private int entryCount = 1;

        public DrefBox() {
            super("dref");
        }

        public int getEntryCount() {
            return entryCount;
        }

        @Override
        protected long calculateSize() {
            // Contains one self-referencing url box
            return 8 + 4 + 4 + 12; // header + version/flags + entry_count + url box
        }

        @Override
        public byte[] serialize() throws IOException {
            this.size = calculateSize();
            ByteBuffer buffer = ByteBuffer.allocate((int) size);
            writeHeader(buffer);
            buffer.putInt((version << 24) | (flags & 0xFFFFFF));
            buffer.putInt(entryCount);

            // Write self-referencing 'url ' box
            buffer.putInt(12); // size
            buffer.put("url ".getBytes(StandardCharsets.UTF_8));
            buffer.putInt(0x000001); // version=0, flags=1 (self-reference)

            return buffer.array();
        }

        @Override
        public void deserialize(ByteBuffer buffer) throws IOException {
            readHeader(buffer);
            int versionFlags = buffer.getInt();
            this.version = (versionFlags >> 24) & 0xFF;
            this.flags = versionFlags & 0xFFFFFF;
            this.entryCount = buffer.getInt();

            // Skip the url box for now
            if (buffer.remaining() >= 12) {
                buffer.position(buffer.position() + 12);
            }
        }
    }
}
