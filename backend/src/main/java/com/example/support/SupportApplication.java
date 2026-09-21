package com.example.support;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AI Customer Support Intelligence System.
 *
 * RAG pipeline:
 *   Customer Query -> EmbeddingService -> EndeeVectorStore (semantic search)
 *   -> Top-K historical tickets -> GeminiService (category + response) -> Angular UI
 */
@SpringBootApplication
public class SupportApplication {

    public static void main(String[] args) {
        SpringApplication.run(SupportApplication.class, args);
    }
}
