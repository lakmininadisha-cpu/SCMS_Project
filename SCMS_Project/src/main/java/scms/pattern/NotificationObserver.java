package scms.pattern;

import scms.model.Notification;

/**
 * BEHAVIOURAL PATTERN – Observer
 * Any class that wants to receive notifications implements this interface.
 */
public interface NotificationObserver {
    void onNotification(Notification notification);
}
