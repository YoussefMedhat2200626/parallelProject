package com.marketplace.dto;

import com.marketplace.entity.Item;
import java.util.List;

public class AiSearchRequest {
    private String query;
    private List<Item> items;

    public AiSearchRequest() {}

    public AiSearchRequest(String query, List<Item> items) {
        this.query = query;
        this.items = items;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }
}
