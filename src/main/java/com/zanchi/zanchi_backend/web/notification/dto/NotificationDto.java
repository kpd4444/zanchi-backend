package com.zanchi.zanchi_backend.web.notification.dto;

import com.zanchi.zanchi_backend.domain.notification.entity.Notification;
import com.zanchi.zanchi_backend.domain.notification.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 알림 응답 DTO
 * 프런트 `notifications.html`에서 사용하는 필드 명세:
 * - id, type, text, read, createdAt, clipId, commentId
 * - (선택) actorNickname, actorAvatarUrl
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {

    private Long id;

    /** 알림 유형 (enum 그대로 내려도 되고, 문자열로 내려도 됨) */
    private NotificationType type;

    /** 알림 메시지 */
    private String text;

    /** 읽음 여부 */
    private boolean read;

    /** 생성 시각 */
    private LocalDateTime createdAt;

    /** 관련 리소스 식별자(선택) */
    private Long clipId;
    private Long commentId;

    /** 액터/리시버(선택 노출) */
    private Long actorId;
    private Long receiverId;

    /** 액터 프로필(선택) – 현재는 null, 추후 조인으로 채울 수 있음 */
    private String actorNickname;
    private String actorAvatarUrl;

    /** 엔티티 → DTO 매핑 */
    public static NotificationDto from(Notification n) {
        if (n == null) return null;
        return NotificationDto.builder()
                .id(n.getId())
                .type(n.getType())
                .text(n.getMessage())
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .clipId(n.getClipId())
                .commentId(n.getCommentId())
                .actorId(n.getActorId())
                .receiverId(n.getReceiverId())
                // 프로필 정보는 현재 엔티티에 없으므로 null 유지(추후 확장)
                .actorNickname(null)
                .actorAvatarUrl(null)
                .build();
    }
}
