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
//spring-boot --> load historical tickets
