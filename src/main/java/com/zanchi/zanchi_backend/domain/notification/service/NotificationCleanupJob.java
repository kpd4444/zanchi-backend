package com.zanchi.zanchi_backend.domain.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@RequiredArgsConstructor
public class NotificationCleanupJob {

    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 * * * ?") // 매시간 정각
    public void run() {
        notificationService.deleteOlderThan7Days();
        // 리스트 API 에서 page/size 로 100건 제한. 필요시 per-user 상위 100건만 유지하는 별도 배치 추가 가능.
    }
}
