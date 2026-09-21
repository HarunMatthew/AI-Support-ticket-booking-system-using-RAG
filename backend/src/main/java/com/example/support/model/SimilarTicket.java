package com.example.support.model;

/**
 * A historical ticket returned by the vector search, together with its
 * similarity score against the customer's query. Equivalent to the Python
 * search() result entries (metadata + score).
 */
public class SimilarTicket {

    private String id;
    private double score;
    private String query;
    private String category;
    private String response;

    public SimilarTicket() {
    }

    public SimilarTicket(String id, double score, String query, String category, String response) {
        this.id = id;
        this.score = score;
        this.query = query;
        this.category = category;
        this.response = response;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
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
