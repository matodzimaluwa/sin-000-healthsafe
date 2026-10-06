package co.wethinkcode.healthsafe;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StaffingRulesTest {
    // Rule: 1 doctor on call at normal (level 0), plus 1 more for every 2 levels

    @Test
    void oneDoctorAtNormalLevel() {
        assertEquals(1, StaffingRules.doctorsOnCall(0));
    }

    @Test
    void oddLevelDoesNotAddADoctorYet() {
        assertEquals(1, StaffingRules.doctorsOnCall(1));
        assertEquals(2, StaffingRules.doctorsOnCall(3));
    }

    @Test
    void anExtraDoctorEveryTwoLevels() {
        assertEquals(2, StaffingRules.doctorsOnCall(2));
        assertEquals(3, StaffingRules.doctorsOnCall(4));
        assertEquals(4, StaffingRules.doctorsOnCall(6));
    }

    // Level 8 is full Code Blue: the most doctors
    @Test
    void codeBlueHasTheMostDoctors() {
        assertEquals(5, StaffingRules.doctorsOnCall(8));
    }

    // A higher level must never mean fewer doctors
    @Test
    void higherLevelNeverMeansFewerDoctors() {
        for (int level = 1; level <= 8; level++) {
            assertTrue(StaffingRules.doctorsOnCall(level) >= StaffingRules.doctorsOnCall(level - 1),
                    "level " + level);
        }
    }

    // Emergency Status only exists from 0 to 8
    @Test
    void rejectsLevelsOutsideZeroToEight() {
        assertThrows(IllegalArgumentException.class, () -> StaffingRules.doctorsOnCall(-1));
        assertThrows(IllegalArgumentException.class, () -> StaffingRules.doctorsOnCall(9));
    }
}


