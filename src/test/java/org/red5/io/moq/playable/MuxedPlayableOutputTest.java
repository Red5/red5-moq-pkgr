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
 * Phase B: Multi-track (muxed) playable output tests.
 *
 * Each test produces a dual-track fragmented MP4 with both H.264 video and Opus audio,
 * optionally round-tripping through LOC or MoqMI serialization first.
 * Validated with ffprobe/ffmpeg for stream count, codec properties, timestamps, and decode cleanliness.
 */
class MuxedPlayableOutputTest {

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

        Path h264Path = TestMediaGenerator.generateH264AnnexB(tempDir);
        Path oggPath = TestMediaGenerator.generateOpusOgg(tempDir);

        avcCConfig = TestMediaGenerator.buildAvcCConfig(h264Path);
        dOpsConfig = TestMediaGenerator.buildDOpsConfig();

        Path fmp4Path = TestMediaGenerator.generateFmp4(h264Path, null, tempDir);
        videoFrames = TestMediaGenerator.extractVideoFrames(fmp4Path, tempDir);
        audioFrames = TestMediaGenerator.extractOpusFrames(oggPath, tempDir);

        assertFalse(videoFrames.isEmpty(), "No video frames extracted");
        assertFalse(audioFrames.isEmpty(), "No audio frames extracted");
    }

    @Test
    void cmafMuxedVideoAudioProducesPlayableMp4() throws Exception {
        // Init segment with both tracks: video trackId=1, audio trackId=2
        byte[] initSegment = new Fmp4InitSegmentBuilder()
                .addVideoTrack(new Fmp4InitSegmentBuilder.VideoTrackConfig(
                        1, 90000, "avc1", avcCConfig, 320, 240))
                .addAudioTrack(new Fmp4InitSegmentBuilder.AudioTrackConfig(
                        2, 48000, "Opus", dOpsConfig, 1, 48000, 16))
                .build();

        Fmp4FragmentBuilder builder = new Fmp4FragmentBuilder();
        ByteArrayOutputStream mp4Out = new ByteArrayOutputStream();
        mp4Out.write(initSegment);

        // Write interleaved video and audio fragments (one video GOP + matching audio per iteration)
        int framesPerGop = 30;
        int gopCount = (videoFrames.size() + framesPerGop - 1) / framesPerGop;

        for (int gop = 0; gop < gopCount; gop++) {
            int startFrame = gop * framesPerGop;
            int endFrame = Math.min(startFrame + framesPerGop, videoFrames.size());
            var gopFrames = videoFrames.subList(startFrame, endFrame);

            // Video fragment
            ByteArrayOutputStream mdatData = new ByteArrayOutputStream();
            List<Fmp4FragmentBuilder.SampleData> samples = new ArrayList<>();

            for (var frame : gopFrames) {
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

            // Interleave: write audio fragment covering the same time range
            // Video GOP covers baseDecodeTime/90000 to (baseDecodeTime + 30*3000)/90000 seconds
            double gopStartSec = baseDecodeTime / 90000.0;
            double gopEndSec = (baseDecodeTime + framesPerGop * 3000L) / 90000.0;

            // Find audio frames in this time window (half-open interval to avoid duplicates)
            List<TestMediaGenerator.AudioFrame> audioInRange = new ArrayList<>();
            for (var af : audioFrames) {
                double afTimeSec = af.pts() / 48000.0;
                if (afTimeSec >= gopStartSec - 0.001 && afTimeSec < gopEndSec - 0.001) {
                    audioInRange.add(af);
                }
            }

            if (!audioInRange.isEmpty()) {
                ByteArrayOutputStream audioMdat = new ByteArrayOutputStream();
                List<Fmp4FragmentBuilder.SampleData> audioSamples = new ArrayList<>();

                for (var af : audioInRange) {
                    audioMdat.write(af.opusData());
                    audioSamples.add(new Fmp4FragmentBuilder.SampleData(
                            af.duration(), af.opusData().length, SampleFlags.createSyncSampleFlags()));
                }

                long audioBaseDecodeTime = audioInRange.get(0).pts();
                Fmp4FragmentBuilder.FragmentConfig audioConfig = new Fmp4FragmentBuilder.FragmentConfig()
                        .setSequenceNumber(gop + 1 + gopCount)
                        .setTrackId(2)
                        .setBaseDecodeTime(audioBaseDecodeTime)
                        .setMediaData(audioMdat.toByteArray())
                        .setMediaType(CmafFragment.MediaType.AUDIO)
                        .setSamples(audioSamples);

                mp4Out.write(builder.buildFragment(audioConfig).serialize());
            }
        }

        Path outputFile = tempDir.resolve("cmaf_muxed.mp4");
        Files.write(outputFile, mp4Out.toByteArray());

        FfprobeValidator.assertStreamCount(outputFile, 2);
        FfprobeValidator.assertValidVideo(outputFile, 320, 240, "h264");
        FfprobeValidator.assertValidAudio(outputFile, 48000, 1, "opus");
        FfprobeValidator.assertTimestampsValid(outputFile, "v:0");
        FfprobeValidator.assertTimestampsValid(outputFile, "a:0");
        FfprobeValidator.assertDurationWithin(outputFile, 2.0, 0.5);
        FfprobeValidator.assertDecodesCleanly(outputFile);
    }

    @Test
    void locMuxedRoundTripProducesPlayableMp4() throws Exception {
        // Round-trip all frames through LOC, then mux into fMP4
        LocSerializer locSerializer = new LocSerializer();
        LocDeserializer locDeserializer = new LocDeserializer();

        byte[] rawAvcC = new byte[avcCConfig.length - 8];
        System.arraycopy(avcCConfig, 8, rawAvcC, 0, rawAvcC.length);

        // Round-trip video through LOC
        List<TestMediaGenerator.VideoFrame> rtVideo = new ArrayList<>();
        for (int i = 0; i < videoFrames.size(); i++) {
            var vf = videoFrames.get(i);
            LocObject locObj = new LocObject(LocObject.MediaType.VIDEO, vf.avccData());
            locObj.setVideoFrameMarking(vf.isKeyframe(), !vf.isKeyframe(), false, 0, 0);
            if (vf.isKeyframe()) locObj.setVideoConfig(rawAvcC);

            byte[] hdr = locSerializer.serializeHeaderExtensions(locObj);
            LocObject deser = locDeserializer.deserialize(hdr, vf.avccData(),
                    LocObject.MediaType.VIDEO);
            rtVideo.add(new TestMediaGenerator.VideoFrame(
                    deser.getPayload(), vf.pts(), vf.dts(), vf.duration(), vf.isKeyframe()));
        }

        // Round-trip audio through LOC
        List<TestMediaGenerator.AudioFrame> rtAudio = new ArrayList<>();
        for (int i = 0; i < audioFrames.size(); i++) {
            var af = audioFrames.get(i);
            LocObject locObj = new LocObject(LocObject.MediaType.AUDIO, af.opusData());
            locObj.setAudioLevel(true, 10);

            byte[] hdr = locSerializer.serializeHeaderExtensions(locObj);
            LocObject deser = locDeserializer.deserialize(hdr, af.opusData(),
                    LocObject.MediaType.AUDIO);
            rtAudio.add(new TestMediaGenerator.AudioFrame(
                    deser.getPayload(), af.pts(), af.duration(), af.sampleRate(), af.channels()));
        }

        // Build muxed fMP4 using round-tripped frames
        Path outputFile = tempDir.resolve("loc_muxed.mp4");
        writeMuxedMp4(rtVideo, rtAudio, outputFile);

        FfprobeValidator.assertStreamCount(outputFile, 2);
        FfprobeValidator.assertValidVideo(outputFile, 320, 240, "h264");
        FfprobeValidator.assertValidAudio(outputFile, 48000, 1, "opus");
        FfprobeValidator.assertTimestampsValid(outputFile, "v:0");
        FfprobeValidator.assertTimestampsValid(outputFile, "a:0");
        FfprobeValidator.assertDurationWithin(outputFile, 2.0, 0.5);
        FfprobeValidator.assertDecodesCleanly(outputFile);
    }

    @Test
    void moqmiMuxedRoundTripProducesPlayableMp4() throws Exception {
        MoqMISerializer moqmiSerializer = new MoqMISerializer();
        MoqMIDeserializer moqmiDeserializer = new MoqMIDeserializer();

        byte[] rawAvcC = new byte[avcCConfig.length - 8];
        System.arraycopy(avcCConfig, 8, rawAvcC, 0, rawAvcC.length);

        // Round-trip video through MoqMI
        List<TestMediaGenerator.VideoFrame> rtVideo = new ArrayList<>();
        for (int i = 0; i < videoFrames.size(); i++) {
            var vf = videoFrames.get(i);
            MoqMIObject obj = new MoqMIObject(MoqMIObject.MediaType.VIDEO_H264_AVCC, vf.avccData());
            obj.addHeaderExtension(new MediaTypeExtension(MoqMIObject.MediaType.VIDEO_H264_AVCC));
            obj.addHeaderExtension(new H264MetadataExtension(
                    i, vf.pts(), vf.dts(), 90000, vf.duration(), 0));
            if (vf.isKeyframe()) {
                obj.addHeaderExtension(new H264ExtradataExtension(rawAvcC));
            }

            byte[] hdr = moqmiSerializer.serializeHeaderExtensions(obj);
            MoqMIObject deser = moqmiDeserializer.deserialize(hdr, vf.avccData());
            rtVideo.add(new TestMediaGenerator.VideoFrame(
                    deser.getPayload(), vf.pts(), vf.dts(), vf.duration(), vf.isKeyframe()));
        }

        // Round-trip audio through MoqMI
        List<TestMediaGenerator.AudioFrame> rtAudio = new ArrayList<>();
        for (int i = 0; i < audioFrames.size(); i++) {
            var af = audioFrames.get(i);
            MoqMIObject obj = new MoqMIObject(MoqMIObject.MediaType.AUDIO_OPUS, af.opusData());
            obj.addHeaderExtension(new MediaTypeExtension(MoqMIObject.MediaType.AUDIO_OPUS));
            obj.addHeaderExtension(new OpusDataExtension(
                    i, af.pts(), 48000, 48000, 1, af.duration(), 0));

            byte[] hdr = moqmiSerializer.serializeHeaderExtensions(obj);
            MoqMIObject deser = moqmiDeserializer.deserialize(hdr, af.opusData());
            rtAudio.add(new TestMediaGenerator.AudioFrame(
                    deser.getPayload(), af.pts(), af.duration(), af.sampleRate(), af.channels()));
        }

        Path outputFile = tempDir.resolve("moqmi_muxed.mp4");
        writeMuxedMp4(rtVideo, rtAudio, outputFile);

        FfprobeValidator.assertStreamCount(outputFile, 2);
        FfprobeValidator.assertValidVideo(outputFile, 320, 240, "h264");
        FfprobeValidator.assertValidAudio(outputFile, 48000, 1, "opus");
        FfprobeValidator.assertTimestampsValid(outputFile, "v:0");
        FfprobeValidator.assertTimestampsValid(outputFile, "a:0");
        FfprobeValidator.assertDurationWithin(outputFile, 2.0, 0.5);
        FfprobeValidator.assertDecodesCleanly(outputFile);
    }

    /**
     * Shared helper: write a muxed dual-track fMP4 file from video and audio frame lists.
     * Video is trackId=1 (90kHz timescale), audio is trackId=2 (48kHz timescale).
     * Fragments are interleaved: one video GOP followed by the corresponding audio fragment.
     */
    private void writeMuxedMp4(List<TestMediaGenerator.VideoFrame> video,
                               List<TestMediaGenerator.AudioFrame> audio,
                               Path outputFile) throws Exception {
        byte[] initSegment = new Fmp4InitSegmentBuilder()
                .addVideoTrack(new Fmp4InitSegmentBuilder.VideoTrackConfig(
                        1, 90000, "avc1", avcCConfig, 320, 240))
                .addAudioTrack(new Fmp4InitSegmentBuilder.AudioTrackConfig(
                        2, 48000, "Opus", dOpsConfig, 1, 48000, 16))
                .build();

        Fmp4FragmentBuilder builder = new Fmp4FragmentBuilder();
        ByteArrayOutputStream mp4Out = new ByteArrayOutputStream();
        mp4Out.write(initSegment);

        int framesPerGop = 30;
        int gopCount = (video.size() + framesPerGop - 1) / framesPerGop;

        for (int gop = 0; gop < gopCount; gop++) {
            int startFrame = gop * framesPerGop;
            int endFrame = Math.min(startFrame + framesPerGop, video.size());
            var gopFrames = video.subList(startFrame, endFrame);

            // Video fragment
            ByteArrayOutputStream mdatData = new ByteArrayOutputStream();
            List<Fmp4FragmentBuilder.SampleData> samples = new ArrayList<>();
            for (var frame : gopFrames) {
                mdatData.write(frame.avccData());
                SampleFlags flags = frame.isKeyframe()
                        ? SampleFlags.createSyncSampleFlags()
                        : SampleFlags.createNonSyncSampleFlags();
                samples.add(new Fmp4FragmentBuilder.SampleData(
                        frame.duration(), frame.avccData().length, flags));
            }
            long baseDecodeTime = gopFrames.get(0).dts();
            mp4Out.write(builder.buildFragment(new Fmp4FragmentBuilder.FragmentConfig()
                    .setSequenceNumber(gop + 1)
                    .setTrackId(1)
                    .setBaseDecodeTime(baseDecodeTime)
                    .setMediaData(mdatData.toByteArray())
                    .setMediaType(CmafFragment.MediaType.VIDEO)
                    .setSamples(samples)).serialize());

            // Audio fragment for same time range
            double gopStartSec = baseDecodeTime / 90000.0;
            double gopEndSec = (baseDecodeTime + framesPerGop * 3000L) / 90000.0;

            List<TestMediaGenerator.AudioFrame> audioInRange = new ArrayList<>();
            for (var af : audio) {
                double t = af.pts() / 48000.0;
                if (t >= gopStartSec - 0.001 && t < gopEndSec - 0.001) {
                    audioInRange.add(af);
                }
            }

            if (!audioInRange.isEmpty()) {
                ByteArrayOutputStream audioMdat = new ByteArrayOutputStream();
                List<Fmp4FragmentBuilder.SampleData> audioSamples = new ArrayList<>();
                for (var af : audioInRange) {
                    audioMdat.write(af.opusData());
                    audioSamples.add(new Fmp4FragmentBuilder.SampleData(
                            af.duration(), af.opusData().length, SampleFlags.createSyncSampleFlags()));
                }

                mp4Out.write(builder.buildFragment(new Fmp4FragmentBuilder.FragmentConfig()
                        .setSequenceNumber(gop + 1 + gopCount)
                        .setTrackId(2)
                        .setBaseDecodeTime(audioInRange.get(0).pts())
                        .setMediaData(audioMdat.toByteArray())
                        .setMediaType(CmafFragment.MediaType.AUDIO)
                        .setSamples(audioSamples)).serialize());
            }
        }

        Files.write(outputFile, mp4Out.toByteArray());
    }
}
