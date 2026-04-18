package scms;

import scms.exception.DuplicateEntityException;
import scms.model.*;
import scms.pattern.*;
import scms.service.*;
import scms.ui.ConsoleUI;

import java.util.List;

/**
 * Application entry point.
 * Seeds demo data, wires services, and launches the console UI.
 */
public class Main {

    public static void main(String[] args) {

        // ── Wire services ─────────────────────────────────────────────────
        UserService        userService        = new UserService();
        RoomService        roomService        = new RoomService();
        BookingService     bookingService     = new BookingService(roomService);
        MaintenanceService maintenanceService = new MaintenanceService(roomService);

        CampusFacade facade = new CampusFacade(
                userService, roomService, bookingService, maintenanceService);

        // ── Seed users ────────────────────────────────────────────────────
        try {
            userService.register(new Admin(  "U001", "Alice Admin",    "alice@cardiffmet.ac.uk",  "admin123"));
            userService.register(new Staff(  "U002", "Bob Staff",      "bob@cardiffmet.ac.uk",    "staff123", "Computer Science"));
            userService.register(new Staff(  "U003", "Carol Lecturer", "carol@cardiffmet.ac.uk",  "staff123", "Engineering"));
            userService.register(new Student("U004", "Dave Student",   "dave@cardiffmet.ac.uk",   "student123", "BSc Computer Science"));
            userService.register(new Student("U005", "Eve Student",    "eve@cardiffmet.ac.uk",    "student123", "MSc Data Science"));
        } catch (DuplicateEntityException e) {
            System.err.println("Seed user error: " + e.getMessage());
        }

        // ── Seed rooms (via Factory) ──────────────────────────────────────
        try {
            roomService.addRoom(RoomFactory.create(RoomFactory.RoomType.LECTURE_HALL,  "R101", "Lecture Hall A",     120));
            roomService.addRoom(RoomFactory.create(RoomFactory.RoomType.LECTURE_HALL,  "R102", "Lecture Hall B",      80));
            roomService.addRoom(RoomFactory.create(RoomFactory.RoomType.COMPUTER_LAB,  "R201", "Computer Lab 1",      40));
            roomService.addRoom(RoomFactory.create(RoomFactory.RoomType.COMPUTER_LAB,  "R202", "Computer Lab 2",      30));
            roomService.addRoom(RoomFactory.create(RoomFactory.RoomType.MEETING_ROOM,  "R301", "Meeting Room A",      10));
            roomService.addRoom(RoomFactory.create(RoomFactory.RoomType.SEMINAR_ROOM,  "R401", "Seminar Room 1",      25));
        } catch (DuplicateEntityException e) {
            System.err.println("Seed room error: " + e.getMessage());
        }

        // ── Print quick-start credentials ─────────────────────────────────
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║         DEMO LOGIN CREDENTIALS               ║");
        System.out.println("╠══════════════════════════════════════════════╣");
        System.out.println("║  Admin  : alice@cardiffmet.ac.uk / admin123  ║");
        System.out.println("║  Staff  : bob@cardiffmet.ac.uk   / staff123  ║");
        System.out.println("║  Student: dave@cardiffmet.ac.uk  / student123║");
        System.out.println("╚══════════════════════════════════════════════╝");
        System.out.println();

        // ── Start UI ──────────────────────────────────────────────────────
        new ConsoleUI(facade).start();
    }
}
