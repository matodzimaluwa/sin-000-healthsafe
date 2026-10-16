package co.wethinkcode.healthsafe;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class EquipmentAlertTest {
    // Ward ids are stored the way the other services write them: trimmed, upper case
    @Test
    void wardIdIsTrimmedAndUppercased() {
        EquipmentAlert alert = new EquipmentAlert("  w-01 ", "ventilator", "2026-10-06T10:00:00Z");
        assertEquals("W-01", alert.wardId);
    }

    @Test
    void equipmentIsTrimmed() {
        EquipmentAlert alert = new EquipmentAlert("W-01", "  ventilator ", "2026-10-06T10:00:00Z");
        assertEquals("ventilator", alert.equipment);
    }

    @Test
    void keepsTheReceivedTime() {
        EquipmentAlert alert = new EquipmentAlert("W-01", "ventilator", "2026-10-06T10:00:00Z");
        assertEquals("2026-10-06T10:00:00Z", alert.receivedAt);
    }

    // An alert with no ward or no equipment is useless, so it is refused
    @Test
    void rejectsMissingWardId() {
        assertThrows(IllegalArgumentException.class,
                () -> new EquipmentAlert(null, "ventilator", "2026-10-06T10:00:00Z"));
    }

    @Test
    void rejectsBlankWardId() {
        assertThrows(IllegalArgumentException.class,
                () -> new EquipmentAlert("   ", "ventilator", "2026-10-06T10:00:00Z"));
    }

    @Test
    void rejectsMissingEquipment() {
        assertThrows(IllegalArgumentException.class,
                () -> new EquipmentAlert("W-01", null, "2026-10-06T10:00:00Z"));
    }

    @Test
    void rejectsBlankEquipment() {
        assertThrows(IllegalArgumentException.class,
                () -> new EquipmentAlert("W-01", "  ", "2026-10-06T10:00:00Z"));
    }

}
