package co.wethinkcode.healthsafe;

public class WardRecord {
    public String wardId;
    public String wing;
    public String department;
    public Integer bedsAvailable;

    public WardRecord(
            String wardId,
            String wing,
            String department,
            Integer bedsAvailable) {

        this.wardId = wardId;
        this.wing = wing;
        this.department = department;
        this.bedsAvailable = bedsAvailable;
    }
}
