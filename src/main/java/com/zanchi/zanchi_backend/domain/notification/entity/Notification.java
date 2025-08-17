package com.zanchi.zanchi_backend.domain.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification",
        indexes = {
                @Index(name = "idx_n_receiver_created", columnList = "receiverId, createdAt"),
                @Index(name = "idx_n_receiver_isread", columnList = "receiverId, isRead, createdAt")
        })
@Getter @Builder
@NoArgsConstructor @AllArgsConstructor
public class Notification {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) private Long receiverId;
    @Column(nullable = false) private Long actorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationType type;

    private Long clipId;
    private Long commentId;

    @Column(nullable = false, length = 200)
    private String message;

    @Column(nullable = false)
    private boolean isRead = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public void markRead() { this.isRead = true; }
}
