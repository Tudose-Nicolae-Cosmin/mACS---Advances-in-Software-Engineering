package com.instakill.analytics;

import com.instakill.infrastructure.security.JwtUserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsRepository repository;

    public AnalyticsController(AnalyticsRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/me")
    public AnalyticsResponse myAnalytics(@AuthenticationPrincipal JwtUserPrincipal principal) {
        var userId = principal.id();
        return new AnalyticsResponse(
                repository.weeklyStats(userId),
                repository.topPosts(userId),
                repository.likeCommentRatio(userId),
                repository.avgLikesByMedia(userId),
                repository.rank(userId));
    }
}