package com.example.governmentsubsidy.repository;

import com.example.governmentsubsidy.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientUsernameOrderByCreatedAtDesc(String recipientUsername);

    List<Notification> findByRecipientUsernameAndIsReadFalseOrderByCreatedAtDesc(String recipientUsername);

    long countByRecipientUsernameAndIsReadFalse(String recipientUsername);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :now WHERE n.recipientUsername = :username AND n.isRead = false")
    int markAllAsReadForUser(@Param("username") String username, @Param("now") LocalDateTime now);
}
