package com.example.governmentsubsidy.controller;

import com.example.governmentsubsidy.dto.assistant.AssistantRequest;
import com.example.governmentsubsidy.dto.assistant.AssistantResponse;
import com.example.governmentsubsidy.dto.common.ApiResponse;
import com.example.governmentsubsidy.entity.User;
import com.example.governmentsubsidy.service.AuthService;
import com.example.governmentsubsidy.service.SmartAssistantService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/assistant")
public class SmartAssistantController {

    private final SmartAssistantService assistantService;
    private final AuthService authService;

    public SmartAssistantController(SmartAssistantService assistantService, AuthService authService) {
        this.assistantService = assistantService;
        this.authService = authService;
    }

    @PostMapping("/query")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AssistantResponse>> queryAssistant(@Valid @RequestBody AssistantRequest request) {
        User currentUser = null;
        try {
            currentUser = authService.getCurrentUser();
        } catch (Exception ignored) {
            // Unauthenticated or anonymous fallback
        }

        AssistantResponse response = assistantService.answerQuery(request.getQuery(), currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
