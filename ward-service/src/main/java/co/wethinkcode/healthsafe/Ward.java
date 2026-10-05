package co.wethinkcode.healthsafe;

// One cleaned ward record, as sent by ingestion-service at GET /wards.
// Jackson needs the public fields and the empty constructor to fill this in from JSON.
public class Ward {
    public String wardId;
    public String wing;
    public String department;
    public Integer bedsAvailable;
    public String notes;

    public Ward() {
    }
}