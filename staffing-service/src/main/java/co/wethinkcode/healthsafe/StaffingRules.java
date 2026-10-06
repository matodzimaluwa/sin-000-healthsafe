package co.wethinkcode.healthsafe;

public class StaffingRules {

    public static final int MIN_LEVEL = 0;
    public static final int MAX_LEVEL = 8;

    private static final int BASE_DOCTORS = 1;
    private static final  int LEVELS_PER_EXTRA_DOCTOR = 2;

    private StaffingRules() {
    }

    public static int doctorsOnCall(int level) {
        if (level < MIN_LEVEL || level > MAX_LEVEL) {
            throw new IllegalArgumentException(
                    "level must be between " + MIN_LEVEL + " and " + MAX_LEVEL + " but was " + level);
        }
        return BASE_DOCTORS + level / LEVELS_PER_EXTRA_DOCTOR;
    }


}
