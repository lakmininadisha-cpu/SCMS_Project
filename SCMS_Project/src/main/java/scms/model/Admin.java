package scms.model;

/** Administrator – full system access. */
public class Admin extends User {
    public Admin(String userId, String name, String email, String password) {
        super(userId, name, email, password, UserRole.ADMIN);
    }

    @Override
    public String getPermissionSummary() {
        return "Manage rooms, users, maintenance requests, and view analytics.";
    }
}
