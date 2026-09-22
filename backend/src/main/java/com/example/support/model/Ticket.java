package com.example.support.model;
public class Ticket {

    private String id;
    private String query;
    private String category;
    private String response;

    public Ticket() {
    }

    public Ticket(String id, String query, String category, String response) {
        this.id = id;
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
//historical customer support ticket 
