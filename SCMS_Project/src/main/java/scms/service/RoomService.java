package scms.service;

import scms.exception.DuplicateEntityException;
import scms.exception.EntityNotFoundException;
import scms.model.Room;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages room inventory in memory.
 */
public class RoomService {

    private final Map<String, Room> rooms = new LinkedHashMap<>();

    public void addRoom(Room room) throws DuplicateEntityException {
        if (rooms.containsKey(room.getRoomId())) {
            throw new DuplicateEntityException("Room ID already exists: " + room.getRoomId());
        }
        rooms.put(room.getRoomId(), room);
    }

    public Room findById(String roomId) throws EntityNotFoundException {
        Room r = rooms.get(roomId);
        if (r == null) throw new EntityNotFoundException("Room not found: " + roomId);
        return r;
    }

    public void deactivateRoom(String roomId) throws EntityNotFoundException {
        findById(roomId).deactivate();
    }

    public void activateRoom(String roomId) throws EntityNotFoundException {
        findById(roomId).activate();
    }

    public void updateRoom(String roomId, String newName, int newCapacity)
            throws EntityNotFoundException {
        Room r = findById(roomId);
        r.setName(newName);
        r.setCapacity(newCapacity);
    }

    public List<Room> getActiveRooms() {
        return rooms.values().stream()
                .filter(Room::isActive)
                .collect(Collectors.toList());
    }

    public List<Room> getAllRooms() {
        return new ArrayList<>(rooms.values());
    }
}
