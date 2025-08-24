package com.zanchi.zanchi_backend.web.reco;

import com.zanchi.zanchi_backend.domain.clip.dto.ClipFeedRes;
import com.zanchi.zanchi_backend.reco.PersonalizeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reco")
public class RecoController {

    private final PersonalizeService personalizeService;

    /**
     * 홈 추천
     * - 디버그: ?personalizeUserId=u001 같이 넘기면 해당 ID로 호출
     * - 미로그인 + 디버그 없음이면 인기 상위로 백업
     */
    @GetMapping("/home")
    public ResponseEntity<List<ClipFeedRes>> home(
            @AuthenticationPrincipal(expression = "member.id") Long meId,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String personalizeUserId
    ) {
        var list = personalizeService.homeRecommendations(meId, size, personalizeUserId);
        return ResponseEntity.ok(list);
    }
}
