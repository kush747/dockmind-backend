package com.nebulabyte.agent.apollo_backend.Controllers;

import java.time.LocalDateTime;

import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nebulabyte.agent.apollo_backend.Dto.ApiResponse;
import com.nebulabyte.agent.apollo_backend.Dto.ChatRequestDto;
import com.nebulabyte.agent.apollo_backend.Dto.ChatResponseDto;
import com.nebulabyte.agent.apollo_backend.Dto.SearchReqestDto;
import com.nebulabyte.agent.apollo_backend.Dto.SearchResultDto;
import com.nebulabyte.agent.apollo_backend.Services.RagService;

import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.Valid;
import reactor.core.publisher.Flux;


@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final RagService ragService;

    public ChatController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping("/query")
    @Operation(
        summary = "Ask a question to the AI Agent",
        description = "This endpoint allows you to ask questions to the AI Agent and get responses."
    )
    public ResponseEntity<ApiResponse<ChatResponseDto>> askQuestion(
       @Valid @RequestBody ChatRequestDto chatRequestDto
    ){

        ChatResponseDto chatResponseDto =  ragService.askQuestion(chatRequestDto);
        return ResponseEntity.ok(ApiResponse.<ChatResponseDto>builder()
            .timestamp(LocalDateTime.now())
            .success(true)
            .message(null)
            .data(chatResponseDto)
            .build()
    );
    }

    @PostMapping("/stream")
    @Operation(
        summary = "Stream a question to the AI Agent",
        description = "This endpoint allows you to stream answers to the AI Agent and get responses."
    )
    public Flux<String> askQuestionStreaming(
        @Valid @RequestBody ChatRequestDto chatRequestDto
    ){
        return ragService.streamQuestionAndAnswer(chatRequestDto);
    }



    @PostMapping("/search/similarity")
    @Operation(
        summary = "Search similarity to the AI Agent",
        description = "This endpoint allows you to search similarity to the AI Agent and get responses."
    )
    public ResponseEntity<ApiResponse<SearchResultDto>> searchSimilarity(
        @Valid @RequestBody SearchReqestDto searchReqestDto
    ){
        SearchResultDto searchResultDto =  ragService.searchSimilarChunks(searchReqestDto);
        return ResponseEntity.ok(ApiResponse.<SearchResultDto>builder()
            .timestamp(LocalDateTime.now())
            .success(true)
            .message(null)
            .data(searchResultDto)
            .build()
    );
    }
    


}
