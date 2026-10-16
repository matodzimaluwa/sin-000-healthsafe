package co.wethinkcode.healthsafe;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

// Keeps the latest staffing update for each ward.
// In memory only, it is empty again when the service restarts.
public class StaffingBoard {

    // ConcurrentHashMap is safe when the topic listener stores an update
    // at the same moment a web request reads one
    private final Map<String, StaffingUpdate> latest = new ConcurrentHashMap<>();

    // A newer update for the same ward replaces the older one
    public void put(StaffingUpdate update) {
        latest.put(update.wardId, update);
    }

    // Ignores capital letters and surrounding spaces
    // Empty if no update has been received for that ward yet.
    public Optional<StaffingUpdate> find(String wardId) {
        if (wardId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(latest.get(wardId.trim().toUpperCase()));
    }
}