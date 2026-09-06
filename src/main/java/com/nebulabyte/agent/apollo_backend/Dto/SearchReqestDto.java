package com.nebulabyte.agent.apollo_backend.Dto;

import java.util.UUID;



import jakarta.validation.constraints.NotBlank;
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
public class SearchReqestDto {

    @NotBlank(message = "query cannot be empty")
    private String query;

    private UUID documentId;
    private Integer topK;
    private Double similaritySearch;

}
