package com.instakill.infrastructure.event;

import com.instakill.interaction.domain.DomainEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Null Object: implementarea care nu face nimic.
 * Asa InteractionService primeste mereu un publisher valid si nu are nevoie de verificari de null.
 */
@Component
public class NoOpDomainEventPublisher implements DomainEventPublisher {

    @Override
    public void publish(Object event) {
        // intentionat gol
    }
}
