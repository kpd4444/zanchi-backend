package com.zanchi.zanchi_backend.domain.notification.event;

import lombok.Getter;

@Getter
public class MentionCreatedEvent {
    private final Long actorId;
    private final Long receiverId;
    private final Long clipId;
    private final Long commentId;

    public MentionCreatedEvent(Long actorId, Long receiverId, Long clipId, Long commentId) {
        this.actorId = actorId;
        this.receiverId = receiverId;
        this.clipId = clipId;
        this.commentId = commentId;
    }
}
