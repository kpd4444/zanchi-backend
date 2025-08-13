package com.zanchi.zanchi_backend.domain.show;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "shows")
@Getter @Setter
public class Show {
    @Id
    @Column(name = "show_id")
    private Integer id;

    private String title;

    @Column(name = "show_date")        // 변경: 컬럼명만 매핑
    private LocalDateTime date;

    private String venue;

    @Column(name = "poster_url")
    private String posterUrl;

    @Column(columnDefinition = "VARCHAR(1000)")
    private String description;

    private Integer price;
    private Integer capacity;

    @Column(name = "remaining_qty")
    private Integer remainingQty;

    @Column(name = "sales_start_at")
    private LocalDateTime salesStartAt;

    @Column(name = "sales_end_at")
    private LocalDateTime salesEndAt;
}

