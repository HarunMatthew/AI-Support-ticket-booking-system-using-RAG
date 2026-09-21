package com.example.support.service;

import com.example.support.model.SimilarTicket;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Java equivalent of query_engine.py's QueryEngine.
 * Orchestrates the retrieval half of the RAG pipeline:
 *   query -> embedding -> Endee semantic search -> top-K similar tickets
 */
@Service
public class QueryEngine {

    private final EmbeddingService embeddingService;
    private final EndeeVectorStore vectorStore;

    public QueryEngine(EmbeddingService embeddingService, EndeeVectorStore vectorStore) {
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
    }

    public List<SimilarTicket> retrieveSimilarTickets(String query, int topK) {
        float[] queryVector = embeddingService.generateEmbedding(query);
        return vectorStore.search(queryVector, topK);
    }
}
