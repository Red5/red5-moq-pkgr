package org.red5.io.moq.carp.timeline;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.util.List;

/**
 * JSON serializer/deserializer for CARP SAP event timelines.
 */
public class CarpSapTimeline {
    private final Gson gson;

    public CarpSapTimeline() {
        this.gson = new GsonBuilder().create();
    }

    public String toJson(List<CarpSapTimelineEntry> entries) {
        return gson.toJson(entries);
    }

    public List<CarpSapTimelineEntry> fromJson(String json) throws IOException {
        try {
            return gson.fromJson(json, new TypeToken<List<CarpSapTimelineEntry>>() {}.getType());
        } catch (Exception e) {
            throw new IOException("Failed to parse CARP SAP timeline JSON", e);
        }
    }
}
