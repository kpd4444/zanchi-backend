package com.zanchi.zanchi_backend.domain.notification.service;

import com.zanchi.zanchi_backend.config.exception.ApiException;
import com.zanchi.zanchi_backend.domain.notification.entity.Notification;
import com.zanchi.zanchi_backend.domain.notification.entity.NotificationType;
import com.zanchi.zanchi_backend.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;

    // ======================================================
    // 조회
    // ======================================================

    /** 사용자별 미읽음 개수 */
    public long unreadCount(Long receiverId) {
        if (receiverId == null) return 0L;
        return notificationRepository.countByReceiverIdAndIsReadFalse(receiverId);
    }

    /**
     * 사용자별 ETag(상태 태그)
     * 포맷: "{receiverId}:{maxId}:{unreadCount}"
     */
    public String stateTag(Long receiverId) {
        long unread = unreadCount(receiverId);
        long maxId = latestId(receiverId);
        return receiverId + ":" + maxId + ":" + unread; // 따옴표 없이 순수 토큰
    }

    /** 사용자별 알림 목록 (최신순) */
    public Page<Notification> list(Long receiverId, Pageable pageable) {
        return notificationRepository.findPageByReceiverId(receiverId, pageable);
    }

    // ======================================================
    // 읽음 처리
    // ======================================================

    /** 단건 읽음 처리 */
    @Transactional
    public void markRead(Long receiverId, Long notificationId) {
        if (receiverId == null || notificationId == null) return;
        notificationRepository.markRead(receiverId, List.of(notificationId));
    }

    /** 모두 읽음 처리 (미읽음만) */
    @Transactional
    public int markAllRead(Long receiverId) {
        if (receiverId == null) return 0;
        return notificationRepository.markAllRead(receiverId);
    }

    // ======================================================
    // 생성 (이벤트 리스너에서 호출) — 새 트랜잭션으로 강제 커밋
    // ======================================================

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createLike(Long actorId, Long receiverId, Long clipId) {
        if (skipSelf(actorId, receiverId)) return;
        save(receiverId, actorId, NotificationType.LIKE, clipId, null,
                "당신의 클립을 좋아합니다.");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createComment(Long actorId, Long receiverId, Long clipId, Long commentId) {
        if (skipSelf(actorId, receiverId)) return;
        save(receiverId, actorId, NotificationType.COMMENT, clipId, commentId,
                "당신의 클립에 댓글이 달렸습니다.");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createReply(Long actorId, Long receiverId, Long clipId, Long commentId) {
        if (skipSelf(actorId, receiverId)) return;
        save(receiverId, actorId, NotificationType.REPLY, clipId, commentId,
                "당신의 댓글에 답글이 달렸습니다.");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createFollow(Long actorId, Long receiverId) {
        if (skipSelf(actorId, receiverId)) return;
        save(receiverId, actorId, NotificationType.FOLLOW, null, null,
                "새로운 팔로워가 생겼습니다.");
    }

    /** 멘션 알림 생성 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createMention(Long actorId, Long receiverId, Long clipId, Long commentId) {
        if (skipSelf(actorId, receiverId)) return;
        save(receiverId, actorId, NotificationType.MENTION, clipId, commentId,
                "당신이 멘션되었습니다.");
    }

    // ======================================================
    // 보관기간 정리(스케줄러용)
    // ======================================================

    /** 스케줄러에서 호출하는 7일 초과 알림 삭제 */
    @Transactional
    public int deleteOlderThan7Days() {
        return deleteOlderThanDays(7);
    }

    /** N일 초과 알림 삭제 */
    @Transactional
    public int deleteOlderThanDays(int days) {
        LocalDateTime threshold = LocalDateTime.now().minusDays(days);
        return notificationRepository.deleteOlderThan(threshold);
    }

    // ======================================================
    // 내부 유틸
    // ======================================================

    private boolean skipSelf(Long actorId, Long receiverId) {
        return actorId != null && receiverId != null && actorId.equals(receiverId);
    }

    private long latestId(Long receiverId) {
        return notificationRepository.findTop1ByReceiverIdOrderByIdDesc(receiverId)
                .map(Notification::getId)
                .orElse(0L);
    }

    @Transactional // REQUIRES_NEW 메서드 내부에서 호출됨
    protected Notification save(Long receiverId,
                                Long actorId,
                                NotificationType type,
                                Long clipId,
                                Long commentId,
                                String message) {
        Notification n = Notification.builder()
                .receiverId(receiverId)
                .actorId(actorId)
                .type(type)
                .clipId(clipId)
                .commentId(commentId)
                .message(message != null ? message : type.name())
                .isRead(false)
                .build();
        Notification saved = notificationRepository.save(n);
        log.info("[NOTIF][SAVE] id={} type={} actor={} -> receiver={} clip={} comment={}",
                saved.getId(), saved.getType(), saved.getActorId(), saved.getReceiverId(),
                saved.getClipId(), saved.getCommentId());
        return saved;
    }

    // 삭제
    @Transactional
    public void delete(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndReceiverId(notificationId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Notification not found"));

        notificationRepository.delete(notification);
    }
}
