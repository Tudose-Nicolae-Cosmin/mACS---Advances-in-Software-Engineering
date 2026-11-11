package com.instakill.feed.application;

import com.instakill.feed.domain.FeedRankingStrategy;
import com.instakill.feed.domain.FeedSort;
import com.instakill.post.domain.Post;
import com.instakill.post.domain.PostRepository;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Postarile cu cele mai multe like-uri primele.
 * La egalitate decid comentariile, iar apoi data crearii (cele mai noi primele).
 */
@Component
public class TrendingFeedRankingStrategy implements FeedRankingStrategy {

    // Cate postari aducem minim din repository, ca sa avem din ce alege cand sortam
    private static final int MIN_CANDIDATES = 10;

    @Override
    public FeedSort sort() {
        return FeedSort.TRENDING;
    }

    @Override
    public List<Post> fetch(PostRepository repository, int page, int size) {
        // Aducem mai multe postari decat cerem, pentru ca sortarea dupa like-uri se face aici, in memorie
        int fetchSize = Math.max(size * 2, MIN_CANDIDATES);
        List<Post> candidates = repository.findLatest(0, (page + 1) * fetchSize);

        // like-uri descrescator, apoi comentarii descrescator, apoi data descrescator
        Comparator<Post> comparator = Comparator
                .comparingLong(Post::likeCount).reversed()
                .thenComparing(Comparator.comparingLong(Post::commentCount).reversed())
                .thenComparing(Comparator.comparing(Post::createdAt).reversed());

        // Sarim peste paginile anterioare si luam doar pagina ceruta
        return candidates.stream()
                .sorted(comparator)
                .skip((long) page * size)
                .limit(size)
                .toList();
    }
}
