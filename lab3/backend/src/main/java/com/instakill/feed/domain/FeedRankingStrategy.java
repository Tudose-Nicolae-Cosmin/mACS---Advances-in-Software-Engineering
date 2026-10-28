package com.instakill.feed.domain;

import com.instakill.post.domain.Post;
import com.instakill.post.domain.PostRepository;

import java.util.List;

/**
 * Strategie de sortare a feed-ului.
 * Fiecare algoritm (newest, trending, ...) este o clasa separata care implementeaza interfata.
 * FeedService nu stie nimic despre algoritmi, doar alege strategia potrivita dupa FeedSort.
 */
public interface FeedRankingStrategy {

    /** Tipul de sortare pe care il acopera strategia (cheia folosita de FeedService in map). */
    FeedSort sort();

    /** Aduce din repository postarile pentru pagina ceruta, deja ordonate dupa algoritmul strategiei. */
    List<Post> fetch(PostRepository repository, int page, int size);
}
