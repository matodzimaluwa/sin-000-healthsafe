package co.wethinkcode.healthsafe;

import java.util.ArrayList;
import java.util.List;

// One staffing update that staffing-service broadcasts on the staffing-events-topic:
// how many doctors are on call for a ward at the current Emergency Status.
public class StaffingEvent {
    public final String wardId;
    public final int level;
    public final int doctorsOnCall;

    public StaffingEvent(String wardId, int level, int doctorsOnCall) {
        if (wardId == null || wardId.isBlank()) {
            throw new IllegalArgumentException("staffing event has no wardId");
        }
        this.wardId = wardId.trim().toUpperCase();
        this.level = level;
        this.doctorsOnCall = doctorsOnCall;
    }

    // One event per ward, all at the same Emergency Status.
    // StaffingRules refuses a level outside 0-8 (IllegalArgumentException).
    public static List<StaffingEvent> forWards(List<String> wardIds, int level) {
        int doctors = StaffingRules.doctorsOnCall(level);
        List<StaffingEvent> events = new ArrayList<>();
        for (String wardId : wardIds) {
            events.add(new StaffingEvent(wardId, level, doctors));
        }
        return events;
    }
}
