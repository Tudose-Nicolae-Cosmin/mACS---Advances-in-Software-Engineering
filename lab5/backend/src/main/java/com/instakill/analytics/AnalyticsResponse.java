package com.instakill.analytics;

import java.util.List;

public record AnalyticsResponse(
        List<WeekStats> weeks,
        List<TopPost> topPosts,
        LikeCommentRatio ratio,
        AvgLikesByMedia avgLikes,
        RankInfo rank
) {

    public record WeekStats(String weekStart, long posts, long likes, long comments) {
    }

    public record TopPost(String id, String content, String mediaUrl, long likes, long comments) {
    }

    public record LikeCommentRatio(long likes, long comments, Double ratio) {
    }

    public record AvgLikesByMedia(Double photoAvg, long photoPosts, Double videoAvg, long videoPosts) {
    }

    public record RankInfo(Double avgEngagement, Long rank, Long totalUsers, Double topPercent) {
    }
}