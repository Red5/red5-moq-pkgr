package org.red5.io.moq.cmaf.model;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Movie Box (moov) - ISO/IEC 14496-12 Section 8.2.1
 *
 * Container for all metadata about a presentation.
 * Part of the initialization segment in CMAF.
 *
 * Structure:
 * - mvhd (Movie Header Box)
 * - trak (Track Box) - one or more
 */
public class MoovBox extends Box {
    private InitializationSegment.MvhdBox mvhd;
    private List<TrakBox> traks;
    private MvexBox mvex;

    public MoovBox() {
        super("moov");
        this.traks = new ArrayList<>();
    }

    public InitializationSegment.MvhdBox getMvhd() {
        return mvhd;
    }

    public void setMvhd(InitializationSegment.MvhdBox mvhd) {
        this.mvhd = mvhd;
    }

    public List<TrakBox> getTraks() {
        return traks;
    }

    public void setTraks(List<TrakBox> traks) {
        this.traks = traks;
    }

    public void addTrak(TrakBox trak) {
        this.traks.add(trak);
    }

    public MvexBox getMvex() {
        return mvex;
    }

    public void setMvex(MvexBox mvex) {
        this.mvex = mvex;
    }

    @Override
    protected long calculateSize() {
        long size = 8; // header
        if (mvhd != null) {
            size += mvhd.calculateSize();
        }
        for (TrakBox trak : traks) {
            size += trak.calculateSize();
        }
        if (mvex != null) {
            size += mvex.calculateSize();
        }
        return size;
    }

    @Override
    public byte[] serialize() throws IOException {
        this.size = calculateSize();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        ByteBuffer header = ByteBuffer.allocate(8);
        writeHeader(header);
        baos.write(header.array());

        if (mvhd != null) {
            baos.write(mvhd.serialize());
        }

        for (TrakBox trak : traks) {
            baos.write(trak.serialize());
        }

        if (mvex != null) {
            baos.write(mvex.serialize());
        }

        return baos.toByteArray();
    }

    @Override
    public void deserialize(ByteBuffer buffer) throws IOException {
        long boxSize = readHeader(buffer);
        int startPosition = buffer.position();
        int endPosition = (int) (startPosition + boxSize - 8);

        while (buffer.position() < endPosition && buffer.remaining() >= 8) {
            int positionBefore = buffer.position();

            int childSize = buffer.getInt();
            byte[] typeBytes = new byte[4];
            buffer.get(typeBytes);
            String childType = new String(typeBytes);

            if (childSize < 8 || childSize > buffer.capacity()) {
                throw new IOException("Invalid child box size in MoovBox: " + childSize);
            }

            buffer.position(positionBefore);

            switch (childType) {
                case "mvhd" -> {
                    mvhd = new InitializationSegment.MvhdBox();
                    mvhd.deserialize(buffer);
                }
                case "trak" -> {
                    TrakBox trak = new TrakBox();
                    trak.deserialize(buffer);
                    traks.add(trak);
                }
                case "mvex" -> {
                    mvex = new MvexBox();
                    mvex.deserialize(buffer);
                }
                default -> buffer.position(positionBefore + childSize);
            }

            if (buffer.position() <= positionBefore) {
                throw new IOException("Buffer position did not advance for MoovBox child: " + childType);
            }
        }
    }

    @Override
    public String toString() {
        return String.format("MoovBox{mvhd=%s, traks=%d}", mvhd, traks.size());
    }

    /**
     * Track Box (trak) - ISO/IEC 14496-12 Section 8.3.1
     *
     * Contains all information about a single track.
     *
     * Structure:
     * - tkhd (Track Header Box)
     * - mdia (Media Box)
     */
    public static class TrakBox extends Box {
        private TrackMetadata.TkhdBox tkhd;
        private MdiaBox mdia;

        public TrakBox() {
            super("trak");
        }

        public TrackMetadata.TkhdBox getTkhd() {
            return tkhd;
        }

        public void setTkhd(TrackMetadata.TkhdBox tkhd) {
            this.tkhd = tkhd;
        }

        public MdiaBox getMdia() {
            return mdia;
        }

        public void setMdia(MdiaBox mdia) {
            this.mdia = mdia;
        }

        @Override
        protected long calculateSize() {
            long size = 8; // header
            if (tkhd != null) {
                size += tkhd.calculateSize();
            }
            if (mdia != null) {
                size += mdia.calculateSize();
            }
            return size;
        }

        @Override
        public byte[] serialize() throws IOException {
            this.size = calculateSize();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            ByteBuffer header = ByteBuffer.allocate(8);
            writeHeader(header);
            baos.write(header.array());

            if (tkhd != null) {
                baos.write(tkhd.serialize());
            }
            if (mdia != null) {
                baos.write(mdia.serialize());
            }

            return baos.toByteArray();
        }

        @Override
        public void deserialize(ByteBuffer buffer) throws IOException {
            long boxSize = readHeader(buffer);
            int startPosition = buffer.position();
            int endPosition = (int) (startPosition + boxSize - 8);

            while (buffer.position() < endPosition && buffer.remaining() >= 8) {
                int positionBefore = buffer.position();

                int childSize = buffer.getInt();
                byte[] typeBytes = new byte[4];
                buffer.get(typeBytes);
                String childType = new String(typeBytes);

                if (childSize < 8 || childSize > buffer.capacity()) {
                    throw new IOException("Invalid child box size in TrakBox: " + childSize);
                }

                buffer.position(positionBefore);

                switch (childType) {
                    case "tkhd" -> {
                        tkhd = new TrackMetadata.TkhdBox();
                        tkhd.deserialize(buffer);
                    }
                    case "mdia" -> {
                        mdia = new MdiaBox();
                        mdia.deserialize(buffer);
                    }
                    default -> buffer.position(positionBefore + childSize);
                }

                if (buffer.position() <= positionBefore) {
                    throw new IOException("Buffer position did not advance for TrakBox child: " + childType);
                }
            }
        }

        @Override
        public String toString() {
            return String.format("TrakBox{tkhd=%s, mdia=%s}", tkhd, mdia);
        }
    }

    /**
     * Media Box (mdia) - ISO/IEC 14496-12 Section 8.4.1
     *
     * Container for all objects that describe the media data within a track.
     *
     * Structure:
     * - mdhd (Media Header Box)
     * - hdlr (Handler Reference Box)
     * - minf (Media Information Box)
     */
    public static class MdiaBox extends Box {
        private InitializationSegment.MdhdBox mdhd;
        private InitializationSegment.HdlrBox hdlr;
        private MinfBox minf;

        public MdiaBox() {
            super("mdia");
        }

        public InitializationSegment.MdhdBox getMdhd() {
            return mdhd;
        }

        public void setMdhd(InitializationSegment.MdhdBox mdhd) {
            this.mdhd = mdhd;
        }

        public InitializationSegment.HdlrBox getHdlr() {
            return hdlr;
        }

        public void setHdlr(InitializationSegment.HdlrBox hdlr) {
            this.hdlr = hdlr;
        }

        public MinfBox getMinf() {
            return minf;
        }

        public void setMinf(MinfBox minf) {
            this.minf = minf;
        }

        @Override
        protected long calculateSize() {
            long size = 8; // header
            if (mdhd != null) {
                size += mdhd.calculateSize();
            }
            if (hdlr != null) {
                size += hdlr.calculateSize();
            }
            if (minf != null) {
                size += minf.calculateSize();
            }
            return size;
        }

        @Override
        public byte[] serialize() throws IOException {
            this.size = calculateSize();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            ByteBuffer header = ByteBuffer.allocate(8);
            writeHeader(header);
            baos.write(header.array());

            if (mdhd != null) {
                baos.write(mdhd.serialize());
            }
            if (hdlr != null) {
                baos.write(hdlr.serialize());
            }
            if (minf != null) {
                baos.write(minf.serialize());
            }

            return baos.toByteArray();
        }

        @Override
        public void deserialize(ByteBuffer buffer) throws IOException {
            long boxSize = readHeader(buffer);
            int startPosition = buffer.position();
            int endPosition = (int) (startPosition + boxSize - 8);

            while (buffer.position() < endPosition && buffer.remaining() >= 8) {
                int positionBefore = buffer.position();

                int childSize = buffer.getInt();
                byte[] typeBytes = new byte[4];
                buffer.get(typeBytes);
                String childType = new String(typeBytes);

                if (childSize < 8 || childSize > buffer.capacity()) {
                    throw new IOException("Invalid child box size in MdiaBox: " + childSize);
                }

                buffer.position(positionBefore);

                switch (childType) {
                    case "mdhd" -> {
                        mdhd = new InitializationSegment.MdhdBox();
                        mdhd.deserialize(buffer);
                    }
                    case "hdlr" -> {
                        hdlr = new InitializationSegment.HdlrBox();
                        hdlr.deserialize(buffer);
                    }
                    case "minf" -> {
                        minf = new MinfBox();
                        minf.deserialize(buffer);
                    }
                    default -> buffer.position(positionBefore + childSize);
                }

                if (buffer.position() <= positionBefore) {
                    throw new IOException("Buffer position did not advance for MdiaBox child: " + childType);
                }
            }
        }

        @Override
        public String toString() {
            return String.format("MdiaBox{mdhd=%s, hdlr=%s, minf=%s}", mdhd, hdlr, minf);
        }
    }

    /**
     * Media Information Box (minf) - ISO/IEC 14496-12 Section 8.4.4
     *
     * Contains all characteristic information about the media in a track.
     *
     * Structure:
     * - vmhd/smhd (Video/Sound Media Header Box)
     * - dinf (Data Information Box)
     * - stbl (Sample Table Box)
     */
    public static class MinfBox extends Box {
        private Box mediaHeaderBox; // vmhd or smhd
        private DinfBox dinf;
        private StblBox stbl;

        public MinfBox() {
            super("minf");
        }

        public Box getMediaHeaderBox() {
            return mediaHeaderBox;
        }

        public void setMediaHeaderBox(Box mediaHeaderBox) {
            this.mediaHeaderBox = mediaHeaderBox;
        }

        public InitializationSegment.VmhdBox getVmhd() {
            if (mediaHeaderBox instanceof InitializationSegment.VmhdBox) {
                return (InitializationSegment.VmhdBox) mediaHeaderBox;
            }
            return null;
        }

        public void setVmhd(InitializationSegment.VmhdBox vmhd) {
            this.mediaHeaderBox = vmhd;
        }

        public InitializationSegment.SmhdBox getSmhd() {
            if (mediaHeaderBox instanceof InitializationSegment.SmhdBox) {
                return (InitializationSegment.SmhdBox) mediaHeaderBox;
            }
            return null;
        }

        public void setSmhd(InitializationSegment.SmhdBox smhd) {
            this.mediaHeaderBox = smhd;
        }

        public DinfBox getDinf() {
            return dinf;
        }

        public void setDinf(DinfBox dinf) {
            this.dinf = dinf;
        }

        public StblBox getStbl() {
            return stbl;
        }

        public void setStbl(StblBox stbl) {
            this.stbl = stbl;
        }

        @Override
        protected long calculateSize() {
            long size = 8; // header
            if (mediaHeaderBox != null) {
                size += mediaHeaderBox.calculateSize();
            }
            if (dinf != null) {
                size += dinf.calculateSize();
            }
            if (stbl != null) {
                size += stbl.calculateSize();
            }
            return size;
        }

        @Override
        public byte[] serialize() throws IOException {
            this.size = calculateSize();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            ByteBuffer header = ByteBuffer.allocate(8);
            writeHeader(header);
            baos.write(header.array());

            if (mediaHeaderBox != null) {
                baos.write(mediaHeaderBox.serialize());
            }
            if (dinf != null) {
                baos.write(dinf.serialize());
            }
            if (stbl != null) {
                baos.write(stbl.serialize());
            }

            return baos.toByteArray();
        }

        @Override
        public void deserialize(ByteBuffer buffer) throws IOException {
            long boxSize = readHeader(buffer);
            int startPosition = buffer.position();
            int endPosition = (int) (startPosition + boxSize - 8);

            while (buffer.position() < endPosition && buffer.remaining() >= 8) {
                int positionBefore = buffer.position();

                int childSize = buffer.getInt();
                byte[] typeBytes = new byte[4];
                buffer.get(typeBytes);
                String childType = new String(typeBytes);

                if (childSize < 8 || childSize > buffer.capacity()) {
                    throw new IOException("Invalid child box size in MinfBox: " + childSize);
                }

                buffer.position(positionBefore);

                switch (childType) {
                    case "vmhd" -> {
                        mediaHeaderBox = new InitializationSegment.VmhdBox();
                        mediaHeaderBox.deserialize(buffer);
                    }
                    case "smhd" -> {
                        mediaHeaderBox = new InitializationSegment.SmhdBox();
                        mediaHeaderBox.deserialize(buffer);
                    }
                    case "dinf" -> {
                        dinf = new DinfBox();
                        dinf.deserialize(buffer);
                    }
                    case "stbl" -> {
                        stbl = new StblBox();
                        stbl.deserialize(buffer);
                    }
                    default -> buffer.position(positionBefore + childSize);
                }

                if (buffer.position() <= positionBefore) {
                    throw new IOException("Buffer position did not advance for MinfBox child: " + childType);
                }
            }
        }

        @Override
        public String toString() {
            return String.format("MinfBox{mediaHeader=%s, dinf=%s, stbl=%s}",
                    mediaHeaderBox != null ? mediaHeaderBox.getType() : "null", dinf, stbl);
        }
    }

    /**
     * Data Information Box (dinf) - ISO/IEC 14496-12 Section 8.7.1
     *
     * Contains objects that declare the location of media data.
     */
    public static class DinfBox extends Box {
        private InitializationSegment.DrefBox dref;

        public DinfBox() {
            super("dinf");
        }

        public InitializationSegment.DrefBox getDref() {
            return dref;
        }

        public void setDref(InitializationSegment.DrefBox dref) {
            this.dref = dref;
        }

        @Override
        protected long calculateSize() {
            long size = 8; // header
            if (dref != null) {
                size += dref.calculateSize();
            }
            return size;
        }

        @Override
        public byte[] serialize() throws IOException {
            this.size = calculateSize();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            ByteBuffer header = ByteBuffer.allocate(8);
            writeHeader(header);
            baos.write(header.array());

            if (dref != null) {
                baos.write(dref.serialize());
            }

            return baos.toByteArray();
        }

        @Override
        public void deserialize(ByteBuffer buffer) throws IOException {
            long boxSize = readHeader(buffer);
            int startPosition = buffer.position();
            int endPosition = (int) (startPosition + boxSize - 8);

            while (buffer.position() < endPosition && buffer.remaining() >= 8) {
                int positionBefore = buffer.position();

                int childSize = buffer.getInt();
                byte[] typeBytes = new byte[4];
                buffer.get(typeBytes);
                String childType = new String(typeBytes);

                if (childSize < 8 || childSize > buffer.capacity()) {
                    throw new IOException("Invalid child box size in DinfBox: " + childSize);
                }

                buffer.position(positionBefore);

                if ("dref".equals(childType)) {
                    dref = new InitializationSegment.DrefBox();
                    dref.deserialize(buffer);
                } else {
                    buffer.position(positionBefore + childSize);
                }

                if (buffer.position() <= positionBefore) {
                    throw new IOException("Buffer position did not advance for DinfBox child: " + childType);
                }
            }
        }

        @Override
        public String toString() {
            return String.format("DinfBox{dref=%s}", dref);
        }
    }

    /**
     * Sample Table Box (stbl) - ISO/IEC 14496-12 Section 8.5.1
     *
     * Contains all time and data indexing of media samples.
     * For fragmented files (CMAF), most of this is empty/minimal.
     */
    public static class StblBox extends Box {
        private TrackMetadata.StsdBox stsd;
        private SttsBox stts;
        private StscBox stsc;
        private StszBox stsz;
        private StcoBox stco;

        public StblBox() {
            super("stbl");
        }

        public TrackMetadata.StsdBox getStsd() {
            return stsd;
        }

        public void setStsd(TrackMetadata.StsdBox stsd) {
            this.stsd = stsd;
        }

        public SttsBox getStts() {
            return stts;
        }

        public void setStts(SttsBox stts) {
            this.stts = stts;
        }

        public StscBox getStsc() {
            return stsc;
        }

        public void setStsc(StscBox stsc) {
            this.stsc = stsc;
        }

        public StszBox getStsz() {
            return stsz;
        }

        public void setStsz(StszBox stsz) {
            this.stsz = stsz;
        }

        public StcoBox getStco() {
            return stco;
        }

        public void setStco(StcoBox stco) {
            this.stco = stco;
        }

        public void setEmptyTables() {
            this.stts = new SttsBox();
            this.stsc = new StscBox();
            this.stsz = new StszBox();
            this.stco = new StcoBox();
        }

        @Override
        protected long calculateSize() {
            long size = 8; // header
            if (stsd != null) {
                size += stsd.calculateSize();
            }
            if (stts != null) {
                size += stts.calculateSize();
            }
            if (stsc != null) {
                size += stsc.calculateSize();
            }
            if (stsz != null) {
                size += stsz.calculateSize();
            }
            if (stco != null) {
                size += stco.calculateSize();
            }
            return size;
        }

        @Override
        public byte[] serialize() throws IOException {
            this.size = calculateSize();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            ByteBuffer header = ByteBuffer.allocate(8);
            writeHeader(header);
            baos.write(header.array());

            if (stsd != null) {
                baos.write(stsd.serialize());
            }
            if (stts != null) {
                baos.write(stts.serialize());
            }
            if (stsc != null) {
                baos.write(stsc.serialize());
            }
            if (stsz != null) {
                baos.write(stsz.serialize());
            }
            if (stco != null) {
                baos.write(stco.serialize());
            }

            return baos.toByteArray();
        }

        @Override
        public void deserialize(ByteBuffer buffer) throws IOException {
            long boxSize = readHeader(buffer);
            int startPosition = buffer.position();
            int endPosition = (int) (startPosition + boxSize - 8);

            while (buffer.position() < endPosition && buffer.remaining() >= 8) {
                int positionBefore = buffer.position();

                int childSize = buffer.getInt();
                byte[] typeBytes = new byte[4];
                buffer.get(typeBytes);
                String childType = new String(typeBytes);

                if (childSize < 8 || childSize > buffer.capacity()) {
                    throw new IOException("Invalid child box size in StblBox: " + childSize);
                }

                buffer.position(positionBefore);

                switch (childType) {
                    case "stsd" -> {
                        stsd = new TrackMetadata.StsdBox();
                        stsd.deserialize(buffer);
                    }
                    case "stts" -> {
                        stts = new SttsBox();
                        stts.deserialize(buffer);
                    }
                    case "stsc" -> {
                        stsc = new StscBox();
                        stsc.deserialize(buffer);
                    }
                    case "stsz" -> {
                        stsz = new StszBox();
                        stsz.deserialize(buffer);
                    }
                    case "stco", "co64" -> {
                        stco = new StcoBox();
                        stco.deserialize(buffer);
                    }
                    default -> buffer.position(positionBefore + childSize);
                }

                if (buffer.position() <= positionBefore) {
                    throw new IOException("Buffer position did not advance for StblBox child: " + childType);
                }
            }
        }

        @Override
        public String toString() {
            return String.format("StblBox{stsd=%s}", stsd);
        }
    }

    // Empty sample table boxes for fragmented files (CMAF)

    /**
     * Decoding Time to Sample Box (stts) - ISO/IEC 14496-12 Section 8.6.1.2
     */
    public static class SttsBox extends Box {
        public SttsBox() {
            super("stts");
        }

        @Override
        protected long calculateSize() {
            return 16; // 8 + 4 + 4 (header + version/flags + entry_count=0)
        }

        @Override
        public byte[] serialize() throws IOException {
            this.size = calculateSize();
            ByteBuffer buffer = ByteBuffer.allocate((int) size);
            writeHeader(buffer);
            buffer.putInt(0); // version/flags
            buffer.putInt(0); // entry_count = 0
            return buffer.array();
        }

        @Override
        public void deserialize(ByteBuffer buffer) throws IOException {
            readHeader(buffer);
            buffer.getInt(); // version/flags
            buffer.getInt(); // entry_count
        }
    }

    /**
     * Sample To Chunk Box (stsc) - ISO/IEC 14496-12 Section 8.7.4
     */
    public static class StscBox extends Box {
        public StscBox() {
            super("stsc");
        }

        @Override
        protected long calculateSize() {
            return 16; // 8 + 4 + 4
        }

        @Override
        public byte[] serialize() throws IOException {
            this.size = calculateSize();
            ByteBuffer buffer = ByteBuffer.allocate((int) size);
            writeHeader(buffer);
            buffer.putInt(0); // version/flags
            buffer.putInt(0); // entry_count = 0
            return buffer.array();
        }

        @Override
        public void deserialize(ByteBuffer buffer) throws IOException {
            readHeader(buffer);
            buffer.getInt(); // version/flags
            buffer.getInt(); // entry_count
        }
    }

    /**
     * Sample Size Box (stsz) - ISO/IEC 14496-12 Section 8.7.3
     */
    public static class StszBox extends Box {
        public StszBox() {
            super("stsz");
        }

        @Override
        protected long calculateSize() {
            return 20; // 8 + 4 + 4 + 4
        }

        @Override
        public byte[] serialize() throws IOException {
            this.size = calculateSize();
            ByteBuffer buffer = ByteBuffer.allocate((int) size);
            writeHeader(buffer);
            buffer.putInt(0); // version/flags
            buffer.putInt(0); // sample_size
            buffer.putInt(0); // sample_count = 0
            return buffer.array();
        }

        @Override
        public void deserialize(ByteBuffer buffer) throws IOException {
            readHeader(buffer);
            buffer.getInt(); // version/flags
            buffer.getInt(); // sample_size
            buffer.getInt(); // sample_count
        }
    }

    /**
     * Chunk Offset Box (stco) - ISO/IEC 14496-12 Section 8.7.5
     */
    public static class StcoBox extends Box {
        public StcoBox() {
            super("stco");
        }

        @Override
        protected long calculateSize() {
            return 16; // 8 + 4 + 4
        }

        @Override
        public byte[] serialize() throws IOException {
            this.size = calculateSize();
            ByteBuffer buffer = ByteBuffer.allocate((int) size);
            writeHeader(buffer);
            buffer.putInt(0); // version/flags
            buffer.putInt(0); // entry_count = 0
            return buffer.array();
        }

        @Override
        public void deserialize(ByteBuffer buffer) throws IOException {
            readHeader(buffer);
            buffer.getInt(); // version/flags
            buffer.getInt(); // entry_count
        }
    }

    /**
     * Movie Extends Box (mvex) - ISO/IEC 14496-12 Section 8.8.1
     *
     * Required in the moov box for fragmented MP4 files.
     * Contains one trex box per track to provide default sample values
     * for the track fragments.
     */
    public static class MvexBox extends Box {
        private List<TrexBox> trexBoxes;

        public MvexBox() {
            super("mvex");
            this.trexBoxes = new ArrayList<>();
        }

        public List<TrexBox> getTrexBoxes() {
            return trexBoxes;
        }

        public void addTrex(TrexBox trex) {
            this.trexBoxes.add(trex);
        }

        @Override
        protected long calculateSize() {
            long size = 8; // header
            for (TrexBox trex : trexBoxes) {
                size += trex.calculateSize();
            }
            return size;
        }

        @Override
        public byte[] serialize() throws IOException {
            this.size = calculateSize();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            ByteBuffer header = ByteBuffer.allocate(8);
            writeHeader(header);
            baos.write(header.array());

            for (TrexBox trex : trexBoxes) {
                baos.write(trex.serialize());
            }

            return baos.toByteArray();
        }

        @Override
        public void deserialize(ByteBuffer buffer) throws IOException {
            long boxSize = readHeader(buffer);
            int startPosition = buffer.position();
            int endPosition = (int) (startPosition + boxSize - 8);

            while (buffer.position() < endPosition && buffer.remaining() >= 8) {
                int positionBefore = buffer.position();

                int childSize = buffer.getInt();
                byte[] typeBytes = new byte[4];
                buffer.get(typeBytes);
                String childType = new String(typeBytes);

                if (childSize < 8 || childSize > buffer.capacity()) {
                    throw new IOException("Invalid child box size in MvexBox: " + childSize);
                }

                buffer.position(positionBefore);

                if ("trex".equals(childType)) {
                    TrexBox trex = new TrexBox();
                    trex.deserialize(buffer);
                    trexBoxes.add(trex);
                } else {
                    buffer.position(positionBefore + childSize);
                }

                if (buffer.position() <= positionBefore) {
                    throw new IOException("Buffer position did not advance for MvexBox child: " + childType);
                }
            }
        }

        @Override
        public String toString() {
            return String.format("MvexBox{trexCount=%d}", trexBoxes.size());
        }
    }

    /**
     * Track Extends Box (trex) - ISO/IEC 14496-12 Section 8.8.3
     *
     * Sets up default values used by the movie fragments. Each track in a
     * fragmented file must have exactly one trex in the mvex box.
     *
     * Fields (all 32-bit unsigned):
     * - track_ID
     * - default_sample_description_index
     * - default_sample_duration
     * - default_sample_size
     * - default_sample_flags
     */
    public static class TrexBox extends Box {
        private long trackId;
        private long defaultSampleDescriptionIndex = 1;
        private long defaultSampleDuration;
        private long defaultSampleSize;
        private int defaultSampleFlags;

        public TrexBox() {
            super("trex");
        }

        public TrexBox(long trackId) {
            super("trex");
            this.trackId = trackId;
        }

        public long getTrackId() {
            return trackId;
        }

        public void setTrackId(long trackId) {
            this.trackId = trackId;
        }

        public long getDefaultSampleDescriptionIndex() {
            return defaultSampleDescriptionIndex;
        }

        public void setDefaultSampleDescriptionIndex(long defaultSampleDescriptionIndex) {
            this.defaultSampleDescriptionIndex = defaultSampleDescriptionIndex;
        }

        public long getDefaultSampleDuration() {
            return defaultSampleDuration;
        }

        public void setDefaultSampleDuration(long defaultSampleDuration) {
            this.defaultSampleDuration = defaultSampleDuration;
        }

        public long getDefaultSampleSize() {
            return defaultSampleSize;
        }

        public void setDefaultSampleSize(long defaultSampleSize) {
            this.defaultSampleSize = defaultSampleSize;
        }

        public int getDefaultSampleFlags() {
            return defaultSampleFlags;
        }

        public void setDefaultSampleFlags(int defaultSampleFlags) {
            this.defaultSampleFlags = defaultSampleFlags;
        }

        @Override
        protected long calculateSize() {
            // 8 (header) + 4 (version/flags) + 4 (trackId) + 4 (descIndex)
            // + 4 (duration) + 4 (size) + 4 (flags) = 32
            return 32;
        }

        @Override
        public byte[] serialize() throws IOException {
            this.size = calculateSize();
            ByteBuffer buffer = ByteBuffer.allocate((int) size);
            writeHeader(buffer);
            buffer.putInt(0); // version=0, flags=0
            buffer.putInt((int) trackId);
            buffer.putInt((int) defaultSampleDescriptionIndex);
            buffer.putInt((int) defaultSampleDuration);
            buffer.putInt((int) defaultSampleSize);
            buffer.putInt(defaultSampleFlags);
            return buffer.array();
        }

        @Override
        public void deserialize(ByteBuffer buffer) throws IOException {
            readHeader(buffer);
            buffer.getInt(); // version/flags
            this.trackId = Integer.toUnsignedLong(buffer.getInt());
            this.defaultSampleDescriptionIndex = Integer.toUnsignedLong(buffer.getInt());
            this.defaultSampleDuration = Integer.toUnsignedLong(buffer.getInt());
            this.defaultSampleSize = Integer.toUnsignedLong(buffer.getInt());
            this.defaultSampleFlags = buffer.getInt();
        }

        @Override
        public String toString() {
            return String.format("TrexBox{trackId=%d, descIndex=%d, duration=%d, size=%d, flags=0x%08X}",
                    trackId, defaultSampleDescriptionIndex, defaultSampleDuration,
                    defaultSampleSize, defaultSampleFlags);
        }
    }
}
