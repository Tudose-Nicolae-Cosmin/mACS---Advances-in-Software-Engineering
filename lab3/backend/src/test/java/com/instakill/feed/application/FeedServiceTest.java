package com.instakill.feed.application;

import com.instakill.feed.domain.FeedRankingStrategy;
import com.instakill.feed.domain.FeedSort;
import com.instakill.post.domain.Post;
import com.instakill.post.domain.PostRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// FeedService nu are logica de sortare, deci testam doar ca alege si apeleaza strategia corecta
class FeedServiceTest {

    private final PostRepository repository = Mockito.mock(PostRepository.class);

    private final FeedRankingStrategy newest = strategyFor(FeedSort.NEW);
    private final FeedRankingStrategy trending = strategyFor(FeedSort.TRENDING);

    private final FeedService service = new FeedService(repository, List.of(newest, trending));

    @Test
    void delegatesToNewestStrategy() {
        List<Post> expected = List.of(post());
        when(newest.fetch(repository, 0, 5)).thenReturn(expected);

        List<Post> result = service.listFeed(FeedSort.NEW, 0, 5);

        assertThat(result).isEqualTo(expected);
        verifyNoInteractions(trending);
    }

    @Test
    void delegatesToTrendingStrategy() {
        List<Post> expected = List.of(post());
        when(trending.fetch(repository, 1, 3)).thenReturn(expected);

        List<Post> result = service.listFeed(FeedSort.TRENDING, 1, 3);

        assertThat(result).isEqualTo(expected);
        verifyNoInteractions(newest);
    }

    @Test
    void defaultsToNewWhenSortIsNull() {
        List<Post> expected = List.of(post());
        when(newest.fetch(repository, 0, 5)).thenReturn(expected);

        List<Post> result = service.listFeed(null, 0, 5);

        assertThat(result).isEqualTo(expected);
        verify(newest).fetch(repository, 0, 5);
    }

    @Test
    void usesAnyStrategyThatIsProvided() {
        // O strategie noua (aici un fals scris in test) inlocuieste TRENDING fara nicio modificare in FeedService
        List<Post> custom = List.of(post());
        FeedRankingStrategy other = new FeedRankingStrategy() {
            @Override
            public FeedSort sort() {
                return FeedSort.TRENDING;
            }

            @Override
            public List<Post> fetch(PostRepository repo, int page, int size) {
                return custom;
            }
        };
        FeedService customService = new FeedService(repository, List.of(newest, other));

        assertThat(customService.listFeed(FeedSort.TRENDING, 0, 5)).isEqualTo(custom);
    }

    @Test
    void failsWhenNoStrategyForRequestedSort() {
        FeedService onlyNewest = new FeedService(repository, List.of(newest));

        assertThatThrownBy(() -> onlyNewest.listFeed(FeedSort.TRENDING, 0, 5))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void failsAtStartupWhenTwoStrategiesShareTheSameSort() {
        FeedRankingStrategy duplicate = strategyFor(FeedSort.NEW);

        assertThatThrownBy(() -> new FeedService(repository, List.of(newest, duplicate)))
                .isInstanceOf(IllegalStateException.class);
    }

    private FeedRankingStrategy strategyFor(FeedSort sort) {
        FeedRankingStrategy strategy = mock(FeedRankingStrategy.class);
        when(strategy.sort()).thenReturn(sort);
        return strategy;
    }

    private Post post() {
        Instant now = Instant.now();
        return new Post(UUID.randomUUID(), UUID.randomUUID(), "content", null, now, now, 0, 0);
    }
}
