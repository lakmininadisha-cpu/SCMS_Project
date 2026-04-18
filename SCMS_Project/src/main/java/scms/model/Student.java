package scms.model;

/** Student – can view rooms and request bookings. */
public class Student extends User {
    private String course;

    public Student(String userId, String name, String email, String password, String course) {
        super(userId, name, email, password, UserRole.STUDENT);
        this.course = course;
    }

    public String getCourse() { return course; }

    @Override
    public String getPermissionSummary() {
        return "View available rooms, request bookings, receive announcements.";
    }
}
