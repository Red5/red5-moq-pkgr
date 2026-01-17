package org.red5.io.moq.warp;

import org.junit.jupiter.api.Test;
import org.red5.io.moq.warp.timeline.WarpTimeline;
import org.red5.io.moq.warp.timeline.WarpTimelineRecord;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WarpTimelineTest {

    @Test
    void testTimelineRoundTrip() {
        WarpTimeline timeline = new WarpTimeline();
        List<WarpTimelineRecord> records = List.of(
                new WarpTimelineRecord(1000, 1L, 0L, 1700000000000L, "start"),
                new WarpTimelineRecord(2000, null, null, 0L, "quoted \"meta\"")
        );

        String csv = timeline.toCsv(records);
        assertTrue(csv.startsWith(WarpTimeline.HEADER));

        List<WarpTimelineRecord> parsed = timeline.fromCsv(csv);
        assertEquals(2, parsed.size());
        assertEquals(1000L, parsed.get(0).getMediaPtsMillis());
        assertEquals(1L, parsed.get(0).getGroupId());
        assertEquals("start", parsed.get(0).getMetadata());
        assertEquals("quoted \"meta\"", parsed.get(1).getMetadata());
    }

    @Test
    void testInvalidHeaderRejected() {
        WarpTimeline timeline = new WarpTimeline();
        String csv = "BAD_HEADER\n1,2,3,4,meta\n";

        assertThrows(IllegalArgumentException.class, () -> timeline.fromCsv(csv));
    }

    @Test
    void testMissingRequiredFieldsRejected() {
        WarpTimeline timeline = new WarpTimeline();
        String csv = WarpTimeline.HEADER + "\r\n" + ",,,,\r\n";

        assertThrows(IllegalArgumentException.class, () -> timeline.fromCsv(csv));
    }
}
