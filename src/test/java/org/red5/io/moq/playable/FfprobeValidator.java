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
        int exit = p.waitFor();
        if (exit != 0) {
            logger.warn("ffprobe packets returned exit {} for {} stream {}", exit, mp4File, streamSelector);
        }
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
                    // skip unparseable values
                }
            }
            idx = valEnd;
        }
        return values;
    }
}
