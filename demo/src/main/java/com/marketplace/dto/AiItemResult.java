package com.marketplace.dto;

public class AiItemResult {
    private Long id;
    private String name;
    private String brand;
    private Double price;
    private Double relevance_score;
    private String reason;

    public AiItemResult() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Double getRelevance_score() {
        return relevance_score;
    }

    public void setRelevance_score(Double relevance_score) {
        this.relevance_score = relevance_score;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
