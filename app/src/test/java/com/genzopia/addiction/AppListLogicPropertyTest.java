package com.genzopia.addiction;

import com.genzopia.addiction.Launcher.FastScrollView;
import com.genzopia.addiction.data.model.AppInfo;
import com.genzopia.addiction.ui.common.AppListAdapter;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.NotEmpty;
import net.jqwik.api.constraints.Size;
import net.jqwik.api.constraints.StringLength;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Property-based tests for the shared app-list logic that replaced the three
 * duplicated adapters (search filtering and the A-Z fast-scroll sections).
 *
 * Pure Java: the tested helpers are static and never touch the Android runtime.
 */
class AppListLogicPropertyTest {

    private static List<AppInfo> appsOf(List<String> labels) {
        List<AppInfo> apps = new ArrayList<>();
        for (int i = 0; i < labels.size(); i++) {
            apps.add(new AppInfo(labels.get(i), "pkg." + i));
        }
        return apps;
    }

    /** Every result of a search is an element of the source list. */
    @Property(tries = 200)
    void filterOnlyReturnsSourceItems(@ForAll @Size(max = 12) List<@AlphaChars @NotEmpty @StringLength(max = 8) String> labels,
                                      @ForAll @AlphaChars @NotEmpty @StringLength(max = 3) String query) {
        List<AppInfo> source = appsOf(labels);

        List<AppInfo> filtered = AppListAdapter.filterApps(source, query, new ArrayList<>());

        assertNotNull(filtered);
        assertTrue("filter must never invent entries", source.containsAll(filtered));
    }

    /** Every result of a search contains the query, case-insensitively. */
    @Property(tries = 200)
    void filterKeepsOnlyMatches(@ForAll @Size(max = 12) List<@AlphaChars @NotEmpty @StringLength(max = 8) String> labels,
                                @ForAll @AlphaChars @NotEmpty @StringLength(max = 3) String query) {
        List<AppInfo> filtered = AppListAdapter.filterApps(appsOf(labels), query, new ArrayList<>());

        for (AppInfo app : filtered) {
            assertTrue("'" + app.getLabel() + "' does not contain '" + query + "'",
                    app.getLabel().toLowerCase().contains(query.toLowerCase()));
        }
    }

    /** Pinned matches always come before the alphabetical remainder. */
    @Property(tries = 200)
    void pinnedMatchesComeFirst(@ForAll @Size(min = 2, max = 10) List<@AlphaChars @NotEmpty @StringLength(max = 6) String> labels) {
        // A shared prefix guarantees that the query below matches every entry.
        List<String> prefixed = new ArrayList<>();
        for (String label : labels) {
            prefixed.add("zz" + label);
        }
        List<AppInfo> source = appsOf(prefixed);

        // Pin the last entry, which alphabetically would not be first.
        List<String> pinned = new ArrayList<>();
        pinned.add(source.get(source.size() - 1).getPackageName());

        List<AppInfo> filtered = AppListAdapter.filterApps(source, "zz", pinned);
        assertEquals("query matches every entry", source.size(), filtered.size());

        boolean seenUnpinned = false;
        for (AppInfo app : filtered) {
            boolean isPinned = pinned.contains(app.getPackageName());
            if (!isPinned) {
                seenUnpinned = true;
            } else {
                assertTrue("a pinned app appeared after an unpinned one", !seenUnpinned);
            }
        }
    }

    /** Section positions are strictly increasing and never point into the pinned block. */
    @Property(tries = 200)
    void sectionsAreOrderedAndSkipPinned(@ForAll @Size(max = 15) List<@AlphaChars @NotEmpty @StringLength(min = 1, max = 6) String> labels,
                                         @ForAll int rawPinnedCount) {
        List<AppInfo> apps = appsOf(labels);
        int pinnedCount = Math.abs(rawPinnedCount % (apps.size() + 1));

        List<FastScrollView.Section> sections = AppListAdapter.computeSections(apps, pinnedCount);

        int previous = -1;
        for (FastScrollView.Section section : sections) {
            assertTrue("section must not point into the pinned block", section.position >= pinnedCount);
            assertTrue("section positions must strictly increase", section.position > previous);
            previous = section.position;
            assertEquals("section letter must match the item label",
                    apps.get(section.position).getLabel().substring(0, 1).toUpperCase(),
                    section.letter);
        }
    }

    /** A null source never blows up. */
    @Property(tries = 10)
    void nullInputsAreTolerated(@ForAll @AlphaChars @StringLength(max = 4) String query) {
        assertTrue(AppListAdapter.filterApps(null, query, null).isEmpty());
        assertTrue(AppListAdapter.computeSections(null, 0).isEmpty());
    }
}
