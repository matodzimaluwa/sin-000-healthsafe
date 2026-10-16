package co.wethinkcode.healthsafe;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class EquipmentFailureTest {
    // Ward ids are stored the way the other services write them: trimmed, upper case
    @Test
    void wardIdIsTrimmedAndUppercased() {
        assertEquals("W-01", new EquipmentFailure("  w-01 ", "ventilator").wardId);
    }

    @Test
    void equipmentIsTrimmed() {
        assertEquals("ventilator", new EquipmentFailure("W-01", "  ventilator ").equipment);
    }

    // equipment-alert-service refuses an alert with no ward or no equipment,
    // so ward-service must never send one
    @Test
    void rejectsMissingEquipment() {
        assertThrows(IllegalArgumentException.class, () -> new EquipmentFailure("W-01", null));
    }

    @Test
    void rejectsBlankEquipment() {
        assertThrows(IllegalArgumentException.class, () -> new EquipmentFailure("W-01", "   "));
    }

    @Test
    void rejectsMissingWardId() {
        assertThrows(IllegalArgumentException.class, () -> new EquipmentFailure(null, "ventilator"));
    }

    @Test
    void rejectsBlankWardId() {
        assertThrows(IllegalArgumentException.class, () -> new EquipmentFailure("  ", "ventilator"));
    }
}



