package com.nebulabyte.agent.apollo_backend.Dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatRequestDto {

    private String question;
    private UUID documentId;
    private Integer topK;
    private Double minSimilaritySearch;
    private String conversationId;

}
