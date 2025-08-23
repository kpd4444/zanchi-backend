package com.zanchi.zanchi_backend.config.s3;

import com.zanchi.zanchi_backend.config.s3.PresignService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.net.URL;
import java.util.Map;

@RestController
@RequestMapping("/api/s3")
@RequiredArgsConstructor
public class PresignController {

    private final PresignService presignService;

    // 요청: {"key":"smoke/hello.txt","contentType":"text/plain"}
    // 응답: {"uploadUrl":"...","method":"PUT"}
    @PostMapping(value = "/presign-put", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> presignPut(@RequestBody Map<String, String> body) {
        String key = body.get("key");
        String contentType = body.getOrDefault("contentType", "application/octet-stream");
        URL url = presignService.createPutUrl(key, contentType);
        return Map.of("uploadUrl", url.toString(), "method", "PUT");
    }
}
