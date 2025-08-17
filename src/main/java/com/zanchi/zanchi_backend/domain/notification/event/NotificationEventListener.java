package com.zanchi.zanchi_backend.domain.notification.event;

import com.zanchi.zanchi_backend.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 알림 관련 도메인 이벤트 리스너
 * - AFTER_COMMIT 단계에서 처리하여 롤백 시 알림 생성이 되지 않도록 함
 * - 각 이벤트 수신 시 로그를 남겨 디버깅이 가능하도록 함
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;

    // 좋아요
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLike(LikeCreatedEvent e) {
        log.info("[NOTIF][EVT] LIKE actor={} -> receiver={} clip={}",
                e.actorId(), e.receiverId(), e.clipId());
        notificationService.createLike(e.actorId(), e.receiverId(), e.clipId());
    }

    // 댓글
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onComment(CommentCreatedEvent e) {
        log.info("[NOTIF][EVT] COMMENT actor={} -> receiver={} clip={} comment={}",
                e.actorId(), e.receiverId(), e.clipId(), e.commentId());
        notificationService.createComment(e.actorId(), e.receiverId(), e.clipId(), e.commentId());
    }

    // 대댓글
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReply(ReplyCreatedEvent e) {
        log.info("[NOTIF][EVT] REPLY actor={} -> receiver={} clip={} comment={}",
                e.actorId(), e.receiverId(), e.clipId(), e.commentId());
        notificationService.createReply(e.actorId(), e.receiverId(), e.clipId(), e.commentId());
    }

    // 팔로우
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFollow(FollowCreatedEvent e) {
        log.info("[NOTIF][EVT] FOLLOW actor={} -> receiver={}",
                e.actorId(), e.receiverId());
        notificationService.createFollow(e.actorId(), e.receiverId());
    }

    // 멘션
    @TransactionalEventListener
    public void onMention(MentionCreatedEvent e) {
        if (e.getActorId().equals(e.getReceiverId())) return; // 자기 자신 멘션 무시
        log.info("[NOTIF][EVT] MENTION actor={} -> receiver={} clip={} comment={}",
                e.getActorId(), e.getReceiverId(), e.getClipId(), e.getCommentId());
        notificationService.createMention(e.getActorId(), e.getReceiverId(), e.getClipId(), e.getCommentId());
    }
}
