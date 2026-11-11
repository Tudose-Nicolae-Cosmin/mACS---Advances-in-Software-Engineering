package com.instakill.analytics;

import com.instakill.analytics.AnalyticsResponse.AvgLikesByMedia;
import com.instakill.analytics.AnalyticsResponse.LikeCommentRatio;
import com.instakill.analytics.AnalyticsResponse.RankInfo;
import com.instakill.analytics.AnalyticsResponse.TopPost;
import com.instakill.analytics.AnalyticsResponse.WeekStats;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class AnalyticsRepository {

    private final JdbcTemplate jdbc;

    public AnalyticsRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static String weekOf(String column) {
        return "CASE typeof(" + column + ") "
                + "WHEN 'integer' THEN date(" + column + " / 1000, 'unixepoch', 'weekday 0', '-6 days') "
                + "ELSE date(" + column + ", 'weekday 0', '-6 days') END";
    }

    public List<WeekStats> weeklyStats(UUID userId) {
        String sql = """
                SELECT week, SUM(posts), SUM(likes), SUM(comments) FROM (
                    SELECT %s AS week, 1 AS posts, 0 AS likes, 0 AS comments
                      FROM posts p WHERE p.author_id = ?
                    UNION ALL
                    SELECT %s, 0, 1, 0
                      FROM likes l JOIN posts p ON p.id = l.post_id WHERE p.author_id = ?
                    UNION ALL
                    SELECT %s, 0, 0, 1
                      FROM comments c JOIN posts p ON p.id = c.post_id WHERE p.author_id = ?
                ) GROUP BY week ORDER BY week
                """.formatted(weekOf("p.created_at"), weekOf("l.created_at"), weekOf("c.created_at"));
        String id = userId.toString();
        return jdbc.query(sql,
                (rs, i) -> new WeekStats(rs.getString(1), rs.getLong(2), rs.getLong(3), rs.getLong(4)),
                id, id, id);
    }

    public List<TopPost> topPosts(UUID userId) {
        String sql = """
                SELECT p.id, p.content, p.image_url,
                       (SELECT COUNT(*) FROM likes l WHERE l.post_id = p.id) AS likes,
                       (SELECT COUNT(*) FROM comments c WHERE c.post_id = p.id) AS comments
                  FROM posts p
                 WHERE p.author_id = ?
                 ORDER BY likes + comments DESC, p.id
                 LIMIT 5
                """;
        return jdbc.query(sql,
                (rs, i) -> new TopPost(rs.getString(1), rs.getString(2), rs.getString(3), rs.getLong(4), rs.getLong(5)),
                userId.toString());
    }

    public LikeCommentRatio likeCommentRatio(UUID userId) {
        String sql = """
                SELECT (SELECT COUNT(*) FROM likes l    JOIN posts p ON p.id = l.post_id WHERE p.author_id = ?),
                       (SELECT COUNT(*) FROM comments c JOIN posts p ON p.id = c.post_id WHERE p.author_id = ?)
                """;
        String id = userId.toString();
        return jdbc.queryForObject(sql, (rs, i) -> {
            long likes = rs.getLong(1);
            long comments = rs.getLong(2);
            return new LikeCommentRatio(likes, comments, comments == 0 ? null : (double) likes / comments);
        }, id, id);
    }

    public AvgLikesByMedia avgLikesByMedia(UUID userId) {
        String sql = """
                SELECT media, COUNT(*), AVG(likes) FROM (
                    SELECT CASE WHEN p.image_url IS NULL OR p.image_url = '' THEN 'none'
                                WHEN lower(p.image_url) LIKE '%.mp4'
                                  OR lower(p.image_url) LIKE '%.mov'
                                  OR lower(p.image_url) LIKE '%.webm' THEN 'video'
                                ELSE 'photo' END AS media,
                           (SELECT COUNT(*) FROM likes l WHERE l.post_id = p.id) AS likes
                      FROM posts p WHERE p.author_id = ?
                ) WHERE media != 'none' GROUP BY media
                """;

        double[] avg = {Double.NaN, Double.NaN};
        long[] count = {0, 0};
        jdbc.query(sql, rs -> {
            int idx = rs.getString(1).equals("video") ? 1 : 0;
            count[idx] = rs.getLong(2);
            avg[idx] = rs.getDouble(3);
        }, userId.toString());
        return new AvgLikesByMedia(
                count[0] == 0 ? null : avg[0], count[0],
                count[1] == 0 ? null : avg[1], count[1]);
    }

    public RankInfo rank(UUID userId) {
        String sql = """
                SELECT avg_e, rnk, total FROM (
                    SELECT author_id, avg_e,
                           RANK() OVER (ORDER BY avg_e DESC) AS rnk,
                           COUNT(*) OVER () AS total
                      FROM (SELECT p.author_id,
                                   AVG((SELECT COUNT(*) FROM likes l WHERE l.post_id = p.id)
                                     + (SELECT COUNT(*) FROM comments c WHERE c.post_id = p.id)) AS avg_e
                              FROM posts p
                             GROUP BY p.author_id)
                ) WHERE author_id = ?
                """;
        List<RankInfo> rows = jdbc.query(sql, (rs, i) -> {
            long rank = rs.getLong(2);
            long total = rs.getLong(3);
            double percent = Math.round(rank * 1000.0 / total) / 10.0;
            return new RankInfo(rs.getDouble(1), rank, total, percent);
        }, userId.toString());
        return rows.isEmpty() ? new RankInfo(null, null, null, null) : rows.get(0);
    }
}