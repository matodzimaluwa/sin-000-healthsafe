package co.wethinkcode.healthsafe;

public class EquipmentFailure {
    public final String wardId;
    public final String equipment;

    public EquipmentFailure(String wardId, String equipment) {
        if (wardId == null || wardId.isBlank()) {
            throw new IllegalArgumentException("equipment failure has no wardId");
        }
        if (equipment == null || equipment.isBlank()) {
            throw new IllegalArgumentException("equipment failure has no equipment");
        }
        this.wardId = wardId.trim().toUpperCase();
        this.equipment = equipment.trim();
    }

}
