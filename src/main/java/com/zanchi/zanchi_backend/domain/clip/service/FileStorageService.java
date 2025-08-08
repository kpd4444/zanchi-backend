package com.zanchi.zanchi_backend.domain.clip.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageService {
    @Value("${app.upload.dir}")
    private String uploadDir;

    public String saveVideo(MultipartFile file) throws Exception {
        Files.createDirectories(Path.of(uploadDir));
        String ext = getExt(file.getOriginalFilename());
        String filename = UUID.randomUUID() + (ext.isEmpty()? "" : "." + ext);
        Path target = Path.of(uploadDir, filename);
        file.transferTo(target.toFile());
        // URL로 접근할 수 있게 /uploads 경로로 치환
        return "/uploads/clips/" + filename;
    }

    private String getExt(String name) {
        if (name == null) return "";
        int i = name.lastIndexOf('.');
        return i == -1 ? "" : name.substring(i+1);
    }
}