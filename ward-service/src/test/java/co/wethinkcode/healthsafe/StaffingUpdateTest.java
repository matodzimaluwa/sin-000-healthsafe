package co.wethinkcode.healthsafe;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StaffingUpdateTest {

    // Ward ids are stored the way the other services write them,trimmed, upper case
    @Test
    void wardIdIsTrimmedAndUppercased() {
        assertEquals("W-01", new StaffingUpdate("  w-01 ", 4, 3).wardId);
    }

    @Test
    void keepsTheLevelAndTheDoctors() {
        StaffingUpdate update = new StaffingUpdate("W-01", 8, 5);
        assertEquals(8, update.level);
        assertEquals(5, update.doctorsOnCall);
    }

    // The Emergency Status runs from 0 to 8, and both ends are valid
    @Test
    void acceptsLevelZeroAndEight() {
        assertEquals(0, new StaffingUpdate("W-01", 0, 1).level);
        assertEquals(8, new StaffingUpdate("W-01", 8, 5).level);
    }

    // An update that makes no sense is refused instead of being stored
    @Test
    void rejectsLevelOutsideZeroToEight() {
        assertThrows(IllegalArgumentException.class, () -> new StaffingUpdate("W-01", 9, 5));
        assertThrows(IllegalArgumentException.class, () -> new StaffingUpdate("W-01", -1, 5));
    }

    @Test
    void rejectsNegativeDoctors() {
        assertThrows(IllegalArgumentException.class, () -> new StaffingUpdate("W-01", 4, -1));
    }

    @Test
    void rejectsMissingOrBlankWardId() {
        assertThrows(IllegalArgumentException.class, () -> new StaffingUpdate(null, 4, 3));
        assertThrows(IllegalArgumentException.class, () -> new StaffingUpdate("   ", 4, 3));
    }
}