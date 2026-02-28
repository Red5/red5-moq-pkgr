# Playable Output Tests Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Unit tests that serialize real H.264/Opus media through CMAF, LOC, and MoqMI, produce playable fragmented MP4 files, and validate them with ffprobe/ffmpeg.

**Architecture:** Four new test-only classes in `src/test/java/org/red5/io/moq/playable/`. `TestMediaGenerator` uses ffmpeg to create short clips and parses them into frame lists. `FfprobeValidator` runs ffprobe/ffmpeg to verify output. `PlayableOutputTest` (Phase A) writes single-track fMP4 files. `MuxedPlayableOutputTest` (Phase B) writes dual-track fMP4 files.

**Tech Stack:** Java 21, JUnit 5 (5.10.1), existing CMAF/LOC/MoqMI serializers, ffmpeg/ffprobe (external, graceful skip if absent)

**Design doc:** `docs/plans/2026-02-27-playable-output-tests-design.md`

---

### Task 1: TestMediaGenerator - ffmpeg availability and video generation

**Files:**
- Create: `src/test/java/org/red5/io/moq/playable/TestMediaGenerator.java`

**Step 1: Create the TestMediaGenerator class with ffmpeg check and video generation**

This class provides shared test infrastructure. It runs ffmpeg to produce raw H.264 and parses the annex-B bitstream into individual NAL units with AVCC framing.

```java
package org.red5.io.moq.playable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates test media using ffmpeg and parses into frame records for test use.
 * All methods are static; the class holds no state.
 */
public class TestMediaGenerator {

    private static final Logger logger = LoggerFactory.getLogger(TestMediaGenerator.class);

    public record VideoFrame(byte[] avccData, long pts, long dts, long duration, boolean isKeyframe) {}
    public record AudioFrame(byte[] opusData, long pts, long duration, int sampleRate, int channels) {}

    /**
     * Check if ffmpeg is available on PATH.
     */
    public static boolean isFfmpegAvailable() {
        try {
            Process p = new ProcessBuilder("ffmpeg", "-version")
                    .redirectErrorStream(true).start();
            p.getInputStream().readAllBytes();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if ffprobe is available on PATH.
     */
    public static boolean isFfprobeAvailable() {
        try {
            Process p = new ProcessBuilder("ffprobe", "-version")
                    .redirectErrorStream(true).start();
            p.getInputStream().readAllBytes();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Generate a 2-second H.264 Baseline annex-B bitstream using ffmpeg.
     * 320x240, 30fps, keyframe every 30 frames (1 GOP per second).
     *
     * @param outputDir directory to write temporary files
     * @return path to the generated .h264 file
     */
    public static Path generateH264AnnexB(Path outputDir) throws IOException, InterruptedException {
        Path output = outputDir.resolve("video.h264");
        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg", "-y",
                "-f", "lavfi", "-i", "testsrc2=duration=2:size=320x240:rate=30",
                "-c:v", "libx264", "-profile:v", "baseline", "-level", "3.0",
                "-x264-params", "keyint=30:min-keyint=30:scenecut=0",
                "-bsf:v", "h264_mp4toannexb",
                "-f", "h264", output.toString()
        );
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String log = new String(p.getInputStream().readAllBytes());
        int exit = p.waitFor();
        if (exit != 0) {
            throw new IOException("ffmpeg video generation failed (exit " + exit + "): " + log);
        }
        logger.info("Generated H.264 annex-B: {} ({} bytes)", output, Files.size(output));
        return output;
    }

    /**
     * Generate a 2-second Opus audio file in OGG container using ffmpeg.
     * 48kHz mono, 20ms frames.
     *
     * @param outputDir directory to write temporary files
     * @return path to the generated .ogg file
     */
    public static Path generateOpusOgg(Path outputDir) throws IOException, InterruptedException {
        Path output = outputDir.resolve("audio.ogg");
        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg", "-y",
                "-f", "lavfi", "-i", "sine=frequency=440:duration=2:sample_rate=48000",
                "-c:a", "libopus", "-b:a", "64k", "-frame_duration", "20",
                "-f", "ogg", output.toString()
        );
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String log = new String(p.getInputStream().readAllBytes());
        int exit = p.waitFor();
        if (exit != 0) {
            throw new IOException("ffmpeg audio generation failed (exit " + exit + "): " + log);
        }
        logger.info("Generated Opus OGG: {} ({} bytes)", output, Files.size(output));
        return output;
    }

    /**
     * Generate a short fMP4 file from the source media using ffmpeg, then return its path.
     * This is used as an intermediate step to extract properly framed AVCC data and Opus packets.
     *
     * @param sourceVideo path to annex-B H.264 file (null to skip video)
     * @param sourceAudio path to OGG Opus file (null to skip audio)
     * @param outputDir directory for output
     * @return path to the generated fMP4
     */
    public static Path generateFmp4(Path sourceVideo, Path sourceAudio, Path outputDir)
            throws IOException, InterruptedException {
        Path output = outputDir.resolve("source.mp4");
        List<String> cmd = new ArrayList<>();
        cmd.addAll(List.of("ffmpeg", "-y"));
        if (sourceVideo != null) {
            cmd.addAll(List.of("-i", sourceVideo.toString()));
        }
        if (sourceAudio != null) {
            cmd.addAll(List.of("-i", sourceAudio.toString()));
        }
        cmd.addAll(List.of(
                "-c", "copy",
                "-movflags", "frag_keyframe+empty_moov+default_base_moof",
                "-f", "mp4", output.toString()
        ));
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String log = new String(p.getInputStream().readAllBytes());
        int exit = p.waitFor();
        if (exit != 0) {
            throw new IOException("ffmpeg fMP4 generation failed (exit " + exit + "): " + log);
        }
        logger.info("Generated fMP4: {} ({} bytes)", output, Files.size(output));
        return output;
    }

    /**
     * Extract video frame data from an fMP4 into raw per-frame AVCC byte arrays using ffprobe packet info.
     * Returns a list of VideoFrame records with AVCC-framed data, timestamps, and keyframe flags.
     *
     * @param fmp4Path path to the fMP4 file
     * @param outputDir temp directory for extraction
     * @return list of video frames
     */
    public static List<VideoFrame> extractVideoFrames(Path fmp4Path, Path outputDir)
            throws IOException, InterruptedException {
        // Use ffprobe to get packet info (pts, dts, flags, size)
        ProcessBuilder probePb = new ProcessBuilder(
                "ffprobe", "-v", "quiet",
                "-select_streams", "v:0",
                "-show_packets",
                "-print_format", "csv=p=0",
                fmp4Path.toString()
        );
        probePb.redirectErrorStream(true);
        Process probeP = probePb.start();
        String probeOut = new String(probeP.getInputStream().readAllBytes());
        probeP.waitFor();

        // Extract raw h264 in AVCC format using ffmpeg -bsf:v h264_mp4toannexb then re-wrap
        // Simpler approach: extract raw video elementary stream
        Path rawVideo = outputDir.resolve("raw_video.h264");
        ProcessBuilder extractPb = new ProcessBuilder(
                "ffmpeg", "-y", "-i", fmp4Path.toString(),
                "-c:v", "copy", "-an",
                "-bsf:v", "h264_mp4toannexb",
                "-f", "h264", rawVideo.toString()
        );
        extractPb.redirectErrorStream(true);
        Process extractP = extractPb.start();
        extractP.getInputStream().readAllBytes();
        extractP.waitFor();

        byte[] h264Data = Files.readAllBytes(rawVideo);
        List<byte[]> nalUnits = parseAnnexBNalUnits(h264Data);

        // Parse ffprobe CSV to get per-packet info
        // CSV columns: codec_type,stream_index,pts,pts_time,dts,dts_time,duration,duration_time,size,pos,flags
        String[] lines = probeOut.strip().split("\n");
        List<VideoFrame> frames = new ArrayList<>();

        // Group NALUs into frames based on ffprobe packet count
        // Each ffprobe packet corresponds to one access unit (may have multiple NALUs)
        int naluIndex = 0;
        long frameDurationTicks = 3000; // 90kHz / 30fps = 3000

        for (String line : lines) {
            if (line.isBlank()) continue;
            String[] cols = line.split(",");
            if (cols.length < 11) continue;

            long pts, dts;
            try {
                pts = Long.parseLong(cols[2].strip());
                dts = Long.parseLong(cols[4].strip());
            } catch (NumberFormatException e) {
                // Use time-based columns and convert to 90kHz ticks
                double ptsTime = Double.parseDouble(cols[3].strip());
                double dtsTime = Double.parseDouble(cols[5].strip());
                pts = Math.round(ptsTime * 90000);
                dts = Math.round(dtsTime * 90000);
            }
            boolean isKey = cols[10].strip().contains("K");

            // Collect NALUs for this frame: skip SPS/PPS NALUs for non-key frames
            // For simplicity, collect one NALU per frame from the parsed list
            // skipping SPS (type 7) and PPS (type 8) parameter set NALUs
            List<byte[]> frameNalus = new ArrayList<>();
            while (naluIndex < nalUnits.size()) {
                byte[] nalu = nalUnits.get(naluIndex);
                int naluType = nalu[0] & 0x1F;
                naluIndex++;
                if (naluType == 7 || naluType == 8) {
                    // SPS/PPS - skip (we extract these separately for init segment)
                    continue;
                }
                frameNalus.add(nalu);
                // AUD (type 9) signals frame boundary, but for baseline we typically get
                // one slice NALU per frame. Break after first VCL NALU.
                if (naluType >= 1 && naluType <= 5) {
                    break;
                }
            }

            // Convert to AVCC format: 4-byte length prefix per NALU
            byte[] avccData = toAvcc(frameNalus);
            frames.add(new VideoFrame(avccData, pts, dts, frameDurationTicks, isKey));
        }

        logger.info("Extracted {} video frames from {}", frames.size(), fmp4Path);
        return frames;
    }

    /**
     * Extract SPS and PPS from annex-B H.264 bitstream and build AVCDecoderConfigurationRecord.
     *
     * @param annexBPath path to annex-B .h264 file
     * @return AVCDecoderConfigurationRecord bytes suitable for avcC box
     */
    public static byte[] buildAvcCConfig(Path annexBPath) throws IOException {
        byte[] data = Files.readAllBytes(annexBPath);
        List<byte[]> nalUnits = parseAnnexBNalUnits(data);

        byte[] sps = null, pps = null;
        for (byte[] nalu : nalUnits) {
            int type = nalu[0] & 0x1F;
            if (type == 7 && sps == null) sps = nalu;
            if (type == 8 && pps == null) pps = nalu;
            if (sps != null && pps != null) break;
        }
        if (sps == null || pps == null) {
            throw new IOException("SPS or PPS not found in H.264 bitstream");
        }

        // Build AVCDecoderConfigurationRecord (ISO 14496-15 section 5.3.3.1)
        // configurationVersion=1, profile/compat/level from SPS, lengthSizeMinusOne=3
        ByteBuffer cfg = ByteBuffer.allocate(11 + sps.length + pps.length);
        cfg.put((byte) 1);             // configurationVersion
        cfg.put(sps[1]);               // AVCProfileIndication
        cfg.put(sps[2]);               // profile_compatibility
        cfg.put(sps[3]);               // AVCLevelIndication
        cfg.put((byte) 0xFF);          // lengthSizeMinusOne=3 (0b11111111)
        cfg.put((byte) 0xE1);          // numOfSequenceParameterSets=1 (0b11100001)
        cfg.putShort((short) sps.length);
        cfg.put(sps);
        cfg.put((byte) 1);             // numOfPictureParameterSets
        cfg.putShort((short) pps.length);
        cfg.put(pps);

        // Wrap in avcC box: [size:4][type:4][config]
        byte[] configBytes = cfg.array();
        ByteBuffer box = ByteBuffer.allocate(8 + configBytes.length);
        box.putInt(8 + configBytes.length);
        box.put(new byte[]{'a', 'v', 'c', 'C'});
        box.put(configBytes);
        return box.array();
    }

    /**
     * Extract Opus packets from an OGG file by re-encoding to raw packets via fMP4 intermediate.
     *
     * @param oggPath path to OGG Opus file
     * @param outputDir temp directory
     * @return list of AudioFrame records
     */
    public static List<AudioFrame> extractOpusFrames(Path oggPath, Path outputDir)
            throws IOException, InterruptedException {
        // Create a temporary fMP4 with Opus to get proper packet framing
        Path tempMp4 = outputDir.resolve("opus_temp.mp4");
        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg", "-y", "-i", oggPath.toString(),
                "-c:a", "copy",
                "-movflags", "frag_keyframe+empty_moov+default_base_moof",
                "-f", "mp4", tempMp4.toString()
        );
        pb.redirectErrorStream(true);
        Process p = pb.start();
        p.getInputStream().readAllBytes();
        int exit = p.waitFor();
        if (exit != 0) {
            throw new IOException("Failed to create Opus fMP4");
        }

        // Use ffprobe to get per-packet data
        ProcessBuilder probePb = new ProcessBuilder(
                "ffprobe", "-v", "quiet",
                "-select_streams", "a:0",
                "-show_packets",
                "-show_data",
                "-print_format", "csv=p=0",
                tempMp4.toString()
        );
        probePb.redirectErrorStream(true);
        Process probeP = probePb.start();
        String probeOut = new String(probeP.getInputStream().readAllBytes());
        probeP.waitFor();

        // Extract raw Opus packets using ffmpeg
        Path rawOpus = outputDir.resolve("raw_opus.ogg");
        ProcessBuilder extractPb = new ProcessBuilder(
                "ffmpeg", "-y", "-i", tempMp4.toString(),
                "-c:a", "copy", "-vn",
                "-f", "ogg", rawOpus.toString()
        );
        extractPb.redirectErrorStream(true);
        Process extractP = extractPb.start();
        extractP.getInputStream().readAllBytes();
        extractP.waitFor();

        // Parse packet info from ffprobe output
        String[] lines = probeOut.strip().split("\n");
        List<AudioFrame> frames = new ArrayList<>();
        long ptsAccumulator = 0;
        long frameDuration48k = 960; // 20ms at 48kHz

        // Read raw Opus data from the temp MP4 using ffmpeg to extract packets
        // Simpler: use ffmpeg to dump each packet as raw data
        Path rawPackets = outputDir.resolve("opus_packets.raw");
        ProcessBuilder rawPb = new ProcessBuilder(
                "ffmpeg", "-y", "-i", tempMp4.toString(),
                "-c:a", "copy", "-vn",
                "-f", "data", rawPackets.toString()
        );
        rawPb.redirectErrorStream(true);
        Process rawP = rawPb.start();
        rawP.getInputStream().readAllBytes();
        rawP.waitFor();

        // For Opus in fMP4, each packet in mdat is a raw Opus packet
        // Parse the ffprobe CSV to get sizes and timestamps, then slice the raw data
        // Since this is complex, use a simpler approach: generate individual packets via ffprobe JSON
        ProcessBuilder jsonPb = new ProcessBuilder(
                "ffprobe", "-v", "quiet",
                "-select_streams", "a:0",
                "-show_packets",
                "-print_format", "json",
                tempMp4.toString()
        );
        jsonPb.redirectErrorStream(true);
        Process jsonP = jsonPb.start();
        String jsonOut = new String(jsonP.getInputStream().readAllBytes());
        jsonP.waitFor();

        // Parse JSON manually (avoid adding Gson dependency to test scope only for this)
        // Extract pts_time, duration_time, size from each packet
        // Simple state machine parser for the JSON
        String[] jsonLines = jsonOut.split("\n");
        double currentPts = 0;
        double currentDuration = 0;
        int currentSize = 0;
        boolean inPacket = false;

        // Also extract actual packet bytes from the fMP4 mdat
        byte[] mp4Bytes = Files.readAllBytes(tempMp4);

        for (String jline : jsonLines) {
            String trimmed = jline.trim();
            if (trimmed.contains("\"pts_time\"")) {
                currentPts = parseJsonDouble(trimmed);
            } else if (trimmed.contains("\"duration_time\"")) {
                currentDuration = parseJsonDouble(trimmed);
            } else if (trimmed.contains("\"size\"")) {
                currentSize = parseJsonInt(trimmed);
            } else if (trimmed.contains("\"pos\"")) {
                long pos = parseJsonLong(trimmed);
                if (currentSize > 0 && pos > 0 && pos + currentSize <= mp4Bytes.length) {
                    byte[] packetData = new byte[currentSize];
                    System.arraycopy(mp4Bytes, (int) pos, packetData, 0, currentSize);
                    long pts48k = Math.round(currentPts * 48000);
                    long dur48k = Math.round(currentDuration * 48000);
                    frames.add(new AudioFrame(packetData, pts48k, dur48k > 0 ? dur48k : frameDuration48k,
                            48000, 1));
                }
            }
        }

        logger.info("Extracted {} Opus frames from {}", frames.size(), oggPath);
        return frames;
    }

    /**
     * Build an Opus dOps (OpusSpecificBox) config for the init segment.
     * Mono, 48kHz, no pre-skip.
     *
     * @return dOps box bytes for AudioSampleEntry codecConfig
     */
    public static byte[] buildDOpsConfig() {
        // OpusSpecificBox per RFC 7845 / Opus in ISO BMFF spec
        // Version=0, OutputChannelCount=1, PreSkip=0, InputSampleRate=48000,
        // OutputGain=0, ChannelMappingFamily=0
        ByteBuffer dOps = ByteBuffer.allocate(19);
        dOps.order(ByteOrder.BIG_ENDIAN);
        dOps.putInt(19);                // box size
        dOps.put(new byte[]{'d', 'O', 'p', 's'}); // box type
        dOps.put((byte) 0);            // Version
        dOps.put((byte) 1);            // OutputChannelCount
        dOps.putShort((short) 0);      // PreSkip
        dOps.putInt(48000);             // InputSampleRate
        dOps.putShort((short) 0);      // OutputGain
        dOps.put((byte) 0);            // ChannelMappingFamily (0 = mono/stereo)
        return dOps.array();
    }

    // --- Internal helpers ---

    /**
     * Parse annex-B bitstream into individual NAL units (without start codes).
     */
    static List<byte[]> parseAnnexBNalUnits(byte[] data) {
        List<byte[]> nalus = new ArrayList<>();
        int i = 0;
        while (i < data.length) {
            // Find start code (0x000001 or 0x00000001)
            int startCodeLen = findStartCode(data, i);
            if (startCodeLen == 0) {
                i++;
                continue;
            }
            int naluStart = i + startCodeLen;

            // Find next start code or end of data
            int naluEnd = data.length;
            for (int j = naluStart; j < data.length - 2; j++) {
                if (data[j] == 0 && data[j + 1] == 0 &&
                        (data[j + 2] == 1 || (j + 3 < data.length && data[j + 2] == 0 && data[j + 3] == 1))) {
                    naluEnd = j;
                    break;
                }
            }

            if (naluEnd > naluStart) {
                byte[] nalu = new byte[naluEnd - naluStart];
                System.arraycopy(data, naluStart, nalu, 0, nalu.length);
                nalus.add(nalu);
            }
            i = naluEnd;
        }
        return nalus;
    }

    private static int findStartCode(byte[] data, int offset) {
        if (offset + 3 < data.length &&
                data[offset] == 0 && data[offset + 1] == 0 && data[offset + 2] == 0 && data[offset + 3] == 1) {
            return 4;
        }
        if (offset + 2 < data.length &&
                data[offset] == 0 && data[offset + 1] == 0 && data[offset + 2] == 1) {
            return 3;
        }
        return 0;
    }

    /**
     * Convert a list of NAL units to AVCC format (4-byte length prefix per NALU).
     */
    static byte[] toAvcc(List<byte[]> nalus) {
        int totalSize = 0;
        for (byte[] nalu : nalus) {
            totalSize += 4 + nalu.length;
        }
        ByteBuffer buf = ByteBuffer.allocate(totalSize);
        for (byte[] nalu : nalus) {
            buf.putInt(nalu.length);
            buf.put(nalu);
        }
        return buf.array();
    }

    private static double parseJsonDouble(String line) {
        String val = line.replaceAll("[^0-9.\\-]", "").strip();
        // Handle case where there might be trailing non-numeric chars
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static int parseJsonInt(String line) {
        String val = line.replaceAll("[^0-9\\-]", "").strip();
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static long parseJsonLong(String line) {
        String val = line.replaceAll("[^0-9\\-]", "").strip();
        try {
            return Long.parseLong(val);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
```

**Step 2: Verify it compiles**

Run: `mvn compile -pl . -q test-compile 2>&1 | tail -5`
Expected: BUILD SUCCESS

**Step 3: Commit**

```bash
git add src/test/java/org/red5/io/moq/playable/TestMediaGenerator.java
git commit -m "Add TestMediaGenerator for ffmpeg-based test media creation"
```

---

### Task 2: FfprobeValidator - external tool validation

**Files:**
- Create: `src/test/java/org/red5/io/moq/playable/FfprobeValidator.java`

**Step 1: Create the FfprobeValidator class**

```java
package org.red5.io.moq.playable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validates fMP4 output files using ffprobe and ffmpeg.
 * All assertions use JUnit 5 so failures produce clear test messages.
 */
public class FfprobeValidator {

    private static final Logger logger = LoggerFactory.getLogger(FfprobeValidator.class);

    /**
     * Run ffprobe and return raw JSON output for streams and format.
     */
    public static String probeJson(Path mp4File) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                "ffprobe", "-v", "quiet",
                "-show_streams", "-show_format",
                "-print_format", "json",
                mp4File.toString()
        );
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String output = new String(p.getInputStream().readAllBytes());
        int exit = p.waitFor();
        if (exit != 0) {
            fail("ffprobe failed (exit " + exit + ") for " + mp4File + ": " + output);
        }
        return output;
    }

    /**
     * Run ffprobe to get per-packet info as JSON.
     */
    public static String probePacketsJson(Path mp4File, String streamSelector)
            throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                "ffprobe", "-v", "quiet",
                "-select_streams", streamSelector,
                "-show_packets",
                "-print_format", "json",
                mp4File.toString()
        );
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String output = new String(p.getInputStream().readAllBytes());
        p.waitFor();
        return output;
    }

    /**
     * Assert the file has the expected number of streams.
     */
    public static void assertStreamCount(Path mp4File, int expected)
            throws IOException, InterruptedException {
        String json = probeJson(mp4File);
        int count = countOccurrences(json, "\"codec_type\"");
        assertEquals(expected, count,
                "Expected " + expected + " streams in " + mp4File);
    }

    /**
     * Assert the file contains a video stream with expected properties.
     */
    public static void assertValidVideo(Path mp4File, int expectedWidth, int expectedHeight, String codecName)
            throws IOException, InterruptedException {
        String json = probeJson(mp4File);
        assertTrue(json.contains("\"codec_name\": \"" + codecName + "\"")
                        || json.contains("\"codec_name\":\"" + codecName + "\""),
                "Expected codec " + codecName + " in " + mp4File);
        assertTrue(json.contains("\"width\": " + expectedWidth)
                        || json.contains("\"width\":" + expectedWidth),
                "Expected width " + expectedWidth);
        assertTrue(json.contains("\"height\": " + expectedHeight)
                        || json.contains("\"height\":" + expectedHeight),
                "Expected height " + expectedHeight);
    }

    /**
     * Assert the file contains an audio stream with expected properties.
     */
    public static void assertValidAudio(Path mp4File, int expectedSampleRate, int expectedChannels, String codecName)
            throws IOException, InterruptedException {
        String json = probeJson(mp4File);
        assertTrue(json.contains("\"codec_name\": \"" + codecName + "\"")
                        || json.contains("\"codec_name\":\"" + codecName + "\""),
                "Expected codec " + codecName + " in " + mp4File);
        assertTrue(json.contains("\"sample_rate\": \"" + expectedSampleRate + "\"")
                        || json.contains("\"sample_rate\":\"" + expectedSampleRate + "\""),
                "Expected sample_rate " + expectedSampleRate);
        assertTrue(json.contains("\"channels\": " + expectedChannels)
                        || json.contains("\"channels\":" + expectedChannels),
                "Expected channels " + expectedChannels);
    }

    /**
     * Assert the file decodes without errors using ffmpeg.
     */
    public static void assertDecodesCleanly(Path mp4File) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg", "-v", "error",
                "-i", mp4File.toString(),
                "-f", "null", "-"
        );
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String errors = new String(p.getInputStream().readAllBytes());
        int exit = p.waitFor();
        if (!errors.isBlank()) {
            logger.warn("ffmpeg decode errors for {}: {}", mp4File, errors);
        }
        assertEquals(0, exit, "ffmpeg decode failed for " + mp4File + ": " + errors);
        assertTrue(errors.isBlank(), "ffmpeg reported errors for " + mp4File + ": " + errors);
    }

    /**
     * Assert timestamps are valid: DTS strictly increasing, PTS non-decreasing.
     */
    public static void assertTimestampsValid(Path mp4File, String streamSelector)
            throws IOException, InterruptedException {
        String json = probePacketsJson(mp4File, streamSelector);

        List<Double> ptsList = extractJsonDoubles(json, "pts_time");
        List<Double> dtsList = extractJsonDoubles(json, "dts_time");

        assertFalse(ptsList.isEmpty(), "No packets found for stream " + streamSelector);

        // DTS strictly increasing
        for (int i = 1; i < dtsList.size(); i++) {
            assertTrue(dtsList.get(i) > dtsList.get(i - 1),
                    "DTS not strictly increasing at packet " + i +
                            ": " + dtsList.get(i - 1) + " -> " + dtsList.get(i));
        }

        // PTS non-decreasing
        for (int i = 1; i < ptsList.size(); i++) {
            assertTrue(ptsList.get(i) >= ptsList.get(i - 1),
                    "PTS decreased at packet " + i +
                            ": " + ptsList.get(i - 1) + " -> " + ptsList.get(i));
        }

        // First packet PTS should be 0 (or very close)
        assertTrue(ptsList.get(0) < 0.1,
                "First packet PTS should be near 0, got " + ptsList.get(0));
    }

    /**
     * Assert total duration is within tolerance.
     */
    public static void assertDurationWithin(Path mp4File, double expectedSeconds, double toleranceSeconds)
            throws IOException, InterruptedException {
        String json = probeJson(mp4File);
        List<Double> durations = extractJsonDoubles(json, "duration");
        assertFalse(durations.isEmpty(), "No duration found in " + mp4File);
        // Use format duration (last one in the JSON output)
        double actual = durations.get(durations.size() - 1);
        assertTrue(Math.abs(actual - expectedSeconds) < toleranceSeconds,
                "Duration " + actual + "s not within " + toleranceSeconds + "s of expected " + expectedSeconds + "s");
    }

    // --- Helpers ---

    private static int countOccurrences(String text, String pattern) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(pattern, idx)) != -1) {
            count++;
            idx += pattern.length();
        }
        return count;
    }

    static List<Double> extractJsonDoubles(String json, String key) {
        List<Double> values = new ArrayList<>();
        String search = "\"" + key + "\"";
        int idx = 0;
        while ((idx = json.indexOf(search, idx)) != -1) {
            int colonIdx = json.indexOf(":", idx + search.length());
            if (colonIdx == -1) break;
            // Find the value - skip whitespace, handle quoted and unquoted
            int valStart = colonIdx + 1;
            while (valStart < json.length() && (json.charAt(valStart) == ' ' || json.charAt(valStart) == '"')) {
                valStart++;
            }
            int valEnd = valStart;
            while (valEnd < json.length() && (Character.isDigit(json.charAt(valEnd))
                    || json.charAt(valEnd) == '.' || json.charAt(valEnd) == '-')) {
                valEnd++;
            }
            if (valEnd > valStart) {
                try {
                    values.add(Double.parseDouble(json.substring(valStart, valEnd)));
                } catch (NumberFormatException e) {
                    // skip
                }
            }
            idx = valEnd;
        }
        return values;
    }
}
```

**Step 2: Verify it compiles**

Run: `mvn test-compile -q 2>&1 | tail -5`
Expected: BUILD SUCCESS

**Step 3: Commit**

```bash
git add src/test/java/org/red5/io/moq/playable/FfprobeValidator.java
git commit -m "Add FfprobeValidator for ffprobe/ffmpeg output validation"
```

---

### Task 3: PlayableOutputTest - CMAF video test (first test end-to-end)

**Files:**
- Create: `src/test/java/org/red5/io/moq/playable/PlayableOutputTest.java`

**Step 1: Write the first failing test - CMAF video**

This test drives the entire pipeline end-to-end for the first time: generate media, parse frames, build init segment + fragments, write fMP4, validate.

```java
package org.red5.io.moq.playable;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.red5.io.moq.cmaf.model.CmafFragment;
import org.red5.io.moq.cmaf.model.SampleFlags;
import org.red5.io.moq.cmaf.util.Fmp4FragmentBuilder;
import org.red5.io.moq.cmaf.util.Fmp4InitSegmentBuilder;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

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
            List<Fmp4FragmentBuilder.SampleData> samples = new java.util.ArrayList<>();

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
            List<Fmp4FragmentBuilder.SampleData> samples = new java.util.ArrayList<>();

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
        var locSerializer = new org.red5.io.moq.loc.serialize.LocSerializer();
        var locDeserializer = new org.red5.io.moq.loc.deserialize.LocDeserializer();

        // Round-trip each frame through LOC
        List<TestMediaGenerator.VideoFrame> roundTrippedFrames = new java.util.ArrayList<>();
        // Get raw avcC config (strip box header for LOC VideoConfigExtension)
        byte[] rawAvcC = new byte[avcCConfig.length - 8];
        System.arraycopy(avcCConfig, 8, rawAvcC, 0, rawAvcC.length);

        for (int i = 0; i < videoFrames.size(); i++) {
            TestMediaGenerator.VideoFrame vf = videoFrames.get(i);

            // Build LOC object
            var locObj = new org.red5.io.moq.loc.model.LocObject(
                    org.red5.io.moq.loc.model.LocObject.MediaType.VIDEO, vf.avccData());
            locObj.setGroupId(vf.isKeyframe() ? i / 30 + 1 : i / 30 + 1);
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
            var deserialized = locDeserializer.deserialize(headerBytes, payloadBytes,
                    org.red5.io.moq.loc.model.LocObject.MediaType.VIDEO);

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
            var gopFrames = roundTrippedFrames.subList(startFrame, endFrame);

            ByteArrayOutputStream mdatData = new ByteArrayOutputStream();
            List<Fmp4FragmentBuilder.SampleData> samples = new java.util.ArrayList<>();

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
        var locSerializer = new org.red5.io.moq.loc.serialize.LocSerializer();
        var locDeserializer = new org.red5.io.moq.loc.deserialize.LocDeserializer();

        List<TestMediaGenerator.AudioFrame> roundTrippedFrames = new java.util.ArrayList<>();

        for (int i = 0; i < audioFrames.size(); i++) {
            TestMediaGenerator.AudioFrame af = audioFrames.get(i);

            var locObj = new org.red5.io.moq.loc.model.LocObject(
                    org.red5.io.moq.loc.model.LocObject.MediaType.AUDIO, af.opusData());
            locObj.setGroupId(i);
            locObj.setObjectId(i);
            locObj.setAudioLevel(true, 10);
            locObj.setCaptureTimestamp(af.pts() * 1000000 / 48000);

            byte[] headerBytes = locSerializer.serializeHeaderExtensions(locObj);
            byte[] payloadBytes = locObj.getPayload();

            var deserialized = locDeserializer.deserialize(headerBytes, payloadBytes,
                    org.red5.io.moq.loc.model.LocObject.MediaType.AUDIO);

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
            var fragFrames = roundTrippedFrames.subList(start, end);

            ByteArrayOutputStream mdatData = new ByteArrayOutputStream();
            List<Fmp4FragmentBuilder.SampleData> samples = new java.util.ArrayList<>();

            for (var frame : fragFrames) {
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
        var moqmiSerializer = new org.red5.io.moq.moqmi.serialize.MoqMISerializer();
        var moqmiDeserializer = new org.red5.io.moq.moqmi.deserialize.MoqMIDeserializer();

        byte[] rawAvcC = new byte[avcCConfig.length - 8];
        System.arraycopy(avcCConfig, 8, rawAvcC, 0, rawAvcC.length);

        List<TestMediaGenerator.VideoFrame> roundTrippedFrames = new java.util.ArrayList<>();

        for (int i = 0; i < videoFrames.size(); i++) {
            TestMediaGenerator.VideoFrame vf = videoFrames.get(i);

            var moqmiObj = new org.red5.io.moq.moqmi.model.MoqMIObject(
                    org.red5.io.moq.moqmi.model.MoqMIObject.MediaType.VIDEO_H264_AVCC, vf.avccData());
            moqmiObj.setGroupId(i / 30 + 1);
            moqmiObj.setObjectId(i % 30);

            // Add required extensions
            moqmiObj.addHeaderExtension(new org.red5.io.moq.moqmi.model.MediaTypeExtension(
                    org.red5.io.moq.moqmi.model.MoqMIObject.MediaType.VIDEO_H264_AVCC));
            moqmiObj.addHeaderExtension(new org.red5.io.moq.moqmi.model.H264MetadataExtension(
                    i, vf.pts(), vf.dts(), 90000, vf.duration(), System.currentTimeMillis()));
            if (vf.isKeyframe()) {
                moqmiObj.addHeaderExtension(new org.red5.io.moq.moqmi.model.H264ExtradataExtension(rawAvcC));
            }

            // Serialize
            byte[] headerBytes = moqmiSerializer.serializeHeaderExtensions(moqmiObj);
            byte[] payloadBytes = moqmiObj.getPayload();

            // Deserialize
            var deserialized = moqmiDeserializer.deserialize(headerBytes, payloadBytes);

            assertEquals(org.red5.io.moq.moqmi.model.MoqMIObject.MediaType.VIDEO_H264_AVCC,
                    deserialized.getMediaType());
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
            var gopFrames = roundTrippedFrames.subList(startFrame, endFrame);

            ByteArrayOutputStream mdatData = new ByteArrayOutputStream();
            List<Fmp4FragmentBuilder.SampleData> samples = new java.util.ArrayList<>();

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
        var moqmiSerializer = new org.red5.io.moq.moqmi.serialize.MoqMISerializer();
        var moqmiDeserializer = new org.red5.io.moq.moqmi.deserialize.MoqMIDeserializer();

        List<TestMediaGenerator.AudioFrame> roundTrippedFrames = new java.util.ArrayList<>();

        for (int i = 0; i < audioFrames.size(); i++) {
            TestMediaGenerator.AudioFrame af = audioFrames.get(i);

            var moqmiObj = new org.red5.io.moq.moqmi.model.MoqMIObject(
                    org.red5.io.moq.moqmi.model.MoqMIObject.MediaType.AUDIO_OPUS, af.opusData());
            moqmiObj.setGroupId(i);
            moqmiObj.setObjectId(0);

            moqmiObj.addHeaderExtension(new org.red5.io.moq.moqmi.model.MediaTypeExtension(
                    org.red5.io.moq.moqmi.model.MoqMIObject.MediaType.AUDIO_OPUS));
            moqmiObj.addHeaderExtension(new org.red5.io.moq.moqmi.model.OpusDataExtension(
                    i, af.pts(), 48000, 48000, 1, af.duration(), System.currentTimeMillis()));

            byte[] headerBytes = moqmiSerializer.serializeHeaderExtensions(moqmiObj);
            byte[] payloadBytes = moqmiObj.getPayload();

            var deserialized = moqmiDeserializer.deserialize(headerBytes, payloadBytes);

            assertEquals(org.red5.io.moq.moqmi.model.MoqMIObject.MediaType.AUDIO_OPUS,
                    deserialized.getMediaType());
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
            var fragFrames = roundTrippedFrames.subList(start, end);

            ByteArrayOutputStream mdatData = new ByteArrayOutputStream();
            List<Fmp4FragmentBuilder.SampleData> samples = new java.util.ArrayList<>();

            for (var frame : fragFrames) {
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
```

**Step 2: Run the tests**

Run: `mvn test -Dtest=PlayableOutputTest -pl . 2>&1 | tail -20`
Expected: 6 tests pass (or skip if ffmpeg absent)

**Step 3: Debug and fix any failures**

The most likely failure points:
- Annex-B parsing: NALU boundary detection off by one
- AVCC config: SPS/PPS extraction from wrong offset
- Timestamp alignment: ffprobe reports different timescale than expected
- fMP4 data offset: trun data_offset calculation may need adjustment for multi-sample fragments

For each failure, read the error, adjust the relevant helper in `TestMediaGenerator` or the fragment building logic, re-run.

**Step 4: Commit**

```bash
git add src/test/java/org/red5/io/moq/playable/PlayableOutputTest.java
git commit -m "Add Phase A playable output tests for CMAF, LOC, MoqMI"
```

---

### Task 4: MuxedPlayableOutputTest - Phase B multi-track tests

**Files:**
- Create: `src/test/java/org/red5/io/moq/playable/MuxedPlayableOutputTest.java`

**Step 1: Write the muxed tests**

```java
package org.red5.io.moq.playable;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.red5.io.moq.cmaf.model.CmafFragment;
import org.red5.io.moq.cmaf.model.SampleFlags;
import org.red5.io.moq.cmaf.util.Fmp4FragmentBuilder;
import org.red5.io.moq.cmaf.util.Fmp4InitSegmentBuilder;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

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
        // Init segment with both tracks
        byte[] initSegment = new Fmp4InitSegmentBuilder()
                .addVideoTrack(new Fmp4InitSegmentBuilder.VideoTrackConfig(
                        1, 90000, "avc1", avcCConfig, 320, 240))
                .addAudioTrack(new Fmp4InitSegmentBuilder.AudioTrackConfig(
                        2, 48000, "Opus", dOpsConfig, 1, 48000, 16))
                .build();

        Fmp4FragmentBuilder builder = new Fmp4FragmentBuilder();
        ByteArrayOutputStream mp4Out = new ByteArrayOutputStream();
        mp4Out.write(initSegment);

        // Write video fragments (one per GOP)
        int framesPerGop = 30;
        int gopCount = (videoFrames.size() + framesPerGop - 1) / framesPerGop;

        for (int gop = 0; gop < gopCount; gop++) {
            int startFrame = gop * framesPerGop;
            int endFrame = Math.min(startFrame + framesPerGop, videoFrames.size());
            var gopFrames = videoFrames.subList(startFrame, endFrame);

            ByteArrayOutputStream mdatData = new ByteArrayOutputStream();
            List<Fmp4FragmentBuilder.SampleData> samples = new java.util.ArrayList<>();

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

            // Find audio frames in this time window
            List<TestMediaGenerator.AudioFrame> audioInRange = new java.util.ArrayList<>();
            for (var af : audioFrames) {
                double afTimeSec = af.pts() / 48000.0;
                if (afTimeSec >= gopStartSec - 0.001 && afTimeSec < gopEndSec + 0.001) {
                    audioInRange.add(af);
                }
            }

            if (!audioInRange.isEmpty()) {
                ByteArrayOutputStream audioMdat = new ByteArrayOutputStream();
                List<Fmp4FragmentBuilder.SampleData> audioSamples = new java.util.ArrayList<>();

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
        var locSerializer = new org.red5.io.moq.loc.serialize.LocSerializer();
        var locDeserializer = new org.red5.io.moq.loc.deserialize.LocDeserializer();

        byte[] rawAvcC = new byte[avcCConfig.length - 8];
        System.arraycopy(avcCConfig, 8, rawAvcC, 0, rawAvcC.length);

        // Round-trip video
        List<TestMediaGenerator.VideoFrame> rtVideo = new java.util.ArrayList<>();
        for (int i = 0; i < videoFrames.size(); i++) {
            var vf = videoFrames.get(i);
            var locObj = new org.red5.io.moq.loc.model.LocObject(
                    org.red5.io.moq.loc.model.LocObject.MediaType.VIDEO, vf.avccData());
            locObj.setVideoFrameMarking(vf.isKeyframe(), !vf.isKeyframe(), false, 0, 0);
            if (vf.isKeyframe()) locObj.setVideoConfig(rawAvcC);

            byte[] hdr = locSerializer.serializeHeaderExtensions(locObj);
            var deser = locDeserializer.deserialize(hdr, vf.avccData(),
                    org.red5.io.moq.loc.model.LocObject.MediaType.VIDEO);
            rtVideo.add(new TestMediaGenerator.VideoFrame(
                    deser.getPayload(), vf.pts(), vf.dts(), vf.duration(), vf.isKeyframe()));
        }

        // Round-trip audio
        List<TestMediaGenerator.AudioFrame> rtAudio = new java.util.ArrayList<>();
        for (int i = 0; i < audioFrames.size(); i++) {
            var af = audioFrames.get(i);
            var locObj = new org.red5.io.moq.loc.model.LocObject(
                    org.red5.io.moq.loc.model.LocObject.MediaType.AUDIO, af.opusData());
            locObj.setAudioLevel(true, 10);

            byte[] hdr = locSerializer.serializeHeaderExtensions(locObj);
            var deser = locDeserializer.deserialize(hdr, af.opusData(),
                    org.red5.io.moq.loc.model.LocObject.MediaType.AUDIO);
            rtAudio.add(new TestMediaGenerator.AudioFrame(
                    deser.getPayload(), af.pts(), af.duration(), af.sampleRate(), af.channels()));
        }

        // Build muxed fMP4 (same structure as cmafMuxed, using round-tripped frames)
        writeMuxedMp4(rtVideo, rtAudio, tempDir.resolve("loc_muxed.mp4"));

        Path outputFile = tempDir.resolve("loc_muxed.mp4");
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
        var moqmiSerializer = new org.red5.io.moq.moqmi.serialize.MoqMISerializer();
        var moqmiDeserializer = new org.red5.io.moq.moqmi.deserialize.MoqMIDeserializer();

        byte[] rawAvcC = new byte[avcCConfig.length - 8];
        System.arraycopy(avcCConfig, 8, rawAvcC, 0, rawAvcC.length);

        // Round-trip video
        List<TestMediaGenerator.VideoFrame> rtVideo = new java.util.ArrayList<>();
        for (int i = 0; i < videoFrames.size(); i++) {
            var vf = videoFrames.get(i);
            var obj = new org.red5.io.moq.moqmi.model.MoqMIObject(
                    org.red5.io.moq.moqmi.model.MoqMIObject.MediaType.VIDEO_H264_AVCC, vf.avccData());
            obj.addHeaderExtension(new org.red5.io.moq.moqmi.model.MediaTypeExtension(
                    org.red5.io.moq.moqmi.model.MoqMIObject.MediaType.VIDEO_H264_AVCC));
            obj.addHeaderExtension(new org.red5.io.moq.moqmi.model.H264MetadataExtension(
                    i, vf.pts(), vf.dts(), 90000, vf.duration(), 0));
            if (vf.isKeyframe()) {
                obj.addHeaderExtension(new org.red5.io.moq.moqmi.model.H264ExtradataExtension(rawAvcC));
            }

            byte[] hdr = moqmiSerializer.serializeHeaderExtensions(obj);
            var deser = moqmiDeserializer.deserialize(hdr, vf.avccData());
            rtVideo.add(new TestMediaGenerator.VideoFrame(
                    deser.getPayload(), vf.pts(), vf.dts(), vf.duration(), vf.isKeyframe()));
        }

        // Round-trip audio
        List<TestMediaGenerator.AudioFrame> rtAudio = new java.util.ArrayList<>();
        for (int i = 0; i < audioFrames.size(); i++) {
            var af = audioFrames.get(i);
            var obj = new org.red5.io.moq.moqmi.model.MoqMIObject(
                    org.red5.io.moq.moqmi.model.MoqMIObject.MediaType.AUDIO_OPUS, af.opusData());
            obj.addHeaderExtension(new org.red5.io.moq.moqmi.model.MediaTypeExtension(
                    org.red5.io.moq.moqmi.model.MoqMIObject.MediaType.AUDIO_OPUS));
            obj.addHeaderExtension(new org.red5.io.moq.moqmi.model.OpusDataExtension(
                    i, af.pts(), 48000, 48000, 1, af.duration(), 0));

            byte[] hdr = moqmiSerializer.serializeHeaderExtensions(obj);
            var deser = moqmiDeserializer.deserialize(hdr, af.opusData());
            rtAudio.add(new TestMediaGenerator.AudioFrame(
                    deser.getPayload(), af.pts(), af.duration(), af.sampleRate(), af.channels()));
        }

        writeMuxedMp4(rtVideo, rtAudio, tempDir.resolve("moqmi_muxed.mp4"));

        Path outputFile = tempDir.resolve("moqmi_muxed.mp4");
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
            List<Fmp4FragmentBuilder.SampleData> samples = new java.util.ArrayList<>();
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

            List<TestMediaGenerator.AudioFrame> audioInRange = new java.util.ArrayList<>();
            for (var af : audio) {
                double t = af.pts() / 48000.0;
                if (t >= gopStartSec - 0.001 && t < gopEndSec + 0.001) {
                    audioInRange.add(af);
                }
            }

            if (!audioInRange.isEmpty()) {
                ByteArrayOutputStream audioMdat = new ByteArrayOutputStream();
                List<Fmp4FragmentBuilder.SampleData> audioSamples = new java.util.ArrayList<>();
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
```

**Step 2: Run the tests**

Run: `mvn test -Dtest=MuxedPlayableOutputTest -pl . 2>&1 | tail -20`
Expected: 3 tests pass (or skip if ffmpeg absent)

**Step 3: Commit**

```bash
git add src/test/java/org/red5/io/moq/playable/MuxedPlayableOutputTest.java
git commit -m "Add Phase B muxed playable output tests for CMAF, LOC, MoqMI"
```

---

### Task 5: Full test suite verification

**Step 1: Run all existing tests to confirm no regressions**

Run: `mvn clean test 2>&1 | tail -30`
Expected: All existing tests pass. New playable tests pass (or skip if ffmpeg absent).

**Step 2: Run only the new tests explicitly**

Run: `mvn test -Dtest="PlayableOutputTest,MuxedPlayableOutputTest" 2>&1 | tail -30`
Expected: 9 tests pass (6 Phase A + 3 Phase B)

**Step 3: Final commit with all files**

```bash
git add docs/plans/2026-02-27-playable-output-tests-design.md
git add docs/plans/2026-02-27-playable-output-tests-plan.md
git commit -m "Add design and plan docs for playable output tests"
```
