package com.example.support.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;


public class SupportRequest {

    @NotBlank(message = "query must not be blank")
    private String query;

    @Min(value = 1, message = "topK must be between 1 and 5")
    @Max(value = 5, message = "topK must be between 1 and 5")
    private int topK = 3;

    public SupportRequest() {
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public int getTopK() {
        return topK;
    }

    public void setTopK(int topK) {
        this.topK = topK;
    }
}
//repest the input coming from the froendend
