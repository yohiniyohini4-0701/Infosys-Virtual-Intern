package com.example.governmentsubsidy.dto.assistant;

import java.util.List;
import java.util.Map;

public class AssistantResponse {

    private String answer;
    private String category;
    private List<Map<String, Object>> relatedData;

    public AssistantResponse() {}

    public AssistantResponse(String answer, String category) {
        this.answer = answer;
        this.category = category;
    }

    public AssistantResponse(String answer, String category, List<Map<String, Object>> relatedData) {
        this.answer = answer;
        this.category = category;
        this.relatedData = relatedData;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<Map<String, Object>> getRelatedData() {
        return relatedData;
    }

    public void setRelatedData(List<Map<String, Object>> relatedData) {
        this.relatedData = relatedData;
    }
}
