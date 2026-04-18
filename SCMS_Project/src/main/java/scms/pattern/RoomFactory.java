package scms.pattern;

import scms.model.Room;

import java.util.List;

/**
 * CREATIONAL PATTERN – Factory Method
 * Provides convenient factory methods for common room types,
 * avoiding the caller needing to know which equipment set belongs to each type.
 */
public class RoomFactory {

    public enum RoomType { LECTURE_HALL, COMPUTER_LAB, MEETING_ROOM, SEMINAR_ROOM }

    public static Room create(RoomType type, String roomId, String name, int capacity) {
        return switch (type) {
            case LECTURE_HALL   -> new Room(roomId, name, capacity,
                    List.of("Projector", "Microphone", "Whiteboard", "AV System"));
            case COMPUTER_LAB   -> new Room(roomId, name, capacity,
                    List.of("Computers", "Projector", "High-Speed Internet", "Printer"));
            case MEETING_ROOM   -> new Room(roomId, name, capacity,
                    List.of("Whiteboard", "Video Conferencing", "Display Screen"));
            case SEMINAR_ROOM   -> new Room(roomId, name, capacity,
                    List.of("Projector", "Whiteboard", "Seating Arrangement"));
        };
    }
}
