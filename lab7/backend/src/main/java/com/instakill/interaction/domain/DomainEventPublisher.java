package com.instakill.interaction.domain;

/**
 * Port prin care serviciile trimit evenimente de domeniu (ex: LikeCreatedEvent).
 * Implementarile pot loga, trimite in Kafka, publica asincron etc.,
 * iar serviciul care publica nu trebuie sa stie care dintre ele este folosita.
 */
public interface DomainEventPublisher {

    void publish(Object event);
}
