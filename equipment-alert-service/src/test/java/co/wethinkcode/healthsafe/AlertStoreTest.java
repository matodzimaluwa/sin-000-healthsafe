package co.wethinkcode.healthsafe;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AlertStoreTest {
    // Builds an alert quickly, so each test stays short
    private static EquipmentAlert alert(String ward, String equipment) {
        return new EquipmentAlert(ward, equipment, "2026-10-06T10:00:00Z");
    }

    @Test
    void startsEmpty() {
        assertTrue(new AlertStore().all().isEmpty());
    }
    // Alerts come back in the order they arrived
    @Test
    void keepsAlertsInArrivalOrder() {
        AlertStore store = new AlertStore();
        store.add(alert("W-01", "ventilator"));
        store.add(alert("W-02", "monitor"));

        List<EquipmentAlert> all = store.all();
        assertEquals(2, all.size());
        assertEquals("ventilator", all.get(0).equipment);
        assertEquals("monitor", all.get(1).equipment);
    }

    // The list we hand out is a copy: later alerts must not change a list already given out
    @Test
    void returnedListIsACopy() {
        AlertStore store = new AlertStore();
        store.add(alert("W-01", "ventilator"));
        List<EquipmentAlert> before = store.all();

        store.add(alert("W-02", "monitor"));

        assertEquals(1, before.size());
        assertEquals(2, store.all().size());
    }
}
