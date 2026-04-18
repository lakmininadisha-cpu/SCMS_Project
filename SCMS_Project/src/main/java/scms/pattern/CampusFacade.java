package scms.pattern;

import scms.exception.*;
import scms.model.*;
import scms.service.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * STRUCTURAL PATTERN – Facade
 *
 * Provides a single, simplified entry point that coordinates the underlying
 * services (UserService, RoomService, BookingService, MaintenanceService).
 * The console UI talks only to this Facade.
 */
public class CampusFacade {

    private final UserService        userService;
    private final RoomService        roomService;
    private final BookingService     bookingService;
    private final MaintenanceService maintenanceService;

    public CampusFacade(UserService us, RoomService rs,
                        BookingService bs, MaintenanceService ms) {
        this.userService        = us;
        this.roomService        = rs;
        this.bookingService     = bs;
        this.maintenanceService = ms;
    }

    // ── Auth ──────────────────────────────────────────────────────────────
    public User login(String email, String password) throws EntityNotFoundException {
        return userService.authenticate(email, password);
    }

    // ── Room Management ───────────────────────────────────────────────────
    public void addRoom(User caller, Room room)
            throws UnauthorizedActionException, DuplicateEntityException {
        requireAdmin(caller);
        roomService.addRoom(room);
    }

    public void deactivateRoom(User caller, String roomId)
            throws UnauthorizedActionException, EntityNotFoundException {
        requireAdmin(caller);
        roomService.deactivateRoom(roomId);
    }

    public List<Room> getAvailableRooms() {
        return roomService.getActiveRooms();
    }

    public List<Room> getAllRooms(User caller)
            throws UnauthorizedActionException {
        requireAdmin(caller);
        return roomService.getAllRooms();
    }

    // ── Booking ───────────────────────────────────────────────────────────
    public Booking bookRoom(User caller, String roomId,
                            LocalDateTime start, LocalDateTime end)
            throws EntityNotFoundException, RoomAlreadyBookedException {
        return bookingService.book(caller, roomId, start, end);
    }

    public void cancelBooking(User caller, String bookingId)
            throws EntityNotFoundException, UnauthorizedActionException {
        bookingService.cancel(caller, bookingId);
    }

    public List<Booking> getUserBookings(String userId) {
        return bookingService.getBookingsForUser(userId);
    }

    // ── Maintenance ───────────────────────────────────────────────────────
    public MaintenanceRequest reportMaintenance(User caller, String roomId,
                                                String description,
                                                MaintenanceRequest.Urgency urgency)
            throws EntityNotFoundException {
        return maintenanceService.report(caller, roomId, description, urgency);
    }

    public void assignMaintenance(User caller, String requestId, String assigneeUserId)
            throws UnauthorizedActionException, EntityNotFoundException {
        requireAdmin(caller);
        maintenanceService.assign(requestId, assigneeUserId);
    }

    public void completeMaintenance(User caller, String requestId)
            throws UnauthorizedActionException, EntityNotFoundException {
        requireAdmin(caller);
        maintenanceService.complete(requestId);
    }

    public List<MaintenanceRequest> getAllMaintenanceRequests(User caller)
            throws UnauthorizedActionException {
        requireAdmin(caller);
        return maintenanceService.getAllRequests();
    }

    public List<MaintenanceRequest> getUserMaintenanceRequests(String userId) {
        return maintenanceService.getRequestsByUser(userId);
    }

    // ── Notifications ─────────────────────────────────────────────────────
    public List<scms.model.Notification> getNotifications(String userId) {
        return NotificationService.getInstance().getInbox(userId);
    }

    // ── Analytics (Admin) ─────────────────────────────────────────────────
    public void printAnalytics(User caller) throws UnauthorizedActionException {
        requireAdmin(caller);
        System.out.println("\n======= ANALYTICS DASHBOARD =======");
        bookingService.printBookingStats();
        maintenanceService.printMaintenanceStats();
        System.out.println("====================================");
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private void requireAdmin(User caller) throws UnauthorizedActionException {
        if (caller.getRole() != UserRole.ADMIN) {
            throw new UnauthorizedActionException(
                    "Action restricted to administrators. User '" + caller.getName() + "' is not an admin.");
        }
    }

    // Expose underlying services for CLI use (user registration, etc.)
    public UserService getUserService()               { return userService; }
    public RoomService getRoomService()               { return roomService; }
    public BookingService getBookingService()          { return bookingService; }
    public MaintenanceService getMaintenanceService() { return maintenanceService; }
}
