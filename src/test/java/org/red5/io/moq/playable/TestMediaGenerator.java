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
        int probeExit = probeP.waitFor();
        if (probeExit != 0) {
            throw new IOException("ffprobe packet extraction failed (exit " + probeExit + "): " + probeOut);
        }

        // Extract raw h264 as annex-B elementary stream for NAL unit parsing
        Path rawVideo = outputDir.resolve("raw_video.h264");
        ProcessBuilder extractPb = new ProcessBuilder(
                "ffmpeg", "-y", "-i", fmp4Path.toString(),
                "-c:v", "copy", "-an",
                "-bsf:v", "h264_mp4toannexb",
                "-f", "h264", rawVideo.toString()
        );
        extractPb.redirectErrorStream(true);
        Process extractP = extractPb.start();
        String extractLog = new String(extractP.getInputStream().readAllBytes());
        int extractExit = extractP.waitFor();
        if (extractExit != 0) {
            throw new IOException("ffmpeg raw video extraction failed (exit " + extractExit + "): " + extractLog);
        }

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

            // Collect NALUs for this frame, skipping SPS (type 7) and PPS (type 8)
            // parameter set NALUs which are extracted separately for the init segment
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
     * The result is wrapped in an avcC box suitable for the init segment codec config.
     *
     * @param annexBPath path to annex-B .h264 file
     * @return avcC box bytes (box header + AVCDecoderConfigurationRecord)
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
     * Extract Opus packets from an OGG file via an fMP4 intermediate.
     * Uses ffprobe JSON output to locate packet positions and sizes within the fMP4,
     * then extracts raw Opus packet bytes directly.
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
        String log = new String(p.getInputStream().readAllBytes());
        int exit = p.waitFor();
        if (exit != 0) {
            throw new IOException("Failed to create Opus fMP4 (exit " + exit + "): " + log);
        }

        // Use ffprobe JSON output to get per-packet positions, sizes, and timestamps
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

        // Read the fMP4 bytes so we can extract packet data by position
        byte[] mp4Bytes = Files.readAllBytes(tempMp4);

        // Parse JSON manually to extract pts_time, duration_time, size, and pos per packet
        long frameDuration48k = 960; // 20ms at 48kHz
        String[] jsonLines = jsonOut.split("\n");
        List<AudioFrame> frames = new ArrayList<>();
        double currentPts = 0;
        double currentDuration = 0;
        int currentSize = 0;

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
                if (data[j] == 0 && data[j + 1] == 0
                        && (data[j + 2] == 1 || (j + 3 < data.length && data[j + 2] == 0 && data[j + 3] == 1))) {
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
        if (offset + 3 < data.length
                && data[offset] == 0 && data[offset + 1] == 0 && data[offset + 2] == 0 && data[offset + 3] == 1) {
            return 4;
        }
        if (offset + 2 < data.length
                && data[offset] == 0 && data[offset + 1] == 0 && data[offset + 2] == 1) {
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
        // Extract value from JSON line like: "pts_time": "0.033333",
        String val = line.replaceAll(".*\":\\s*\"?", "").replaceAll("\"?,?\\s*$", "").strip();
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static int parseJsonInt(String line) {
        // Extract value from JSON line like: "size": "1234",
        String val = line.replaceAll(".*\":\\s*\"?", "").replaceAll("\"?,?\\s*$", "").strip();
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static long parseJsonLong(String line) {
        // Extract value from JSON line like: "pos": "12345",
        String val = line.replaceAll(".*\":\\s*\"?", "").replaceAll("\"?,?\\s*$", "").strip();
        try {
            return Long.parseLong(val);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
