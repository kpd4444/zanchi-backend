package com.zanchi.zanchi_backend.domain.notification.repository;

import com.zanchi.zanchi_backend.domain.notification.entity.Notification;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.*;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("select n from Notification n where n.receiverId = :rid order by n.createdAt desc, n.id desc")
    Page<Notification> findPageByReceiverId(@Param("rid") Long receiverId, Pageable pageable);

    long countByReceiverIdAndIsReadFalse(Long receiverId);

    @Modifying
    @Query("update Notification n set n.isRead = true where n.receiverId = :rid and n.id in :ids and n.isRead = false")
    int markRead(@Param("rid") Long receiverId, @Param("ids") List<Long> ids);

    @Modifying
    @Query("update Notification n set n.isRead = true where n.receiverId = :rid and n.isRead = false")
    int markAllRead(@Param("rid") Long receiverId);

    Optional<Notification> findTop1ByReceiverIdOrderByIdDesc(Long receiverId);

    @Modifying
    @Query("delete from Notification n where n.createdAt < :threshold")
    int deleteOlderThan(@Param("threshold") LocalDateTime threshold);
}
