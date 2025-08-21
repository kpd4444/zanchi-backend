package com.zanchi.zanchi_backend.web.ai;


import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@RestController
@RequestMapping("/test/kakao")
@RequiredArgsConstructor
public class TestKakaoController {

    private final WebClient kakaoWebClient; // @Bean(name="kakaoWebClient") 주입

    // e.g. /test/kakao/search?query=홍대%20양식&lat=37.5563&lng=126.9236
    @GetMapping("/search")
    public ResponseEntity<Map> search(@RequestParam String query,
                                      @RequestParam double lat,
                                      @RequestParam double lng) {
        Map body = kakaoWebClient.get()
                .uri(uri -> uri.path("/v2/local/search/keyword.json")
                        .queryParam("query", query)
                        .queryParam("y", lat)  // Kakao는 y=위도(lat)
                        .queryParam("x", lng)  // Kakao는 x=경도(lng)
                        .queryParam("radius", 1200)
                        .queryParam("size", 5)
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        return ResponseEntity.ok(body);
    }
}
