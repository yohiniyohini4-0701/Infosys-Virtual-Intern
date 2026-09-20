package com.example.governmentsubsidy.dto.notification;

import com.example.governmentsubsidy.enums.NotificationType;
import java.time.LocalDateTime;

public class NotificationResponse {

    private Long id;
    private String recipientUsername;
    private String title;
    private String message;
    private NotificationType type;
    private String entityName;
    private Long entityId;
    private String actionUrl;
    private boolean isRead;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;

    public NotificationResponse() {}

    public NotificationResponse(Long id, String recipientUsername, String title, String message,
                                NotificationType type, String entityName, Long entityId,
                                String actionUrl, boolean isRead, LocalDateTime createdAt,
                                LocalDateTime readAt) {
        this.id = id;
        this.recipientUsername = recipientUsername;
        this.title = title;
        this.message = message;
        this.type = type;
        this.entityName = entityName;
        this.entityId = entityId;
        this.actionUrl = actionUrl;
        this.isRead = isRead;
        this.createdAt = createdAt;
        this.readAt = readAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRecipientUsername() {
        return recipientUsername;
    }

    public void setRecipientUsername(String recipientUsername) {
        this.recipientUsername = recipientUsername;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public String getActionUrl() {
        return actionUrl;
    }

    public void setActionUrl(String actionUrl) {
        this.actionUrl = actionUrl;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }
}
