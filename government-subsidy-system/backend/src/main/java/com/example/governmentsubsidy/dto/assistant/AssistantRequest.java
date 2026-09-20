package com.example.governmentsubsidy.dto.assistant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AssistantRequest {

    @NotBlank(message = "Query text is required")
    @Size(max = 500, message = "Query must not exceed 500 characters")
    private String query;

    public AssistantRequest() {}

    public AssistantRequest(String query) {
        this.query = query;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }
}
