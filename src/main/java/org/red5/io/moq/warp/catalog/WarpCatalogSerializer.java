package org.red5.io.moq.warp.catalog;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;

/**
 * JSON serializer/deserializer for WARP catalogs.
 */
public class WarpCatalogSerializer {
    private final Gson gson;

    public WarpCatalogSerializer() {
        this.gson = new GsonBuilder().create();
    }

    public WarpCatalogSerializer(Gson gson) {
        this.gson = gson;
    }

    public String toJson(WarpCatalog catalog) {
        return gson.toJson(catalog);
    }

    public WarpCatalog fromJson(String json) throws IOException {
        try {
            return gson.fromJson(json, WarpCatalog.class);
        } catch (Exception e) {
            throw new IOException("Failed to parse WARP catalog JSON", e);
        }
    }
}
