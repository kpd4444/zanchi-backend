package com.zanchi.zanchi_backend.web.reservation.dto;

import lombok.Builder;
import lombok.Getter;
import com.zanchi.zanchi_backend.domain.reservation.Reservation;
import com.zanchi.zanchi_backend.domain.reservation.ReservationStatus;

import java.time.LocalDateTime;

public class ReservationDtos {

    @Getter
    public static class CreateRequest {
        private Integer showId;
        private Integer quantity;
        private Integer pointUsed;
    }

    @Builder @Getter
    public static class Response {
        private Integer reservationId;
        private ReservationStatus status;
        private Integer memberId;
        private Integer showId;
        private Integer quantity;
        private Integer unitPrice;
        private Integer totalPrice;
        private Integer pointUsed;
        private LocalDateTime createdAt;

        public static Response of(Reservation r) {
            return Response.builder()
                    .reservationId(r.getId())
                    .status(r.getStatus())
                    .memberId(r.getMemberId())
                    .showId(r.getShow().getId())
                    .quantity(r.getQuantity())
                    .unitPrice(r.getUnitPrice())
                    .totalPrice(r.getTotalPrice())
                    .pointUsed(r.getPointUsed())
                    .createdAt(r.getCreatedAt())
                    .build();
        }
    }

    @Builder @Getter
    public static class CancelResponse {
        private Integer reservationId;
        private ReservationStatus status;
    }
}
