package com.nebulabyte.agent.apollo_backend.Controllers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nebulabyte.agent.apollo_backend.Dto.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api/v1/sessions")
public class SessionController {

    private final ChatMemory chatMemory;

    public SessionController(ChatMemory chatMemory) {
        this.chatMemory = chatMemory;
    }

    @PostMapping
    @Operation(
        summary = "Create a new chat session",
        description = "Creates a new conversation session and returns a unique conversationId"
    )
    public ResponseEntity<ApiResponse<Map<String, String>>> createSession() {
        String conversationId = UUID.randomUUID().toString();
        return ResponseEntity.ok(ApiResponse.<Map<String, String>>builder()
                .timestamp(LocalDateTime.now())
                .success(true)
                .message("Session created successfully")
                .data(Map.of("conversationId", conversationId))
                .build());
    }

    @GetMapping("/{conversationId}/history")
    @Operation(
        summary = "Get chat history for a session",
        description = "Returns all messages for a given conversationId"
    )
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getHistory(
            @PathVariable String conversationId) {

        List<Map<String, String>> history = chatMemory.get(conversationId)
                .stream()
                .map(msg -> Map.of(
                        "role", msg.getMessageType().getValue(),
                        "content", msg.getText()))
                .toList();

        return ResponseEntity.ok(ApiResponse.<List<Map<String, String>>>builder()
                .timestamp(LocalDateTime.now())
                .success(true)
                .message(null)
                .data(history)
                .build());
    }

    @DeleteMapping("/{conversationId}")
    @Operation(
        summary = "Delete a chat session",
        description = "Clears all messages for a given conversationId"
    )
    public ResponseEntity<ApiResponse<Void>> deleteSession(
            @PathVariable String conversationId) {

        chatMemory.clear(conversationId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .timestamp(LocalDateTime.now())
                .success(true)
                .message("Session cleared successfully")
                .data(null)
                .build());
    }
}