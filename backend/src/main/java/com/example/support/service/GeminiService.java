package com.example.support.service;

import com.example.support.dto.GeminiResponse;
import com.example.support.model.SimilarTicket;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Service responsible for calling Google's Gemini Generative Language API.
 *
 * Uses Spring RestClient directly instead of a Gemini SDK.
 *
 * Includes automatic retry handling for temporary 503 Service Unavailable
 * responses from Gemini.
 */
@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

    private final String apiKey;
    private final String model;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    // Number of additional attempts after the first request fails.
    private static final int MAX_RETRIES = 3;

    public GeminiService(
            @Value("${gemini.api.key:}") String apiKey,
            @Value("${gemini.model:gemini-3.6-flash}") String model,
            ObjectMapper objectMapper) {

        this.apiKey = apiKey;
        this.model = model;
        this.objectMapper = objectMapper;

        this.restClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .build();

        log.info("Gemini model configured: {}", model);

        if (apiKey == null || apiKey.isBlank()
                || apiKey.equals("your_gemini_api_key_here")) {

            log.warn(
                    "Warning: GEMINI_API_KEY is missing or not set. "
                    + "LLM features will use a mock response."
            );
        }
    }

    public GeminiResponse generateResponseAndCategory(
            String userQuery,
            List<SimilarTicket> similarTickets) {

        // If API key is not configured, use mock response.
        if (apiKey == null || apiKey.isBlank()
                || apiKey.equals("your_gemini_api_key_here")) {

            return new GeminiResponse(
                    "General Inquiry",
                    "This is an AI-generated mock response since the Gemini API Key is not configured."
            );
        }

        String prompt = buildPrompt(userQuery, similarTickets);

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of(
                                "parts", List.of(
                                        Map.of("text", prompt)
                                )
                        )
                ),
                "generationConfig", Map.of(
                        "temperature", 0.2,
                        "responseMimeType", "application/json"
                )
        );

        /*
         * Retry Gemini API when it temporarily returns HTTP 503.
         *
         * Attempt 1 -> wait 1 second
         * Attempt 2 -> wait 2 seconds
         * Attempt 3 -> wait 4 seconds
         * Attempt 4 -> return error
         */
        for (int attempt = 1; attempt <= MAX_RETRIES + 1; attempt++) {

            try {

                log.info(
                        "Calling Gemini API using model '{}' - attempt {}/{}",
                        model,
                        attempt,
                        MAX_RETRIES + 1
                );

                Map<?, ?> raw = restClient.post()
                        .uri(
                                "/v1beta/models/{model}:generateContent?key={key}",
                                model,
                                apiKey
                        )
                        .body(requestBody)
                        .retrieve()
                        .body(Map.class);

                String rawText = extractText(raw);

                String cleaned = rawText
                        .replace("```json", "")
                        .replace("```", "")
                        .trim();

                GeminiResponse response =
                        objectMapper.readValue(cleaned, GeminiResponse.class);

                log.info("Gemini response generated successfully.");

                return response;

            } catch (HttpServerErrorException.ServiceUnavailable e) {

                log.warn(
                        "Gemini returned 503 Service Unavailable. Attempt {}/{}",
                        attempt,
                        MAX_RETRIES + 1
                );

                // If this was the final attempt, stop retrying.
                if (attempt > MAX_RETRIES) {

                    log.error(
                            "Gemini is still unavailable after {} attempts.",
                            MAX_RETRIES + 1,
                            e
                    );

                    return new GeminiResponse(
                            "Error",
                            "The AI service is temporarily unavailable. Please try again in a moment."
                    );
                }

                // Exponential backoff:
                // 1 second -> 2 seconds -> 4 seconds
                long waitTime = (long) Math.pow(2, attempt - 1) * 1000;

                log.info(
                        "Waiting {} ms before retrying Gemini...",
                        waitTime
                );

                try {
                    Thread.sleep(waitTime);

                } catch (InterruptedException interruptedException) {

                    Thread.currentThread().interrupt();

                    log.error(
                            "Gemini retry interrupted.",
                            interruptedException
                    );

                    return new GeminiResponse(
                            "Error",
                            "The AI request was interrupted. Please try again."
                    );
                }

            } catch (Exception e) {

                /*
                 * Other errors such as:
                 * - invalid API key
                 * - malformed request
                 * - JSON parsing error
                 * - 400/401/404 errors
                 *
                 * are not retried here.
                 */
                log.error(
                        "Error calling Gemini LLM",
                        e
                );

                return new GeminiResponse(
                        "Error",
                        "We encountered an error generating the AI response. Please check server logs."
                );
            }
        }

        // Safety fallback.
        return new GeminiResponse(
                "Error",
                "Unable to generate an AI response."
        );
    }

    @SuppressWarnings("unchecked")
    private String extractText(Map<?, ?> raw) {

        List<Map<String, Object>> candidates =
                (List<Map<String, Object>>) raw.get("candidates");

        if (candidates == null || candidates.isEmpty()) {
            throw new IllegalStateException(
                    "Gemini response does not contain any candidates."
            );
        }

        Map<String, Object> content =
                (Map<String, Object>) candidates.get(0).get("content");

        if (content == null) {
            throw new IllegalStateException(
                    "Gemini response does not contain content."
            );
        }

        List<Map<String, Object>> parts =
                (List<Map<String, Object>>) content.get("parts");

        if (parts == null || parts.isEmpty()) {
            throw new IllegalStateException(
                    "Gemini response does not contain any parts."
            );
        }

        return String.valueOf(parts.get(0).get("text"));
    }

    private String buildPrompt(
            String userQuery,
            List<SimilarTicket> similarTickets) {

        StringBuilder context = new StringBuilder();

        int i = 1;

        for (SimilarTicket match : similarTickets) {

            context.append("\n[Ticket ")
                    .append(i++)
                    .append("]\n")
                    .append("Category: ")
                    .append(match.getCategory())
                    .append("\n")
                    .append("Past Query: ")
                    .append(match.getQuery())
                    .append("\n")
                    .append("Past Resolution: ")
                    .append(match.getResponse())
                    .append("\n");
        }

        String contextText =
                context.length() == 0
                        ? "No relevant past tickets found."
                        : context.toString();

        return """
                You are an expert AI Customer Support Agent. You need to analyze a new customer query.
                Below you are provided with some similar historical customer support tickets (from a vector database context) to help you formulate a consistent and professional response.

                Context (Past Similar Tickets):
                %s

                New Customer Query:
                "%s"

                Task:
                1. Predict the 'Category' of this new query based on its content (e.g., Billing, Technical, Account, Delivery, or another relevant brief noun).
                2. Generate a professional and helpful 'Response' resolving the query. If the context has a standard procedure, leverage it. If not, write a polite holding message or standard troubleshooting steps.

                Respond strictly with valid JSON only in the following format (no markdown formatting, no code blocks):
                {
                  "category": "Predicted Category Here",
                  "response": "Your full professional response here"
                }
                """.formatted(contextText, userQuery);
    }
}