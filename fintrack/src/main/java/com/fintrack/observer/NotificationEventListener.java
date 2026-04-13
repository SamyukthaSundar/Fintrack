package com.fintrack.observer;

import com.fintrack.model.Notification;
import com.fintrack.model.User;
import com.fintrack.repository.NotificationRepository;
import com.fintrack.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * NotificationEventListener — Observer Pattern: Concrete Observer
 * Owner: Samyuktha S / Saanvi Kakkar
 */
@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationEventListener(NotificationRepository notificationRepository,
                                      UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository         = userRepository;
    }

    @Async
    @EventListener
    public void handleFinTrackEvent(FinTrackEvent event) {
        try {
            User recipient = userRepository.findById(event.getRecipientUserId()).orElse(null);
            if (recipient == null) {
                log.warn("Notification target user {} not found.", event.getRecipientUserId());
                return;
            }
            Notification notification = new Notification();
            notification.setUser(recipient);
            notification.setType(event.getType());
            notification.setTitle(event.getTitle());
            notification.setMessage(event.getMessage());
            notification.setRefId(event.getRefId());
            notification.setRefType(event.getRefType());
            notificationRepository.save(notification);
            log.debug("Notification [{}] saved for user {}", event.getType(), recipient.getUsername());
        } catch (Exception ex) {
            log.error("Failed to persist notification: {}", ex.getMessage(), ex);
        }
    }
}
