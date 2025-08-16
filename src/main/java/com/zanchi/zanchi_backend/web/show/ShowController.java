package com.zanchi.zanchi_backend.web.show;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;
import com.zanchi.zanchi_backend.domain.show.Show;
import com.zanchi.zanchi_backend.domain.show.ShowRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shows")
@RequiredArgsConstructor
public class ShowController {

    private final ShowRepository showRepository;

    @GetMapping
    public Map<String, Object> list(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        Page<Show> p = showRepository.findAllByOrderByDateDesc(PageRequest.of(page, size));
        List<Map<String, Object>> content = p.getContent().stream().map(s -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("showId", s.getId());
            m.put("title", s.getTitle());
            m.put("date", s.getDate());
            m.put("venue", s.getVenue());
            m.put("posterUrl", s.getPosterUrl());
            m.put("price", s.getPrice());
            m.put("remainingQty", s.getRemainingQty());
            m.put("salesStartAt", s.getSalesStartAt());
            m.put("salesEndAt", s.getSalesEndAt());
            return m;
        }).toList();

        return Map.of("content", content,
                "page", p.getNumber(), "size", p.getSize(), "totalElements", p.getTotalElements());
    }

    @GetMapping("/{showId}")
    public Map<String, Object> detail(@PathVariable Integer showId) {
        Show s = showRepository.findById(showId)
                .orElseThrow(() -> new java.util.NoSuchElementException("show not found"));
        return Map.of(
                "showId", s.getId(),
                "title", s.getTitle(),
                "date", s.getDate(),
                "venue", s.getVenue(),
                "posterUrl", s.getPosterUrl(),
                "price", s.getPrice(),
                "remainingQty", s.getRemainingQty(),
                "salesStartAt", s.getSalesStartAt(),
                "salesEndAt", s.getSalesEndAt(),
                "description", s.getDescription()
        );
    }
}
