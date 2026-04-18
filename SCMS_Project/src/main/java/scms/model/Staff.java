package scms.model;

/** Staff member – can book rooms and report maintenance. */
public class Staff extends User {
    private String department;

    public Staff(String userId, String name, String email, String password, String department) {
        super(userId, name, email, password, UserRole.STAFF);
        this.department = department;
    }

    public String getDepartment() { return department; }

    @Override
    public String getPermissionSummary() {
        return "Book rooms, submit maintenance requests, view notifications.";
    }
}
