package com.zanchi.zanchi_backend.web.notification;

import com.zanchi.zanchi_backend.config.security.MemberPrincipal;
import com.zanchi.zanchi_backend.domain.notification.entity.Notification;
import com.zanchi.zanchi_backend.domain.notification.service.NotificationService;
import com.zanchi.zanchi_backend.web.notification.dto.NotificationDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<Page<NotificationDto>> list(
            @AuthenticationPrincipal MemberPrincipal me,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch
    ) {
        String etag = notificationService.stateTag(me.getId());
        if (etag.equals(ifNoneMatch)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .eTag(etag)
                    .cacheControl(CacheControl.noStore())
                    .build();
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> entities = notificationService.list(me.getId(), pageable);
        Page<NotificationDto> body = entities.map(NotificationDto::from); // NotificationDto.from(Notification) 가정

        return ResponseEntity.ok()
                .eTag(etag)
                .cacheControl(CacheControl.noStore())
                .body(body);
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Long> unreadCount(
            @AuthenticationPrincipal MemberPrincipal me,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch
    ) {
        String etag = notificationService.stateTag(me.getId());
        if (etag.equals(ifNoneMatch)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .eTag(etag)
                    .cacheControl(CacheControl.noStore())
                    .build();
        }
        long count = notificationService.unreadCount(me.getId());
        return ResponseEntity.ok()
                .eTag(etag)
                .cacheControl(CacheControl.noStore())
                .body(count);
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markRead(
            @AuthenticationPrincipal MemberPrincipal me,
            @PathVariable Long id
    ) {
        notificationService.markRead(me.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllRead(
            @AuthenticationPrincipal MemberPrincipal me
    ) {
        notificationService.markAllRead(me.getId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(
            @AuthenticationPrincipal MemberPrincipal me,
            @PathVariable Long id
    ) {
        notificationService.delete(me.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
