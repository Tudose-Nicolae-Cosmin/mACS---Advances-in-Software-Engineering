package com.instakill.feed.domain;

public enum FeedSort {
    NEW,
    TRENDING;

    // Cauta valoarea dupa nume (fara sa conteze literele mari/mici); orice altceva da NEW.
    // Fiind generic, o valoare noua in enum nu mai cere modificari aici.
    public static FeedSort from(String value) {
        if (value != null) {
            for (FeedSort sort : values()) {
                if (sort.name().equalsIgnoreCase(value)) {
                    return sort;
                }
            }
        }
        return NEW;
    }
}
