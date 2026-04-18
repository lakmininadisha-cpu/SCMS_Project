package scms.service;

import scms.exception.DuplicateEntityException;
import scms.exception.EntityNotFoundException;
import scms.model.User;

import java.util.*;

/**
 * Manages user accounts in memory.
 */
public class UserService {

    private final Map<String, User> usersById    = new LinkedHashMap<>();
    private final Map<String, User> usersByEmail = new HashMap<>();

    public void register(User user) throws DuplicateEntityException {
        if (usersById.containsKey(user.getUserId())) {
            throw new DuplicateEntityException("User ID already exists: " + user.getUserId());
        }
        if (usersByEmail.containsKey(user.getEmail())) {
            throw new DuplicateEntityException("Email already registered: " + user.getEmail());
        }
        usersById.put(user.getUserId(), user);
        usersByEmail.put(user.getEmail(), user);
    }

    public User authenticate(String email, String password) throws EntityNotFoundException {
        User user = usersByEmail.get(email);
        if (user == null || !user.getPassword().equals(password)) {
            throw new EntityNotFoundException("Invalid email or password.");
        }
        return user;
    }

    public User findById(String userId) throws EntityNotFoundException {
        User u = usersById.get(userId);
        if (u == null) throw new EntityNotFoundException("User not found: " + userId);
        return u;
    }

    public Collection<User> getAllUsers() { return Collections.unmodifiableCollection(usersById.values()); }
}
