package scms.pattern;

import scms.model.Notification;

import java.util.*;

/**
 * CREATIONAL PATTERN  – Singleton
 * BEHAVIOURAL PATTERN – Observer (Subject / Publisher side)
 *
 * Central hub for dispatching notifications to registered observers.
 */
public class NotificationService {

    // ── Singleton ──────────────────────────────────────────────────────────
    private static NotificationService instance;

    private NotificationService() {}

    public static NotificationService getInstance() {
        if (instance == null) {
            instance = new NotificationService();
        }
        return instance;
    }

    // ── Observer registry ─────────────────────────────────────────────────
    /** userId → list of observers (typically one per user session) */
    private final Map<String, List<NotificationObserver>> observers = new HashMap<>();

    /** All notifications ever created, keyed by recipientUserId */
    private final Map<String, List<Notification>> inbox = new HashMap<>();

    public void subscribe(String userId, NotificationObserver observer) {
        observers.computeIfAbsent(userId, k -> new ArrayList<>()).add(observer);
    }

    public void unsubscribe(String userId, NotificationObserver observer) {
        List<NotificationObserver> list = observers.get(userId);
        if (list != null) list.remove(observer);
    }

    /** Create a notification for a user and push it to all their observers. */
    public Notification notify(String recipientUserId, String message) {
        Notification n = new Notification(recipientUserId, message);
        inbox.computeIfAbsent(recipientUserId, k -> new ArrayList<>()).add(n);

        List<NotificationObserver> obs = observers.getOrDefault(recipientUserId, Collections.emptyList());
        for (NotificationObserver o : obs) {
            o.onNotification(n);
        }
        return n;
    }

    /** Retrieve all notifications for a given user. */
    public List<Notification> getInbox(String userId) {
        return inbox.getOrDefault(userId, Collections.emptyList());
    }

    /** For testing – reset singleton state */
    public static void reset() {
        instance = null;
    }
}
