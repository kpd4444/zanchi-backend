package com.zanchi.zanchi_backend.domain.clip.repository;

import com.zanchi.zanchi_backend.domain.clip.Clip;
import com.zanchi.zanchi_backend.domain.clip.dto.ClipSummary;
import com.zanchi.zanchi_backend.domain.ranking.dto.ClipRankItem;
import com.zanchi.zanchi_backend.domain.ranking.dto.ClipRankView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDateTime;

public interface ClipRepository extends JpaRepository<Clip, Long> {

    long countByUploader_Id(Long uploaderId);

    @Query("""
      select c from Clip c
      left join c.uploader u
      where lower(coalesce(c.caption, '')) like lower(concat('%', :q, '%'))
         or lower(coalesce(u.name, u.loginId, '')) like lower(concat('%', :q, '%'))
         or lower(coalesce(u.loginId, '')) like lower(concat('%', :q, '%'))
      order by c.id desc
    """)
    Page<Clip> search(@Param("q") String q, Pageable pageable);

    Page<Clip> findByUploader_IdOrderByIdDesc(Long uploaderId, Pageable pageable);

    @EntityGraph(attributePaths = {"uploader"})
    Page<Clip> findAllByOrderByIdDesc(Pageable pageable);

    @Query(
            value = """
            select c from Clip c
            join fetch c.uploader u
            order by c.id desc
        """,
            countQuery = "select count(c) from Clip c"
    )
    Page<Clip> findAllWithUploader(Pageable pageable);

    @Modifying
    @Query("update Clip c set c.viewCount = c.viewCount + 1 where c.id = :clipId")
    int incrementViewCount(@Param("clipId") Long clipId);

    // 태그로 클립 검색
    @Query("""
      select c
      from Clip c
      where exists (
        select 1 from ClipTag ct
        join ct.tag t
        where ct.clip = c and t.normalizedName = :normalized
      )
      order by c.id desc
    """)
    Page<Clip> findByTagNormalized(@Param("normalized") String normalized, Pageable pageable);

    // ========= 랭킹 =========

    /** 전체 랭킹 (RankingController용) — since: Instant */
    @Query("""
      select c.id as clipId,
             u.id as uploaderId,
             coalesce(u.name, u.loginId) as uploaderName,
             u.avatarUrl as uploaderAvatarUrl,
             c.videoUrl as videoUrl,
             coalesce(c.caption, '') as caption,
             c.likeCount as likeCount
      from Clip c
        join c.uploader u
      where (:since is null or c.createdAt >= :since)
      order by c.likeCount desc, c.createdAt desc
    """)
    Page<ClipRankView> findRanking(@Param("since") Instant since, Pageable pageable);

    /** 전체 Top10 */
    @Query("""
      select c.id as clipId,
             u.id as uploaderId,
             coalesce(u.name, u.loginId) as uploaderName,
             u.avatarUrl as uploaderAvatarUrl,
             c.videoUrl as videoUrl,
             c.likeCount as likeCount
      from Clip c
        join c.uploader u
      order by c.likeCount desc, c.createdAt desc
    """)
    Page<ClipRankView> findTop10ClipsByLikeCount(Pageable pageable);

    /** 공연별 TopN (since 기준) — showId는 Long 권장 */
    @Query("""
      select c.id as clipId,
             u.id as uploaderId,
             coalesce(u.name, u.loginId) as uploaderName,
             c.likeCount as likeCount,
             coalesce(c.caption, '') as caption,
             c.videoUrl as videoUrl,
             c.createdAt as createdAt
      from Clip c
        join c.uploader u
      where c.show.id = :showId
        and (:since is null or c.createdAt >= :since)
      order by c.likeCount desc, c.createdAt desc
    """)
    Page<ClipRankItem> findRankingByShowId(@Param("showId") Long showId,
                                           @Param("since") LocalDateTime since,
                                           Pageable pageable);

    /** 공연별 기간 랭킹 (start <= createdAt < end) */
    @Query("""
      select c.id as clipId,
             u.id as uploaderId,
             coalesce(u.name, u.loginId) as uploaderName,
             c.likeCount as likeCount,
             coalesce(c.caption, '') as caption,
             c.videoUrl as videoUrl,
             c.createdAt as createdAt
      from Clip c
      join c.uploader u
      where c.show.id = :showId
        and c.createdAt >= :start
        and c.createdAt <  :end
      order by c.likeCount desc, c.createdAt desc
    """)
    Page<ClipRankItem> findRankingByShowAndDate(@Param("showId") Long showId,
                                                @Param("start") LocalDateTime start,
                                                @Param("end") LocalDateTime end,
                                                Pageable pageable);

    // ========= 팔로잉 피드 =========
    @Query(
            value = """
        select new com.zanchi.zanchi_backend.domain.clip.dto.ClipSummary(
            c.id,
            u.id,
            case when (u.name is null or u.name = '') then u.loginId else u.name end,
            u.avatarUrl,
            c.videoUrl,
            coalesce(c.caption, ''),
            c.likeCount,
            c.commentCount,
            c.createdAt
        )
        from Clip c
        join c.uploader u
        where exists (
            select 1
            from MemberFollow f
            where f.follower.id = :userId
              and f.following.id = u.id
        )
        and (
            :q = '' or
            lower(coalesce(c.caption, '')) like lower(concat('%', :q, '%')) or
            lower(coalesce(u.name, u.loginId, '')) like lower(concat('%', :q, '%')) or
            lower(coalesce(u.loginId, '')) like lower(concat('%', :q, '%'))
        )
        order by c.createdAt desc
        """,
            countQuery = """
        select count(c)
        from Clip c
        join c.uploader u
        where exists (
            select 1
            from MemberFollow f
            where f.follower.id = :userId
              and f.following.id = u.id
        )
        and (
            :q = '' or
            lower(coalesce(c.caption, '')) like lower(concat('%', :q, '%')) or
            lower(coalesce(u.name, u.loginId, '')) like lower(concat('%', :q, '%')) or
            lower(coalesce(u.loginId, '')) like lower(concat('%', :q, '%'))
        )
        """
    )
    Page<ClipSummary> findFollowingClips(@Param("userId") Long userId,
                                         @Param("q") String q,
                                         Pageable pageable);
}
