package com.example.governmentsubsidy;

import com.example.governmentsubsidy.dto.notification.NotificationResponse;
import com.example.governmentsubsidy.entity.Notification;
import com.example.governmentsubsidy.enums.NotificationType;
import com.example.governmentsubsidy.exception.ResourceNotFoundException;
import com.example.governmentsubsidy.exception.UnauthorizedException;
import com.example.governmentsubsidy.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotificationServiceTest {

    @Autowired
    private NotificationService notificationService;

    @Test
    @DisplayName("Create in-app notification successfully")
    void testCreateNotification() {
        Notification created = notificationService.createNotification(
                "farmer_john",
                "New Scheme Announced",
                "Solar Pump subsidy scheme is now accepting applications.",
                NotificationType.SYSTEM_NOTICE,
                "Scheme",
                101L,
                "/beneficiary/schemes/101"
        );

        assertNotNull(created.getId());
        assertEquals("farmer_john", created.getRecipientUsername());
        assertEquals(NotificationType.SYSTEM_NOTICE, created.getType());
        assertFalse(created.isRead());
    }

    @Test
    @DisplayName("Fetch user notifications and unread count")
    void testGetUserNotificationsAndUnreadCount() {
        String testUser = "notify_test_user_" + System.currentTimeMillis();

        notificationService.createNotification(
                testUser,
                "Notification 1",
                "Message 1",
                NotificationType.APPLICATION_STATUS_UPDATE,
                "SubsidyApplication",
                1L,
                "/beneficiary/applications/1"
        );

        notificationService.createNotification(
                testUser,
                "Notification 2",
                "Message 2",
                NotificationType.DISBURSEMENT_RELEASED,
                "FundRelease",
                2L,
                "/beneficiary/applications/1"
        );

        long unreadCount = notificationService.getUnreadCount(testUser);
        assertEquals(2, unreadCount);

        List<NotificationResponse> list = notificationService.getUserNotifications(testUser, false);
        assertEquals(2, list.size());
    }

    @Test
    @DisplayName("Mark single notification as read")
    void testMarkAsRead() {
        String testUser = "read_test_user_" + System.currentTimeMillis();

        Notification created = notificationService.createNotification(
                testUser,
                "Verification Notice",
                "Inspection scheduled",
                NotificationType.VERIFICATION_UPDATE,
                "SubsidyApplication",
                5L,
                "/beneficiary/applications/5"
        );

        NotificationResponse updated = notificationService.markAsRead(created.getId(), testUser);
        assertTrue(updated.isRead());
        assertNotNull(updated.getReadAt());

        long unreadCount = notificationService.getUnreadCount(testUser);
        assertEquals(0, unreadCount);
    }

    @Test
    @DisplayName("Mark all notifications as read")
    void testMarkAllAsRead() {
        String testUser = "bulk_read_user_" + System.currentTimeMillis();

        notificationService.createNotification(testUser, "N1", "M1", NotificationType.SYSTEM_NOTICE, null, null, null);
        notificationService.createNotification(testUser, "N2", "M2", NotificationType.SYSTEM_NOTICE, null, null, null);
        notificationService.createNotification(testUser, "N3", "M3", NotificationType.SYSTEM_NOTICE, null, null, null);

        assertEquals(3, notificationService.getUnreadCount(testUser));

        int updatedCount = notificationService.markAllAsRead(testUser);
        assertEquals(3, updatedCount);
        assertEquals(0, notificationService.getUnreadCount(testUser));
    }

    @Test
    @DisplayName("Marking notification of another user throws UnauthorizedException")
    void testUnauthorizedMarkAsRead() {
        Notification created = notificationService.createNotification(
                "user_alice",
                "Private Alert",
                "Confidential info",
                NotificationType.APPLICATION_STATUS_UPDATE,
                null,
                null,
                null
        );

        assertThrows(UnauthorizedException.class, () ->
                notificationService.markAsRead(created.getId(), "user_bob")
        );
    }

    @Test
    @DisplayName("Delete notification by owner succeeds")
    void testDeleteNotification() {
        String testUser = "delete_test_user_" + System.currentTimeMillis();

        Notification created = notificationService.createNotification(
                testUser,
                "To Delete",
                "Will be removed",
                NotificationType.SYSTEM_NOTICE,
                null,
                null,
                null
        );

        notificationService.deleteNotification(created.getId(), testUser);

        assertThrows(ResourceNotFoundException.class, () ->
                notificationService.markAsRead(created.getId(), testUser)
        );
    }
}
