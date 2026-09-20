package com.example.governmentsubsidy.controller;

import com.example.governmentsubsidy.dto.common.ApiResponse;
import com.example.governmentsubsidy.dto.notification.NotificationResponse;
import com.example.governmentsubsidy.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getUserNotifications(
            @RequestParam(name = "unreadOnly", defaultValue = "false") boolean unreadOnly,
            Principal principal) {
        String username = principal.getName();
        List<NotificationResponse> list = notificationService.getUserNotifications(username, unreadOnly);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(Principal principal) {
        String username = principal.getName();
        long count = notificationService.getUnreadCount(username);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            @PathVariable Long id,
            Principal principal) {
        String username = principal.getName();
        NotificationResponse updated = notificationService.markAsRead(id, username);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", updated));
    }

    @PostMapping("/read-all")
    public ResponseEntity<ApiResponse<Integer>> markAllAsRead(Principal principal) {
        String username = principal.getName();
        int count = notificationService.markAllAsRead(username);
        return ResponseEntity.ok(ApiResponse.success(count + " notification(s) marked as read", count));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(
            @PathVariable Long id,
            Principal principal) {
        String username = principal.getName();
        notificationService.deleteNotification(id, username);
        return ResponseEntity.ok(ApiResponse.success("Notification dismissed", null));
    }
}
