package com.zanchi.zanchi_backend.domain.notification.event;

public record ReplyCreatedEvent(Long actorId, Long receiverId, Long clipId, Long commentId) {}

