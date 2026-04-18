package scms.test;

import scms.exception.*;
import scms.model.*;
import scms.pattern.*;
import scms.service.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Self-contained test runner – mirrors the JUnit test plan exactly.
 * Run with:  java -cp out scms.test.TestRunner
 *
 * Each test method is called reflectively via the inner framework so that
 * a single test failure does NOT abort the remaining tests – identical
 * behaviour to JUnit's @Test model.
 *
 * TC09 is deliberately written to FAIL (assertDoesNotThrow on an operation
 * that must throw) to prove the UnauthorizedActionException guard works.
 */
public class TestRunner {

    // ── Tiny assertion helpers ────────────────────────────────────────────
    private static void assertEquals(Object expected, Object actual, String msg) {
        if (!expected.equals(actual))
            throw new AssertionError(msg + "  expected=<" + expected + ">  actual=<" + actual + ">");
    }
    private static void assertNotNull(Object v, String msg) {
        if (v == null) throw new AssertionError(msg + " (was null)");
    }
    private static void assertTrue(boolean cond, String msg) {
        if (!cond) throw new AssertionError(msg);
    }
    private static void assertFalse(boolean cond, String msg) {
        if (cond) throw new AssertionError(msg);
    }
    private static void assertThrows(Class<? extends Exception> expected,
                                     ThrowingRunnable r, String msg) {
        try {
            r.run();
            throw new AssertionError(msg + " – expected " + expected.getSimpleName() + " but nothing was thrown");
        } catch (Exception e) {
            if (!expected.isInstance(e))
                throw new AssertionError(msg + " – expected " + expected.getSimpleName()
                        + " but got " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
    private static void assertDoesNotThrow(ThrowingRunnable r, String msg) {
        try { r.run(); }
        catch (Exception e) {
            throw new AssertionError(msg + " – unexpected exception: "
                    + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    @FunctionalInterface interface ThrowingRunnable { void run() throws Exception; }

    // ── Test tracking ─────────────────────────────────────────────────────
    record TestResult(String name, boolean passed, String detail) {}
    private static final List<TestResult> results = new ArrayList<>();

    private static void run(String name, ThrowingRunnable test) {
        try {
            test.run();
            results.add(new TestResult(name, true, ""));
        } catch (AssertionError | Exception e) {
            results.add(new TestResult(name, false, e.getMessage()));
        }
    }

    // ── Fixtures (recreated per test via helper) ──────────────────────────
    record Fixtures(CampusFacade facade, Admin admin, Staff staff, Student student) {}

    private static final LocalDateTime T1 = LocalDateTime.of(2025, 9, 1,  9, 0);
    private static final LocalDateTime T2 = LocalDateTime.of(2025, 9, 1, 11, 0);
    private static final LocalDateTime T3 = LocalDateTime.of(2025, 9, 1, 10, 0);  // overlaps T1-T2
    private static final LocalDateTime T4 = LocalDateTime.of(2025, 9, 1, 13, 0);

    private static Fixtures setup() throws Exception {
        NotificationService.reset();
        UserService        us = new UserService();
        RoomService        rs = new RoomService();
        BookingService     bs = new BookingService(rs);
        MaintenanceService ms = new MaintenanceService(rs);
        CampusFacade facade   = new CampusFacade(us, rs, bs, ms);

        Admin   admin   = new Admin(  "U001", "Alice Admin",  "alice@test.com", "admin123");
        Staff   staff   = new Staff(  "U002", "Bob Staff",    "bob@test.com",   "staff123", "CS");
        Student student = new Student("U004", "Dave Student", "dave@test.com",  "student123", "BSc CS");

        us.register(admin);
        us.register(staff);
        us.register(student);

        rs.addRoom(RoomFactory.create(RoomFactory.RoomType.LECTURE_HALL, "R101", "Lecture Hall A", 120));
        rs.addRoom(RoomFactory.create(RoomFactory.RoomType.COMPUTER_LAB, "R201", "Computer Lab 1",  40));

        return new Fixtures(facade, admin, staff, student);
    }

    // ════════════════════════════════════════════════════════════════════
    // TESTS
    // ════════════════════════════════════════════════════════════════════

    public static void main(String[] args) {

        // TC01 – Successful booking
        run("TC01 – Staff can book an available room", () -> {
            var f = setup();
            Booking b = f.facade().bookRoom(f.staff(), "R101", T1, T2);
            assertNotNull(b, "Booking should not be null");
            assertEquals("R101",                   b.getRoomId(),  "Room ID");
            assertEquals("U002",                   b.getUserId(),  "User ID");
            assertEquals(Booking.Status.CONFIRMED, b.getStatus(),  "Status");
        });

        // TC02 – Double-booking
        run("TC02 – Double-booking throws RoomAlreadyBookedException", () -> {
            var f = setup();
            f.facade().bookRoom(f.staff(), "R101", T1, T2);
            assertThrows(RoomAlreadyBookedException.class,
                    () -> f.facade().bookRoom(f.student(), "R101", T3, T4),
                    "Overlapping booking must be rejected");
        });

        // TC03 – Non-overlapping bookings
        run("TC03 – Non-overlapping bookings on same room both succeed", () -> {
            var f = setup();
            f.facade().bookRoom(f.staff(),   "R101", T1, T2);
            Booking b2 = f.facade().bookRoom(f.student(), "R101", T4, T4.plusHours(2));
            assertEquals(Booking.Status.CONFIRMED, b2.getStatus(), "Second booking status");
        });

        // TC04 – Cancel own booking
        run("TC04 – Owner can cancel their booking", () -> {
            var f = setup();
            Booking b = f.facade().bookRoom(f.staff(), "R101", T1, T2);
            f.facade().cancelBooking(f.staff(), b.getBookingId());
            assertEquals(Booking.Status.CANCELLED, b.getStatus(), "Booking should be cancelled");
        });

        // TC05 – Booking inactive room
        run("TC05 – Booking a deactivated room throws EntityNotFoundException", () -> {
            var f = setup();
            f.facade().deactivateRoom(f.admin(), "R101");
            assertThrows(EntityNotFoundException.class,
                    () -> f.facade().bookRoom(f.staff(), "R101", T1, T2),
                    "Inactive room must not be bookable");
        });

        // TC06 – Maintenance lifecycle
        run("TC06 – Full maintenance lifecycle PENDING→ASSIGNED→COMPLETED", () -> {
            var f = setup();
            MaintenanceRequest req = f.facade().reportMaintenance(
                    f.staff(), "R101", "Projector not working", MaintenanceRequest.Urgency.HIGH);
            assertEquals(MaintenanceRequest.Status.PENDING, req.getStatus(), "Initially PENDING");

            f.facade().assignMaintenance(f.admin(), req.getRequestId(), "U002");
            assertEquals(MaintenanceRequest.Status.ASSIGNED, req.getStatus(), "After assign: ASSIGNED");
            assertEquals("U002", req.getAssignedTo(), "Assigned to U002");

            f.facade().completeMaintenance(f.admin(), req.getRequestId());
            assertEquals(MaintenanceRequest.Status.COMPLETED, req.getStatus(), "After complete: COMPLETED");
        });

        // TC07 – Booking notification
        run("TC07 – Booking confirmation notification delivered to user", () -> {
            var f = setup();
            f.facade().bookRoom(f.staff(), "R101", T1, T2);
            List<Notification> inbox = NotificationService.getInstance().getInbox("U002");
            assertFalse(inbox.isEmpty(), "Inbox should not be empty");
            assertTrue(inbox.get(0).getMessage().contains("CONFIRMED"), "Message should say CONFIRMED");
        });

        // TC08 – Maintenance notification
        run("TC08 – Maintenance assignment creates notification for reporter", () -> {
            var f = setup();
            MaintenanceRequest req = f.facade().reportMaintenance(
                    f.staff(), "R201", "AC broken", MaintenanceRequest.Urgency.MEDIUM);
            int before = NotificationService.getInstance().getInbox("U002").size();
            f.facade().assignMaintenance(f.admin(), req.getRequestId(), "U002");
            int after = NotificationService.getInstance().getInbox("U002").size();
            assertTrue(after > before, "Reporter should receive an assignment notification");
        });

        // TC09 – INTENTIONALLY FAILING
        run("TC09 (EXPECTED FAIL) – Student adding room should fail; test expects success", () -> {
            var f = setup();
            Room newRoom = RoomFactory.create(RoomFactory.RoomType.SEMINAR_ROOM, "R999", "Unauth Room", 20);
            // Deliberately wrong assertion – proves UnauthorizedActionException is thrown
            assertDoesNotThrow(
                    () -> f.facade().addRoom(f.student(), newRoom),
                    "INTENTIONAL FAIL: student must NOT add a room"
            );
        });

        // TC10 – Duplicate room ID
        run("TC10 – Duplicate Room ID throws DuplicateEntityException", () -> {
            var f = setup();
            Room dup = RoomFactory.create(RoomFactory.RoomType.MEETING_ROOM, "R101", "Dup Hall", 10);
            assertThrows(DuplicateEntityException.class,
                    () -> f.facade().addRoom(f.admin(), dup),
                    "Duplicate Room ID must be rejected");
        });

        // TC11 – Invalid login
        run("TC11 – Invalid login throws EntityNotFoundException", () -> {
            var f = setup();
            assertThrows(EntityNotFoundException.class,
                    () -> f.facade().login("nobody@test.com", "wrong"),
                    "Bad credentials must raise EntityNotFoundException");
        });

        // TC12 – Non-owner cancel
        run("TC12 – Non-owner cancelling another's booking throws UnauthorizedActionException", () -> {
            var f = setup();
            Booking b = f.facade().bookRoom(f.staff(), "R101", T1, T2);
            assertThrows(UnauthorizedActionException.class,
                    () -> f.facade().cancelBooking(f.student(), b.getBookingId()),
                    "Student must not cancel staff's booking");
        });

        // TC13 – Admin can cancel any booking
        run("TC13 – Admin can cancel any user's booking", () -> {
            var f = setup();
            Booking b = f.facade().bookRoom(f.staff(), "R101", T1, T2);
            f.facade().cancelBooking(f.admin(), b.getBookingId());
            assertEquals(Booking.Status.CANCELLED, b.getStatus(), "Admin should cancel booking");
        });

        // TC14 – RoomFactory equipment
        run("TC14 – RoomFactory.COMPUTER_LAB includes 'Computers' in equipment", () -> {
            Room lab = RoomFactory.create(RoomFactory.RoomType.COMPUTER_LAB, "X1", "Lab", 30);
            assertTrue(lab.getEquipment().contains("Computers"), "Should contain 'Computers'");
        });

        // TC15 – Invalid time window
        run("TC15 – Booking with start >= end throws IllegalArgumentException", () -> {
            var f = setup();
            assertThrows(IllegalArgumentException.class,
                    () -> f.facade().bookRoom(f.staff(), "R101", T2, T1),
                    "start >= end must be rejected");
        });

        // ── Print results ─────────────────────────────────────────────────
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                 SCMS – TEST RESULTS                             ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════╣");

        long passed = results.stream().filter(r -> r.passed()).count();
        long failed = results.size() - passed;

        for (TestResult r : results) {
            String icon   = r.passed() ? "✔ PASS" : "✘ FAIL";
            String detail = r.passed() ? "" : "  → " + r.detail();
            System.out.printf("║  %s  %s%n", icon, r.name());
            if (!detail.isEmpty())
                System.out.printf("║         %s%n", detail);
        }

        System.out.println("╠══════════════════════════════════════════════════════════════════╣");
        System.out.printf( "║  Total: %d   Passed: %d   Failed: %d%n", results.size(), passed, failed);
        System.out.println("║  NOTE: TC09 failure is INTENTIONAL (proves exception guard)      ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
    }
}
