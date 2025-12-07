package org.red5.io.moq.cmaf;

import org.red5.io.moq.cmaf.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.IOException;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SampleFlags parsing and interpretation.
 * Tests the parsing of sample flags according to ISO/IEC 14496-12 Section 8.8.3.1.
 */
class SampleFlagsTest {

    @Test
    @DisplayName("Test sync sample (key frame) flags parsing")
    void testSyncSampleFlags() {
        // Sync sample: sampleDependsOn=2 (does not depend), sampleIsNonSync=0 (is sync)
        int flags = (2 << 24); // 0x02000000
        SampleFlags sampleFlags = new SampleFlags(flags);

        assertEquals(2, sampleFlags.getSampleDependsOn());
        assertTrue(sampleFlags.isSyncSample());
        assertFalse(sampleFlags.isSampleNonSync());
        assertTrue(sampleFlags.isIndependent());
    }

    @Test
    @DisplayName("Test non-sync sample (non-key frame) flags parsing")
    void testNonSyncSampleFlags() {
        // Non-sync sample: sampleDependsOn=1 (depends on others), sampleIsNonSync=1
        int flags = (1 << 24) | (1 << 16); // 0x01010000
        SampleFlags sampleFlags = new SampleFlags(flags);

        assertEquals(1, sampleFlags.getSampleDependsOn());
        assertFalse(sampleFlags.isSyncSample());
        assertTrue(sampleFlags.isSampleNonSync());
        assertFalse(sampleFlags.isIndependent());
    }

    @Test
    @DisplayName("Test all sample dependency flags")
    void testSampleDependencyFlags() {
        // Test is_leading (bits 4-5 from MSB, which is bits 26-27 from right)
        SampleFlags flags1 = new SampleFlags(3 << 26);
        assertEquals(3, flags1.getIsLeading());

        // Test sample_depends_on (bits 6-7 from MSB, which is bits 24-25 from right)
        SampleFlags flags2 = new SampleFlags(3 << 24);
        assertEquals(3, flags2.getSampleDependsOn());

        // Test sample_is_depended_on (bits 8-9 from MSB, which is bits 22-23 from right)
        SampleFlags flags3 = new SampleFlags(1 << 22); // Value 1 = others depend on this
        assertEquals(1, flags3.getSampleIsDependedOn());
        assertTrue(flags3.isDependedUpon()); // sampleIsDependedOn == 1

        // Test sample_has_redundancy (bits 10-11 from MSB, which is bits 20-21 from right)
        SampleFlags flags4 = new SampleFlags(3 << 20);
        assertEquals(3, flags4.getSampleHasRedundancy());
    }

    @Test
    @DisplayName("Test sample padding value")
    void testSamplePaddingValue() {
        // Sample padding value is bits 12-14 (3 bits, max value 7)
        int flags = (7 << 17); // Set padding value to 7
        SampleFlags sampleFlags = new SampleFlags(flags);

        assertEquals(7, sampleFlags.getSamplePaddingValue());
    }

    @Test
    @DisplayName("Test sample degradation priority")
    void testSampleDegradationPriority() {
        // Degradation priority is bits 16-31 (16 bits)
        int flags = 0xFFFF; // Max priority
        SampleFlags sampleFlags = new SampleFlags(flags);

        assertEquals(0xFFFF, sampleFlags.getSampleDegradationPriority());
    }

    @Test
    @DisplayName("Test complex sample flags with all fields set")
    void testComplexSampleFlags() {
        // Create flags with multiple fields set:
        // isLeading=2, sampleDependsOn=1, sampleIsDependedOn=1, sampleHasRedundancy=2,
        // paddingValue=3, isNonSync=1, degradationPriority=100
        int flags = (2 << 26) |  // isLeading = 2
                    (1 << 24) |  // sampleDependsOn = 1
                    (1 << 22) |  // sampleIsDependedOn = 1
                    (2 << 20) |  // sampleHasRedundancy = 2
                    (3 << 17) |  // paddingValue = 3
                    (1 << 16) |  // isNonSync = 1
                    100;         // degradationPriority = 100

        SampleFlags sampleFlags = new SampleFlags(flags);

        assertEquals(2, sampleFlags.getIsLeading());
        assertEquals(1, sampleFlags.getSampleDependsOn());
        assertEquals(1, sampleFlags.getSampleIsDependedOn());
        assertEquals(2, sampleFlags.getSampleHasRedundancy());
        assertEquals(3, sampleFlags.getSamplePaddingValue());
        assertTrue(sampleFlags.isSampleNonSync());
        assertEquals(100, sampleFlags.getSampleDegradationPriority());
    }

    @Test
    @DisplayName("Test createSyncSampleFlags factory method")
    void testCreateSyncSampleFlags() {
        SampleFlags flags = SampleFlags.createSyncSampleFlags();

        assertTrue(flags.isSyncSample());
        assertFalse(flags.isSampleNonSync());
        assertTrue(flags.isIndependent());
        assertEquals(2, flags.getSampleDependsOn());
    }

    @Test
    @DisplayName("Test createNonSyncSampleFlags factory method")
    void testCreateNonSyncSampleFlags() {
        SampleFlags flags = SampleFlags.createNonSyncSampleFlags();

        assertFalse(flags.isSyncSample());
        assertTrue(flags.isSampleNonSync());
        assertFalse(flags.isIndependent());
        assertEquals(1, flags.getSampleDependsOn());
    }

    @Test
    @DisplayName("Test TrunBox with sample flags")
    void testTrunBoxWithSampleFlags() throws IOException {
        // Create a trun box with sample flags
        MoofBox.TrunBox trun = new MoofBox.TrunBox();
        trun.setVersion(0);
        trun.setTrunFlags(0x000401); // data_offset_present + sample_flags_present
        trun.setSampleCount(2);
        trun.setDataOffset(100);

        // Create samples with different flags
        MoofBox.TrunBox.Sample sample1 = new MoofBox.TrunBox.Sample();
        sample1.setSampleFlags(SampleFlags.createSyncSampleFlags());

        MoofBox.TrunBox.Sample sample2 = new MoofBox.TrunBox.Sample();
        sample2.setSampleFlags(SampleFlags.createNonSyncSampleFlags());

        trun.addSample(sample1);
        trun.addSample(sample2);

        // Serialize
        byte[] data = trun.serialize();
        assertNotNull(data);
        assertTrue(data.length > 0);

        // Deserialize
        MoofBox.TrunBox deserialized = new MoofBox.TrunBox();
        deserialized.deserialize(ByteBuffer.wrap(data));

        // Verify
        assertEquals(2, deserialized.getSampleCount());
        assertEquals(2, deserialized.getSamples().size());

        // Verify first sample (sync)
        assertTrue(deserialized.getSamples().get(0).isSyncSample());
        assertTrue(deserialized.getSamples().get(0).isIndependent());

        // Verify second sample (non-sync)
        assertFalse(deserialized.getSamples().get(1).isSyncSample());
        assertFalse(deserialized.getSamples().get(1).isIndependent());
    }

    @Test
    @DisplayName("Test TrunBox with all sample data fields")
    void testTrunBoxWithAllSampleFields() throws IOException {
        // Create a trun box with all flags set
        MoofBox.TrunBox trun = new MoofBox.TrunBox();
        trun.setVersion(0);
        trun.setTrunFlags(0x000F01); // data_offset + duration + size + flags + composition_time_offset
        trun.setSampleCount(1);
        trun.setDataOffset(200);

        // Create a sample with all fields
        MoofBox.TrunBox.Sample sample = new MoofBox.TrunBox.Sample();
        sample.setDuration(1000);
        sample.setSize(5000);
        sample.setSampleFlags(SampleFlags.createSyncSampleFlags());
        sample.setCompositionTimeOffset(500);

        trun.addSample(sample);

        // Serialize
        byte[] data = trun.serialize();
        assertNotNull(data);

        // Deserialize
        MoofBox.TrunBox deserialized = new MoofBox.TrunBox();
        deserialized.deserialize(ByteBuffer.wrap(data));

        // Verify
        assertEquals(1, deserialized.getSampleCount());
        assertEquals(1, deserialized.getSamples().size());

        MoofBox.TrunBox.Sample deserializedSample = deserialized.getSamples().get(0);
        assertEquals(1000, deserializedSample.getDuration());
        assertEquals(5000, deserializedSample.getSize());
        assertTrue(deserializedSample.isSyncSample());
        assertEquals(500, deserializedSample.getCompositionTimeOffset());
    }

    @Test
    @DisplayName("Test TrunBox with first_sample_flags")
    void testTrunBoxWithFirstSampleFlags() throws IOException {
        // Create a trun box with first_sample_flags
        MoofBox.TrunBox trun = new MoofBox.TrunBox();
        trun.setVersion(0);
        trun.setTrunFlags(0x000005); // data_offset_present + first_sample_flags_present
        trun.setSampleCount(2);
        trun.setDataOffset(150);
        trun.setFirstSampleFlags(SampleFlags.createSyncSampleFlags());

        // Create samples - first one should inherit first_sample_flags
        MoofBox.TrunBox.Sample sample1 = new MoofBox.TrunBox.Sample();
        MoofBox.TrunBox.Sample sample2 = new MoofBox.TrunBox.Sample();

        trun.addSample(sample1);
        trun.addSample(sample2);

        // Serialize
        byte[] data = trun.serialize();
        assertNotNull(data);

        // Deserialize
        MoofBox.TrunBox deserialized = new MoofBox.TrunBox();
        deserialized.deserialize(ByteBuffer.wrap(data));

        // Verify
        assertEquals(2, deserialized.getSampleCount());
        assertNotNull(deserialized.getFirstSampleFlags());
        assertTrue(deserialized.getFirstSampleFlags().isSyncSample());

        // First sample should inherit first_sample_flags
        assertNotNull(deserialized.getSamples().get(0).getSampleFlags());
        assertTrue(deserialized.getSamples().get(0).isSyncSample());

        // Second sample should have null flags (no per-sample flags)
        assertNull(deserialized.getSamples().get(1).getSampleFlags());
    }

    @Test
    @DisplayName("Test TrunBox with version 1 and signed composition time offset")
    void testTrunBoxVersion1() throws IOException {
        // Create a trun box with version 1 for signed composition time offset
        MoofBox.TrunBox trun = new MoofBox.TrunBox();
        trun.setVersion(1);
        trun.setTrunFlags(0x000801); // data_offset + composition_time_offset
        trun.setSampleCount(1);
        trun.setDataOffset(100);

        // Create a sample with negative composition time offset
        MoofBox.TrunBox.Sample sample = new MoofBox.TrunBox.Sample();
        sample.setCompositionTimeOffset(-500); // Negative offset (B-frames)

        trun.addSample(sample);

        // Serialize
        byte[] data = trun.serialize();
        assertNotNull(data);

        // Deserialize
        MoofBox.TrunBox deserialized = new MoofBox.TrunBox();
        deserialized.deserialize(ByteBuffer.wrap(data));

        // Verify
        assertEquals(1, deserialized.getVersion());
        assertEquals(1, deserialized.getSampleCount());
        assertEquals(-500, deserialized.getSamples().get(0).getCompositionTimeOffset());
    }

    @Test
    @DisplayName("Test SampleFlags toString format")
    void testSampleFlagsToString() {
        SampleFlags flags = SampleFlags.createSyncSampleFlags();
        String str = flags.toString();

        assertNotNull(str);
        assertTrue(str.contains("sync=yes"));
        assertTrue(str.contains("dependsOn=2"));
    }

    @Test
    @DisplayName("Test TrunBox with multiple samples having different flags")
    void testMultipleSamplesWithDifferentFlags() throws IOException {
        // Create a trun box with 5 samples - typical GOP pattern: I-B-B-P-P
        MoofBox.TrunBox trun = new MoofBox.TrunBox();
        trun.setVersion(0);
        trun.setTrunFlags(0x000701); // data_offset + duration + size + flags
        trun.setSampleCount(5);
        trun.setDataOffset(300);

        // I-frame (sync sample)
        MoofBox.TrunBox.Sample iFrame = new MoofBox.TrunBox.Sample();
        iFrame.setDuration(3000);
        iFrame.setSize(50000);
        iFrame.setSampleFlags(SampleFlags.createSyncSampleFlags());

        // B-frames (non-sync, depends on others)
        MoofBox.TrunBox.Sample bFrame1 = new MoofBox.TrunBox.Sample();
        bFrame1.setDuration(3000);
        bFrame1.setSize(5000);
        bFrame1.setSampleFlags(SampleFlags.createNonSyncSampleFlags());

        MoofBox.TrunBox.Sample bFrame2 = new MoofBox.TrunBox.Sample();
        bFrame2.setDuration(3000);
        bFrame2.setSize(5000);
        bFrame2.setSampleFlags(SampleFlags.createNonSyncSampleFlags());

        // P-frames (non-sync, but others may depend on them)
        int pFrameFlags = (1 << 24) | (1 << 22) | (1 << 16); // depends=1, depended_on=1, non-sync=1
        MoofBox.TrunBox.Sample pFrame1 = new MoofBox.TrunBox.Sample();
        pFrame1.setDuration(3000);
        pFrame1.setSize(10000);
        pFrame1.setSampleFlags(new SampleFlags(pFrameFlags));

        MoofBox.TrunBox.Sample pFrame2 = new MoofBox.TrunBox.Sample();
        pFrame2.setDuration(3000);
        pFrame2.setSize(10000);
        pFrame2.setSampleFlags(new SampleFlags(pFrameFlags));

        trun.addSample(iFrame);
        trun.addSample(bFrame1);
        trun.addSample(bFrame2);
        trun.addSample(pFrame1);
        trun.addSample(pFrame2);

        // Serialize
        byte[] data = trun.serialize();
        assertNotNull(data);

        // Deserialize
        MoofBox.TrunBox deserialized = new MoofBox.TrunBox();
        deserialized.deserialize(ByteBuffer.wrap(data));

        // Verify
        assertEquals(5, deserialized.getSampleCount());
        assertEquals(5, deserialized.getSamples().size());

        // Verify I-frame
        assertTrue(deserialized.getSamples().get(0).isSyncSample());
        assertTrue(deserialized.getSamples().get(0).isIndependent());
        assertEquals(50000, deserialized.getSamples().get(0).getSize());

        // Verify B-frames
        assertFalse(deserialized.getSamples().get(1).isSyncSample());
        assertFalse(deserialized.getSamples().get(1).isIndependent());
        assertFalse(deserialized.getSamples().get(2).isSyncSample());

        // Verify P-frames
        assertFalse(deserialized.getSamples().get(3).isSyncSample());
        assertFalse(deserialized.getSamples().get(3).isIndependent());
        assertTrue(deserialized.getSamples().get(3).getSampleFlags().isDependedUpon());
    }

    @Test
    @DisplayName("Test raw flags value round-trip")
    void testRawFlagsRoundTrip() {
        int originalFlags = 0x12345678;
        SampleFlags sampleFlags = new SampleFlags(originalFlags);

        assertEquals(originalFlags, sampleFlags.getFlags());
    }

    // ========== SAP Type Tests for CARP Support ==========

    @Test
    @DisplayName("Test SAP Type 1 detection (closed GOP IDR)")
    void testSapType1Detection() {
        // SAP Type 1: sync sample, independent, not a leading sample
        SampleFlags flags = SampleFlags.createSapType1Flags();

        assertEquals(SampleFlags.SapType.TYPE_1, flags.getSapType());
        assertEquals(1, flags.getSapTypeValue());
        assertTrue(flags.isStreamAccessPoint());
        assertTrue(flags.isSyncSample());
        assertTrue(flags.isIndependent());
        assertEquals(2, flags.getIsLeading()); // Not a leading sample
    }

    @Test
    @DisplayName("Test SAP Type 2 detection (open GOP IDR)")
    void testSapType2Detection() {
        // SAP Type 2: sync sample, independent, leading sample without dependency
        SampleFlags flags = SampleFlags.createSapType2Flags();

        assertEquals(SampleFlags.SapType.TYPE_2, flags.getSapType());
        assertEquals(2, flags.getSapTypeValue());
        assertTrue(flags.isStreamAccessPoint());
        assertTrue(flags.isSyncSample());
        assertTrue(flags.isIndependent());
        assertEquals(3, flags.getIsLeading()); // Leading, no dependency
    }

    @Test
    @DisplayName("Test SAP Type 3 detection (CRA/GDR)")
    void testSapType3Detection() {
        // SAP Type 3: sync sample, independent, leading sample with dependency
        SampleFlags flags = SampleFlags.createSapType3Flags();

        assertEquals(SampleFlags.SapType.TYPE_3, flags.getSapType());
        assertEquals(3, flags.getSapTypeValue());
        assertTrue(flags.isStreamAccessPoint());
        assertTrue(flags.isSyncSample());
        assertTrue(flags.isIndependent());
        assertEquals(1, flags.getIsLeading()); // Leading with dependency
    }

    @Test
    @DisplayName("Test non-SAP (non-sync sample)")
    void testNonSapNonSync() {
        SampleFlags flags = SampleFlags.createNonSyncSampleFlags();

        assertEquals(SampleFlags.SapType.NONE, flags.getSapType());
        assertEquals(0, flags.getSapTypeValue());
        assertFalse(flags.isStreamAccessPoint());
        assertFalse(flags.isSyncSample());
    }

    @Test
    @DisplayName("Test non-SAP (depends on other samples)")
    void testNonSapDependsOnOthers() {
        // Sync sample but depends on others - not a valid SAP
        int flags = (1 << 24); // sampleDependsOn=1 (depends on others), isNonSync=0
        SampleFlags sampleFlags = new SampleFlags(flags);

        assertEquals(SampleFlags.SapType.NONE, sampleFlags.getSapType());
        assertFalse(sampleFlags.isStreamAccessPoint());
    }

    @Test
    @DisplayName("Test SAP type with unknown leading status defaults to Type 1")
    void testSapTypeUnknownLeadingDefaultsToType1() {
        // Sync sample, independent (sampleDependsOn=2), but isLeading=0 (unknown)
        int flags = (0 << 26) | (2 << 24); // isLeading=0, sampleDependsOn=2
        SampleFlags sampleFlags = new SampleFlags(flags);

        // Should default to SAP Type 1 when independent but leading status unknown
        assertEquals(SampleFlags.SapType.TYPE_1, sampleFlags.getSapType());
        assertTrue(sampleFlags.isStreamAccessPoint());
    }

    @Test
    @DisplayName("Test createForSapType factory method with int")
    void testCreateForSapTypeInt() {
        assertEquals(SampleFlags.SapType.TYPE_1, SampleFlags.createForSapType(1).getSapType());
        assertEquals(SampleFlags.SapType.TYPE_2, SampleFlags.createForSapType(2).getSapType());
        assertEquals(SampleFlags.SapType.TYPE_3, SampleFlags.createForSapType(3).getSapType());

        // Invalid SAP types should return non-sync
        assertFalse(SampleFlags.createForSapType(0).isSyncSample());
        assertFalse(SampleFlags.createForSapType(4).isSyncSample());
    }

    @Test
    @DisplayName("Test createForSapType factory method with enum")
    void testCreateForSapTypeEnum() {
        assertEquals(SampleFlags.SapType.TYPE_1,
                SampleFlags.createForSapType(SampleFlags.SapType.TYPE_1).getSapType());
        assertEquals(SampleFlags.SapType.TYPE_2,
                SampleFlags.createForSapType(SampleFlags.SapType.TYPE_2).getSapType());
        assertEquals(SampleFlags.SapType.TYPE_3,
                SampleFlags.createForSapType(SampleFlags.SapType.TYPE_3).getSapType());
    }

    @Test
    @DisplayName("Test SapType enum fromValue")
    void testSapTypeEnumFromValue() {
        assertEquals(SampleFlags.SapType.NONE, SampleFlags.SapType.fromValue(0));
        assertEquals(SampleFlags.SapType.TYPE_1, SampleFlags.SapType.fromValue(1));
        assertEquals(SampleFlags.SapType.TYPE_2, SampleFlags.SapType.fromValue(2));
        assertEquals(SampleFlags.SapType.TYPE_3, SampleFlags.SapType.fromValue(3));
        assertEquals(SampleFlags.SapType.NONE, SampleFlags.SapType.fromValue(99)); // Invalid
    }

    @Test
    @DisplayName("Test CARP SAP timeline scenario - HEVC with CRA pictures")
    void testCarpSapTimelineScenario() {
        // Simulate the CARP spec example: 30fps HEVC with SAP-2 at group start, SAP-3 mid-group
        // Group 0, Object 0: SAP Type 2 (IDR)
        SampleFlags idrFlags = SampleFlags.createSapType2Flags();
        assertEquals(2, idrFlags.getSapTypeValue());

        // Group 0, Object 60: SAP Type 3 (CRA with RASL pictures)
        SampleFlags craFlags = SampleFlags.createSapType3Flags();
        assertEquals(3, craFlags.getSapTypeValue());

        // Other objects: non-SAP
        SampleFlags deltaFlags = SampleFlags.createNonSyncSampleFlags();
        assertEquals(0, deltaFlags.getSapTypeValue());

        // Verify CARP timeline format: [sapType, EPT]
        // Per spec: [2, 0], [3, 2100], [2, 4000], [3, 6100]...
        assertTrue(idrFlags.isStreamAccessPoint());
        assertTrue(craFlags.isStreamAccessPoint());
        assertFalse(deltaFlags.isStreamAccessPoint());
    }

    @Test
    @DisplayName("Test SAP type round-trip through TrunBox")
    void testSapTypeRoundTripTrunBox() throws IOException {
        // Create trun with different SAP types
        MoofBox.TrunBox trun = new MoofBox.TrunBox();
        trun.setVersion(0);
        trun.setTrunFlags(0x000401); // data_offset + sample_flags
        trun.setSampleCount(4);
        trun.setDataOffset(100);

        // SAP Type 1 (closed GOP IDR)
        MoofBox.TrunBox.Sample sap1 = new MoofBox.TrunBox.Sample();
        sap1.setSampleFlags(SampleFlags.createSapType1Flags());

        // SAP Type 2 (open GOP IDR)
        MoofBox.TrunBox.Sample sap2 = new MoofBox.TrunBox.Sample();
        sap2.setSampleFlags(SampleFlags.createSapType2Flags());

        // SAP Type 3 (CRA)
        MoofBox.TrunBox.Sample sap3 = new MoofBox.TrunBox.Sample();
        sap3.setSampleFlags(SampleFlags.createSapType3Flags());

        // Non-SAP
        MoofBox.TrunBox.Sample nonSap = new MoofBox.TrunBox.Sample();
        nonSap.setSampleFlags(SampleFlags.createNonSyncSampleFlags());

        trun.addSample(sap1);
        trun.addSample(sap2);
        trun.addSample(sap3);
        trun.addSample(nonSap);

        // Serialize and deserialize
        byte[] data = trun.serialize();
        MoofBox.TrunBox deserialized = new MoofBox.TrunBox();
        deserialized.deserialize(ByteBuffer.wrap(data));

        // Verify SAP types preserved
        assertEquals(1, deserialized.getSamples().get(0).getSampleFlags().getSapTypeValue());
        assertEquals(2, deserialized.getSamples().get(1).getSampleFlags().getSapTypeValue());
        assertEquals(3, deserialized.getSamples().get(2).getSampleFlags().getSapTypeValue());
        assertEquals(0, deserialized.getSamples().get(3).getSampleFlags().getSapTypeValue());
    }
}
