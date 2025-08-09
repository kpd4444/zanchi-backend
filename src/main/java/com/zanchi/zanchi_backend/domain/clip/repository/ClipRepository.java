package com.zanchi.zanchi_backend.domain.clip.repository;

import com.zanchi.zanchi_backend.domain.clip.Clip;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClipRepository extends JpaRepository<Clip,Long> {

    @EntityGraph(attributePaths = {"uploader"})
    Page<Clip> findAllByOrderByIdDesc(Pageable pageable);
}
