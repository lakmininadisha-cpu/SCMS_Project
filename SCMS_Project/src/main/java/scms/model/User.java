package scms.model;

/**
 * Abstract base class for all system users.
 * Demonstrates abstraction and encapsulation (OOP principles).
 */
public abstract class User {
    private final String userId;
    private final String name;
    private final String email;
    private final String password;
    private final UserRole role;

    public User(String userId, String name, String email, String password, UserRole role) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    public String getUserId()   { return userId; }
    public String getName()     { return name; }
    public String getEmail()    { return email; }
    public String getPassword() { return password; }
    public UserRole getRole()   { return role; }

    /** Each subclass describes its permissions. */
    public abstract String getPermissionSummary();

    @Override
    public String toString() {
        return String.format("[%s] %s (%s)", role, name, email);
    }
}
