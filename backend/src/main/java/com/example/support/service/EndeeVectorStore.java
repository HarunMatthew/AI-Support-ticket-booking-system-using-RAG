package com.example.support.service;

import com.example.support.model.SimilarTicket;
import com.example.support.model.Ticket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Java equivalent of vector_store.py's EndeeVectorStore.
 *
 * IMPORTANT / AMBIGUITY CALLED OUT (per the "do not silently invent" rule):
 * "Endee" has no publicly documented official Java SDK, and the Python source
 * itself already wraps every Endee call in try/except and falls back to an
 * in-memory MockCollection with cosine similarity whenever the SDK call
 * fails. That means the Python app, as written, is *designed* to run in
 * local/demo mode unless a real Endee endpoint + token are supplied.
 *
 * This Java version preserves that exact behavior:
 *  - endee.mock-mode=true  (default) -> use the in-memory cosine-similarity
 *    store below, so the project runs out of the box with no external
 *    service.
 *  - endee.mock-mode=false -> call a real Endee HTTP endpoint via Spring's
 *    RestClient, using the same insert/search request shapes described in
 *    the Python client. If your real Endee deployment uses a different REST
 *    contract, adjust insertViaRestApi()/searchViaRestApi() accordingly -
 *    the method boundaries are kept intentionally small for that purpose.
 */
@Service
public class EndeeVectorStore {

    private static final Logger log = LoggerFactory.getLogger(EndeeVectorStore.class);

    private final boolean mockMode;
    private final String collectionName;
    private final RestClient restClient;

    // In-memory fallback store: one entry per ticket (vector + metadata)
    private final List<StoredVector> localStore = new CopyOnWriteArrayList<>();

    public EndeeVectorStore(
            @Value("${endee.mock-mode:true}") boolean mockMode,
            @Value("${endee.collection:support_tickets}") String collectionName,
            @Value("${endee.base-url:http://localhost:9000}") String baseUrl,
            @Value("${endee.api.token:}") String apiToken) {
        this.mockMode = mockMode;
        this.collectionName = collectionName;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiToken)
                .build();

        log.info("EndeeVectorStore initialized (mockMode={}, collection={})", mockMode, collectionName);
    }

    /**
     * Inserts tickets + their embeddings into the vector store.
     */
    public void insertTickets(List<Ticket> tickets, List<float[]> embeddings) {
        if (tickets == null || embeddings == null || tickets.isEmpty() || embeddings.isEmpty()) {
            return;
        }

        if (mockMode) {
            for (int i = 0; i < tickets.size(); i++) {
                localStore.add(new StoredVector(tickets.get(i), embeddings.get(i)));
            }
            log.info("Inserted {} tickets into local/mock vector store.", tickets.size());
            return;
        }

        try {
            insertViaRestApi(tickets, embeddings);
        } catch (Exception e) {
            log.warn("Error inserting tickets into Endee ({}). Falling back to local store for this run.",
                    e.getMessage());
            for (int i = 0; i < tickets.size(); i++) {
                localStore.add(new StoredVector(tickets.get(i), embeddings.get(i)));
            }
        }
    }

    /**
     * Searches for the topK most similar tickets to the given query vector.
     */
    public List<SimilarTicket> search(float[] queryVector, int topK) {
        if (mockMode) {
            return searchLocal(queryVector, topK);
        }

        try {
            return searchViaRestApi(queryVector, topK);
        } catch (Exception e) {
            log.warn("Endee search failed ({}). Falling back to local cosine-similarity search.", e.getMessage());
            return searchLocal(queryVector, topK);
        }
    }

    // ------------------------------------------------------------------
    // Local / mock mode: cosine similarity over an in-memory list.
    // Mirrors the Python MockCollection.search() fallback exactly.
    // ------------------------------------------------------------------
    private List<SimilarTicket> searchLocal(float[] queryVector, int topK) {
        return localStore.stream()
                .map(sv -> {
                    double score = cosineSimilarity(queryVector, sv.embedding());
                    Ticket t = sv.ticket();
                    return new SimilarTicket(t.getId(), score, t.getQuery(), t.getCategory(), t.getResponse());
                })
                .sorted(Comparator.comparingDouble(SimilarTicket::getScore).reversed())
                .limit(topK)
                .toList();
    }

    private double cosineSimilarity(float[] a, float[] b) {
        double dot = 0.0, normA = 0.0, normB = 0.0;
        int len = Math.min(a.length, b.length);
        for (int i = 0; i < len; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    // ------------------------------------------------------------------
    // Real Endee REST integration.
    // Adjust the request/response shapes here if your Endee deployment's
    // API contract differs from what is assumed below.
    // ------------------------------------------------------------------
    private void insertViaRestApi(List<Ticket> tickets, List<float[]> embeddings) {
        List<Map<String, Object>> points = new ArrayList<>();
        for (int i = 0; i < tickets.size(); i++) {
            Ticket t = tickets.get(i);
            points.add(Map.of(
                    "id", t.getId(),
                    "vector", embeddings.get(i),
                    "payload", Map.of(
                            "query_text", t.getQuery(),
                            "category", t.getCategory(),
                            "response", t.getResponse()
                    )
            ));
        }

        restClient.post()
                .uri("/collections/{name}/insert", collectionName)
                .body(Map.of("points", points))
                .retrieve()
                .toBodilessEntity();
    }

    @SuppressWarnings("unchecked")
    private List<SimilarTicket> searchViaRestApi(float[] queryVector, int topK) {
        Map<String, Object> body = Map.of(
                "query_vector", queryVector,
                "top_k", topK
        );

        Map<String, Object> raw = restClient.post()
                .uri("/collections/{name}/search", collectionName)
                .body(body)
                .retrieve()
                .body(Map.class);

        List<Map<String, Object>> results = (List<Map<String, Object>>) raw.getOrDefault("results", List.of());
        List<SimilarTicket> matches = new ArrayList<>();
        for (Map<String, Object> r : results) {
            Map<String, Object> payload = (Map<String, Object>) r.getOrDefault("payload", Map.of());
            matches.add(new SimilarTicket(
                    String.valueOf(r.get("id")),
                    ((Number) r.getOrDefault("score", 0.0)).doubleValue(),
                    String.valueOf(payload.getOrDefault("query_text", "")),
                    String.valueOf(payload.getOrDefault("category", "")),
                    String.valueOf(payload.getOrDefault("response", ""))
            ));
        }
        return matches;
    }

    private record StoredVector(Ticket ticket, float[] embedding) {
    }
}
