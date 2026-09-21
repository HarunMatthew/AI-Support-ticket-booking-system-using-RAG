package com.example.support.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Maps the JSON object Gemini is instructed to return:
 * { "category": "...", "response": "..." }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeminiResponse {

    private String category;
    private String response;

    public GeminiResponse() {
    }

    public GeminiResponse(String category, String response) {
        this.category = category;
        this.response = response;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }
}
