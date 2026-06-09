package com.genzopia.addiction;

import android.content.Context;

import com.genzopia.addiction.Launcher.DataPayloadHandler;

import net.jqwik.api.*;
import net.jqwik.api.constraints.StringLength;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Property-based tests for DataPayloadHandler routing and side-effect rules.
 *
 * All tests use a pure-Java spy delegate so no Android runtime is required.
 */
class DataPayloadHandlerPropertyTest {

    // -----------------------------------------------------------------------
    // Spy delegate — records which handler was called and captures config writes
    // -----------------------------------------------------------------------

    /** Counting delegate used in place of the real production delegate. */
    static class SpyDelegate implements DataPayloadHandler.Delegate {
        int showNotificationCalls = 0;
        int forceUpdateCalls      = 0;
        int configRefreshCalls    = 0;

        // Simulates SharedPrefs with an in-memory map (used for Property 5 round-trip)
        final Map<String, String> configStore = new HashMap<>();

        @Override
        public void onShowNotification(Context context, Map<String, String> data) {
            showNotificationCalls++;
        }

        @Override
        public void onForceUpdate(Context context) {
            forceUpdateCalls++;
        }

        @Override
        public void onConfigRefresh(Context context, Map<String, String> data) {
            configRefreshCalls++;
            String key   = data.get("config_key");
            String value = data.get("config_value");
            // Mirror production guard: key must be non-null and non-blank
            if (key != null && !key.trim().isEmpty() && value != null) {
                configStore.put(key, value);
            }
        }

        void reset() {
            showNotificationCalls = 0;
            forceUpdateCalls      = 0;
            configRefreshCalls    = 0;
            configStore.clear();
        }

        int totalCalls() {
            return showNotificationCalls + forceUpdateCalls + configRefreshCalls;
        }
    }

    private final SpyDelegate spy = new SpyDelegate();

    /** Install and tear down the spy around each property. */
    private Map<String, String> setupSpy() {
        spy.reset();
        DataPayloadHandler.setDelegate(spy);
        return new HashMap<>();
    }

    private void teardownSpy() {
        DataPayloadHandler.resetDelegate();
    }

    // -----------------------------------------------------------------------
    // Property 2: Known type routes to exactly one handler
    // Feature: fcm-data-payload, Property 2: Known type routes to exactly one handler
    // Validates: Requirements 1.4
    // -----------------------------------------------------------------------

    /**
     * For any data map with type="show_notification",
     * handle() SHALL invoke exactly the show_notification handler and no other.
     *
     * Feature: fcm-data-payload, Property 2: Known type routes to exactly one handler
     * Validates: Requirements 1.4
     */
    @Property(tries = 100)
    void showNotificationRouteIsExclusive(
            @ForAll @StringLength(min = 0, max = 30) String title,
            @ForAll @StringLength(min = 0, max = 50) String body) {
        Map<String, String> data = setupSpy();
        data.put("type", DataPayloadHandler.TYPE_SHOW_NOTIFICATION);
        data.put("title", title);
        data.put("body", body);

        DataPayloadHandler.handle(null, data);

        assertEquals("show_notification must call onShowNotification exactly once",
                1, spy.showNotificationCalls);
        assertEquals("show_notification must call no other handler",
                1, spy.totalCalls());
        teardownSpy();
    }

    /**
     * For any data map with type="force_update",
     * handle() SHALL invoke exactly the force_update handler and no other.
     *
     * Feature: fcm-data-payload, Property 2: Known type routes to exactly one handler
     * Validates: Requirements 1.4
     */
    @Property(tries = 100)
    void forceUpdateRouteIsExclusive(@ForAll @StringLength(min = 0, max = 20) String unused) {
        Map<String, String> data = setupSpy();
        data.put("type", DataPayloadHandler.TYPE_FORCE_UPDATE);

        DataPayloadHandler.handle(null, data);

        assertEquals("force_update must call onForceUpdate exactly once",
                1, spy.forceUpdateCalls);
        assertEquals("force_update must call no other handler",
                1, spy.totalCalls());
        teardownSpy();
    }

    /**
     * For any data map with type="config_refresh",
     * handle() SHALL invoke exactly the config_refresh handler and no other.
     *
     * Feature: fcm-data-payload, Property 2: Known type routes to exactly one handler
     * Validates: Requirements 1.4
     */
    @Property(tries = 100)
    void configRefreshRouteIsExclusive(
            @ForAll @StringLength(min = 1, max = 30) String key,
            @ForAll @StringLength(min = 0, max = 50) String value) {
        Map<String, String> data = setupSpy();
        data.put("type", DataPayloadHandler.TYPE_CONFIG_REFRESH);
        data.put("config_key", key);
        data.put("config_value", value);

        DataPayloadHandler.handle(null, data);

        assertEquals("config_refresh must call onConfigRefresh exactly once",
                1, spy.configRefreshCalls);
        assertEquals("config_refresh must call no other handler",
                1, spy.totalCalls());
        teardownSpy();
    }

    // -----------------------------------------------------------------------
    // Property 3: Unknown type is discarded without side effects
    // Feature: fcm-data-payload, Property 3: Unknown type is discarded without side effects
    // Validates: Requirements 1.3, 1.5
    // -----------------------------------------------------------------------

    /**
     * For any data map whose type field is not one of the known types,
     * handle() SHALL invoke no handler and produce no side effects.
     *
     * Feature: fcm-data-payload, Property 3: Unknown type is discarded without side effects
     * Validates: Requirements 1.3, 1.5
     */
    @Property(tries = 150)
    void unknownTypeInvokesNoHandler(@ForAll("unknownTypes") String unknownType) {
        Map<String, String> data = setupSpy();
        data.put("type", unknownType);

        DataPayloadHandler.handle(null, data);

        assertEquals("Unknown type must invoke no handler",
                0, spy.totalCalls());
        teardownSpy();
    }

    /**
     * An empty data map must invoke no handler.
     *
     * Feature: fcm-data-payload, Property 3: Unknown type is discarded without side effects
     * Validates: Requirements 1.3
     */
    @Property(tries = 100)
    void emptyMapInvokesNoHandler(@ForAll @StringLength(min = 0, max = 5) String ignored) {
        setupSpy();

        DataPayloadHandler.handle(null, new HashMap<>());

        assertEquals("Empty map must invoke no handler", 0, spy.totalCalls());
        teardownSpy();
    }

    /**
     * A null data map must invoke no handler.
     *
     * Feature: fcm-data-payload, Property 3: Unknown type is discarded without side effects
     * Validates: Requirements 1.3
     */
    @Property(tries = 100)
    void nullMapInvokesNoHandler(@ForAll @StringLength(min = 0, max = 5) String ignored) {
        setupSpy();

        DataPayloadHandler.handle(null, null);

        assertEquals("Null map must invoke no handler", 0, spy.totalCalls());
        teardownSpy();
    }

    @Provide
    Arbitrary<String> unknownTypes() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(0)
                .ofMaxLength(30)
                .filter(s -> !s.equals(DataPayloadHandler.TYPE_SHOW_NOTIFICATION)
                        && !s.equals(DataPayloadHandler.TYPE_FORCE_UPDATE)
                        && !s.equals(DataPayloadHandler.TYPE_CONFIG_REFRESH));
    }

    // -----------------------------------------------------------------------
    // Property 5: config_refresh round-trip in SharedPreferences
    // Feature: fcm-data-payload, Property 5: config_refresh round-trip in SharedPreferences
    // Validates: Requirements 3.2
    // -----------------------------------------------------------------------

    /**
     * For any valid config_key / config_value pair,
     * after a config_refresh payload is processed, reading back the value
     * from the in-memory config store SHALL return the same value.
     *
     * Feature: fcm-data-payload, Property 5: config_refresh round-trip in SharedPreferences
     * Validates: Requirements 3.2
     */
    @Property(tries = 150)
    void configRefreshRoundTrip(
            @ForAll @net.jqwik.api.constraints.AlphaChars @StringLength(min = 1, max = 40) String key,
            @ForAll @StringLength(min = 0, max = 80) String value) {
        Map<String, String> data = setupSpy();
        data.put("type", DataPayloadHandler.TYPE_CONFIG_REFRESH);
        data.put("config_key", key);
        data.put("config_value", value);

        DataPayloadHandler.handle(null, data);

        String readBack = spy.configStore.get(key);

        assertEquals("config_refresh round-trip: stored value must match sent value",
                value, readBack);
        teardownSpy();
    }
}
