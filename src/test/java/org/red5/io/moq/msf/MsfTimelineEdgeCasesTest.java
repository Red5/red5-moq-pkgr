package org.red5.io.moq.msf;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;
import org.red5.io.moq.msf.timeline.MsfEventTimeline;
import org.red5.io.moq.msf.timeline.MsfEventTimelineEntry;
import org.red5.io.moq.msf.timeline.MsfMediaTimeline;
import org.red5.io.moq.msf.timeline.MsfMediaTimelineRecord;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Edge case and error handling tests for MSF timeline classes.
 */
class MsfTimelineEdgeCasesTest {

    // Media Timeline edge cases

    @Test
    void testMediaTimelineEmptyList() throws Exception {
        MsfMediaTimeline timeline = new MsfMediaTimeline();
        List<MsfMediaTimelineRecord> empty = Collections.emptyList();

        String json = timeline.toJson(empty);
        assertEquals("[]", json);

        List<MsfMediaTimelineRecord> parsed = timeline.fromJson(json);
        assertTrue(parsed.isEmpty());
    }

    @Test
    void testMediaTimelineSingleRecord() throws Exception {
        MsfMediaTimeline timeline = new MsfMediaTimeline();
        List<MsfMediaTimelineRecord> records = List.of(
            new MsfMediaTimelineRecord(0, 0, 0, 0)
        );

        String json = timeline.toJson(records);
        List<MsfMediaTimelineRecord> parsed = timeline.fromJson(json);

        assertEquals(1, parsed.size());
        assertEquals(0, parsed.get(0).getMediaPtsMillis());
        assertEquals(0, parsed.get(0).getWallclockMillis());
    }

    @Test
    void testMediaTimelineZeroWallclock() throws Exception {
        // VOD assets use wallclock=0
        MsfMediaTimeline timeline = new MsfMediaTimeline();
        List<MsfMediaTimelineRecord> records = List.of(
            new MsfMediaTimelineRecord(0, 0, 0, 0),
            new MsfMediaTimelineRecord(1000, 1, 0, 0),
            new MsfMediaTimelineRecord(2000, 2, 0, 0)
        );

        String json = timeline.toJson(records);
        List<MsfMediaTimelineRecord> parsed = timeline.fromJson(json);

        for (MsfMediaTimelineRecord record : parsed) {
            assertEquals(0, record.getWallclockMillis());
        }
    }

    @Test
    void testMediaTimelineLargeValues() throws Exception {
        MsfMediaTimeline timeline = new MsfMediaTimeline();
        List<MsfMediaTimelineRecord> records = List.of(
            new MsfMediaTimelineRecord(Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE)
        );

        String json = timeline.toJson(records);
        List<MsfMediaTimelineRecord> parsed = timeline.fromJson(json);

        assertEquals(Long.MAX_VALUE, parsed.get(0).getMediaPtsMillis());
        assertEquals(Long.MAX_VALUE, parsed.get(0).getGroupId());
        assertEquals(Long.MAX_VALUE, parsed.get(0).getObjectId());
        assertEquals(Long.MAX_VALUE, parsed.get(0).getWallclockMillis());
    }

    @Test
    void testMediaTimelineNegativeValues() throws Exception {
        // Negative values should be preserved (though unusual)
        MsfMediaTimeline timeline = new MsfMediaTimeline();
        List<MsfMediaTimelineRecord> records = List.of(
            new MsfMediaTimelineRecord(-1, -1, -1, -1)
        );

        String json = timeline.toJson(records);
        List<MsfMediaTimelineRecord> parsed = timeline.fromJson(json);

        assertEquals(-1, parsed.get(0).getMediaPtsMillis());
    }

    @Test
    void testMediaTimelineInvalidJson() {
        MsfMediaTimeline timeline = new MsfMediaTimeline();

        assertThrows(IOException.class, () -> timeline.fromJson("not json"));
        assertThrows(IOException.class, () -> timeline.fromJson("{}")); // object instead of array
        assertThrows(IOException.class, () -> timeline.fromJson("[[0,0,0]]")); // wrong inner format
    }

    @Test
    void testMediaTimelineMalformedRecord() {
        MsfMediaTimeline timeline = new MsfMediaTimeline();

        // Missing fields
        assertThrows(IOException.class, () -> timeline.fromJson("[[0,[0,0]]]")); // missing wallclock
        assertThrows(IOException.class, () -> timeline.fromJson("[[0,[0]]]")); // missing objectId in location
    }

    @Test
    void testMediaTimelineGzipEmptyList() throws Exception {
        MsfMediaTimeline timeline = new MsfMediaTimeline();
        List<MsfMediaTimelineRecord> empty = Collections.emptyList();

        byte[] gzip = timeline.toGzipJson(empty);
        assertTrue(MsfMediaTimeline.isGzipCompressed(gzip));

        List<MsfMediaTimelineRecord> parsed = timeline.fromGzipJson(gzip);
        assertTrue(parsed.isEmpty());
    }

    @Test
    void testMediaTimelineGzipInvalidData() {
        MsfMediaTimeline timeline = new MsfMediaTimeline();
        byte[] notGzip = "not gzip data".getBytes();

        assertThrows(IOException.class, () -> timeline.fromGzipJson(notGzip));
    }

    // Event Timeline edge cases

    @Test
    void testEventTimelineEmptyList() throws Exception {
        MsfEventTimeline timeline = new MsfEventTimeline();
        List<MsfEventTimelineEntry> empty = Collections.emptyList();

        String json = timeline.toJson(empty);
        assertEquals("[]", json);

        List<MsfEventTimelineEntry> parsed = timeline.fromJson(json);
        assertTrue(parsed.isEmpty());
    }

    @Test
    void testEventTimelineNullData() throws Exception {
        MsfEventTimeline timeline = new MsfEventTimeline();
        List<MsfEventTimelineEntry> entries = List.of(
            MsfEventTimelineEntry.withWallclock(1000L, JsonNull.INSTANCE)
        );

        String json = timeline.toJson(entries);
        List<MsfEventTimelineEntry> parsed = timeline.fromJson(json);

        // JsonNull serializes to "null" and may deserialize as null or JsonNull
        var data = parsed.get(0).getData();
        assertTrue(data == null || data.isJsonNull());
    }

    @Test
    void testEventTimelineArrayData() throws Exception {
        MsfEventTimeline timeline = new MsfEventTimeline();
        JsonArray arrayData = new JsonArray();
        arrayData.add(1);
        arrayData.add(2);
        arrayData.add(3);

        List<MsfEventTimelineEntry> entries = List.of(
            MsfEventTimelineEntry.withLocation(0, 0, arrayData)
        );

        String json = timeline.toJson(entries);
        List<MsfEventTimelineEntry> parsed = timeline.fromJson(json);

        assertTrue(parsed.get(0).getData().isJsonArray());
        assertEquals(3, parsed.get(0).getData().getAsJsonArray().size());
    }

    @Test
    void testEventTimelinePrimitiveData() throws Exception {
        MsfEventTimeline timeline = new MsfEventTimeline();
        List<MsfEventTimelineEntry> entries = List.of(
            MsfEventTimelineEntry.withMediaPts(1000L, new JsonPrimitive("simple string")),
            MsfEventTimelineEntry.withMediaPts(2000L, new JsonPrimitive(42)),
            MsfEventTimelineEntry.withMediaPts(3000L, new JsonPrimitive(true))
        );

        String json = timeline.toJson(entries);
        List<MsfEventTimelineEntry> parsed = timeline.fromJson(json);

        assertEquals("simple string", parsed.get(0).getData().getAsString());
        assertEquals(42, parsed.get(1).getData().getAsInt());
        assertTrue(parsed.get(2).getData().getAsBoolean());
    }

    @Test
    void testEventTimelineNestedData() throws Exception {
        MsfEventTimeline timeline = new MsfEventTimeline();

        JsonObject nested = new JsonObject();
        nested.addProperty("level1", "value");
        JsonObject level2 = new JsonObject();
        level2.addProperty("level2", "deep value");
        nested.add("nested", level2);

        List<MsfEventTimelineEntry> entries = List.of(
            MsfEventTimelineEntry.withWallclock(1000L, nested)
        );

        String json = timeline.toJson(entries);
        List<MsfEventTimelineEntry> parsed = timeline.fromJson(json);

        JsonObject parsedData = parsed.get(0).getData().getAsJsonObject();
        assertEquals("value", parsedData.get("level1").getAsString());
        assertEquals("deep value", parsedData.get("nested").getAsJsonObject().get("level2").getAsString());
    }

    @Test
    void testEventTimelineMissingIndexReference() {
        MsfEventTimeline timeline = new MsfEventTimeline();

        // Entry with only data, no index reference
        String invalidJson = "[{\"data\":{\"foo\":\"bar\"}}]";
        assertThrows(IOException.class, () -> timeline.fromJson(invalidJson));
    }

    @Test
    void testEventTimelineInvalidJson() {
        MsfEventTimeline timeline = new MsfEventTimeline();

        assertThrows(IOException.class, () -> timeline.fromJson("not json"));
        assertThrows(IOException.class, () -> timeline.fromJson("{}")); // object instead of array
    }

    @Test
    void testEventTimelineGzipEmptyList() throws Exception {
        MsfEventTimeline timeline = new MsfEventTimeline();
        List<MsfEventTimelineEntry> empty = Collections.emptyList();

        byte[] gzip = timeline.toGzipJson(empty);
        assertTrue(MsfEventTimeline.isGzipCompressed(gzip));

        List<MsfEventTimelineEntry> parsed = timeline.fromGzipJson(gzip);
        assertTrue(parsed.isEmpty());
    }

    // MsfMediaTimelineRecord tests

    @Test
    void testMediaTimelineRecordGetters() {
        MsfMediaTimelineRecord record = new MsfMediaTimelineRecord(100, 5, 10, 1234567890L);

        assertEquals(100, record.getMediaPtsMillis());
        assertEquals(5, record.getGroupId());
        assertEquals(10, record.getObjectId());
        assertEquals(1234567890L, record.getWallclockMillis());
    }

    // MsfEventTimelineEntry tests

    @Test
    void testEventTimelineEntryIndexConstants() {
        assertEquals("t", MsfEventTimelineEntry.INDEX_WALLCLOCK);
        assertEquals("l", MsfEventTimelineEntry.INDEX_LOCATION);
        assertEquals("m", MsfEventTimelineEntry.INDEX_MEDIA_PTS);
    }

    @Test
    void testEventTimelineEntryWallclockIndexed() {
        JsonObject data = new JsonObject();
        MsfEventTimelineEntry entry = MsfEventTimelineEntry.withWallclock(1000L, data);

        assertTrue(entry.isWallclockIndexed());
        assertFalse(entry.isLocationIndexed());
        assertFalse(entry.isMediaPtsIndexed());
        assertEquals("t", entry.getIndexType());
        assertEquals(1000L, entry.getWallclockMillis());
        assertNull(entry.getGroupId());
        assertNull(entry.getObjectId());
        assertNull(entry.getMediaPtsMillis());
    }

    @Test
    void testEventTimelineEntryLocationIndexed() {
        JsonObject data = new JsonObject();
        MsfEventTimelineEntry entry = MsfEventTimelineEntry.withLocation(5, 10, data);

        assertFalse(entry.isWallclockIndexed());
        assertTrue(entry.isLocationIndexed());
        assertFalse(entry.isMediaPtsIndexed());
        assertEquals("l", entry.getIndexType());
        assertNull(entry.getWallclockMillis());
        assertEquals(5L, entry.getGroupId());
        assertEquals(10L, entry.getObjectId());
        assertNull(entry.getMediaPtsMillis());
    }

    @Test
    void testEventTimelineEntryMediaPtsIndexed() {
        JsonObject data = new JsonObject();
        MsfEventTimelineEntry entry = MsfEventTimelineEntry.withMediaPts(2000L, data);

        assertFalse(entry.isWallclockIndexed());
        assertFalse(entry.isLocationIndexed());
        assertTrue(entry.isMediaPtsIndexed());
        assertEquals("m", entry.getIndexType());
        assertNull(entry.getWallclockMillis());
        assertNull(entry.getGroupId());
        assertNull(entry.getObjectId());
        assertEquals(2000L, entry.getMediaPtsMillis());
    }

    // Large timeline handling

    @Test
    void testLargeMediaTimeline() throws Exception {
        MsfMediaTimeline timeline = new MsfMediaTimeline();
        List<MsfMediaTimelineRecord> records = new ArrayList<>();

        // Create 10000 records
        for (int i = 0; i < 10000; i++) {
            records.add(new MsfMediaTimelineRecord(i * 33, i, 0, System.currentTimeMillis()));
        }

        String json = timeline.toJson(records);
        List<MsfMediaTimelineRecord> parsed = timeline.fromJson(json);

        assertEquals(10000, parsed.size());
        assertEquals(0, parsed.get(0).getMediaPtsMillis());
        assertEquals(9999 * 33, parsed.get(9999).getMediaPtsMillis());
    }

    @Test
    void testLargeEventTimeline() throws Exception {
        MsfEventTimeline timeline = new MsfEventTimeline();
        List<MsfEventTimelineEntry> entries = new ArrayList<>();

        // Create 1000 entries
        for (int i = 0; i < 1000; i++) {
            JsonObject data = new JsonObject();
            data.addProperty("index", i);
            entries.add(MsfEventTimelineEntry.withWallclock(i * 1000L, data));
        }

        String json = timeline.toJson(entries);
        List<MsfEventTimelineEntry> parsed = timeline.fromJson(json);

        assertEquals(1000, parsed.size());
    }

    // GZIP compression effectiveness

    @Test
    void testGzipCompressionEffectiveness() throws Exception {
        MsfMediaTimeline timeline = new MsfMediaTimeline();
        List<MsfMediaTimelineRecord> records = new ArrayList<>();

        // Create records with repetitive data (good for compression)
        for (int i = 0; i < 1000; i++) {
            records.add(new MsfMediaTimelineRecord(i * 33, i, 0, 1700000000000L));
        }

        String json = timeline.toJson(records);
        byte[] gzip = timeline.toGzipJson(records);

        // GZIP should be smaller than raw JSON for repetitive data
        assertTrue(gzip.length < json.getBytes().length,
            "GZIP should compress repetitive data effectively");
    }
}
