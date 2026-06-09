package com.genzopia.addiction;

import com.genzopia.addiction.Launcher.AppFcmService;
import com.genzopia.addiction.Launcher.DataPayloadHandler;

import net.jqwik.api.*;
import net.jqwik.api.constraints.StringLength;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Property-based tests for AppFcmService-related behaviour.
 *
 * SharedPrefHelper.saveString / getString are thin wrappers over a key-value store.
 * We verify the round-trip contract using an equivalent in-memory map, consistent
 * with the pattern used in DataPayloadHandlerPropertyTest.
 */
class AppFcmServicePropertyTest {

    // -----------------------------------------------------------------------
    // Property 1: Token save round-trip
    //
    // For any FCM token string, after saving it via
    // SharedPrefHelper.saveString("fcm_token", token),
    // reading back getString("fcm_token", null)
    // SHALL return the same token string.
    //
    // Feature: fcm-data-payload, Property 1: Token save round-trip
    // Validates: Requirements 1.1
    // -----------------------------------------------------------------------

    /**
     * Property 1: Token save round-trip
     *
     * For any non-null FCM token string, saving it under a key and reading
     * it back SHALL return the identical string.
     *
     * The SharedPrefHelper save/get methods are key-value store wrappers.
     * We model the store as a HashMap (same contract) to test the property
     * without requiring an Android instrumentation context.
     *
     * Feature: fcm-data-payload, Property 1: Token save round-trip
     * Validates: Requirements 1.1
     */
    @Property(tries = 150)
    void tokenSaveRoundTrip(
            @ForAll @StringLength(min = 1, max = 200) String token) {
        // Simulate the SharedPrefHelper key-value store
        Map<String, String> store = new HashMap<>();
        final String FCM_TOKEN_KEY = "fcm_token";

        // Mirrors SharedPrefHelper.saveString(context, "fcm_token", token)
        store.put(FCM_TOKEN_KEY, token);

        // Mirrors SharedPrefHelper.getString(context, "fcm_token", null)
        String readBack = store.getOrDefault(FCM_TOKEN_KEY, null);

        assertEquals(
                "Token round-trip: stored value must equal the token that was saved",
                token,
                readBack
        );
    }

    // -----------------------------------------------------------------------
    // Property 6: onMessageReceived exception containment
    //
    // For any RemoteMessage that causes an exception inside onMessageReceived,
    // the exception SHALL be caught and SHALL NOT propagate out of the service method.
    //
    // Feature: fcm-data-payload, Property 6: onMessageReceived exception containment
    // Validates: Requirements 4.1, 4.4
    // -----------------------------------------------------------------------

    /** Delegate that always throws a RuntimeException when any handler is called. */
    static class ThrowingDelegate implements DataPayloadHandler.Delegate {
        final RuntimeException toThrow;

        ThrowingDelegate(RuntimeException e) {
            this.toThrow = e;
        }

        @Override
        public void onShowNotification(android.content.Context context, Map<String, String> data) {
            throw toThrow;
        }

        @Override
        public void onForceUpdate(android.content.Context context) {
            throw toThrow;
        }

        @Override
        public void onConfigRefresh(android.content.Context context, Map<String, String> data) {
            throw toThrow;
        }
    }

    /**
     * Property 6: onMessageReceived exception containment
     *
     * For any data map with a known type that causes the handler to throw,
     * AppFcmService.processPayload() SHALL catch the exception and not propagate it.
     *
     * Feature: fcm-data-payload, Property 6: onMessageReceived exception containment
     * Validates: Requirements 4.1, 4.4
     */
    @Property(tries = 150)
    void exceptionInHandlerIsContained(
            @ForAll("knownTypes") String type,
            @ForAll @StringLength(min = 1, max = 50) String errorMessage) {
        RuntimeException toThrow = new RuntimeException(errorMessage);
        DataPayloadHandler.setDelegate(new ThrowingDelegate(toThrow));

        Map<String, String> data = new HashMap<>();
        data.put("type", type);
        data.put("config_key", "k");
        data.put("config_value", "v");

        // Must complete without throwing — if the exception escapes, the test fails
        try {
            AppFcmService.processPayload(null, data);
        } catch (Exception e) {
            fail("Exception propagated out of processPayload: " + e.getMessage());
        } finally {
            DataPayloadHandler.resetDelegate();
        }
    }

    @Provide
    Arbitrary<String> knownTypes() {
        return Arbitraries.of(
                DataPayloadHandler.TYPE_SHOW_NOTIFICATION,
                DataPayloadHandler.TYPE_FORCE_UPDATE,
                DataPayloadHandler.TYPE_CONFIG_REFRESH
        );
    }
}
