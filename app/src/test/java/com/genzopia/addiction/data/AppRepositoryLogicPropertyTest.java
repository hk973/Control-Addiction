package com.genzopia.addiction.data;

import com.genzopia.addiction.data.model.AppInfo;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.NotEmpty;
import net.jqwik.api.constraints.Size;
import net.jqwik.api.constraints.StringLength;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Property-based tests for the pure helpers of {@link AppRepository}: the alphabetical
 * ordering and the change detection that decides whether a new list is published.
 */
class AppRepositoryLogicPropertyTest {

    private static List<AppInfo> appsOf(List<String> labels) {
        List<AppInfo> apps = new ArrayList<>();
        for (int i = 0; i < labels.size(); i++) {
            apps.add(new AppInfo(labels.get(i), "pkg." + i));
        }
        return apps;
    }

    /** Sorting is case-insensitive and total. */
    @Property(tries = 200)
    void sortIsAlphabeticalAndKeepsEveryEntry(
            @ForAll @Size(max = 15) List<@AlphaChars @NotEmpty @StringLength(max = 8) String> labels) {
        List<AppInfo> apps = appsOf(labels);
        int originalSize = apps.size();

        List<AppInfo> sorted = AppRepository.sortByLabel(new ArrayList<>(apps));

        assertEquals("sorting must not drop entries", originalSize, sorted.size());
        assertTrue("sorting must not invent entries", apps.containsAll(sorted));
        for (int i = 1; i < sorted.size(); i++) {
            assertTrue("list is not alphabetically ordered",
                    sorted.get(i - 1).getLabel().compareToIgnoreCase(sorted.get(i).getLabel()) <= 0);
        }
    }

    /** An identical list is never republished, which is what keeps refresh() cheap. */
    @Property(tries = 200)
    void identicalListsAreDetected(
            @ForAll @Size(max = 10) List<@AlphaChars @NotEmpty @StringLength(max = 8) String> labels) {
        List<AppInfo> first = appsOf(labels);
        List<AppInfo> second = appsOf(labels);

        assertTrue(AppRepository.hasSameContent(first, second));
    }

    /** Installing an app changes the content, so a new list must be published. */
    @Property(tries = 200)
    void addedPackageIsDetectedAsChange(
            @ForAll @Size(max = 10) List<@AlphaChars @NotEmpty @StringLength(max = 8) String> labels) {
        List<AppInfo> before = appsOf(labels);
        List<AppInfo> after = new ArrayList<>(before);
        after.add(new AppInfo("Newly Installed", "com.example.installed"));

        assertFalse(AppRepository.hasSameContent(before, after));
    }

    /** A renamed app (same package, new label) also counts as a change. */
    @Property(tries = 200)
    void renamedAppIsDetectedAsChange(
            @ForAll @AlphaChars @NotEmpty @StringLength(min = 1, max = 8) String label) {
        List<AppInfo> before = new ArrayList<>();
        before.add(new AppInfo(label, "com.example.app"));
        List<AppInfo> after = new ArrayList<>();
        after.add(new AppInfo(label + "X", "com.example.app"));

        assertFalse(AppRepository.hasSameContent(before, after));
    }
}
