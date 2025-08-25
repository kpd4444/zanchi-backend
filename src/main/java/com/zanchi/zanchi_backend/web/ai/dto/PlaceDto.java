package com.zanchi.zanchi_backend.web.ai.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PlaceDto {
    private String name;        // 장소명
    private String address;     // 주소(도로명 or 지번)
    private double lat;         // 위도
    private double lng;         // 경도
    private String externalUrl; // 카카오 장소 상세 URL
    private Double rating;      // 평점(카카오 Local에는 없음 -> null)
    private Integer ratingCount;// 평점 수(카카오 Local에는 없음 -> null)
    private String category;    // 카테고리 이름(카카오 응답)
}