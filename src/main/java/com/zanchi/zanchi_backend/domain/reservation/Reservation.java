package com.zanchi.zanchi_backend.domain.reservation;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import com.zanchi.zanchi_backend.domain.show.Show;

import java.time.LocalDateTime;

@Entity @Table(name = "reservation")
@Getter @Setter
public class Reservation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_id")
    private Integer id;

    @Column(name = "member_id", nullable = false)
    private Integer memberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "show_id", nullable = false)
    private Show show;

    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('CONFIRMED','CANCELLED')", nullable = false)
    private ReservationStatus status = ReservationStatus.CONFIRMED;

    @Column(name = "unit_price")
    private Integer unitPrice;

    @Column(name = "total_price")
    private Integer totalPrice;

    @Column(name = "point_used")
    private Integer pointUsed;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;
}