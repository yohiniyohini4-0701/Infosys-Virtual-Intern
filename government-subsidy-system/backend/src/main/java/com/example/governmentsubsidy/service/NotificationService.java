package com.example.governmentsubsidy.service;

import com.example.governmentsubsidy.dto.notification.NotificationResponse;
import com.example.governmentsubsidy.entity.Notification;
import com.example.governmentsubsidy.enums.NotificationType;
import com.example.governmentsubsidy.exception.ResourceNotFoundException;
import com.example.governmentsubsidy.exception.UnauthorizedException;
import com.example.governmentsubsidy.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public Notification createNotification(String recipientUsername, String title, String message,
                                          NotificationType type, String entityName, Long entityId,
                                          String actionUrl) {
        if (recipientUsername == null || recipientUsername.trim().isEmpty()) {
            log.warn("Cannot create notification with empty recipient username");
            return null;
        }

        Notification notification = new Notification(
                recipientUsername.trim(),
                title,
                message,
                type,
                entityName,
                entityId,
                actionUrl
        );

        Notification saved = notificationRepository.save(notification);
        log.info("Created notification ID={} type={} for user={}", saved.getId(), type, recipientUsername);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getUserNotifications(String username, boolean unreadOnly) {
        List<Notification> notifications;
        if (unreadOnly) {
            notifications = notificationRepository.findByRecipientUsernameAndIsReadFalseOrderByCreatedAtDesc(username);
        } else {
            notifications = notificationRepository.findByRecipientUsernameOrderByCreatedAtDesc(username);
        }
        return notifications.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String username) {
        return notificationRepository.countByRecipientUsernameAndIsReadFalse(username);
    }

    @Transactional
    public NotificationResponse markAsRead(Long notificationId, String username) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with ID: " + notificationId));

        if (!notification.getRecipientUsername().equalsIgnoreCase(username)) {
            throw new UnauthorizedException("You are not authorized to access this notification.");
        }

        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            notification = notificationRepository.save(notification);
        }

        return mapToResponse(notification);
    }

    @Transactional
    public int markAllAsRead(String username) {
        return notificationRepository.markAllAsReadForUser(username, LocalDateTime.now());
    }

    @Transactional
    public void deleteNotification(Long notificationId, String username) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with ID: " + notificationId));

        if (!notification.getRecipientUsername().equalsIgnoreCase(username)) {
            throw new UnauthorizedException("You are not authorized to delete this notification.");
        }

        notificationRepository.delete(notification);
        log.info("Deleted notification ID={} for user={}", notificationId, username);
    }

    private NotificationResponse mapToResponse(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getRecipientUsername(),
                n.getTitle(),
                n.getMessage(),
                n.getType(),
                n.getEntityName(),
                n.getEntityId(),
                n.getActionUrl(),
                n.isRead(),
                n.getCreatedAt(),
                n.getReadAt()
        );
    }
}
