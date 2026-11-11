package com.instakill.post.domain;

import java.time.Instant;
import java.util.UUID;

public record PostCreatedEvent(UUID postId, String username, Instant createdAt) {
}