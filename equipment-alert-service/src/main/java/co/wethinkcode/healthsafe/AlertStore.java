package co.wethinkcode.healthsafe;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

// Keeps the alerts received so far, in the order they arrived.
// In memory only: the list is empty again when the service restarts.
public class AlertStore {

    // CopyOnWriteArrayList is safe when the queue listener adds an alert
    // at the same moment a web request reads the list
    private final List<EquipmentAlert> alerts = new CopyOnWriteArrayList<>();

    public void add(EquipmentAlert alert) {
        alerts.add(alert);
    }

    // A copy, so the caller cannot change our list and later alerts do not change theirs
    public List<EquipmentAlert> all() {
        return List.copyOf(alerts);
    }
}
