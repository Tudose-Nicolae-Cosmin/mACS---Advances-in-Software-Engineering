package com.instakill.feed.application;

import com.instakill.feed.domain.FeedSort;
import com.instakill.post.domain.Post;
import com.instakill.post.domain.PostRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.when;

// Testeaza strategia izolat: fara FeedService, doar cu un repository fals
class TrendingFeedRankingStrategyTest {

    private final PostRepository repository = Mockito.mock(PostRepository.class);
    private final TrendingFeedRankingStrategy strategy = new TrendingFeedRankingStrategy();

    @Test
    void handlesTrendingSort() {
        assertThat(strategy.sort()).isEqualTo(FeedSort.TRENDING);
    }

    @Test
    void sortsByLikesThenCommentsThenRecency() {
        Instant now = Instant.now();
        List<Post> posts = List.of(
                post(now.minusSeconds(60), 2, 5),
                post(now.minusSeconds(30), 5, 1),
                post(now.minusSeconds(10), 5, 3)
        );
        when(repository.findLatest(0, 10)).thenReturn(posts);

        List<Post> result = strategy.fetch(repository, 0, 2);

        assertThat(result)
                .hasSize(2)
                .extracting(Post::likeCount, Post::commentCount)
                .containsExactly(
                        tuple(5L, 3L),
                        tuple(5L, 1L)
                );
    }

    @Test
    void usesRecencyWhenLikesAndCommentsAreEqual() {
        Instant now = Instant.now();
        Post older = post(now.minusSeconds(100), 3, 3);
        Post newer = post(now.minusSeconds(5), 3, 3);
        when(repository.findLatest(0, 10)).thenReturn(List.of(older, newer));

        List<Post> result = strategy.fetch(repository, 0, 2);

        assertThat(result).containsExactly(newer, older);
    }

    @Test
    void returnsOnlyRequestedPage() {
        Instant now = Instant.now();
        Post first = post(now, 9, 0);
        Post second = post(now, 8, 0);
        Post third = post(now, 7, 0);
        // pagina 1 cu size 1 => se cere findLatest(0, (1 + 1) * 10)
        when(repository.findLatest(0, 20)).thenReturn(List.of(third, first, second));

        List<Post> result = strategy.fetch(repository, 1, 1);

        assertThat(result).containsExactly(second);
    }

    private Post post(Instant createdAt, long likes, long comments) {
        return new Post(UUID.randomUUID(), UUID.randomUUID(), "content", null, createdAt, createdAt, likes, comments);
    }
}
