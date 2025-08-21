package com.zanchi.zanchi_backend;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class AiConfig {

    @Bean
    ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }

    // Kakao Local API 호출용 WebClient
    @Bean(name = "kakaoWebClient")
    public WebClient kakaoWebClient(
            @Value("${app.maps.kakao.host}") String host,
            @Value("${app.maps.kakao.rest-api-key}") String restKey
    ) {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(8))
                .doOnConnected(conn -> {
                    conn.addHandlerLast(new ReadTimeoutHandler(8, TimeUnit.SECONDS));
                    conn.addHandlerLast(new WriteTimeoutHandler(8, TimeUnit.SECONDS));
                });

        return WebClient.builder()
                .baseUrl(host)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "KakaoAK " + restKey) // 헤더 고정
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(c -> c.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                        .build())
                .build();
    }

    @Bean(name = "googlePlacesWebClient")
    public WebClient googlePlacesWebClient(
            @Value("${app.maps.google.api-key}") String key
    ) {
        return WebClient.builder()
                .baseUrl("https://places.googleapis.com")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("X-Goog-Api-Key", key)
                .build();
    }

    @Bean(name = "googleRoutesWebClient")
    public WebClient googleRoutesWebClient(
            @Value("${app.maps.google.api-key}") String key
    ) {
        return WebClient.builder()
                .baseUrl("https://routes.googleapis.com")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("X-Goog-Api-Key", key)
                .build();
    }
}