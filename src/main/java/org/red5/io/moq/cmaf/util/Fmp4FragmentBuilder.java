package org.red5.io.moq.cmaf.util;

import org.red5.io.moq.cmaf.model.CmafFragment;
import org.red5.io.moq.cmaf.model.MdatBox;
import org.red5.io.moq.cmaf.model.MoofBox;
import org.red5.io.moq.cmaf.model.SampleFlags;
import org.red5.io.moq.cmaf.model.StypBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility builder for CMAF fragments (styp + moof + mdat).
 * This assumes a single track per fragment, which is typical for CMAF.
 */
public class Fmp4FragmentBuilder {

    private String majorBrand = "cmf2";
    private long minorVersion = 0;
    private List<String> compatibleBrands = List.of("cmfc", "iso6");

    public Fmp4FragmentBuilder setBrands(String major, long minor, List<String> compatibles) {
        this.majorBrand = major;
        this.minorVersion = minor;
        this.compatibleBrands = compatibles != null ? compatibles : List.of();
        return this;
    }

    public CmafFragment buildFragment(FragmentConfig config) {
        if (config.mediaData == null) {
            throw new IllegalStateException("Fragment mediaData is required");
        }
        if (config.samples.isEmpty()) {
            throw new IllegalStateException("Fragment must contain at least one sample");
        }

        StypBox styp = new StypBox(majorBrand, minorVersion, compatibleBrands);

        MoofBox moof = new MoofBox();
        MoofBox.MfhdBox mfhd = new MoofBox.MfhdBox(config.sequenceNumber);
        moof.setMfhd(mfhd);

        MoofBox.TrafBox traf = new MoofBox.TrafBox();
        MoofBox.TfhdBox tfhd = new MoofBox.TfhdBox();
        tfhd.setTrackId(config.trackId);
        tfhd.setTfhdFlags(0x020000); // default-base-is-moof
        traf.setTfhd(tfhd);

        MoofBox.TfdtBox tfdt = new MoofBox.TfdtBox(config.baseDecodeTime);
        traf.setTfdt(tfdt);

        MoofBox.TrunBox trun = new MoofBox.TrunBox();
        trun.setVersion(config.hasSignedCompositionOffsets ? 1 : 0);
        trun.setTrunFlags(buildTrunFlags(config));
        trun.setSampleCount(config.samples.size());

        for (SampleData sample : config.samples) {
            MoofBox.TrunBox.Sample trunSample = new MoofBox.TrunBox.Sample();
            trunSample.setDuration(sample.duration);
            trunSample.setSize(sample.size);
            trunSample.setSampleFlags(sample.flags);
            if (sample.compositionTimeOffset != null) {
                trunSample.setCompositionTimeOffset(sample.compositionTimeOffset);
            }
            trun.addSample(trunSample);
        }

        traf.addTrun(trun);
        moof.addTraf(traf);

        // Compute data offset after moof size is known; mdat header is 8 bytes.
        try {
            int moofSize = moof.serialize().length;
            trun.setDataOffset(moofSize + 8);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize moof for data offset sizing", e);
        }

        MdatBox mdat = new MdatBox(config.mediaData);
        CmafFragment fragment = new CmafFragment(styp, moof, mdat);
        fragment.setGroupId(config.groupId);
        fragment.setObjectId(config.sequenceNumber);
        fragment.setMediaType(config.mediaType);

        return fragment;
    }

    private int buildTrunFlags(FragmentConfig config) {
        int flags = 0x000001; // data-offset-present
        flags |= 0x000100; // sample-duration-present
        flags |= 0x000200; // sample-size-present
        flags |= 0x000400; // sample-flags-present
        if (config.hasCompositionOffsets) {
            flags |= 0x000800; // composition-time-offsets-present
        }
        return flags;
    }

    public static class FragmentConfig {
        private long sequenceNumber;
        private long trackId;
        private long baseDecodeTime;
        private long groupId = 1;
        private CmafFragment.MediaType mediaType = CmafFragment.MediaType.VIDEO;
        private boolean hasCompositionOffsets;
        private boolean hasSignedCompositionOffsets;
        private byte[] mediaData;
        private List<SampleData> samples = new ArrayList<>();

        public FragmentConfig setSequenceNumber(long sequenceNumber) {
            this.sequenceNumber = sequenceNumber;
            return this;
        }

        public FragmentConfig setTrackId(long trackId) {
            this.trackId = trackId;
            return this;
        }

        public FragmentConfig setBaseDecodeTime(long baseDecodeTime) {
            this.baseDecodeTime = baseDecodeTime;
            return this;
        }

        public FragmentConfig setGroupId(long groupId) {
            this.groupId = groupId;
            return this;
        }

        public FragmentConfig setMediaType(CmafFragment.MediaType mediaType) {
            this.mediaType = mediaType;
            return this;
        }

        public FragmentConfig setMediaData(byte[] mediaData) {
            this.mediaData = mediaData;
            return this;
        }

        public FragmentConfig addSample(SampleData sample) {
            this.samples.add(sample);
            if (sample.compositionTimeOffset != null) {
                hasCompositionOffsets = true;
                if (sample.compositionTimeOffset < 0) {
                    hasSignedCompositionOffsets = true;
                }
            }
            return this;
        }

        public FragmentConfig setSamples(List<SampleData> samples) {
            this.samples = samples != null ? samples : new ArrayList<>();
            for (SampleData sample : this.samples) {
                if (sample.compositionTimeOffset != null) {
                    hasCompositionOffsets = true;
                    if (sample.compositionTimeOffset < 0) {
                        hasSignedCompositionOffsets = true;
                    }
                }
            }
            return this;
        }
    }

    public static class SampleData {
        private final long duration;
        private final long size;
        private final SampleFlags flags;
        private final Long compositionTimeOffset;

        public SampleData(long duration, long size, SampleFlags flags) {
            this(duration, size, flags, null);
        }

        public SampleData(long duration, long size, SampleFlags flags, Long compositionTimeOffset) {
            this.duration = duration;
            this.size = size;
            this.flags = flags;
            this.compositionTimeOffset = compositionTimeOffset;
        }
    }
}
