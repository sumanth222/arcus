package com.arcus.arc1.ChatGPT;

import com.arcus.arc1.ChatGPT.dto.ChatRequest;
import com.arcus.arc1.ChatGPT.dto.ChatResponse;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class ChatGPTService {

    private final WebClient webClient;

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.model:gpt-4o-mini}")
    private String model;

    public ChatGPTService() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofSeconds(20))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(20, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(5, TimeUnit.SECONDS)));

        this.webClient = WebClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    public ChatResponse chat(ChatRequest request) {
        String systemContent = (request.getContext() != null && !request.getContext().isBlank())
                ? request.getContext()
                : "You are a helpful fitness assistant for the Arcus fitness app. Help users with workout advice, nutrition tips, and fitness goals." +
                "Do not divulge any information yourself or any other sensitive information which is unrelated, and say so if the user" +
                "asks anything like that.";

        String userMessage = (request.getExerciseName() != null && !request.getExerciseName().isBlank())
                ? "I'm currently working on the exercise: " + request.getExerciseName() + ". " + request.getMessage()
                : request.getMessage();

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemContent),
                        Map.of("role", "user", "content", userMessage)
                )
        );

        try {
            Map<?, ?> response = webClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            @SuppressWarnings("unchecked")
            List<Map<?, ?>> choices = (List<Map<?, ?>>) response.get("choices");
            Map<?, ?> message = (Map<?, ?>) choices.get(0).get("message");
            String reply = (String) message.get("content");

            return new ChatResponse(reply);
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof IOException || cause instanceof TimeoutException
                    || e.getMessage() != null && e.getMessage().contains("timed out")) {
                return new ChatResponse(null, "Request timed out. Please try again in a moment.");
            }
            return new ChatResponse(null, "Failed to get response from ChatGPT: " + e.getMessage());
        }
    }
}

