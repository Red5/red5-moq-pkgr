package org.red5.io.moq.msf.timeline;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
 * JSON serializer/deserializer for MSF event timeline payloads per draft-ietf-moq-msf section 8.
 * <p>
 * Format: Array of objects with index reference ('t', 'l', or 'm') and 'data' object.
 * <pre>
 * // Wallclock indexed:
 * [{"t": 1756885678361, "data": {...}}]
 *
 * // Location indexed:
 * [{"l": [0,0], "data": {...}}]
 *
 * // Media PTS indexed:
 * [{"m": 2002, "data": {...}}]
 * </pre>
 */
public class MsfEventTimeline {
    private final Gson gson;

    public MsfEventTimeline() {
        this.gson = new GsonBuilder().create();
    }

    /**
     * Serialize entries to JSON string.
     */
    public String toJson(List<MsfEventTimelineEntry> entries) {
        JsonArray array = new JsonArray();
        for (MsfEventTimelineEntry entry : entries) {
            JsonObject obj = new JsonObject();
            switch (entry.getIndexType()) {
                case MsfEventTimelineEntry.INDEX_WALLCLOCK:
                    obj.addProperty(MsfEventTimelineEntry.INDEX_WALLCLOCK, entry.getWallclockMillis());
                    break;
                case MsfEventTimelineEntry.INDEX_LOCATION:
                    JsonArray location = new JsonArray();
                    location.add(entry.getGroupId());
                    location.add(entry.getObjectId());
                    obj.add(MsfEventTimelineEntry.INDEX_LOCATION, location);
                    break;
                case MsfEventTimelineEntry.INDEX_MEDIA_PTS:
                    obj.addProperty(MsfEventTimelineEntry.INDEX_MEDIA_PTS, entry.getMediaPtsMillis());
                    break;
            }
            obj.add("data", entry.getData());
            array.add(obj);
        }
        return gson.toJson(array);
    }

    /**
     * Deserialize JSON string to entries.
     */
    public List<MsfEventTimelineEntry> fromJson(String json) throws IOException {
        try {
            JsonArray array = JsonParser.parseString(json).getAsJsonArray();
            List<MsfEventTimelineEntry> entries = new ArrayList<>();
            for (JsonElement element : array) {
                JsonObject obj = element.getAsJsonObject();
                JsonElement data = obj.get("data");
                MsfEventTimelineEntry entry;
                if (obj.has(MsfEventTimelineEntry.INDEX_WALLCLOCK)) {
                    long wallclock = obj.get(MsfEventTimelineEntry.INDEX_WALLCLOCK).getAsLong();
                    entry = MsfEventTimelineEntry.withWallclock(wallclock, data);
                } else if (obj.has(MsfEventTimelineEntry.INDEX_LOCATION)) {
                    JsonArray location = obj.get(MsfEventTimelineEntry.INDEX_LOCATION).getAsJsonArray();
                    long groupId = location.get(0).getAsLong();
                    long objectId = location.get(1).getAsLong();
                    entry = MsfEventTimelineEntry.withLocation(groupId, objectId, data);
                } else if (obj.has(MsfEventTimelineEntry.INDEX_MEDIA_PTS)) {
                    long mediaPts = obj.get(MsfEventTimelineEntry.INDEX_MEDIA_PTS).getAsLong();
                    entry = MsfEventTimelineEntry.withMediaPts(mediaPts, data);
                } else {
                    throw new IllegalArgumentException("Event timeline entry missing index reference (t, l, or m)");
                }
                entries.add(entry);
            }
            return entries;
        } catch (Exception e) {
            throw new IOException("Failed to parse MSF event timeline JSON", e);
        }
    }

    /**
     * Serialize entries to GZIP-compressed JSON bytes.
     */
    public byte[] toGzipJson(List<MsfEventTimelineEntry> entries) throws IOException {
        String json = toJson(entries);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
            gzos.write(json.getBytes(StandardCharsets.UTF_8));
        }
        return baos.toByteArray();
    }

    /**
     * Deserialize GZIP-compressed JSON bytes to entries.
     */
    public List<MsfEventTimelineEntry> fromGzipJson(byte[] gzipData) throws IOException {
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
