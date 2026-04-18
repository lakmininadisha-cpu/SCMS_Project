package scms.exception;

/** Thrown when a room booking conflicts with an existing booking. */
public class RoomAlreadyBookedException extends Exception {
    public RoomAlreadyBookedException(String roomId, Object start, Object end) {
        super(String.format("Room '%s' is already booked between %s and %s.", roomId, start, end));
    }
}
