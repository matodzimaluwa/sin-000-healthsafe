package co.wethinkcode.healthsafe;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AlertLevelTest {

    // README: Emergency Status is 0-8; a fresh service is at normal (0)
    @Test
    void startsAtZero() {
        assertEquals(0, new AlertLevel().get());
    }

    @Test
    void acceptsEveryLevelFromZeroToEight() {
        AlertLevel alertLevel = new AlertLevel();
        for (int level = 0; level <= 8; level++) {
            alertLevel.set(level);
            assertEquals(level, alertLevel.get());
        }
    }

    @Test
    void rejectsLevelAboveEight() {
        assertThrows(IllegalArgumentException.class, () -> new AlertLevel().set(9));
    }

    @Test
    void rejectsNegativeLevel() {
        assertThrows(IllegalArgumentException.class, () -> new AlertLevel().set(-1));
    }

    // A rejected value must not change the level
    @Test
    void rejectedLevelLeavesTheOldLevelInPlace() {
        AlertLevel alertLevel = new AlertLevel();
        alertLevel.set(5);
        assertThrows(IllegalArgumentException.class, () -> alertLevel.set(20));
        assertEquals(5, alertLevel.get());
    }
}