package org.red5.io.moq.msf.catalog;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * MSF predefined track roles per draft-ietf-moq-msf section 5.1.14.
 * Custom roles are allowed as long as they don't collide with these predefined values.
 */
public enum TrackRole {
    /** An audio description for visually impaired users */
    AUDIO_DESCRIPTION("audiodescription"),

    /** Visual content */
    VIDEO("video"),

    /** Audio content */
    AUDIO("audio"),

    /** An MSF media timeline (section 7) */
    MEDIA_TIMELINE("mediatimeline"),

    /** An MSF event timeline (section 8) */
    EVENT_TIMELINE("eventtimeline"),

    /** A textual representation of the audio track */
    CAPTION("caption"),

    /** A transcription of the spoken dialogue */
    SUBTITLE("subtitle"),

    /** A visual track for hearing impaired users */
    SIGN_LANGUAGE("signlanguage");

    private final String value;

    TrackRole(String value) {
        this.value = value;
    }

    /**
     * Get the string value used in JSON catalog.
     */
    public String getValue() {
        return value;
    }

    /**
     * Get all predefined role values as a set.
     */
    public static Set<String> allValues() {
        return Arrays.stream(values())
                .map(TrackRole::getValue)
                .collect(Collectors.toSet());
    }

    /**
     * Check if a role value is a predefined MSF role.
     */
    public static boolean isPredefined(String role) {
        return allValues().contains(role);
    }

    /**
     * Parse a string value to TrackRole enum.
     * @return the matching TrackRole or null if not a predefined role
     */
    public static TrackRole fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (TrackRole role : values()) {
            if (role.value.equals(value)) {
                return role;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return value;
    }
}
