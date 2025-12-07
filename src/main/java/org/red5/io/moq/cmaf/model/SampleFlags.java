package org.red5.io.moq.cmaf.model;

/**
 * Sample flags as defined in ISO/IEC 14496-12 Section 8.8.3.1.
 *
 * Sample flags are a 32-bit field that provide information about the sample's characteristics:
 * - Whether it's a sync sample (key frame)
 * - Dependencies on other samples
 * - Leading samples
 * - Redundancy
 * - Degradation priority
 *
 * Also provides SAP (Stream Access Point) type detection per ISO/IEC 14496-12 Annex I
 * for CARP (CMAF compliant WARP) support:
 * - SAP Type 0: Not a stream access point
 * - SAP Type 1: Closed GOP, IDR (all subsequent samples decodable)
 * - SAP Type 2: Open GOP, IDR (I-frame but some prior B-frames may reference it)
 * - SAP Type 3: Open GOP, gradual decoder refresh (CRA/GDR)
 */
public class SampleFlags {

    /**
     * SAP (Stream Access Point) types per ISO/IEC 14496-12 Annex I.
     * Used by CARP for random access point signaling.
     */
    public enum SapType {
        /** Not a stream access point */
        NONE(0),
        /** Closed GOP IDR - all subsequent samples decodable immediately */
        TYPE_1(1),
        /** Open GOP IDR - I-frame, but prior B-frames in output order may exist */
        TYPE_2(2),
        /** Open GOP GDR/CRA - gradual decoder refresh, RASL pictures may be skipped */
        TYPE_3(3);

        private final int value;

        SapType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static SapType fromValue(int value) {
            return switch (value) {
                case 1 -> TYPE_1;
                case 2 -> TYPE_2;
                case 3 -> TYPE_3;
                default -> NONE;
            };
        }
    }

    // Raw 32-bit flags value
    private final int flags;

    // Sample dependency information (bits 4-11)
    private final int isLeading;              // bits 4-5: 0=unknown, 1=has dependency before ref I-picture, 2=not leading, 3=no dependency before ref I-picture
    private final int sampleDependsOn;        // bits 6-7: 0=unknown, 1=depends on others, 2=does not depend, 3=reserved
    private final int sampleIsDependedOn;     // bits 8-9: 0=unknown, 1=others depend on this, 2=no others depend, 3=reserved
    private final int sampleHasRedundancy;    // bits 10-11: 0=unknown, 1=has redundant coding, 2=no redundant coding, 3=reserved

    // Padding and sync information (bits 12-15)
    private final int samplePaddingValue;     // bits 12-14: padding value
    private final boolean sampleIsNonSync;    // bit 15: false=sync sample (key frame), true=non-sync sample

    // Degradation priority (bits 16-31)
    private final int sampleDegradationPriority; // bits 16-31: 0=no priority

    /**
     * Creates SampleFlags from a 32-bit flags value.
     *
     * @param flags The 32-bit flags value from the trun box
     */
    public SampleFlags(int flags) {
        this.flags = flags;

        // Parse bit fields according to ISO/IEC 14496-12
        // Note: bits are numbered from MSB (bit 31) to LSB (bit 0)
        this.isLeading = (flags >> 26) & 0x3;                    // bits 4-5 (from MSB: bits 26-27)
        this.sampleDependsOn = (flags >> 24) & 0x3;              // bits 6-7 (from MSB: bits 24-25)
        this.sampleIsDependedOn = (flags >> 22) & 0x3;           // bits 8-9 (from MSB: bits 22-23)
        this.sampleHasRedundancy = (flags >> 20) & 0x3;          // bits 10-11 (from MSB: bits 20-21)
        this.samplePaddingValue = (flags >> 17) & 0x7;           // bits 12-14 (from MSB: bits 17-19)
        this.sampleIsNonSync = ((flags >> 16) & 0x1) != 0;       // bit 15 (from MSB: bit 16)
        this.sampleDegradationPriority = flags & 0xFFFF;         // bits 16-31 (from MSB: bits 0-15)
    }

    /**
     * @return The raw 32-bit flags value
     */
    public int getFlags() {
        return flags;
    }

    /**
     * @return Is leading (0=unknown, 1=has dependency before ref I-picture, 2=not leading, 3=no dependency before ref I-picture)
     */
    public int getIsLeading() {
        return isLeading;
    }

    /**
     * @return Sample depends on (0=unknown, 1=depends on others, 2=does not depend, 3=reserved)
     */
    public int getSampleDependsOn() {
        return sampleDependsOn;
    }

    /**
     * @return Sample is depended on (0=unknown, 1=others depend on this, 2=no others depend, 3=reserved)
     */
    public int getSampleIsDependedOn() {
        return sampleIsDependedOn;
    }

    /**
     * @return Sample has redundancy (0=unknown, 1=has redundant coding, 2=no redundant coding, 3=reserved)
     */
    public int getSampleHasRedundancy() {
        return sampleHasRedundancy;
    }

    /**
     * @return Sample padding value
     */
    public int getSamplePaddingValue() {
        return samplePaddingValue;
    }

    /**
     * @return true if this is a non-sync sample (not a key frame), false if it's a sync sample (key frame)
     */
    public boolean isSampleNonSync() {
        return sampleIsNonSync;
    }

    /**
     * @return true if this is a sync sample (key frame), false if it's a non-sync sample
     */
    public boolean isSyncSample() {
        return !sampleIsNonSync;
    }

    /**
     * @return Sample degradation priority (0 = no priority)
     */
    public int getSampleDegradationPriority() {
        return sampleDegradationPriority;
    }

    /**
     * @return true if this sample does not depend on other samples (can be decoded independently)
     */
    public boolean isIndependent() {
        return sampleDependsOn == 2;
    }

    /**
     * @return true if other samples depend on this one
     */
    public boolean isDependedUpon() {
        return sampleIsDependedOn == 1;
    }

    /**
     * Determines the SAP (Stream Access Point) type based on sample flags.
     * Per ISO/IEC 14496-12 Annex I and CARP draft-law-moq-carp-00:
     *
     * <ul>
     *   <li>SAP Type 1: sync sample, independent, no leading samples before reference I-picture</li>
     *   <li>SAP Type 2: sync sample, independent, has leading samples (open GOP IDR)</li>
     *   <li>SAP Type 3: sync sample, independent, leading samples with dependency (CRA/GDR)</li>
     *   <li>Type 0/NONE: not a sync sample or depends on other samples</li>
     * </ul>
     *
     * @return the SAP type for this sample
     */
    public SapType getSapType() {
        // Not a sync sample - not a SAP
        if (sampleIsNonSync) {
            return SapType.NONE;
        }

        // Depends on other samples - not a SAP
        if (sampleDependsOn == 1) {
            return SapType.NONE;
        }

        // Sync sample that doesn't depend on others - this is a SAP
        // Now determine the type based on leading sample info

        // isLeading values per ISO/IEC 14496-12:
        // 0 = unknown
        // 1 = has dependency before reference I-picture (leading sample with dependency)
        // 2 = not a leading sample
        // 3 = no dependency before reference I-picture (leading sample without dependency)

        switch (isLeading) {
            case 2:
                // Not a leading sample - closed GOP, SAP Type 1
                return SapType.TYPE_1;
            case 3:
                // Leading sample without dependency - open GOP IDR, SAP Type 2
                return SapType.TYPE_2;
            case 1:
                // Leading sample with dependency - CRA/GDR, SAP Type 3
                return SapType.TYPE_3;
            default:
                // Unknown leading status - assume SAP Type 1 if sync and independent
                if (sampleDependsOn == 2) {
                    return SapType.TYPE_1;
                }
                return SapType.NONE;
        }
    }

    /**
     * @return true if this sample is a Stream Access Point (SAP types 1, 2, or 3)
     */
    public boolean isStreamAccessPoint() {
        return getSapType() != SapType.NONE;
    }

    /**
     * @return the SAP type as an integer (0, 1, 2, or 3) for CARP timeline use
     */
    public int getSapTypeValue() {
        return getSapType().getValue();
    }

    @Override
    public String toString() {
        return String.format("SampleFlags{0x%08X, sync=%s, dependsOn=%d, isDependedOn=%d, leading=%d, redundancy=%d, priority=%d}",
                flags,
                isSyncSample() ? "yes" : "no",
                sampleDependsOn,
                sampleIsDependedOn,
                isLeading,
                sampleHasRedundancy,
                sampleDegradationPriority);
    }

    /**
     * Creates default flags for a sync sample (key frame) that doesn't depend on others.
     *
     * @return SampleFlags for a key frame
     */
    public static SampleFlags createSyncSampleFlags() {
        // sampleDependsOn=2 (does not depend), sampleIsNonSync=0 (is sync/key frame)
        int flags = (2 << 24); // Set sampleDependsOn to 2
        return new SampleFlags(flags);
    }

    /**
     * Creates default flags for a non-sync sample (non-key frame) that depends on others.
     *
     * @return SampleFlags for a non-key frame
     */
    public static SampleFlags createNonSyncSampleFlags() {
        // sampleDependsOn=1 (depends on others), sampleIsNonSync=1 (is non-sync)
        int flags = (1 << 24) | (1 << 16);
        return new SampleFlags(flags);
    }

    /**
     * Creates flags for a SAP Type 1 sample (closed GOP IDR).
     * This is a sync sample that doesn't depend on others and has no leading samples.
     *
     * @return SampleFlags for SAP Type 1
     */
    public static SampleFlags createSapType1Flags() {
        // isLeading=2 (not leading), sampleDependsOn=2 (independent), sampleIsNonSync=0
        int flags = (2 << 26) | (2 << 24);
        return new SampleFlags(flags);
    }

    /**
     * Creates flags for a SAP Type 2 sample (open GOP IDR).
     * This is a sync sample with leading samples that don't have dependencies.
     *
     * @return SampleFlags for SAP Type 2
     */
    public static SampleFlags createSapType2Flags() {
        // isLeading=3 (leading, no dependency), sampleDependsOn=2 (independent), sampleIsNonSync=0
        int flags = (3 << 26) | (2 << 24);
        return new SampleFlags(flags);
    }

    /**
     * Creates flags for a SAP Type 3 sample (CRA/GDR - gradual decoder refresh).
     * This is a sync sample with leading samples that have dependencies (RASL pictures).
     *
     * @return SampleFlags for SAP Type 3
     */
    public static SampleFlags createSapType3Flags() {
        // isLeading=1 (leading with dependency), sampleDependsOn=2 (independent), sampleIsNonSync=0
        int flags = (1 << 26) | (2 << 24);
        return new SampleFlags(flags);
    }

    /**
     * Creates flags for a specific SAP type.
     *
     * @param sapType the SAP type (1, 2, or 3)
     * @return SampleFlags for the specified SAP type, or non-sync flags for type 0/invalid
     */
    public static SampleFlags createForSapType(int sapType) {
        return switch (sapType) {
            case 1 -> createSapType1Flags();
            case 2 -> createSapType2Flags();
            case 3 -> createSapType3Flags();
            default -> createNonSyncSampleFlags();
        };
    }

    /**
     * Creates flags for a specific SAP type.
     *
     * @param sapType the SAP type enum
     * @return SampleFlags for the specified SAP type
     */
    public static SampleFlags createForSapType(SapType sapType) {
        return createForSapType(sapType.getValue());
    }
}
