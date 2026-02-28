package org.red5.io.moq.playable;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.red5.io.moq.cmaf.model.CmafFragment;
import org.red5.io.moq.cmaf.model.SampleFlags;
import org.red5.io.moq.cmaf.util.Fmp4FragmentBuilder;
import org.red5.io.moq.cmaf.util.Fmp4InitSegmentBuilder;
import org.red5.io.moq.loc.deserialize.LocDeserializer;
import org.red5.io.moq.loc.model.LocObject;
import org.red5.io.moq.loc.serialize.LocSerializer;
import org.red5.io.moq.moqmi.deserialize.MoqMIDeserializer;
import org.red5.io.moq.moqmi.model.H264ExtradataExtension;
import org.red5.io.moq.moqmi.model.H264MetadataExtension;
import org.red5.io.moq.moqmi.model.MediaTypeExtension;
import org.red5.io.moq.moqmi.model.MoqMIObject;
import org.red5.io.moq.moqmi.model.OpusDataExtension;
import org.red5.io.moq.moqmi.serialize.MoqMISerializer;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Phase A: Single-track playable output tests.
 *
 * Each test generates real H.264/Opus media via TestMediaGenerator, serializes through
 * a packaging format (CMAF, LOC, or MoqMI), wraps into a playable fragmented MP4 via
 * Fmp4InitSegmentBuilder/Fmp4FragmentBuilder, and validates with FfprobeValidator.
 */
class PlayableOutputTest {

    @TempDir
    static Path tempDir;

    static List<TestMediaGenerator.VideoFrame> videoFrames;
    static List<TestMediaGenerator.AudioFrame> audioFrames;
    static byte[] avcCConfig;
    static byte[] dOpsConfig;

    @BeforeAll
    static void generateMedia() throws Exception {
        assumeTrue(TestMediaGenerator.isFfmpegAvailable(), "ffmpeg not available");
        assumeTrue(TestMediaGenerator.isFfprobeAvailable(), "ffprobe not available");

        // Generate raw media
        Path h264Path = TestMediaGenerator.generateH264AnnexB(tempDir);
        Path oggPath = TestMediaGenerator.generateOpusOgg(tempDir);

        // Build codec configs
        avcCConfig = TestMediaGenerator.buildAvcCConfig(h264Path);
        dOpsConfig = TestMediaGenerator.buildDOpsConfig();

        // Generate fMP4 intermediate for frame extraction
        Path fmp4Path = TestMediaGenerator.generateFmp4(h264Path, null, tempDir);
        videoFrames = TestMediaGenerator.extractVideoFrames(fmp4Path, tempDir);
        audioFrames = TestMediaGenerator.extractOpusFrames(oggPath, tempDir);

        assertFalse(videoFrames.isEmpty(), "No video frames extracted");
        assertFalse(audioFrames.isEmpty(), "No audio frames extracted");
    }

    @Test
    void cmafVideoProducesPlayableMp4() throws Exception {
        // Build init segment with video track
        byte[] initSegment = new Fmp4InitSegmentBuilder()
                .addVideoTrack(new Fmp4InitSegmentBuilder.VideoTrackConfig(
                        1, 90000, "avc1", avcCConfig, 320, 240))
                .build();

        // Build one fragment per GOP (group of 30 frames at 30fps = 1 second)
        Fmp4FragmentBuilder builder = new Fmp4FragmentBuilder();
        ByteArrayOutputStream mp4Out = new ByteArrayOutputStream();
        mp4Out.write(initSegment);

        int framesPerGop = 30;
        int gopCount = (videoFrames.size() + framesPerGop - 1) / framesPerGop;

        for (int gop = 0; gop < gopCount; gop++) {
            int startFrame = gop * framesPerGop;
            int endFrame = Math.min(startFrame + framesPerGop, videoFrames.size());
            List<TestMediaGenerator.VideoFrame> gopFrames = videoFrames.subList(startFrame, endFrame);

            // Concatenate all frame data for this GOP into mdat
            ByteArrayOutputStream mdatData = new ByteArrayOutputStream();
            List<Fmp4FragmentBuilder.SampleData> samples = new ArrayList<>();

            for (TestMediaGenerator.VideoFrame frame : gopFrames) {
                mdatData.write(frame.avccData());
                SampleFlags flags = frame.isKeyframe()
                        ? SampleFlags.createSyncSampleFlags()
                        : SampleFlags.createNonSyncSampleFlags();
                samples.add(new Fmp4FragmentBuilder.SampleData(
                        frame.duration(), frame.avccData().length, flags));
            }

            long baseDecodeTime = gopFrames.get(0).dts();
            Fmp4FragmentBuilder.FragmentConfig config = new Fmp4FragmentBuilder.FragmentConfig()
                    .setSequenceNumber(gop + 1)
                    .setTrackId(1)
                    .setBaseDecodeTime(baseDecodeTime)
                    .setMediaData(mdatData.toByteArray())
                    .setMediaType(CmafFragment.MediaType.VIDEO)
                    .setSamples(samples);

            CmafFragment fragment = builder.buildFragment(config);
            mp4Out.write(fragment.serialize());
        }

        // Write to file
        Path outputFile = tempDir.resolve("cmaf_video.mp4");
        Files.write(outputFile, mp4Out.toByteArray());

        // Validate
        FfprobeValidator.assertStreamCount(outputFile, 1);
        FfprobeValidator.assertValidVideo(outputFile, 320, 240, "h264");
        FfprobeValidator.assertTimestampsValid(outputFile, "v:0");
        FfprobeValidator.assertDurationWithin(outputFile, 2.0, 0.5);
        FfprobeValidator.assertDecodesCleanly(outputFile);
    }

    @Test
    void cmafAudioProducesPlayableMp4() throws Exception {
        // Build init segment with Opus audio track
        byte[] initSegment = new Fmp4InitSegmentBuilder()
                .addAudioTrack(new Fmp4InitSegmentBuilder.AudioTrackConfig(
                        1, 48000, "Opus", dOpsConfig, 1, 48000, 16))
                .build();

        Fmp4FragmentBuilder builder = new Fmp4FragmentBuilder();
        ByteArrayOutputStream mp4Out = new ByteArrayOutputStream();
        mp4Out.write(initSegment);

        // One fragment per ~50 audio frames (1 second at 20ms per frame)
        int framesPerFragment = 50;
        int fragmentCount = (audioFrames.size() + framesPerFragment - 1) / framesPerFragment;

        for (int frag = 0; frag < fragmentCount; frag++) {
            int startFrame = frag * framesPerFragment;
            int endFrame = Math.min(startFrame + framesPerFragment, audioFrames.size());
            List<TestMediaGenerator.AudioFrame> fragFrames = audioFrames.subList(startFrame, endFrame);

            ByteArrayOutputStream mdatData = new ByteArrayOutputStream();
            List<Fmp4FragmentBuilder.SampleData> samples = new ArrayList<>();

            for (TestMediaGenerator.AudioFrame frame : fragFrames) {
                mdatData.write(frame.opusData());
                // Audio samples are always sync
                samples.add(new Fmp4FragmentBuilder.SampleData(
                        frame.duration(), frame.opusData().length, SampleFlags.createSyncSampleFlags()));
            }

            long baseDecodeTime = fragFrames.get(0).pts();
            Fmp4FragmentBuilder.FragmentConfig config = new Fmp4FragmentBuilder.FragmentConfig()
                    .setSequenceNumber(frag + 1)
                    .setTrackId(1)
                    .setBaseDecodeTime(baseDecodeTime)
                    .setMediaData(mdatData.toByteArray())
                    .setMediaType(CmafFragment.MediaType.AUDIO)
                    .setSamples(samples);

            CmafFragment fragment = builder.buildFragment(config);
            mp4Out.write(fragment.serialize());
        }

        Path outputFile = tempDir.resolve("cmaf_audio.mp4");
        Files.write(outputFile, mp4Out.toByteArray());

        FfprobeValidator.assertStreamCount(outputFile, 1);
        FfprobeValidator.assertValidAudio(outputFile, 48000, 1, "opus");
        FfprobeValidator.assertTimestampsValid(outputFile, "a:0");
        FfprobeValidator.assertDurationWithin(outputFile, 2.0, 0.5);
        FfprobeValidator.assertDecodesCleanly(outputFile);
    }

    @Test
    void locVideoRoundTripProducesPlayableMp4() throws Exception {
        // Serialize video frames through LOC, deserialize, extract payloads, wrap in fMP4
        LocSerializer locSerializer = new LocSerializer();
        LocDeserializer locDeserializer = new LocDeserializer();

        // Round-trip each frame through LOC
        List<TestMediaGenerator.VideoFrame> roundTrippedFrames = new ArrayList<>();
        // Get raw avcC config (strip box header for LOC VideoConfigExtension)
        byte[] rawAvcC = new byte[avcCConfig.length - 8];
        System.arraycopy(avcCConfig, 8, rawAvcC, 0, rawAvcC.length);

        for (int i = 0; i < videoFrames.size(); i++) {
            TestMediaGenerator.VideoFrame vf = videoFrames.get(i);

            // Build LOC object
            LocObject locObj = new LocObject(LocObject.MediaType.VIDEO, vf.avccData());
            locObj.setGroupId(i / 30 + 1);
            locObj.setObjectId(i);
            locObj.setVideoFrameMarking(vf.isKeyframe(), !vf.isKeyframe(), false, 0, 0);
            locObj.setCaptureTimestamp(vf.pts() * 1000 / 90); // Convert 90kHz ticks to microseconds
            if (vf.isKeyframe()) {
                locObj.setVideoConfig(rawAvcC);
            }

            // Serialize
            byte[] headerBytes = locSerializer.serializeHeaderExtensions(locObj);
            byte[] payloadBytes = locObj.getPayload();

            // Deserialize
            LocObject deserialized = locDeserializer.deserialize(headerBytes, payloadBytes,
                    LocObject.MediaType.VIDEO);

            // Extract payload - should match original
            assertArrayEquals(vf.avccData(), deserialized.getPayload(),
                    "LOC round-trip payload mismatch at frame " + i);

            roundTrippedFrames.add(new TestMediaGenerator.VideoFrame(
                    deserialized.getPayload(), vf.pts(), vf.dts(), vf.duration(), vf.isKeyframe()));
        }

        // Now wrap in fMP4 using the same logic as cmafVideoProducesPlayableMp4
        byte[] initSegment = new Fmp4InitSegmentBuilder()
                .addVideoTrack(new Fmp4InitSegmentBuilder.VideoTrackConfig(
                        1, 90000, "avc1", avcCConfig, 320, 240))
                .build();

        Fmp4FragmentBuilder builder = new Fmp4FragmentBuilder();
        ByteArrayOutputStream mp4Out = new ByteArrayOutputStream();
        mp4Out.write(initSegment);

        int framesPerGop = 30;
        int gopCount = (roundTrippedFrames.size() + framesPerGop - 1) / framesPerGop;

        for (int gop = 0; gop < gopCount; gop++) {
            int startFrame = gop * framesPerGop;
            int endFrame = Math.min(startFrame + framesPerGop, roundTrippedFrames.size());
            List<TestMediaGenerator.VideoFrame> gopFrames = roundTrippedFrames.subList(startFrame, endFrame);

            ByteArrayOutputStream mdatData = new ByteArrayOutputStream();
            List<Fmp4FragmentBuilder.SampleData> samples = new ArrayList<>();

            for (TestMediaGenerator.VideoFrame frame : gopFrames) {
                mdatData.write(frame.avccData());
                SampleFlags flags = frame.isKeyframe()
                        ? SampleFlags.createSyncSampleFlags()
                        : SampleFlags.createNonSyncSampleFlags();
                samples.add(new Fmp4FragmentBuilder.SampleData(
                        frame.duration(), frame.avccData().length, flags));
            }

            long baseDecodeTime = gopFrames.get(0).dts();
            Fmp4FragmentBuilder.FragmentConfig config = new Fmp4FragmentBuilder.FragmentConfig()
                    .setSequenceNumber(gop + 1)
                    .setTrackId(1)
                    .setBaseDecodeTime(baseDecodeTime)
                    .setMediaData(mdatData.toByteArray())
                    .setMediaType(CmafFragment.MediaType.VIDEO)
                    .setSamples(samples);

            mp4Out.write(builder.buildFragment(config).serialize());
        }

        Path outputFile = tempDir.resolve("loc_video.mp4");
        Files.write(outputFile, mp4Out.toByteArray());

        FfprobeValidator.assertStreamCount(outputFile, 1);
        FfprobeValidator.assertValidVideo(outputFile, 320, 240, "h264");
        FfprobeValidator.assertTimestampsValid(outputFile, "v:0");
        FfprobeValidator.assertDurationWithin(outputFile, 2.0, 0.5);
        FfprobeValidator.assertDecodesCleanly(outputFile);
    }

    @Test
    void locAudioRoundTripProducesPlayableMp4() throws Exception {
        LocSerializer locSerializer = new LocSerializer();
        LocDeserializer locDeserializer = new LocDeserializer();

        List<TestMediaGenerator.AudioFrame> roundTrippedFrames = new ArrayList<>();

        for (int i = 0; i < audioFrames.size(); i++) {
            TestMediaGenerator.AudioFrame af = audioFrames.get(i);

            LocObject locObj = new LocObject(LocObject.MediaType.AUDIO, af.opusData());
            locObj.setGroupId(i);
            locObj.setObjectId(i);
            locObj.setAudioLevel(true, 10);
            locObj.setCaptureTimestamp(af.pts() * 1000000 / 48000);

            byte[] headerBytes = locSerializer.serializeHeaderExtensions(locObj);
            byte[] payloadBytes = locObj.getPayload();

            LocObject deserialized = locDeserializer.deserialize(headerBytes, payloadBytes,
                    LocObject.MediaType.AUDIO);

            assertArrayEquals(af.opusData(), deserialized.getPayload(),
                    "LOC audio round-trip payload mismatch at frame " + i);

            roundTrippedFrames.add(new TestMediaGenerator.AudioFrame(
                    deserialized.getPayload(), af.pts(), af.duration(), af.sampleRate(), af.channels()));
        }

        byte[] initSegment = new Fmp4InitSegmentBuilder()
                .addAudioTrack(new Fmp4InitSegmentBuilder.AudioTrackConfig(
                        1, 48000, "Opus", dOpsConfig, 1, 48000, 16))
                .build();

        Fmp4FragmentBuilder builder = new Fmp4FragmentBuilder();
        ByteArrayOutputStream mp4Out = new ByteArrayOutputStream();
        mp4Out.write(initSegment);

        int framesPerFragment = 50;
        int fragmentCount = (roundTrippedFrames.size() + framesPerFragment - 1) / framesPerFragment;

        for (int frag = 0; frag < fragmentCount; frag++) {
            int start = frag * framesPerFragment;
            int end = Math.min(start + framesPerFragment, roundTrippedFrames.size());
            List<TestMediaGenerator.AudioFrame> fragFrames = roundTrippedFrames.subList(start, end);

            ByteArrayOutputStream mdatData = new ByteArrayOutputStream();
            List<Fmp4FragmentBuilder.SampleData> samples = new ArrayList<>();

            for (TestMediaGenerator.AudioFrame frame : fragFrames) {
                mdatData.write(frame.opusData());
                samples.add(new Fmp4FragmentBuilder.SampleData(
                        frame.duration(), frame.opusData().length, SampleFlags.createSyncSampleFlags()));
            }

            long baseDecodeTime = fragFrames.get(0).pts();
            Fmp4FragmentBuilder.FragmentConfig config = new Fmp4FragmentBuilder.FragmentConfig()
                    .setSequenceNumber(frag + 1)
                    .setTrackId(1)
                    .setBaseDecodeTime(baseDecodeTime)
                    .setMediaData(mdatData.toByteArray())
                    .setMediaType(CmafFragment.MediaType.AUDIO)
                    .setSamples(samples);

            mp4Out.write(builder.buildFragment(config).serialize());
        }

        Path outputFile = tempDir.resolve("loc_audio.mp4");
        Files.write(outputFile, mp4Out.toByteArray());

        FfprobeValidator.assertStreamCount(outputFile, 1);
        FfprobeValidator.assertValidAudio(outputFile, 48000, 1, "opus");
        FfprobeValidator.assertTimestampsValid(outputFile, "a:0");
        FfprobeValidator.assertDurationWithin(outputFile, 2.0, 0.5);
        FfprobeValidator.assertDecodesCleanly(outputFile);
    }

    @Test
    void moqmiVideoRoundTripProducesPlayableMp4() throws Exception {
        MoqMISerializer moqmiSerializer = new MoqMISerializer();
        MoqMIDeserializer moqmiDeserializer = new MoqMIDeserializer();

        byte[] rawAvcC = new byte[avcCConfig.length - 8];
        System.arraycopy(avcCConfig, 8, rawAvcC, 0, rawAvcC.length);

        List<TestMediaGenerator.VideoFrame> roundTrippedFrames = new ArrayList<>();

        for (int i = 0; i < videoFrames.size(); i++) {
            TestMediaGenerator.VideoFrame vf = videoFrames.get(i);

            MoqMIObject moqmiObj = new MoqMIObject(MoqMIObject.MediaType.VIDEO_H264_AVCC, vf.avccData());
            moqmiObj.setGroupId(i / 30 + 1);
            moqmiObj.setObjectId(i % 30);

            // Add required extensions
            moqmiObj.addHeaderExtension(new MediaTypeExtension(MoqMIObject.MediaType.VIDEO_H264_AVCC));
            moqmiObj.addHeaderExtension(new H264MetadataExtension(
                    i, vf.pts(), vf.dts(), 90000, vf.duration(), System.currentTimeMillis()));
            if (vf.isKeyframe()) {
                moqmiObj.addHeaderExtension(new H264ExtradataExtension(rawAvcC));
            }

            // Serialize
            byte[] headerBytes = moqmiSerializer.serializeHeaderExtensions(moqmiObj);
            byte[] payloadBytes = moqmiObj.getPayload();

            // Deserialize
            MoqMIObject deserialized = moqmiDeserializer.deserialize(headerBytes, payloadBytes);

            assertEquals(MoqMIObject.MediaType.VIDEO_H264_AVCC, deserialized.getMediaType());
            assertArrayEquals(vf.avccData(), deserialized.getPayload(),
                    "MoqMI video round-trip payload mismatch at frame " + i);

            roundTrippedFrames.add(new TestMediaGenerator.VideoFrame(
                    deserialized.getPayload(), vf.pts(), vf.dts(), vf.duration(), vf.isKeyframe()));
        }

        // Wrap in fMP4 (same as CMAF video test)
        byte[] initSegment = new Fmp4InitSegmentBuilder()
                .addVideoTrack(new Fmp4InitSegmentBuilder.VideoTrackConfig(
                        1, 90000, "avc1", avcCConfig, 320, 240))
                .build();

        Fmp4FragmentBuilder builder = new Fmp4FragmentBuilder();
        ByteArrayOutputStream mp4Out = new ByteArrayOutputStream();
        mp4Out.write(initSegment);

        int framesPerGop = 30;
        int gopCount = (roundTrippedFrames.size() + framesPerGop - 1) / framesPerGop;

        for (int gop = 0; gop < gopCount; gop++) {
            int startFrame = gop * framesPerGop;
            int endFrame = Math.min(startFrame + framesPerGop, roundTrippedFrames.size());
            List<TestMediaGenerator.VideoFrame> gopFrames = roundTrippedFrames.subList(startFrame, endFrame);

            ByteArrayOutputStream mdatData = new ByteArrayOutputStream();
            List<Fmp4FragmentBuilder.SampleData> samples = new ArrayList<>();

            for (TestMediaGenerator.VideoFrame frame : gopFrames) {
                mdatData.write(frame.avccData());
                SampleFlags flags = frame.isKeyframe()
                        ? SampleFlags.createSyncSampleFlags()
                        : SampleFlags.createNonSyncSampleFlags();
                samples.add(new Fmp4FragmentBuilder.SampleData(
                        frame.duration(), frame.avccData().length, flags));
            }

            long baseDecodeTime = gopFrames.get(0).dts();
            Fmp4FragmentBuilder.FragmentConfig config = new Fmp4FragmentBuilder.FragmentConfig()
                    .setSequenceNumber(gop + 1)
                    .setTrackId(1)
                    .setBaseDecodeTime(baseDecodeTime)
                    .setMediaData(mdatData.toByteArray())
                    .setMediaType(CmafFragment.MediaType.VIDEO)
                    .setSamples(samples);

            mp4Out.write(builder.buildFragment(config).serialize());
        }

        Path outputFile = tempDir.resolve("moqmi_video.mp4");
        Files.write(outputFile, mp4Out.toByteArray());

        FfprobeValidator.assertStreamCount(outputFile, 1);
        FfprobeValidator.assertValidVideo(outputFile, 320, 240, "h264");
        FfprobeValidator.assertTimestampsValid(outputFile, "v:0");
        FfprobeValidator.assertDurationWithin(outputFile, 2.0, 0.5);
        FfprobeValidator.assertDecodesCleanly(outputFile);
    }

    @Test
    void moqmiAudioRoundTripProducesPlayableMp4() throws Exception {
        MoqMISerializer moqmiSerializer = new MoqMISerializer();
        MoqMIDeserializer moqmiDeserializer = new MoqMIDeserializer();

        List<TestMediaGenerator.AudioFrame> roundTrippedFrames = new ArrayList<>();

        for (int i = 0; i < audioFrames.size(); i++) {
            TestMediaGenerator.AudioFrame af = audioFrames.get(i);

            MoqMIObject moqmiObj = new MoqMIObject(MoqMIObject.MediaType.AUDIO_OPUS, af.opusData());
            moqmiObj.setGroupId(i);
            moqmiObj.setObjectId(0);

            moqmiObj.addHeaderExtension(new MediaTypeExtension(MoqMIObject.MediaType.AUDIO_OPUS));
            moqmiObj.addHeaderExtension(new OpusDataExtension(
                    i, af.pts(), 48000, 48000, 1, af.duration(), System.currentTimeMillis()));

            byte[] headerBytes = moqmiSerializer.serializeHeaderExtensions(moqmiObj);
            byte[] payloadBytes = moqmiObj.getPayload();

            MoqMIObject deserialized = moqmiDeserializer.deserialize(headerBytes, payloadBytes);

            assertEquals(MoqMIObject.MediaType.AUDIO_OPUS, deserialized.getMediaType());
            assertArrayEquals(af.opusData(), deserialized.getPayload(),
                    "MoqMI audio round-trip payload mismatch at frame " + i);

            roundTrippedFrames.add(new TestMediaGenerator.AudioFrame(
                    deserialized.getPayload(), af.pts(), af.duration(), af.sampleRate(), af.channels()));
        }

        byte[] initSegment = new Fmp4InitSegmentBuilder()
                .addAudioTrack(new Fmp4InitSegmentBuilder.AudioTrackConfig(
                        1, 48000, "Opus", dOpsConfig, 1, 48000, 16))
                .build();

        Fmp4FragmentBuilder builder = new Fmp4FragmentBuilder();
        ByteArrayOutputStream mp4Out = new ByteArrayOutputStream();
        mp4Out.write(initSegment);

        int framesPerFragment = 50;
        int fragmentCount = (roundTrippedFrames.size() + framesPerFragment - 1) / framesPerFragment;

        for (int frag = 0; frag < fragmentCount; frag++) {
            int start = frag * framesPerFragment;
            int end = Math.min(start + framesPerFragment, roundTrippedFrames.size());
            List<TestMediaGenerator.AudioFrame> fragFrames = roundTrippedFrames.subList(start, end);

            ByteArrayOutputStream mdatData = new ByteArrayOutputStream();
            List<Fmp4FragmentBuilder.SampleData> samples = new ArrayList<>();

            for (TestMediaGenerator.AudioFrame frame : fragFrames) {
                mdatData.write(frame.opusData());
                samples.add(new Fmp4FragmentBuilder.SampleData(
                        frame.duration(), frame.opusData().length, SampleFlags.createSyncSampleFlags()));
            }

            long baseDecodeTime = fragFrames.get(0).pts();
            Fmp4FragmentBuilder.FragmentConfig config = new Fmp4FragmentBuilder.FragmentConfig()
                    .setSequenceNumber(frag + 1)
                    .setTrackId(1)
                    .setBaseDecodeTime(baseDecodeTime)
                    .setMediaData(mdatData.toByteArray())
                    .setMediaType(CmafFragment.MediaType.AUDIO)
                    .setSamples(samples);

            mp4Out.write(builder.buildFragment(config).serialize());
        }

        Path outputFile = tempDir.resolve("moqmi_audio.mp4");
        Files.write(outputFile, mp4Out.toByteArray());

        FfprobeValidator.assertStreamCount(outputFile, 1);
        FfprobeValidator.assertValidAudio(outputFile, 48000, 1, "opus");
        FfprobeValidator.assertTimestampsValid(outputFile, "a:0");
        FfprobeValidator.assertDurationWithin(outputFile, 2.0, 0.5);
        FfprobeValidator.assertDecodesCleanly(outputFile);
    }
}
