package org.red5.io.moq.msf.catalog;

import org.red5.io.moq.warp.catalog.WarpCatalog;
import org.red5.io.moq.warp.catalog.WarpCatalogSerializer;

import java.io.IOException;

/**
 * JSON serializer/deserializer for MSF catalogs.
 * Wraps WarpCatalogSerializer with MSF validation.
 */
public class MsfCatalogSerializer {
    private final WarpCatalogSerializer delegate;

    public MsfCatalogSerializer() {
        this.delegate = new WarpCatalogSerializer();
    }

    /**
     * Serialize catalog to JSON string.
     */
    public String toJson(WarpCatalog catalog) {
        return delegate.toJson(catalog);
    }

    /**
     * Deserialize JSON string to catalog.
     */
    public MsfCatalog fromJson(String json) throws IOException {
        WarpCatalog warpCatalog = delegate.fromJson(json);
        // Convert to MsfCatalog
        MsfCatalog msfCatalog = new MsfCatalog();
        msfCatalog.setVersion(warpCatalog.getVersion());
        msfCatalog.setDeltaUpdate(warpCatalog.getDeltaUpdate());
        msfCatalog.setIsComplete(warpCatalog.getIsComplete());
        msfCatalog.setGeneratedAt(warpCatalog.getGeneratedAt());
        msfCatalog.setTracks(warpCatalog.getTracks());
        msfCatalog.setAddTracks(warpCatalog.getAddTracks());
        msfCatalog.setRemoveTracks(warpCatalog.getRemoveTracks());
        msfCatalog.setCloneTracks(warpCatalog.getCloneTracks());
        return msfCatalog;
    }

    /**
     * Deserialize and validate JSON string to catalog.
     * @throws IOException if parsing fails
     * @throws IllegalArgumentException if validation fails
     */
    public MsfCatalog fromJsonValidated(String json) throws IOException {
        MsfCatalog catalog = fromJson(json);
        catalog.validate();
        return catalog;
    }
}
