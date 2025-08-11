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
}
