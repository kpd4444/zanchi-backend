package com.zanchi.zanchi_backend.reco;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.personalizeruntime.PersonalizeRuntimeClient;
import software.amazon.awssdk.services.personalizeruntime.model.GetRecommendationsRequest;
import software.amazon.awssdk.services.personalizeruntime.model.PredictedItem;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PersonalizeService {

    private final PersonalizeRuntimeClient runtime;

    @Value("${aws.personalize.campaign-arn:${PERSONALIZE_CAMPAIGN_ARN}}")
    private String campaignArn;

    /** 기본 추천: userId 기준, 상위 N개 itemId 반환 */
    public List<String> recommendItemIds(String userId, int numResults) {
        if (userId == null || userId.isBlank()) return List.of();
        try {
            var req = GetRecommendationsRequest.builder()
                    .campaignArn(campaignArn)
                    .userId(userId)
                    .numResults(numResults)
                    .build();
            var resp = runtime.getRecommendations(req);
            return resp.itemList().stream().map(PredictedItem::itemId).toList();
        } catch (Exception e) {
            // 실패 시 빈 목록 반환(서비스 죽이지 않기)
            return Collections.emptyList();
        }
    }
}