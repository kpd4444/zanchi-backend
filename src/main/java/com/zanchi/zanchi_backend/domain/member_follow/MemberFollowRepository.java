package com.zanchi.zanchi_backend.domain.member_follow;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberFollowRepository extends JpaRepository<MemberFollow, Long> {
    boolean existsByFollower_IdAndFollowing_Id(Long followerId, Long followingId);
    void deleteByFollower_IdAndFollowing_Id(Long followerId, Long followingId);

    long countByFollowing_Id(Long userId); // 그 사람의 팔로워 수
    long countByFollower_Id(Long userId);  // 그 사람이 팔로잉 수

    Page<MemberFollow> findByFollowing_Id(Long userId, Pageable pageable); // followers
    Page<MemberFollow> findByFollower_Id(Long userId, Pageable pageable);  // following
}