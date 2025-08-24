package com.zanchi.zanchi_backend.domain.ai;

import java.util.List;
import java.util.Optional;

public interface RouteStore {
    SavedRoute save(SavedRoute route);
    List<SavedRoute> list();
    Optional<SavedRoute> get(String id);
    void delete(String id);
}