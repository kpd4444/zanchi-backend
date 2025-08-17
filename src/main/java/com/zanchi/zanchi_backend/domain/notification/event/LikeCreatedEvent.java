package com.zanchi.zanchi_backend.domain.notification.event;

public record LikeCreatedEvent(Long actorId, Long receiverId, Long clipId) {}

