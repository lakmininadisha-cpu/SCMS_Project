package scms.model;

import java.time.LocalDateTime;

/**
 * Represents a room booking.
 */
public class Booking {
    public enum Status { CONFIRMED, CANCELLED }

    private static int counter = 1;

    private final String bookingId;
    private final String roomId;
    private final String userId;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private Status status;

    public Booking(String roomId, String userId, LocalDateTime startTime, LocalDateTime endTime) {
        this.bookingId = "BK" + String.format("%04d", counter++);
        this.roomId    = roomId;
        this.userId    = userId;
        this.startTime = startTime;
        this.endTime   = endTime;
        this.status    = Status.CONFIRMED;
    }

    public String getBookingId()        { return bookingId; }
    public String getRoomId()           { return roomId; }
    public String getUserId()           { return userId; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime()   { return endTime; }
    public Status getStatus()           { return status; }

    public void cancel() { this.status = Status.CANCELLED; }

    /** True if this booking's time window overlaps with the given window. */
    public boolean overlaps(LocalDateTime start, LocalDateTime end) {
        return status == Status.CONFIRMED
                && startTime.isBefore(end)
                && endTime.isAfter(start);
    }

    @Override
    public String toString() {
        return String.format("Booking[%s] Room:%s User:%s %s -> %s [%s]",
                bookingId, roomId, userId, startTime, endTime, status);
    }
}
