package com.zanchi.zanchi_backend.domain.ai;


import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
@Service
public class InMemoryRouteStore implements RouteStore {
    private final Map<String, SavedRoute> db = new ConcurrentHashMap<>();

    @Override
    public SavedRoute save(SavedRoute r) {
        String id = (r.id()==null || r.id().isBlank()) ? UUID.randomUUID().toString() : r.id();
        SavedRoute copy = new SavedRoute(
                id,
                r.createdAt()==null ? Instant.now() : r.createdAt(),
                r.title(), r.companion(), r.mobility(),
                r.startName(), r.startLat(), r.startLng(),
                r.tags(), r.totalTravelMinutes(), r.explain(),
                r.steps()
        );
        db.put(id, copy);
        return copy;
    }

    @Override public List<SavedRoute> list() {
        return db.values().stream()
                .sorted(Comparator.comparing(SavedRoute::createdAt).reversed())
                .toList();
    }
    @Override public Optional<SavedRoute> get(String id){ return Optional.ofNullable(db.get(id)); }
    @Override public void delete(String id){ db.remove(id); }
}
