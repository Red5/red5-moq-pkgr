package org.red5.io.moq.msf.catalog;

/**
 * MSF constants per draft-ietf-moq-msf.
 */
public final class MsfConstants {

    private MsfConstants() {
        // Utility class
    }

    /** MSF version 1 per section 5.1.1 */
    public static final int VERSION = 1;

    /** Catalog track name per section 5 */
    public static final String CATALOG_TRACK_NAME = "catalog";

    /** JSON mime type required for timeline tracks */
    public static final String JSON_MIME_TYPE = "application/json";

    /**
     * Latency thresholds (informative, per section 3).
     */
    public static final class Latency {
        private Latency() {}

        /** Real-time latency threshold: less than 500ms */
        public static final long REALTIME_MAX_MS = 500;

        /** Interactive latency threshold: 500ms to 2500ms */
        public static final long INTERACTIVE_MIN_MS = 500;
        public static final long INTERACTIVE_MAX_MS = 2500;

        /** Standard latency: above 2500ms */
        public static final long STANDARD_MIN_MS = 2500;
    }

    /**
     * Group numbering convention per section 6.1.
     * First Group ID should be unique (milliseconds since epoch recommended).
     */
    public static long generateInitialGroupId() {
        return System.currentTimeMillis();
    }
}
