package com.example.support.model;

import java.util.List;
\
public class SupportResponse {

    private String category;
    private String response;
    private double searchTime;
    private List<SimilarTicket> similarTickets;

    public SupportResponse() {
    }

    public SupportResponse(String category, String response, double searchTime, List<SimilarTicket> similarTickets) {
        this.category = category;
        this.response = response;
        this.searchTime = searchTime;
        this.similarTickets = similarTickets;
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

    public double getSearchTime() {
        return searchTime;
    }

    public void setSearchTime(double searchTime) {
        this.searchTime = searchTime;
    }

    public List<SimilarTicket> getSimilarTickets() {
        return similarTickets;
    }

    public void setSimilarTickets(List<SimilarTicket> similarTickets) {
        this.similarTickets = similarTickets;
    }
}

// it will reponse back to froendend 
