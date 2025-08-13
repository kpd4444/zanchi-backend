package com.zanchi.zanchi_backend.domain.clip.repository;

import com.zanchi.zanchi_backend.domain.clip.Clip;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClipRepository extends JpaRepository<Clip,Long> {

    long countByUploader_Id(Long uploaderId);

    @Query("""
      select c from Clip c
      left join c.uploader u
      where lower(coalesce(c.caption,'')) like lower(concat('%', :q, '%'))
         or lower(coalesce(u.name, u.loginId, '')) like lower(concat('%', :q, '%'))
         or lower(coalesce(u.loginId, '')) like lower(concat('%', :q, '%'))
      order by c.id desc
    """)
    Page<Clip> search(@Param("q") String q, Pageable pageable);

    Page<Clip> findByUploader_IdOrderByIdDesc(Long uploaderId, Pageable pageable);

    @EntityGraph(attributePaths = {"uploader"})
    Page<Clip> findAllByOrderByIdDesc(Pageable pageable);

    @Query(value = """
    select c from Clip c
    join fetch c.uploader u
    order by c.id desc
    """,
            countQuery = "select count(c) from Clip c")
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
}
