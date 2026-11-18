package com.instakill.feed.application;

import com.instakill.feed.domain.FeedRankingStrategy;
import com.instakill.feed.domain.FeedSort;
import com.instakill.post.domain.Post;
import com.instakill.post.domain.PostRepository;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Nu contine nicio logica de sortare: alege strategia potrivita si ii delega munca.
 * Pentru un algoritm nou se adauga doar o clasa @Component care implementeaza FeedRankingStrategy
 * (plus o valoare noua in FeedSort), fara nicio modificare aici.
 */
@Service
public class FeedService {

    private static final FeedSort DEFAULT_SORT = FeedSort.NEW;

    private final PostRepository posts;

    // FeedSort -> strategia care il implementeaza
    private final Map<FeedSort, FeedRankingStrategy> strategies = new EnumMap<>(FeedSort.class);

    // Spring injecteaza automat toate bean-urile care implementeaza FeedRankingStrategy
    public FeedService(PostRepository posts, List<FeedRankingStrategy> rankingStrategies) {
        this.posts = posts;
        for (FeedRankingStrategy strategy : rankingStrategies) {
            FeedRankingStrategy old = strategies.put(strategy.sort(), strategy);
            if (old != null) {
                // Doua strategii pentru acelasi FeedSort ar fi ambigue, deci oprim aplicatia la pornire
                throw new IllegalStateException("Exista mai multe strategii pentru " + strategy.sort());
            }
        }
    }

    public List<Post> listFeed(FeedSort sort, int page, int size) {
        FeedSort requested = (sort == null) ? DEFAULT_SORT : sort;

        FeedRankingStrategy strategy = strategies.get(requested);
        if (strategy == null) {
            throw new IllegalArgumentException("Nu exista strategie de sortare pentru " + requested);
        }
        return strategy.fetch(posts, page, size);
    }
}
