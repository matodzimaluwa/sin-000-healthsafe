package co.wethinkcode.healthsafe;

public class Schedule {
    public final String wardId;
    public final String department;
    public final int level;
    public final int doctorsOnCall;

    public Schedule(String wardId, String department, int level, int doctorsOnCall) {
        this.wardId = wardId;
        this.department = department;
        this.level = level;
        this.doctorsOnCall = doctorsOnCall;
    }

}
