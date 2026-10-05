package co.wethinkcode.healthsafe;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WardDataCleanerTest {

    private static List<WardRecord> wards;

    @BeforeAll
    static void load() throws Exception {
        wards = WardDataCleaner.loadAndClean("test-wards.csv");
    }

    private static WardRecord find(String id) {
        return wards.stream()
                .filter(w -> w.wardId.equals(id))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No ward with id " + id));
    }

    @Test
    void duplicateRowsAreMergedIntoOneRecord() {
        // 18 data rows, w-05 appears twice -> 17 records
        assertEquals(17, wards.size());
    }

    @Test
    void idsAreTrimmedAndUppercased() {
        assertEquals("W-03", find("W-03").wardId);
        assertEquals("W-02", find("W-02").wardId);
    }

    @Test
    void wingsAreTrimmedCollapsedAndCapitalised() {
        assertEquals("East Wing", find("W-01").wing);
        assertEquals("South Wing", find("W-10").wing); // double space collapsed
        assertEquals("North Wing", find("W-17").wing);
    }

    @Test
    void missingWingBecomesNullWithNote() {
        WardRecord w = find("W-08");
        assertNull(w.wing);
        assertNotNull(w.notes);
        assertTrue(w.notes.contains("wing was missing"));
    }

    @Test
    void paediatricsSpellingVariantsAreUnified() {
        assertEquals("Paediatrics", find("W-02").department);
        assertEquals("Paediatrics", find("W-11").department);
        assertEquals("ICU", find("W-09").department);
    }

    @Test
    void placeholderBedsBecomeNullWithNote() {
        for (String id : List.of("W-02", "W-07", "W-09", "W-15")) {
            WardRecord w = find(id);
            assertNull(w.bedsAvailable, id);
            assertTrue(w.notes.contains("missing/placeholder"), id);
        }
    }

    @Test
    void invalidBedCountsAreFlaggedNotCrashing() {
        assertTrue(find("W-04").notes.contains("negative"));
        assertTrue(find("W-14").notes.contains("negative"));
        assertTrue(find("W-12").notes.contains("non-numeric"));
        assertTrue(find("W-13").notes.contains("unrealistic"));
        assertNull(find("W-13").bedsAvailable);
    }

    @Test
    void validBedCountsAreKeptIncludingZero() {
        assertEquals(3, find("W-01").bedsAvailable);
        assertEquals(0, find("W-03").bedsAvailable);
        assertNull(find("W-01").notes);
    }

}