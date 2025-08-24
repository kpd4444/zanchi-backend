// com.zanchi.zanchi_backend.domain.notification.event.NotificationEventListener
package com.zanchi.zanchi_backend.domain.notification.event;

import com.zanchi.zanchi_backend.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLike(LikeCreatedEvent e) {
        log.info("[NOTIF][EVT] LIKE actor={} -> receiver={} clip={}",
                e.actorId(), e.receiverId(), e.clipId());
        notificationService.createLike(e.actorId(), e.receiverId(), e.clipId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onComment(CommentCreatedEvent e) {
        log.info("[NOTIF][EVT] COMMENT actor={} -> receiver={} clip={} comment={}",
                e.actorId(), e.receiverId(), e.clipId(), e.commentId());
        notificationService.createComment(e.actorId(), e.receiverId(), e.clipId(), e.commentId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReply(ReplyCreatedEvent e) {
        log.info("[NOTIF][EVT] REPLY actor={} -> receiver={} clip={} comment={}",
                e.actorId(), e.receiverId(), e.clipId(), e.commentId());
        notificationService.createReply(e.actorId(), e.receiverId(), e.clipId(), e.commentId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFollow(FollowCreatedEvent e) {
        log.info("[NOTIF][EVT] FOLLOW actor={} -> receiver={}",
                e.actorId(), e.receiverId());
        notificationService.createFollow(e.actorId(), e.receiverId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMention(MentionCreatedEvent e) {
        log.info("[NOTIF][EVT] MENTION actor={} -> receiver={} clip={} comment={}",
                e.getActorId(), e.getReceiverId(), e.getClipId(), e.getCommentId());
        // 자기 자신 멘션 무시는 service의 skipSelf가 null-safe로 처리
        notificationService.createMention(e.getActorId(), e.getReceiverId(), e.getClipId(), e.getCommentId());
    }
}
