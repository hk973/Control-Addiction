package com.genzopia.addiction;

import com.genzopia.addiction.Launcher.NotificationHelper;

import net.jqwik.api.*;
import net.jqwik.api.constraints.StringLength;

import static org.junit.Assert.*;

/**
 * Property-based tests for NotificationHelper default substitution logic.
 *
 * Feature: fcm-data-payload, Property 4: Notification fallback defaults are applied
 * Validates: Requirements 2.6
 */
class NotificationHelperPropertyTest {

    /**
     * Property 4: Notification fallback defaults are applied
     *
     * For any show_notification data map missing title or body,
     * NotificationHelper.show() SHALL be called with a non-null, non-empty
     * string for both title and body.
     *
     * We test the applyTitleDefault / applyBodyDefault helpers which are the
     * exact substitution step that runs before show() is invoked.
     *
     * Feature: fcm-data-payload, Property 4: Notification fallback defaults are applied
     * Validates: Requirements 2.6
     */
    @Property(tries = 200)
    void titleDefaultIsNeverNullOrEmpty(@ForAll @StringLength(max = 20) String rawTitle) {
        String result = NotificationHelper.applyTitleDefault(rawTitle);
        assertNotNull("title default must not be null", result);
        assertFalse("title default must not be empty", result.trim().isEmpty());
    }

    @Property(tries = 200)
    void bodyDefaultIsNeverNullOrEmpty(@ForAll @StringLength(max = 20) String rawBody) {
        String result = NotificationHelper.applyBodyDefault(rawBody);
        assertNotNull("body default must not be null", result);
        assertFalse("body default must not be empty", result.trim().isEmpty());
    }

    @Property(tries = 200)
    void nullTitleProducesDefaultString(@ForAll("nullOrBlankStrings") String nullOrBlank) {
        String result = NotificationHelper.applyTitleDefault(nullOrBlank);
        assertEquals("Notification", result);
    }

    @Property(tries = 200)
    void nullBodyProducesDefaultString(@ForAll("nullOrBlankStrings") String nullOrBlank) {
        String result = NotificationHelper.applyBodyDefault(nullOrBlank);
        assertEquals("You have a new message", result);
    }

    @Provide
    Arbitrary<String> nullOrBlankStrings() {
        // Generate null, empty string, or whitespace-only strings
        Arbitrary<String> blank = Arbitraries.strings()
                .withChars(' ', '\t', '\n', '\r')
                .ofMinLength(0)
                .ofMaxLength(10);
        Arbitrary<String> nullArb = Arbitraries.just(null);
        return Arbitraries.oneOf(blank, nullArb);
    }

    @Property(tries = 200)
    void nonBlankTitleIsPreserved(@ForAll("nonBlankStrings") String nonBlank) {
        String result = NotificationHelper.applyTitleDefault(nonBlank);
        assertEquals(nonBlank, result);
    }

    @Property(tries = 200)
    void nonBlankBodyIsPreserved(@ForAll("nonBlankStrings") String nonBlank) {
        String result = NotificationHelper.applyBodyDefault(nonBlank);
        assertEquals(nonBlank, result);
    }

    @Provide
    Arbitrary<String> nonBlankStrings() {
        // Generate strings that contain at least one non-whitespace character
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(30);
    }
}
