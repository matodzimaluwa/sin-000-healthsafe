package co.wethinkcode.healthsafe;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class StaffingEventTest {
    // One event for every ward, in the order the wards were given
    @Test
    void oneEventPerWardInOrder() {
        List<StaffingEvent> events = StaffingEvent.forWards(List.of("W-01", "W-02", "W-03"), 4);

        assertEquals(3, events.size());
        assertEquals("W-01", events.get(0).wardId);
        assertEquals("W-02", events.get(1).wardId);
        assertEquals("W-03", events.get(2).wardId);
    }

    // Every event carries the Emergency Status and the doctors the staffing rule gives for it
    @Test
    void everyEventCarriesTheLevelAndTheDoctorsFromTheRule() {
        List<StaffingEvent> events = StaffingEvent.forWards(List.of("W-01", "W-02"), 8);

        for (StaffingEvent event : events) {
            assertEquals(8, event.level);
            assertEquals(StaffingRules.doctorsOnCall(8), event.doctorsOnCall);
        }
        assertEquals(5, events.get(0).doctorsOnCall);
    }

    // Ward ids are sent the way the other services write them: trimmed, upper case
    @Test
    void wardIdsAreTrimmedAndUppercased() {
        assertEquals("W-01", StaffingEvent.forWards(List.of("  w-01 "), 0).get(0).wardId);
    }

    @Test
    void noWardsMeansNoEvents() {
        assertTrue(StaffingEvent.forWards(List.of(), 3).isEmpty());
    }

    // The Emergency Status only exists from 0 to 8
    @Test
    void rejectsLevelOutsideZeroToEight() {
        assertThrows(IllegalArgumentException.class, () -> StaffingEvent.forWards(List.of("W-01"), 9));
        assertThrows(IllegalArgumentException.class, () -> StaffingEvent.forWards(List.of("W-01"), -1));
    }

    // An event for no ward is useless, so it is refused
    @Test
    void rejectsBlankWardId() {
        assertThrows(IllegalArgumentException.class, () -> StaffingEvent.forWards(List.of("  "), 0));
    }

}
