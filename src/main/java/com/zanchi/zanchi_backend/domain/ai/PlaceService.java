package com.zanchi.zanchi_backend.domain.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlaceService {

    private final KakaoLocalClient kakao;

    /** 기존: 좌표 기반 */
    public List<PlaceCandidate> searchCandidates(String query, double lat, double lng,
                                                 int radiusM, int size) {
        var resp = kakao.searchKeyword(query, lat, lng, radiusM, size);
        if (resp == null || resp.documents() == null) return List.of();
        return resp.documents().stream().map(PlaceMapper::from).toList();
    }


    /** 출발지처럼 ‘장소명 → 1건’ 주소/좌표를 얻고 싶을 때 사용 */
    public PlaceCandidate lookupOneByNameNear(String name, double lat, double lng) {
        var resp = kakao.searchKeyword(name, lat, lng, 2000, 1); // 반경 2km 안에서 가장 가까운 1건
        if (resp == null || resp.documents() == null || resp.documents().isEmpty()) return null;
        return PlaceMapper.from(resp.documents().get(0));
    }
}