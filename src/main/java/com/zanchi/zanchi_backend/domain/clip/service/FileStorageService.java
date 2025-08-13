package com.zanchi.zanchi_backend.domain.clip.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Value("${app.upload.avatar-dir}")
    private String avatarDir;

    private static final String CLIP_PREFIX   = "/uploads/clips/";
    private static final String AVATAR_PREFIX = "/uploads/avatars/";

    /**
     * 비디오 저장
     */
    public String saveVideo(MultipartFile file) throws Exception {
        Files.createDirectories(Path.of(uploadDir));
        String ext = getExt(file.getOriginalFilename());
        String filename = UUID.randomUUID() + (ext.isEmpty() ? "" : "." + ext);
        Path target = Path.of(uploadDir, filename);
        file.transferTo(target.toFile());
        return CLIP_PREFIX + filename;
    }

    /**
     * 아바타 저장
     */
    public String saveAvatar(MultipartFile file) throws Exception {
        Files.createDirectories(Path.of(avatarDir));
        String ext = getExt(file.getOriginalFilename());
        String filename = UUID.randomUUID() + (ext.isEmpty() ? "" : "." + ext);
        Path target = Path.of(avatarDir, filename);
        file.transferTo(target.toFile());
        return AVATAR_PREFIX + filename;
    }

    /**
     * 업로드된 파일 삭제
     */
    public void deleteByUrl(String url) {
        if (url == null || url.isBlank()) return;
        try {
            int q = url.indexOf('?');
            if (q != -1) url = url.substring(0, q);
            if (url.startsWith("http://") || url.startsWith("https://")) {
                try { url = java.net.URI.create(url).getPath(); } catch (Exception ignored) {}
            }

            String baseDir;
            String prefix;
            if (url.startsWith(CLIP_PREFIX)) {
                baseDir = uploadDir; prefix = CLIP_PREFIX;
            } else if (url.startsWith(AVATAR_PREFIX)) {
                baseDir = avatarDir; prefix = AVATAR_PREFIX;
            } else {
                log.warn("Skip delete: not our prefix url={}", url);
                return;
            }

            String filename = url.substring(prefix.length());
            if (filename.isBlank() || filename.contains("..") || filename.contains("/")) {
                log.warn("Skip delete: suspicious filename: {}", filename);
                return;
            }

            Files.deleteIfExists(Path.of(baseDir, filename));
        } catch (Exception e) {
            log.warn("Fail to delete file by url: {}", url, e);
        }
    }

    private String getExt(String name) {
        if (name == null) return "";
        int i = name.lastIndexOf('.');
        return i == -1 ? "" : name.substring(i + 1);
    }
}
