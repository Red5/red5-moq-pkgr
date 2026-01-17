package org.red5.io.moq.warp.timeline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * CSV serializer/deserializer for WARP timeline payloads.
 */
public class WarpTimeline {
    public static final String HEADER = "MEDIA_PTS,GROUP_ID,OBJECT_ID,WALLCLOCK,METADATA";

    public String toCsv(List<WarpTimelineRecord> records) {
        StringBuilder builder = new StringBuilder();
        builder.append(HEADER).append("\r\n");
        if (records == null) {
            return builder.toString();
        }
        for (WarpTimelineRecord record : records) {
            builder.append(record.getMediaPtsMillis()).append(',');
            builder.append(record.getGroupId() != null ? record.getGroupId() : "").append(',');
            builder.append(record.getObjectId() != null ? record.getObjectId() : "").append(',');
            builder.append(record.getWallclockMillis()).append(',');
            builder.append(encodeMetadata(record.getMetadata()));
            builder.append("\r\n");
        }
        return builder.toString();
    }

    public List<WarpTimelineRecord> fromCsv(String csv) {
        if (csv == null || csv.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String normalized = csv.replace("\r\n", "\n").replace("\r", "\n");
        String[] lines = normalized.split("\n");
        int index = 0;
        while (index < lines.length && lines[index].trim().isEmpty()) {
            index++;
        }
        if (index >= lines.length) {
            return Collections.emptyList();
        }
        if (!HEADER.equals(lines[index].trim())) {
            throw new IllegalArgumentException("Timeline header row is missing or invalid");
        }
        index++;

        List<WarpTimelineRecord> records = new ArrayList<>();
        for (; index < lines.length; index++) {
            String line = lines[index];
            if (line.trim().isEmpty()) {
                continue;
            }
            List<String> fields = parseCsvLine(line);
            if (fields.size() != 5) {
                throw new IllegalArgumentException("Timeline row must have 5 columns");
            }
            long mediaPts = parseRequiredLong(fields.get(0), "MEDIA_PTS");
            Long groupId = parseOptionalLong(fields.get(1));
            Long objectId = parseOptionalLong(fields.get(2));
            long wallclock = parseRequiredLong(fields.get(3), "WALLCLOCK");
            String metadata = fields.get(4).isEmpty() ? null : fields.get(4);
            records.add(new WarpTimelineRecord(mediaPts, groupId, objectId, wallclock, metadata));
        }
        return records;
    }

    private static String encodeMetadata(String metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "";
        }
        String escaped = metadata.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    private static List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    fields.add(current.toString());
                    current.setLength(0);
                } else {
                    current.append(c);
                }
            }
        }
        fields.add(current.toString());
        return fields;
    }

    private static Long parseOptionalLong(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return Long.parseLong(value);
    }

    private static long parseRequiredLong(String value, String fieldName) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Missing required field " + fieldName);
        }
        return Long.parseLong(value);
    }
}
