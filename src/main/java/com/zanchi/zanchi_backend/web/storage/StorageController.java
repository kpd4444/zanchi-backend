package com.zanchi.zanchi_backend.web.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/storage")
public class StorageController {

    private final S3Presigner presigner;

    @Value("${app.s3.bucket}")
    private String bucket;

    @GetMapping("/presign")
    public Map<String, Object> presign(@RequestParam String filename,
                                       @RequestParam String contentType) {
        String ext = "mp4";
        int p = filename.lastIndexOf('.');
        if (p >= 0 && p + 1 < filename.length()) ext = filename.substring(p + 1);

        String key = "clips/%s.%s".formatted(UUID.randomUUID(), ext);

        PutObjectRequest por = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();

        PresignedPutObjectRequest pre = presigner.presignPutObject(b -> b
                .signatureDuration(Duration.ofMinutes(10))
                .putObjectRequest(por));

        return Map.of(
                "url", pre.url().toString(),
                "method", "PUT",
                "key", key,
                "expiresInSec", 600
        );
    }
}
