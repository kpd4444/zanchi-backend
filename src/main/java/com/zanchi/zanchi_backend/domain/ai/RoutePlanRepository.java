package com.zanchi.zanchi_backend.domain.ai;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoutePlanRepository extends JpaRepository<RoutePlan, Long> {

    // 내 목록
    @EntityGraph(attributePaths = {"steps"})
    List<RoutePlan> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    // 내 한 건 상세
    @EntityGraph(attributePaths = {"steps"})
    Optional<RoutePlan> findByIdAndOwnerId(Long id, Long ownerId);

    // 안전 삭제 (소유자 확인)
    @Modifying
    @Query("delete from RoutePlan p where p.id=:id and p.owner.id=:ownerId")
    int deleteByIdAndOwnerId(@Param("id") Long id, @Param("ownerId") Long ownerId);
}