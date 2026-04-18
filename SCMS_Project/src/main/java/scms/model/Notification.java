package scms.model;

import java.time.LocalDateTime;

/** Simple notification message. */
public class Notification {
    private static int counter = 1;

    private final String notificationId;
    private final String recipientUserId;
    private final String message;
    private final LocalDateTime timestamp;
    private boolean read;

    public Notification(String recipientUserId, String message) {
        this.notificationId   = "N" + String.format("%04d", counter++);
        this.recipientUserId  = recipientUserId;
        this.message          = message;
        this.timestamp        = LocalDateTime.now();
        this.read             = false;
    }

    public String getNotificationId()    { return notificationId; }
    public String getRecipientUserId()   { return recipientUserId; }
    public String getMessage()           { return message; }
    public LocalDateTime getTimestamp()  { return timestamp; }
    public boolean isRead()              { return read; }
    public void markRead()               { this.read = true; }

    @Override
    public String toString() {
        return String.format("[%s] %s  (%s)%s", notificationId, message,
                timestamp.toLocalTime(), read ? " [READ]" : "");
    }
}
