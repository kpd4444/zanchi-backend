package com.zanchi.zanchi_backend.domain.ai;

import com.zanchi.zanchi_backend.domain.member.Member;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "route_plan",
        indexes = { @Index(name = "idx_route_owner_created", columnList = "owner_id, created_at") }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RoutePlan {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Member owner;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    @Column(length = 100, nullable = false)
    private String title;

    private String companion;
    private String mobility;

    @Column(name = "start_name")
    private String startName;

    @Column(name = "start_lat", columnDefinition = "double")
    private double startLat;

    @Column(name = "start_lng", columnDefinition = "double")
    private double startLng;

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "route_plan_tags", joinColumns = @JoinColumn(name = "route_plan_id"))
    @Column(name = "tag")
    private List<String> tags = new ArrayList<>();

    @Column(name = "total_travel_minutes")
    private Integer totalTravelMinutes;

    @Column(name = "plan_explain", columnDefinition = "text")
    private String planExplain;

    @Builder.Default
    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RouteStep> steps = new ArrayList<>();

    /** tags를 통째로 교체 (null 안전) */
    public void setTags(List<String> tags) {
        if (this.tags == null) this.tags = new ArrayList<>();
        this.tags.clear();
        if (tags != null) this.tags.addAll(tags);
    }

    /** steps를 통째로 교체 (양방향 연결 + null 안전) */
    public void setSteps(List<RouteStep> steps) {
        if (this.steps == null) this.steps = new ArrayList<>();
        this.steps.clear();
        if (steps != null) {
            for (RouteStep s : steps) {
                s.setPlan(this);     // 양방향 관계 세팅
                this.steps.add(s);
            }
        }
    }
}
