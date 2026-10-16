package co.wethinkcode.healthsafe;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StaffingBoardTest {

    @Test
    void findsTheUpdateThatWasStored() {
        StaffingBoard board = new StaffingBoard();
        board.put(new StaffingUpdate("W-01", 8, 5));

        assertEquals(5, board.find("W-01").get().doctorsOnCall);
    }

    // Callers may type the id in lower case or with spaces
    @Test
    void findIgnoresCaseAndSpaces() {
        StaffingBoard board = new StaffingBoard();
        board.put(new StaffingUpdate("W-01", 8, 5));

        assertEquals("W-01", board.find("  w-01 ").get().wardId);
    }

    // No update received for this ward yet
    @Test
    void unknownWardHasNoUpdate() {
        assertTrue(new StaffingBoard().find("W-99").isEmpty());
    }

    @Test
    void nullHasNoUpdate() {
        assertTrue(new StaffingBoard().find(null).isEmpty());
    }

    // Only the latest update for a ward is kept
    @Test
    void aNewerUpdateReplacesTheOlderOne() {
        StaffingBoard board = new StaffingBoard();
        board.put(new StaffingUpdate("W-01", 2, 2));
        board.put(new StaffingUpdate("W-01", 8, 5));

        assertEquals(8, board.find("W-01").get().level);
        assertEquals(5, board.find("W-01").get().doctorsOnCall);
    }

    // Updates for different wards do not disturb each other
    @Test
    void wardsAreKeptSeparately() {
        StaffingBoard board = new StaffingBoard();
        board.put(new StaffingUpdate("W-01", 8, 5));
        board.put(new StaffingUpdate("W-02", 2, 2));

        assertEquals(5, board.find("W-01").get().doctorsOnCall);
        assertEquals(2, board.find("W-02").get().doctorsOnCall);
    }
}