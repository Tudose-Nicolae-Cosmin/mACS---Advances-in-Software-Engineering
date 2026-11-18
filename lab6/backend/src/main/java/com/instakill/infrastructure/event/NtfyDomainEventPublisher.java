package com.instakill.infrastructure.event;

import com.instakill.interaction.domain.DomainEventPublisher;
import com.instakill.post.domain.PostCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;


@Primary
@Component
public class NtfyDomainEventPublisher implements DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(NtfyDomainEventPublisher.class);

    private final String baseUrl;
    private final String topic;
    private final HttpClient client = HttpClient.newHttpClient();

    public NtfyDomainEventPublisher(@Value("${ntfy.url:https://ntfy.sh}") String baseUrl,
                                    @Value("${ntfy.topic:}") String topic) {
        this.baseUrl = baseUrl;
        this.topic = topic;
    }

    @Override
    public void publish(Object event) {
        if (topic.isBlank() || !(event instanceof PostCreatedEvent created)) {
            return;
        }

        String message = "User: " + created.username()
                + "\nPost ID: " + created.postId()
                + "\nTime: " + created.createdAt();

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/" + topic))
                    .header("Title", "New post on Instakill")
                    .POST(HttpRequest.BodyPublishers.ofString(message))
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .exceptionally(ex -> {
                        log.warn("Could not send notification: {}", ex.getMessage());
                        return null;
                    });
        } catch (RuntimeException ex) {
            log.warn("Could not send notification: {}", ex.getMessage());
        }
    }
}