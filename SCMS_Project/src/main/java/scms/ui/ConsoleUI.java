package scms.ui;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;
import scms.exception.*;
import scms.model.*;
import scms.pattern.*;

/**
 * Console-based menu application.
 * Demonstrates the Facade pattern – the UI only calls CampusFacade methods.
 */
public class ConsoleUI implements scms.pattern.NotificationObserver {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final CampusFacade facade;
    private final Scanner      scanner;
    private       User         currentUser;

    public ConsoleUI(CampusFacade facade) {
        this.facade  = facade;
        this.scanner = new Scanner(System.in);
    }

    // ── Entry point ───────────────────────────────────────────────────────
    public void start() {
        banner();
        while (true) {
            if (currentUser == null) {
                showAuthMenu();
            } else {
                switch (currentUser.getRole()) {
                    case ADMIN   -> showAdminMenu();
                    case STAFF   -> showStaffMenu();
                    case STUDENT -> showStudentMenu();
                }
            }
        }
    }

    // ── NotificationObserver callback ─────────────────────────────────────
    @Override
    public void onNotification(Notification n) {
        System.out.println("\n  🔔 NOTIFICATION: " + n.getMessage());
    }

    // ── Auth Menu ─────────────────────────────────────────────────────────
    private void showAuthMenu() {
        System.out.println("""
                
                ╔══════════════════════════════╗
                ║  Smart Campus Mgmt System    ║
                ╠══════════════════════════════╣
                ║  1. Login                    ║
                ║  0. Exit                     ║
                ╚══════════════════════════════╝""");
        int choice = readInt("Choice: ");
        switch (choice) {
            case 1 -> doLogin();
            case 0 -> { System.out.println("Goodbye!"); System.exit(0); }
            default -> System.out.println("Invalid option.");
        }
    }

    private void doLogin() {
        String email    = prompt("Email: ");
        String password = prompt("Password: ");
        try {
            currentUser = facade.login(email, password);
            // Subscribe this UI instance to live notifications for this user
            NotificationService.getInstance().subscribe(currentUser.getUserId(), this);
            System.out.println("Welcome, " + currentUser.getName()
                    + " [" + currentUser.getRole() + "]");
        } catch (EntityNotFoundException e) {
            System.out.println("Login failed: " + e.getMessage());
        }
    }

    // ── Admin Menu ────────────────────────────────────────────────────────
    private void showAdminMenu() {
        System.out.println("""
                
                ── Admin Menu ──────────────────
                 1. Add Room
                 2. Deactivate Room
                 3. View All Rooms
                 4. Assign Maintenance Request
                 5. Complete Maintenance Request
                 6. View All Maintenance Requests
                 7. View Analytics
                 8. View All Bookings
                 9. View My Notifications
                10. Logout
                """);
        int choice = readInt("Choice: ");
        try {
            switch (choice) {
                case  1 -> adminAddRoom();
                case  2 -> adminDeactivateRoom();
                case  3 -> printList(facade.getAllRooms(currentUser));
                case  4 -> adminAssignMaintenance();
                case  5 -> adminCompleteMaintenance();
                case  6 -> printList(facade.getAllMaintenanceRequests(currentUser));
                case  7 -> facade.printAnalytics(currentUser);
                case  8 -> printList(facade.getBookingService().getAllBookings());
                case  9 -> viewNotifications();
                case 10 -> logout();
                default -> System.out.println("Invalid option.");
            }
        } catch (UnauthorizedActionException | EntityNotFoundException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void adminAddRoom() throws UnauthorizedActionException {
        String id   = prompt("Room ID: ");
        String name = prompt("Room Name: ");
        int cap     = readInt("Capacity: ");

        System.out.println("Room types: 1=LECTURE_HALL  2=COMPUTER_LAB  3=MEETING_ROOM  4=SEMINAR_ROOM");
        int t = readInt("Type: ");
        RoomFactory.RoomType type = switch (t) {
            case 1 -> RoomFactory.RoomType.LECTURE_HALL;
            case 2 -> RoomFactory.RoomType.COMPUTER_LAB;
            case 3 -> RoomFactory.RoomType.MEETING_ROOM;
            default -> RoomFactory.RoomType.SEMINAR_ROOM;
        };

        Room room = RoomFactory.create(type, id, name, cap);
        try {
            facade.addRoom(currentUser, room);
            System.out.println("Room added: " + room);
        } catch (DuplicateEntityException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void adminDeactivateRoom() throws UnauthorizedActionException, EntityNotFoundException {
        String id = prompt("Room ID to deactivate: ");
        facade.deactivateRoom(currentUser, id);
        System.out.println("Room " + id + " deactivated.");
    }

    private void adminAssignMaintenance() throws UnauthorizedActionException, EntityNotFoundException {
        String reqId    = prompt("Maintenance Request ID: ");
        String assignee = prompt("Assignee User ID: ");
        facade.assignMaintenance(currentUser, reqId, assignee);
        System.out.println("Assigned.");
    }

    private void adminCompleteMaintenance() throws UnauthorizedActionException, EntityNotFoundException {
        String reqId = prompt("Maintenance Request ID: ");
        facade.completeMaintenance(currentUser, reqId);
        System.out.println("Marked as completed.");
    }

    // ── Staff Menu ────────────────────────────────────────────────────────
    private void showStaffMenu() {
        System.out.println("""
                
                ── Staff Menu ──────────────────
                1. View Available Rooms
                2. Book a Room
                3. Cancel a Booking
                4. My Bookings
                5. Report Maintenance Issue
                6. My Maintenance Requests
                7. My Notifications
                8. Logout
                """);
        int choice = readInt("Choice: ");
        try {
            switch (choice) {
                case 1 -> printList(facade.getAvailableRooms());
                case 2 -> bookRoom();
                case 3 -> cancelBooking();
                case 4 -> printList(facade.getUserBookings(currentUser.getUserId()));
                case 5 -> reportMaintenance();
                case 6 -> printList(facade.getUserMaintenanceRequests(currentUser.getUserId()));
                case 7 -> viewNotifications();
                case 8 -> logout();
                default -> System.out.println("Invalid option.");
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // ── Student Menu ──────────────────────────────────────────────────────
    private void showStudentMenu() {
        System.out.println("""
                
                ── Student Menu ────────────────
                1. View Available Rooms
                2. Request Room Booking
                3. Cancel a Booking
                4. My Bookings
                5. My Notifications
                6. Logout
                """);
        int choice = readInt("Choice: ");
        try {
            switch (choice) {
                case 1 -> printList(facade.getAvailableRooms());
                case 2 -> bookRoom();
                case 3 -> cancelBooking();
                case 4 -> printList(facade.getUserBookings(currentUser.getUserId()));
                case 5 -> viewNotifications();
                case 6 -> logout();
                default -> System.out.println("Invalid option.");
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // ── Shared Actions ────────────────────────────────────────────────────
    private void bookRoom() throws EntityNotFoundException, RoomAlreadyBookedException {
        printList(facade.getAvailableRooms());
        String roomId = prompt("Room ID: ");
        LocalDateTime start = readDateTime("Start (yyyy-MM-dd HH:mm): ");
        LocalDateTime end   = readDateTime("End   (yyyy-MM-dd HH:mm): ");
        Booking b = facade.bookRoom(currentUser, roomId, start, end);
        System.out.println("Success! " + b);
    }

    private void cancelBooking() throws EntityNotFoundException, UnauthorizedActionException {
        String bookingId = prompt("Booking ID to cancel: ");
        facade.cancelBooking(currentUser, bookingId);
        System.out.println("Booking cancelled.");
    }

    private void reportMaintenance() throws EntityNotFoundException {
        String roomId = prompt("Room ID: ");
        String desc   = prompt("Description: ");
        System.out.println("Urgency: 1=LOW  2=MEDIUM  3=HIGH");
        int u = readInt("Urgency: ");
        MaintenanceRequest.Urgency urgency = switch (u) {
            case 3 -> MaintenanceRequest.Urgency.HIGH;
            case 2 -> MaintenanceRequest.Urgency.MEDIUM;
            default -> MaintenanceRequest.Urgency.LOW;
        };
        MaintenanceRequest req = facade.reportMaintenance(currentUser, roomId, desc, urgency);
        System.out.println("Submitted: " + req);
    }

    private void viewNotifications() {
        List<Notification> notes = facade.getNotifications(currentUser.getUserId());
        if (notes.isEmpty()) {
            System.out.println("No notifications.");
        } else {
            System.out.println("── Notifications ──────────────");
            notes.forEach(n -> { System.out.println("  " + n); n.markRead(); });
        }
    }

    private void logout() {
        NotificationService.getInstance().unsubscribe(currentUser.getUserId(), this);
        System.out.println("Logged out: " + currentUser.getName());
        currentUser = null;
    }

    // ── Utility ───────────────────────────────────────────────────────────
    private void banner() {
        System.out.println("""
                ╔══════════════════════════════════════════════╗
                ║  Cardiff Metropolitan University             ║
                ║  Smart Campus Management System (SCMS)       ║
                ╚══════════════════════════════════════════════╝
                """);
    }

    private String prompt(String msg) {
        System.out.print(msg);
        return scanner.nextLine().trim();
    }

    private int readInt(String msg) {
        System.out.print(msg);
        try {
            int v = Integer.parseInt(scanner.nextLine().trim());
            return v;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private LocalDateTime readDateTime(String msg) {
        while (true) {
            System.out.print(msg);
            try {
                return LocalDateTime.parse(scanner.nextLine().trim(), FMT);
            } catch (DateTimeParseException e) {
                System.out.println("  Invalid format. Use yyyy-MM-dd HH:mm");
            }
        }
    }

    private <T> void printList(List<T> items) {
        if (items.isEmpty()) {
            System.out.println("  (none)");
        } else {
            items.forEach(i -> System.out.println("  " + i));
        }
    }
}
