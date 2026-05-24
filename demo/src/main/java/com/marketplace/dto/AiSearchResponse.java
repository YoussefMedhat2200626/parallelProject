package com.marketplace.dto;

import java.util.List;

public class AiSearchResponse {
    private List<AiItemResult> results;
    private String search_summary;

    public AiSearchResponse() {}

    public List<AiItemResult> getResults() {
        return results;
    }

    public void setResults(List<AiItemResult> results) {
        this.results = results;
    }

    public String getSearch_summary() {
        return search_summary;
    }

    public void setSearch_summary(String search_summary) {
        this.search_summary = search_summary;
    }
}
