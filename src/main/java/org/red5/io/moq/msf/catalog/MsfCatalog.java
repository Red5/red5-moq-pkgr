package org.red5.io.moq.msf.catalog;

import org.red5.io.moq.warp.catalog.WarpCatalog;
import org.red5.io.moq.warp.catalog.WarpTrack;

import java.util.ArrayList;
import java.util.List;

/**
 * MSF catalog with builder pattern for easier construction.
 * Extends WarpCatalog with MSF-specific defaults and convenience methods.
 */
public class MsfCatalog extends WarpCatalog {

    /**
     * Create a new MSF catalog builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Create a catalog for terminating a live broadcast per MSF section 5.3.9.
     */
    public static MsfCatalog termination() {
        MsfCatalog catalog = new MsfCatalog();
        catalog.setVersion(MsfConstants.VERSION);
        catalog.setGeneratedAt(System.currentTimeMillis());
        catalog.setIsComplete(true);
        catalog.setTracks(new ArrayList<>());
        return catalog;
    }

    /**
     * Validate this catalog against MSF rules.
     * @throws IllegalArgumentException if validation fails
     */
    public void validate() {
        MsfCatalogValidator.validateCatalog(this);
    }

    /**
     * Builder for constructing MSF catalogs.
     */
    public static class Builder {
        private final MsfCatalog catalog;
        private final List<WarpTrack> tracks;

        private Builder() {
            this.catalog = new MsfCatalog();
            this.catalog.setVersion(MsfConstants.VERSION);
            this.tracks = new ArrayList<>();
        }

        /**
         * Set the generation timestamp (defaults to current time if not set).
         */
        public Builder generatedAt(long timestampMillis) {
            catalog.setGeneratedAt(timestampMillis);
            return this;
        }

        /**
         * Mark this catalog as complete (for VOD or terminated broadcasts).
         */
        public Builder complete() {
            catalog.setIsComplete(true);
            return this;
        }

        /**
         * Add a track to the catalog.
         */
        public Builder addTrack(WarpTrack track) {
            tracks.add(track);
            return this;
        }

        /**
         * Add a track using MsfTrack builder.
         */
        public Builder addTrack(MsfTrack.Builder trackBuilder) {
            tracks.add(trackBuilder.build());
            return this;
        }

        /**
         * Add multiple tracks to the catalog.
         */
        public Builder addTracks(List<WarpTrack> trackList) {
            tracks.addAll(trackList);
            return this;
        }

        /**
         * Build and validate the catalog.
         * @throws IllegalArgumentException if validation fails
         */
        public MsfCatalog build() {
            if (catalog.getGeneratedAt() == null) {
                catalog.setGeneratedAt(System.currentTimeMillis());
            }
            catalog.setTracks(new ArrayList<>(tracks));
            catalog.validate();
            return catalog;
        }

        /**
         * Build the catalog without validation (for testing or partial catalogs).
         */
        public MsfCatalog buildWithoutValidation() {
            if (catalog.getGeneratedAt() == null) {
                catalog.setGeneratedAt(System.currentTimeMillis());
            }
            catalog.setTracks(new ArrayList<>(tracks));
            return catalog;
        }
    }
}
