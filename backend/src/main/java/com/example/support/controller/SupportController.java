package com.example.support.controller;

import com.example.support.dto.GeminiResponse;
import com.example.support.model.SimilarTicket;
import com.example.support.model.SupportRequest;
import com.example.support.model.SupportResponse;
import com.example.support.service.GeminiService;
import com.example.support.service.QueryEngine;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/support")
public class SupportController {

    private final QueryEngine queryEngine;
    private final GeminiService geminiService;

    public SupportController(QueryEngine queryEngine, GeminiService geminiService) {
        this.queryEngine = queryEngine;
        this.geminiService = geminiService;
    }

    @PostMapping("/analyze")
    public SupportResponse analyze(@Valid @RequestBody SupportRequest request) {
        long start = System.nanoTime();
        List<SimilarTicket> similarTickets =
                queryEngine.retrieveSimilarTickets(request.getQuery(), request.getTopK());
        double searchTime = (System.nanoTime() - start) / 1_000_000_000.0;

        GeminiResponse llmResult =
                geminiService.generateResponseAndCategory(request.getQuery(), similarTickets);

        return new SupportResponse(
                llmResult.getCategory(),
                llmResult.getResponse(),
                searchTime,
                similarTickets
        );
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
