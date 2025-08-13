package com.zanchi.zanchi_backend.web.reservation;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import com.zanchi.zanchi_backend.config.exception.ApiException;
import com.zanchi.zanchi_backend.domain.member.MemberRepository;
import com.zanchi.zanchi_backend.domain.reservation.*;
import com.zanchi.zanchi_backend.web.reservation.dto.ReservationDtos;

import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;
    private final MemberRepository memberRepository;

    // 현재 인증 정보에서 memberId 추출
    private Integer currentMemberId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated())
            throw new ApiException(UNAUTHORIZED, "Unauthenticated");

        Object principal = auth.getPrincipal();

        // 커스텀 Principal에 memberId가 있을 때
        try {
            var m = principal.getClass().getMethod("getMemberId");
            Object idObj = m.invoke(principal);
            if (idObj instanceof Long l) return l.intValue();
            if (idObj instanceof Integer i) return i;
        } catch (Exception ignore) { /* fall through */ }

        // 일반 UserDetails나 String(username=loginId)인 경우 → DB에서 id 조회
        String username = null;
        if (principal instanceof UserDetails ud) username = ud.getUsername();
        else if (principal instanceof String s)  username = s;

        if (username == null || "anonymousUser".equals(username))
            throw new IllegalStateException("Cannot extract memberId from principal type=" +
                    (principal == null ? "null" : principal.getClass().getName()));

        Long memberId = memberRepository.findByLoginId(username)
                .orElseThrow(() -> new ApiException(UNAUTHORIZED, "member not found"))
                .getId();
        return memberId.intValue();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationDtos.Response create(@RequestBody ReservationDtos.CreateRequest req) {
        Integer memberId = currentMemberId();                             // 여기로 통일
        Reservation r = reservationService.create(
                memberId, req.getShowId(), req.getQuantity(), req.getPointUsed());
        return ReservationDtos.Response.of(r);
    }

    @GetMapping("/me")
    public Map<String, Object> myReservations(@RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size,
                                              @RequestParam(required = false) ReservationStatus status) {
        Integer memberId = currentMemberId();                             // ★
        Page<Reservation> p = reservationService.findMyReservations(memberId, page, size);
        List<ReservationDtos.Response> content = p.getContent().stream()
                .filter(r -> status == null || r.getStatus() == status)
                .map(ReservationDtos.Response::of)
                .toList();
        return Map.of("content", content,
                "page", p.getNumber(), "size", p.getSize(), "totalElements", p.getTotalElements());
    }

    @PostMapping("/{reservationId}/cancel")
    public ReservationDtos.CancelResponse cancel(@PathVariable Integer reservationId) {
        Integer memberId = currentMemberId();
        reservationService.cancel(memberId, reservationId);
        return ReservationDtos.CancelResponse.builder()
                .reservationId(reservationId)
                .status(ReservationStatus.CANCELLED)
                .build();
    }
}
