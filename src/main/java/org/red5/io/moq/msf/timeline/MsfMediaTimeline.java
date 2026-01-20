package org.red5.io.moq.msf.timeline;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * JSON serializer/deserializer for MSF media timeline payloads per draft-ietf-moq-msf section 7.
 * <p>
 * Format: Array of records where each record is [mediaPTS, [groupId, objectId], wallclockMs]
 * <pre>
 * [
 *   [0, [0,0], 1759924158381],
 *   [2002, [1,0], 1759924160383]
 * ]
 * </pre>
 */
public class MsfMediaTimeline {
    private final Gson gson;

    public MsfMediaTimeline() {
        this.gson = new GsonBuilder().create();
    }

    /**
     * Serialize records to JSON string.
     */
    public String toJson(List<MsfMediaTimelineRecord> records) {
        JsonArray array = new JsonArray();
        for (MsfMediaTimelineRecord record : records) {
            JsonArray entry = new JsonArray();
            entry.add(record.getMediaPtsMillis());
            JsonArray location = new JsonArray();
            location.add(record.getGroupId());
            location.add(record.getObjectId());
            entry.add(location);
            entry.add(record.getWallclockMillis());
            array.add(entry);
        }
        return gson.toJson(array);
    }

    /**
     * Deserialize JSON string to records.
     */
    public List<MsfMediaTimelineRecord> fromJson(String json) throws IOException {
        try {
            JsonArray array = JsonParser.parseString(json).getAsJsonArray();
            List<MsfMediaTimelineRecord> records = new ArrayList<>();
            for (JsonElement element : array) {
                JsonArray entry = element.getAsJsonArray();
                long mediaPts = entry.get(0).getAsLong();
                JsonArray location = entry.get(1).getAsJsonArray();
                long groupId = location.get(0).getAsLong();
                long objectId = location.get(1).getAsLong();
                long wallclock = entry.get(2).getAsLong();
                records.add(new MsfMediaTimelineRecord(mediaPts, groupId, objectId, wallclock));
            }
            return records;
        } catch (Exception e) {
            throw new IOException("Failed to parse MSF media timeline JSON", e);
        }
    }

    /**
     * Serialize records to GZIP-compressed JSON bytes.
     */
    public byte[] toGzipJson(List<MsfMediaTimelineRecord> records) throws IOException {
        String json = toJson(records);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
            gzos.write(json.getBytes(StandardCharsets.UTF_8));
        }
        return baos.toByteArray();
    }

    /**
     * Deserialize GZIP-compressed JSON bytes to records.
     */
    public List<MsfMediaTimelineRecord> fromGzipJson(byte[] gzipData) throws IOException {
        try (GZIPInputStream gzis = new GZIPInputStream(new ByteArrayInputStream(gzipData));
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzis.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            String json = baos.toString(StandardCharsets.UTF_8);
            return fromJson(json);
        }
    }

    /**
     * Check if data appears to be GZIP compressed (magic bytes 1f 8b).
     */
    public static boolean isGzipCompressed(byte[] data) {
        return data != null && data.length >= 2
            && (data[0] & 0xFF) == 0x1F
            && (data[1] & 0xFF) == 0x8B;
    }
}
