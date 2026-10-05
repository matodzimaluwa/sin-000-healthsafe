package co.wethinkcode.healthsafe;

public class AlertLevel {
    public static final int MIN_LEVEL = 0;
    public static final int MAX_LEVEL = 8;

    private int level = MIN_LEVEL;

    // synchronized: the web server can handle two requests at the same moment
    public synchronized int get() {
        return level;
    }

    // Rejects anything outside 0-8 and keeps the old level.
    public synchronized void set(int newLevel) {
        if (newLevel < MIN_LEVEL || newLevel > MAX_LEVEL) {
            throw new IllegalArgumentException(
                    "level must be between " + MIN_LEVEL + " and " + MAX_LEVEL + " but was " + newLevel);
        }
        level = newLevel;
    }
}

