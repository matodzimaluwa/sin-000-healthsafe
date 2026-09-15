package co.wethinkcode.healthsafe;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WardRecordTest {

    @Test
    void shouldCreateWardRecord() {

        WardRecord ward = new WardRecord(
                "W-01",
                "East Wing",
                "Cardiology",
                3,
                "No notes"

        );

        assertEquals("W-01", ward.wardId);
        assertEquals("East Wing", ward.wing);
        assertEquals("Cardiology", ward.department);
        assertEquals(3, ward.bedsAvailable);
        assertEquals("No notes", ward.notes);
    }
}
