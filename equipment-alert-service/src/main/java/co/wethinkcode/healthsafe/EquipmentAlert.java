package co.wethinkcode.healthsafe;

// An alert without a ward or without equipment is refused (IllegalArgumentException)
public class EquipmentAlert {
    public final String wardId;
    public final String equipment;
    public final String receivedAt;

    public EquipmentAlert(String wardId, String equipment, String receivedAt) {
        if (wardId == null || wardId.isBlank()) {
            throw new IllegalArgumentException("alert has no wardId");
        }
        if (equipment == null || equipment.isBlank()) {
            throw new IllegalArgumentException("alert has no equipment");
        }
        this.wardId = wardId.trim().toUpperCase();
        this.equipment = equipment.trim();
        this.receivedAt = receivedAt;
    }
}
