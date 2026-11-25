package com.instakill.funnyfy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.instakill.common.error.ServiceUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

@Service
public class FunnyfyService {

    private static final String SYSTEM_PROMPT = "Always reply with the user's message, but rewritten in a more funny style.";
    private static final int MAX_LENGTH = 1000;

    private final HttpClient http;
    private final ObjectMapper mapper;
    private final String url;
    private final String model;
    private final Duration timeout;

    public FunnyfyService(ObjectMapper mapper,
                          @Value("${llm.url}") String url,
                          @Value("${llm.model}") String model,
                          @Value("${llm.timeout-seconds}") long timeoutSeconds) {
        this.mapper = mapper;
        this.url = url;
        this.model = model;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    public String funnyfy(String text) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(buildRequestBody(text)))
                .build();

        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (HttpTimeoutException e) {
            throw new ServiceUnavailableException("The AI model took too long to answer. Please try again.", e);
        } catch (IOException e) {
            throw new ServiceUnavailableException("Cannot reach the local AI model. Is LM Studio running?", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceUnavailableException("The request was interrupted.", e);
        }

        if (response.statusCode() != 200) {
            throw new ServiceUnavailableException("The AI model returned an error (HTTP " + response.statusCode() + ").");
        }
        return extractAnswer(response.body());
    }

    private String buildRequestBody(String text) {
        ObjectNode root = mapper.createObjectNode();
        root.put("model", model);
        ArrayNode messages = root.putArray("messages");
        messages.addObject().put("role", "system").put("content", SYSTEM_PROMPT);
        messages.addObject().put("role", "user").put("content", text);
        root.put("temperature", 0.7);
        root.put("max_tokens", 256);
        root.put("stream", false);
        return root.toString();
    }

    private String extractAnswer(String responseBody) {
        try {
            JsonNode root = mapper.readTree(responseBody);
            String answer = root.path("choices").path(0).path("message").path("content").asText("").trim();
            if (answer.isEmpty()) {
                throw new ServiceUnavailableException("The AI model returned an empty answer.");
            }
            return answer.length() > MAX_LENGTH ? answer.substring(0, MAX_LENGTH) : answer;
        } catch (JsonProcessingException e) {
            throw new ServiceUnavailableException("The AI model returned an invalid answer.", e);
        }
    }
}