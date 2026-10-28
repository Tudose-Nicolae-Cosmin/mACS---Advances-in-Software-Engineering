package com.instakill.infrastructure.event;

import com.instakill.interaction.domain.LikeCreatedEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;

class NoOpDomainEventPublisherTest {

    private final NoOpDomainEventPublisher publisher = new NoOpDomainEventPublisher();

    @Test
    void publishDoesNothingAndDoesNotThrow() {
        LikeCreatedEvent event = new LikeCreatedEvent(UUID.randomUUID(), UUID.randomUUID(), Instant.now());

        assertThatCode(() -> publisher.publish(event)).doesNotThrowAnyException();
    }
}
