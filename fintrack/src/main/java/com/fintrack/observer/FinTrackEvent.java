package com.fintrack.observer;

import com.fintrack.model.Notification.NotificationType;
import org.springframework.context.ApplicationEvent;

/**
 * FinTrackEvent — Observer Pattern: Event
 * Owner: Samyuktha S / Saanvi Kakkar
 */
public class FinTrackEvent extends ApplicationEvent {

    private final NotificationType type;
    private final Long recipientUserId;
    private final String title;
    private final String message;
    private final Long refId;
    private final String refType;

    public FinTrackEvent(Object source, NotificationType type, Long recipientUserId,
                         String title, String message, Long refId, String refType) {
        super(source);
        this.type            = type;
        this.recipientUserId = recipientUserId;
        this.title           = title;
        this.message         = message;
        this.refId           = refId;
        this.refType         = refType;
    }

    public NotificationType getType()        { return type; }
    public Long getRecipientUserId()         { return recipientUserId; }
    public String getTitle()                 { return title; }
    public String getMessage()               { return message; }
    public Long getRefId()                   { return refId; }
    public String getRefType()               { return refType; }
}
