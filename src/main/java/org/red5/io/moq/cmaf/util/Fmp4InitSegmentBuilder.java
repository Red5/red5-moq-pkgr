package org.red5.io.moq.cmaf.util;

import org.red5.io.moq.cmaf.model.Box;
import org.red5.io.moq.cmaf.model.InitializationSegment;
import org.red5.io.moq.cmaf.model.MoovBox;
import org.red5.io.moq.cmaf.model.TrackMetadata;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility builder for fMP4 initialization segments (ftyp + moov).
 * This is intended for CMAF-style streaming where the init segment
 * is sent once, followed by fragments.
 */
public class Fmp4InitSegmentBuilder {

    private final List<TrackConfig> tracks = new ArrayList<>();
    private String majorBrand = "cmf2";
    private long minorVersion = 0;
    private List<String> compatibleBrands = List.of("cmfc", "iso6");
    private long movieTimescale = 1000;

    public Fmp4InitSegmentBuilder setBrands(String major, long minor, List<String> compatibles) {
        this.majorBrand = major;
        this.minorVersion = minor;
        this.compatibleBrands = compatibles != null ? compatibles : List.of();
        return this;
    }

    public Fmp4InitSegmentBuilder setMovieTimescale(long movieTimescale) {
        this.movieTimescale = movieTimescale;
        return this;
    }

    public Fmp4InitSegmentBuilder addVideoTrack(VideoTrackConfig config) {
        tracks.add(config);
        return this;
    }

    public Fmp4InitSegmentBuilder addAudioTrack(AudioTrackConfig config) {
        tracks.add(config);
        return this;
    }

    public byte[] build() throws IOException {
        if (tracks.isEmpty()) {
            throw new IllegalStateException("At least one track is required");
        }

        InitializationSegment.FtypBox ftyp = new InitializationSegment.FtypBox();
        ftyp.setMajorBrand(majorBrand);
        ftyp.setMinorVersion(minorVersion);
        ftyp.setCompatibleBrands(compatibleBrands);

        InitializationSegment.MvhdBox mvhd = new InitializationSegment.MvhdBox();
        mvhd.setVersion(0);
        mvhd.setTimescale(movieTimescale);
        mvhd.setDuration(0);
        mvhd.setNextTrackId(getNextTrackId());

        MoovBox moov = new MoovBox();
        moov.setMvhd(mvhd);

        for (TrackConfig track : tracks) {
            moov.addTrak(track.toTrak());
        }

        // Build mvex box with trex entries (required for fragmented MP4)
        MoovBox.MvexBox mvex = new MoovBox.MvexBox();
        for (TrackConfig track : tracks) {
            MoovBox.TrexBox trex = new MoovBox.TrexBox(track.trackId);
            trex.setDefaultSampleDescriptionIndex(1);
            trex.setDefaultSampleDuration(0);
            trex.setDefaultSampleSize(0);
            trex.setDefaultSampleFlags(0);
            mvex.addTrex(trex);
        }
        moov.setMvex(mvex);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(ftyp.serialize());
        out.write(moov.serialize());
        return out.toByteArray();
    }

    private long getNextTrackId() {
        long maxId = 0;
        for (TrackConfig track : tracks) {
            maxId = Math.max(maxId, track.trackId);
        }
        return maxId + 1;
    }

    public abstract static class TrackConfig {
        protected long trackId;
        protected long timescale;
        protected String codecFourcc;
        protected byte[] codecConfig;
        protected String handlerType;
        protected String handlerName;

        protected TrackConfig(long trackId, long timescale, String codecFourcc, byte[] codecConfig,
                              String handlerType, String handlerName) {
            this.trackId = trackId;
            this.timescale = timescale;
            this.codecFourcc = codecFourcc;
            this.codecConfig = codecConfig;
            this.handlerType = handlerType;
            this.handlerName = handlerName;
        }

        protected MoovBox.TrakBox toTrak() {
            MoovBox.TrakBox trak = new MoovBox.TrakBox();
            trak.setTkhd(buildTkhd());

            MoovBox.MdiaBox mdia = new MoovBox.MdiaBox();
            mdia.setMdhd(buildMdhd());
            mdia.setHdlr(new InitializationSegment.HdlrBox(handlerType, handlerName));

            MoovBox.MinfBox minf = new MoovBox.MinfBox();
            minf.setMediaHeaderBox(buildMediaHeader());

            MoovBox.DinfBox dinf = new MoovBox.DinfBox();
            dinf.setDref(new InitializationSegment.DrefBox());
            minf.setDinf(dinf);

            MoovBox.StblBox stbl = new MoovBox.StblBox();
            stbl.setStsd(buildStsd());
            stbl.setEmptyTables();
            minf.setStbl(stbl);

            mdia.setMinf(minf);
            trak.setMdia(mdia);

            return trak;
        }

        protected InitializationSegment.MdhdBox buildMdhd() {
            InitializationSegment.MdhdBox mdhd = new InitializationSegment.MdhdBox();
            mdhd.setVersion(0);
            mdhd.setTimescale(timescale);
            mdhd.setDuration(0);
            return mdhd;
        }

        protected TrackMetadata.StsdBox buildStsd() {
            TrackMetadata.StsdBox stsd = new TrackMetadata.StsdBox();
            stsd.setEntries(new TrackMetadata.SampleEntry[] { buildSampleEntry() });
            return stsd;
        }

        protected abstract TrackMetadata.TkhdBox buildTkhd();

        protected abstract TrackMetadata.SampleEntry buildSampleEntry();

        protected abstract Box buildMediaHeader();
    }

    public static class VideoTrackConfig extends TrackConfig {
        private int width;
        private int height;

        public VideoTrackConfig(long trackId, long timescale, String codecFourcc, byte[] codecConfig,
                                int width, int height) {
            super(trackId, timescale, codecFourcc, codecConfig, "vide", "VideoHandler");
            this.width = width;
            this.height = height;
        }

        @Override
        protected TrackMetadata.TkhdBox buildTkhd() {
            TrackMetadata.TkhdBox tkhd = new TrackMetadata.TkhdBox();
            tkhd.setVersion(0);
            tkhd.setFlags(0x00000007);
            tkhd.setTrackId(trackId);
            tkhd.setDuration(0);
            tkhd.setWidthPixels(width);
            tkhd.setHeightPixels(height);
            tkhd.setVolume(0);
            return tkhd;
        }

        @Override
        protected TrackMetadata.SampleEntry buildSampleEntry() {
            TrackMetadata.VisualSampleEntry entry = new TrackMetadata.VisualSampleEntry(codecFourcc);
            entry.setWidth(width);
            entry.setHeight(height);
            entry.setCodecConfig(codecConfig);
            return entry;
        }

        @Override
        protected Box buildMediaHeader() {
            return new InitializationSegment.VmhdBox();
        }
    }

    public static class AudioTrackConfig extends TrackConfig {
        private int channelCount;
        private int sampleRate;
        private int sampleSize;

        public AudioTrackConfig(long trackId, long timescale, String codecFourcc, byte[] codecConfig,
                                int channelCount, int sampleRate, int sampleSize) {
            super(trackId, timescale, codecFourcc, codecConfig, "soun", "SoundHandler");
            this.channelCount = channelCount;
            this.sampleRate = sampleRate;
            this.sampleSize = sampleSize;
        }

        @Override
        protected TrackMetadata.TkhdBox buildTkhd() {
            TrackMetadata.TkhdBox tkhd = new TrackMetadata.TkhdBox();
            tkhd.setVersion(0);
            tkhd.setFlags(0x00000007);
            tkhd.setTrackId(trackId);
            tkhd.setDuration(0);
            tkhd.setVolume(0x0100);
            return tkhd;
        }

        @Override
        protected TrackMetadata.SampleEntry buildSampleEntry() {
            TrackMetadata.AudioSampleEntry entry = new TrackMetadata.AudioSampleEntry(codecFourcc);
            entry.setChannelCount(channelCount);
            entry.setSampleRateHz(sampleRate);
            entry.setSampleSize(sampleSize);
            entry.setCodecConfig(codecConfig);
            return entry;
        }

        @Override
        protected Box buildMediaHeader() {
            return new InitializationSegment.SmhdBox();
        }
    }
}
