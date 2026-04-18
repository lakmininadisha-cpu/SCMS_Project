package scms.test;

import org.junit.jupiter.api.*;
import scms.exception.*;
import scms.model.*;
import scms.pattern.*;
import scms.service.*;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Test Suite for the Smart Campus Management System.
 *
 * Covers:
 *  - Booking logic (success, double-booking, inactive room)
 *  - Maintenance workflows (report, assign, complete)
 *  - Notification delivery (Observer pattern)
 *  - Exception / error-handling cases
 *
 * NOTE: TestT09 is intentionally FAILING to demonstrate robust
 *       exception handling (student cannot access admin operations).
 */
@TestMethodOrder(MethodOrderer.DisplayName.class)
class SCMSTests {

    // ── Shared fixtures ───────────────────────────────────────────────────
    private UserService        userService;
    private RoomService        roomService;
    private BookingService     bookingService;
    private MaintenanceService maintenanceService;
    private CampusFacade       facade;

    private Admin   admin;
    private Staff   staff;
    private Student student;

    private static final LocalDateTime T1 = LocalDateTime.of(2025, 9, 1, 9,  0);
    private static final LocalDateTime T2 = LocalDateTime.of(2025, 9, 1, 11, 0);
    private static final LocalDateTime T3 = LocalDateTime.of(2025, 9, 1, 10, 0);  // overlaps T1-T2
    private static final LocalDateTime T4 = LocalDateTime.of(2025, 9, 1, 13, 0);

    @BeforeEach
    void setup() throws Exception {
        // Reset Singleton so each test gets a clean notification inbox
        NotificationService.reset();

        userService        = new UserService();
        roomService        = new RoomService();
        bookingService     = new BookingService(roomService);
        maintenanceService = new MaintenanceService(roomService);
        facade = new CampusFacade(userService, roomService, bookingService, maintenanceService);

        admin   = new Admin(  "U001", "Alice Admin",   "alice@test.com", "admin123");
        staff   = new Staff(  "U002", "Bob Staff",     "bob@test.com",   "staff123", "CS");
        student = new Student("U004", "Dave Student",  "dave@test.com",  "student123", "BSc CS");

        userService.register(admin);
        userService.register(staff);
        userService.register(student);

        roomService.addRoom(RoomFactory.create(RoomFactory.RoomType.LECTURE_HALL, "R101", "Lecture Hall A", 120));
        roomService.addRoom(RoomFactory.create(RoomFactory.RoomType.COMPUTER_LAB, "R201", "Computer Lab 1",  40));
    }

    // ═════════════════════════════════════════════════════════════════════
    // TC01 – Successful room booking
    // ═════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("TC01 – Staff can book an available room")
    void TC01_successfulBooking() throws Exception {
        Booking b = facade.bookRoom(staff, "R101", T1, T2);

        assertNotNull(b, "Booking should not be null");
        assertEquals("R101",  b.getRoomId());
        assertEquals("U002",  b.getUserId());
        assertEquals(Booking.Status.CONFIRMED, b.getStatus());
    }

    // ═════════════════════════════════════════════════════════════════════
    // TC02 – Double-booking prevention
    // ═════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("TC02 – Double-booking same room/time throws RoomAlreadyBookedException")
    void TC02_doubleBookingThrows() throws Exception {
        facade.bookRoom(staff, "R101", T1, T2);

        assertThrows(RoomAlreadyBookedException.class,
                () -> facade.bookRoom(student, "R101", T3, T4),
                "Expected RoomAlreadyBookedException for overlapping booking");
    }

    // ═════════════════════════════════════════════════════════════════════
    // TC03 – Non-overlapping booking is allowed
    // ═════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("TC03 – Non-overlapping bookings on the same room succeed")
    void TC03_nonOverlappingBookings() throws Exception {
        facade.bookRoom(staff,   "R101", T1, T2);
        Booking b2 = facade.bookRoom(student, "R101", T4,
                T4.plusHours(2));

        assertEquals(Booking.Status.CONFIRMED, b2.getStatus());
    }

    // ═════════════════════════════════════════════════════════════════════
    // TC04 – Cancelling a booking
    // ═════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("TC04 – Owner can cancel their booking")
    void TC04_cancelBooking() throws Exception {
        Booking b = facade.bookRoom(staff, "R101", T1, T2);
        facade.cancelBooking(staff, b.getBookingId());

        assertEquals(Booking.Status.CANCELLED, b.getStatus());
    }

    // ═════════════════════════════════════════════════════════════════════
    // TC05 – Booking an inactive room fails
    // ═════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("TC05 – Booking a deactivated room throws EntityNotFoundException")
    void TC05_bookInactiveRoom() throws Exception {
        facade.deactivateRoom(admin, "R101");

        assertThrows(EntityNotFoundException.class,
                () -> facade.bookRoom(staff, "R101", T1, T2),
                "Cannot book a deactivated room");
    }

    // ═════════════════════════════════════════════════════════════════════
    // TC06 – Maintenance lifecycle: report → assign → complete
    // ═════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("TC06 – Full maintenance lifecycle: PENDING → ASSIGNED → COMPLETED")
    void TC06_maintenanceLifecycle() throws Exception {
        MaintenanceRequest req = facade.reportMaintenance(
                staff, "R101", "Projector not working", MaintenanceRequest.Urgency.HIGH);

        assertEquals(MaintenanceRequest.Status.PENDING, req.getStatus());

        facade.assignMaintenance(admin, req.getRequestId(), "U002");
        assertEquals(MaintenanceRequest.Status.ASSIGNED, req.getStatus());
        assertEquals("U002", req.getAssignedTo());

        facade.completeMaintenance(admin, req.getRequestId());
        assertEquals(MaintenanceRequest.Status.COMPLETED, req.getStatus());
    }

    // ═════════════════════════════════════════════════════════════════════
    // TC07 – Notifications are delivered on booking
    // ═════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("TC07 – Booking confirmation notification is sent to user")
    void TC07_bookingNotification() throws Exception {
        facade.bookRoom(staff, "R101", T1, T2);

        List<Notification> inbox =
                NotificationService.getInstance().getInbox("U002");

        assertFalse(inbox.isEmpty(), "Staff should have received a notification");
        assertTrue(inbox.get(0).getMessage().contains("CONFIRMED"),
                "Notification should mention CONFIRMED");
    }

    // ═════════════════════════════════════════════════════════════════════
    // TC08 – Maintenance status-change notification
    // ═════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("TC08 – Maintenance assignment generates notification for reporter")
    void TC08_maintenanceNotification() throws Exception {
        MaintenanceRequest req = facade.reportMaintenance(
                staff, "R201", "AC broken", MaintenanceRequest.Urgency.MEDIUM);

        long before = NotificationService.getInstance().getInbox("U002").size();

        facade.assignMaintenance(admin, req.getRequestId(), "U002");

        long after = NotificationService.getInstance().getInbox("U002").size();
        assertTrue(after > before, "Reporter should receive an assignment notification");
    }

    // ═════════════════════════════════════════════════════════════════════
    // TC09 – INTENTIONALLY FAILING TEST
    //        A student trying to add a room should throw
    //        UnauthorizedActionException, but this test incorrectly
    //        EXPECTS the operation to SUCCEED → demonstrates exception handling.
    // ═════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("TC09 (EXPECTED FAIL) – Student adding a room should fail but test expects success")
    void TC09_expectedFailure_studentAddRoom() {
        Room newRoom = RoomFactory.create(
                RoomFactory.RoomType.SEMINAR_ROOM, "R999", "Unauthorised Room", 20);

        // This assertion is deliberately wrong:
        // The facade WILL throw UnauthorizedActionException,
        // so assertDoesNotThrow() will fail – proving the guard works.
        assertDoesNotThrow(
                () -> facade.addRoom(student, newRoom),
                "INTENTIONAL FAIL: student should NOT be able to add a room"
        );
    }

    // ═════════════════════════════════════════════════════════════════════
    // TC10 – Duplicate room ID throws DuplicateEntityException
    // ═════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("TC10 – Adding duplicate Room ID throws DuplicateEntityException")
    void TC10_duplicateRoomId() {
        Room duplicate = RoomFactory.create(
                RoomFactory.RoomType.MEETING_ROOM, "R101", "Duplicate Hall", 10);

        assertThrows(DuplicateEntityException.class,
                () -> facade.addRoom(admin, duplicate),
                "Duplicate Room ID should be rejected");
    }

    // ═════════════════════════════════════════════════════════════════════
    // TC11 – Login with bad credentials throws EntityNotFoundException
    // ═════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("TC11 – Invalid login throws EntityNotFoundException")
    void TC11_invalidLogin() {
        assertThrows(EntityNotFoundException.class,
                () -> facade.login("nobody@test.com", "wrong"),
                "Bad credentials should raise EntityNotFoundException");
    }

    // ═════════════════════════════════════════════════════════════════════
    // TC12 – Non-owner cannot cancel another user's booking
    // ═════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("TC12 – Non-owner cancelling another's booking throws UnauthorizedActionException")
    void TC12_unauthorizedCancel() throws Exception {
        Booking b = facade.bookRoom(staff, "R101", T1, T2);

        assertThrows(UnauthorizedActionException.class,
                () -> facade.cancelBooking(student, b.getBookingId()),
                "Student should not cancel a booking they do not own");
    }

    // ═════════════════════════════════════════════════════════════════════
    // TC13 – Admin can cancel any booking
    // ═════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("TC13 – Admin can cancel any user's booking")
    void TC13_adminCancelAnyBooking() throws Exception {
        Booking b = facade.bookRoom(staff, "R101", T1, T2);
        assertDoesNotThrow(() -> facade.cancelBooking(admin, b.getBookingId()));
        assertEquals(Booking.Status.CANCELLED, b.getStatus());
    }

    // ═════════════════════════════════════════════════════════════════════
    // TC14 – RoomFactory produces correct equipment lists
    // ═════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("TC14 – RoomFactory.COMPUTER_LAB room includes 'Computers' in equipment")
    void TC14_roomFactoryEquipment() {
        Room lab = RoomFactory.create(RoomFactory.RoomType.COMPUTER_LAB, "X1", "Lab", 30);
        assertTrue(lab.getEquipment().contains("Computers"),
                "Computer Lab should include 'Computers' in equipment");
    }

    // ═════════════════════════════════════════════════════════════════════
    // TC15 – Invalid booking (start >= end) throws IllegalArgumentException
    // ═════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("TC15 – Booking with start >= end throws IllegalArgumentException")
    void TC15_invalidTimeWindow() {
        assertThrows(IllegalArgumentException.class,
                () -> facade.bookRoom(staff, "R101", T2, T1),
                "Start time after end time should be rejected");
    }
}
