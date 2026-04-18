package scms.service;

import scms.exception.EntityNotFoundException;
import scms.exception.RoomAlreadyBookedException;
import scms.exception.UnauthorizedActionException;
import scms.model.Booking;
import scms.model.Room;
import scms.model.User;
import scms.model.UserRole;
import scms.pattern.NotificationService;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles all room booking logic including double-booking prevention.
 */
public class BookingService {

    private final Map<String, Booking> bookings = new LinkedHashMap<>();
    private final RoomService roomService;

    public BookingService(RoomService roomService) {
        this.roomService = roomService;
    }

    public Booking book(User user, String roomId,
                        LocalDateTime start, LocalDateTime end)
            throws EntityNotFoundException, RoomAlreadyBookedException {

        // Validate room exists and is active
        Room room = roomService.findById(roomId);
        if (!room.isActive()) {
            throw new EntityNotFoundException("Room '" + roomId + "' is not currently active.");
        }

        // Validate time window
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("Start time must be before end time.");
        }

        // Check for double-booking
        boolean conflict = bookings.values().stream()
                .filter(b -> b.getRoomId().equals(roomId))
                .anyMatch(b -> b.overlaps(start, end));

        if (conflict) {
            throw new RoomAlreadyBookedException(roomId, start, end);
        }

        Booking booking = new Booking(roomId, user.getUserId(), start, end);
        bookings.put(booking.getBookingId(), booking);

        // Notify the user (Observer pattern via NotificationService)
        NotificationService.getInstance().notify(
                user.getUserId(),
                "Booking CONFIRMED: " + booking.getBookingId()
                        + " | Room: " + room.getName()
                        + " | " + start + " -> " + end
        );

        return booking;
    }

    public void cancel(User caller, String bookingId)
            throws EntityNotFoundException, UnauthorizedActionException {

        Booking booking = findById(bookingId);

        // Only the booking owner or an admin may cancel
        boolean isOwner = booking.getUserId().equals(caller.getUserId());
        boolean isAdmin = caller.getRole() == UserRole.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new UnauthorizedActionException(
                    "Only the booking owner or an administrator can cancel booking " + bookingId);
        }

        booking.cancel();

        NotificationService.getInstance().notify(
                booking.getUserId(),
                "Booking CANCELLED: " + bookingId
        );
    }

    public Booking findById(String bookingId) throws EntityNotFoundException {
        Booking b = bookings.get(bookingId);
        if (b == null) throw new EntityNotFoundException("Booking not found: " + bookingId);
        return b;
    }

    public List<Booking> getBookingsForUser(String userId) {
        return bookings.values().stream()
                .filter(b -> b.getUserId().equals(userId))
                .collect(Collectors.toList());
    }

    public List<Booking> getAllBookings() {
        return new ArrayList<>(bookings.values());
    }

    /** Analytics helper */
    public void printBookingStats() {
        Map<String, Long> countByRoom = bookings.values().stream()
                .filter(b -> b.getStatus() == Booking.Status.CONFIRMED)
                .collect(Collectors.groupingBy(Booking::getRoomId, Collectors.counting()));

        System.out.println("-- Booking Stats --");
        countByRoom.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(e -> System.out.printf("  Room %-8s : %d booking(s)%n", e.getKey(), e.getValue()));

        if (countByRoom.isEmpty()) System.out.println("  No confirmed bookings yet.");
    }
}
