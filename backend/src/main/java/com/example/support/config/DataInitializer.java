package com.example.support.config;

import com.example.support.model.Ticket;
import com.example.support.service.DataLoaderService;
import com.example.support.service.EmbeddingService;
import com.example.support.service.EndeeVectorStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Runs once when the Spring context is fully up (ApplicationRunner is the
 * Spring Boot 3.x-idiomatic replacement for doing startup work, preferred
 * here over @PostConstruct because all beans, including the embedding model,
 * are guaranteed to be fully initialized by the time this runs).
 *
 * Flow:
 *   Load sample_tickets.json -> generate embeddings -> insert into Endee/local store
 *
 * Duplicate-insert avoidance: tickets carry stable IDs (TKT-001 ...). Since the
 * in-memory mock store is rebuilt fresh on every restart there is nothing to
 * dedupe there. Against a real Endee deployment, insert is expected to behave
 * as an upsert keyed by id (standard behavior for vector DBs), so re-running
 * this on every startup simply re-upserts the same points rather than
 * duplicating them - if your Endee deployment does NOT upsert by id, guard
 * this with a "collection already populated" check before calling insertTickets.
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final DataLoaderService dataLoaderService;
    private final EmbeddingService embeddingService;
    private final EndeeVectorStore vectorStore;

    public DataInitializer(DataLoaderService dataLoaderService,
                            EmbeddingService embeddingService,
                            EndeeVectorStore vectorStore) {
        this.dataLoaderService = dataLoaderService;
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Loading historical tickets...");
        List<Ticket> tickets = dataLoaderService.getTicketsForInsert();

        if (tickets.isEmpty()) {
            log.warn("No tickets loaded - vector store will start empty.");
            return;
        }

        List<String> queries = tickets.stream().map(Ticket::getQuery).collect(Collectors.toList());
        List<float[]> embeddings = embeddingService.generateEmbeddings(queries);

        vectorStore.insertTickets(tickets, embeddings);
        log.info("System Ready! {} tickets indexed.", tickets.size());
    }
}
