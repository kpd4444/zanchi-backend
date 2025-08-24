package com.zanchi.zanchi_backend.domain.ai;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name="route_step")
public class RouteStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 소유 루트
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name="route_plan_id", nullable = false)
    private RoutePlan plan;

    @Column(length = 1)
    private String label; // A/B/C/D

    // LLM/프론트에서 쓰는 식별자들
    private String ideaId;
    private String role;

    @Column(length = 120)
    private String name;

    @Column(length = 255)
    private String address;

    private double lat;
    private double lng;

    @Column(length=500)
    private String externalUrl;
    @Column(length=500)
    private String mapLink;

    private Double rating;
    private Integer ratingCount;
}