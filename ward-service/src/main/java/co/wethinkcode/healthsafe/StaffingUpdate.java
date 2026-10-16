package co.wethinkcode.healthsafe;

// One staffing update received from the staffing-events-topic:
// doctors that are on call for a ward at a given Emergency Status.
// A nonsense update is refused (IllegalArgumentException) instead of being stored.
public class StaffingUpdate {
    public final String wardId;
    public final int level;
    public final int doctorsOnCall;

    public StaffingUpdate(String wardId, int level, int doctorsOnCall) {
        if (wardId == null || wardId.isBlank()) {
            throw new IllegalArgumentException("staffing update has no wardId");
        }
        if (level < 0 || level > 8) {
            throw new IllegalArgumentException("staffing update has a level outside 0-8: " + level);
        }
        if (doctorsOnCall < 0) {
            throw new IllegalArgumentException("staffing update has a negative number of doctors: " + doctorsOnCall);
        }
        this.wardId = wardId.trim().toUpperCase();
        this.level = level;
        this.doctorsOnCall = doctorsOnCall;
    }
}