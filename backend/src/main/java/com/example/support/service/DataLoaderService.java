package com.example.support.service;

import com.example.support.model.Ticket;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

/**
 * Java equivalent of data_loader.py:
 *   load_sample_tickets()  -> loadSampleTickets()
 *   get_tickets_for_insert() -> getTicketsForInsert()
 *
 * The Python version reads a JSON file off disk with a plain filepath;
 * here we read it from the classpath (src/main/resources/sample_tickets.json)
 * which is the idiomatic Spring Boot way of shipping bundled resource data.
 */
@Service
public class DataLoaderService {

    private static final Logger log = LoggerFactory.getLogger(DataLoaderService.class);
    private static final String RESOURCE_PATH = "sample_tickets.json";

    private final ObjectMapper objectMapper;

    public DataLoaderService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Loads sample tickets bundled at src/main/resources/sample_tickets.json.
     */
    public List<Ticket> loadSampleTickets() {
        ClassPathResource resource = new ClassPathResource(RESOURCE_PATH);
        if (!resource.exists()) {
            log.warn("Warning: Data file not found at {}", RESOURCE_PATH);
            return Collections.emptyList();
        }

        try (InputStream is = resource.getInputStream()) {
            return objectMapper.readValue(is, new TypeReference<List<Ticket>>() {
            });
        } catch (IOException e) {
            log.warn("Warning: Failed to parse {}: {}", RESOURCE_PATH, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Returns tickets in a form ready to be embedded + inserted into the vector store.
     * Mirrors get_tickets_for_insert() in the Python source, which is currently just
     * a pass-through over the loaded data.
     */
    public List<Ticket> getTicketsForInsert() {
        return loadSampleTickets();
    }
}
