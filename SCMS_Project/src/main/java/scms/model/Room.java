package scms.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a campus room.
 */
public class Room {
    private final String roomId;
    private String name;
    private int capacity;
    private List<String> equipment;
    private boolean active;

    public Room(String roomId, String name, int capacity, List<String> equipment) {
        if (roomId == null || roomId.isBlank()) throw new IllegalArgumentException("Room ID cannot be blank.");
        if (capacity <= 0)                      throw new IllegalArgumentException("Capacity must be positive.");
        this.roomId    = roomId;
        this.name      = name;
        this.capacity  = capacity;
        this.equipment = new ArrayList<>(equipment);
        this.active    = true;
    }

    public String getRoomId()               { return roomId; }
    public String getName()                 { return name; }
    public int    getCapacity()             { return capacity; }
    public List<String> getEquipment()      { return Collections.unmodifiableList(equipment); }
    public boolean isActive()               { return active; }

    public void setName(String name)        { this.name = name; }
    public void setCapacity(int capacity)   { this.capacity = capacity; }
    public void deactivate()                { this.active = false; }
    public void activate()                  { this.active = true; }

    @Override
    public String toString() {
        return String.format("Room[%s] %s | Capacity: %d | Equipment: %s | Active: %s",
                roomId, name, capacity, equipment, active);
    }
}
