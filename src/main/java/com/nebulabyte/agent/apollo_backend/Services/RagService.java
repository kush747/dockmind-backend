package com.nebulabyte.agent.apollo_backend.Services;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import com.nebulabyte.agent.apollo_backend.Dto.ChatRequestDto;
import com.nebulabyte.agent.apollo_backend.Dto.ChatResponseDto;
import com.nebulabyte.agent.apollo_backend.Dto.CitationDto;
import com.nebulabyte.agent.apollo_backend.Dto.SearchReqestDto;
import com.nebulabyte.agent.apollo_backend.Dto.SearchResultDto;
import com.nebulabyte.agent.apollo_backend.config.AppProperties;

import reactor.core.publisher.Flux;

@Service
public class RagService {

    private static final Logger logger = LoggerFactory.getLogger(RagService.class);
    private final AppProperties appProperties;
    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private final ChatMemory chatMemory;

    public RagService(AppProperties appProperties, VectorStore vectorStore,
            ChatClient chatClient, ChatMemory chatMemory) {
        this.appProperties = appProperties;
        this.vectorStore = vectorStore;
        this.chatClient = chatClient;
        this.chatMemory = chatMemory;
    }

    public ChatResponseDto askQuestion(ChatRequestDto request) {
        long startTime = System.currentTimeMillis();

        // Use provided conversationId or generate a new one
        String conversationId = (request.getConversationId() != null && !request.getConversationId().isBlank())
                ? request.getConversationId()
                : UUID.randomUUID().toString();

        List<Document> similarDocuments = this.retrieveRelevantDocument(
                request.getQuestion(),
                request.getDocumentId(),
                request.getTopK(),
                request.getMinSimilaritySearch());

        List<CitationDto> citations = similarDocuments.stream().map(this::maptoCitation).toList();
        String contextText = buildContextString(similarDocuments);
        String prompt = buildPrompt(request.getQuestion(), contextText);

        String answer = chatClient.prompt()
                .user(prompt)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();

        long duration = System.currentTimeMillis() - startTime;

        return ChatResponseDto.builder()
                .answer(answer)
                .conversationId(conversationId)
                .citations(citations)
                .responseTimeMs(duration)
                .build();
    }

    public Flux<String> streamQuestionAndAnswer(ChatRequestDto request) {

        String conversationId = (request.getConversationId() != null && !request.getConversationId().isBlank())
                ? request.getConversationId()
                : UUID.randomUUID().toString();

        List<Document> similarDocuments = this.retrieveRelevantDocument(
                request.getQuestion(),
                request.getDocumentId(),
                request.getTopK(),
                request.getMinSimilaritySearch());

        String contextText = buildContextString(similarDocuments);
        String prompt = buildPrompt(request.getQuestion(), contextText);

        return chatClient.prompt()
                .user(prompt)
                .advisors(a -> a.param(
                        ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content();
    }

    private String buildPrompt(String question, String contextText) {
        if (contextText != null && !contextText.isEmpty()) {
            return String.format(
                    """
                            You are a helpful and precise assistant.

                            Document Context:
                            -------------------
                            %s
                            -------------------

                            User Message / Question: %s

                            Instructions:
                            1. Context-Based Questions: If the user's query relates to the Document Context provided above, prioritize using that information to form your answer. Ground your response directly in the text.
                            2. Out-of-Scope & General Queries: If the user asks a general question, greets you, or discusses topics outside the provided document context, answer naturally using your general knowledge while remaining helpful and polite.
                            3. Clarity & Accuracy: If the document context does not contain enough detail to fully answer a context-related question, state clearly what is missing rather than speculating.
                            """,
                    contextText, question);
        } else {
            return String.format(
                    """
                            You are a friendly, intelligent, and accurate AI assistant.

                            User Message / Question: %s

                            Instructions:
                            1. Direct Assistance: Answer the user's message or question directly, thoroughly, and accurately using your general knowledge.
                            2. Adaptability: Adjust your tone to match the user's intent, whether they are engaging in casual conversation, asking for assistance, or seeking technical advice.
                            3. Clarity & Precision: Provide clear, concise, and structured responses without unnecessary filler.
                            """,
                    question);
        }
    }

    private String buildContextString(List<Document> similarDocuments) {
        if (similarDocuments == null || similarDocuments.isEmpty()) {
            return "";
        }
        return similarDocuments.stream()
                .map(doc -> {
                    String fileName = (String) doc.getMetadata().getOrDefault("fileName", "unknown_file");
                    Object page = doc.getMetadata().getOrDefault("pageNumber", "NA");
                    return String.format("[Source: %s | Page: %s]\n%s", fileName, page, doc.getText());
                })
                .collect(Collectors.joining("\n\n----\n\n"));
    }

    public SearchResultDto searchSimilarChunks(SearchReqestDto request) {
        List<Document> matchedDocs = retrieveRelevantDocument(
                request.getQuery(),
                request.getDocumentId(),
                request.getTopK(),
                request.getSimilaritySearch());

        List<CitationDto> citations = matchedDocs.stream().map(this::maptoCitation).toList();
        return SearchResultDto.builder()
                .query(request.getQuery())
                .totalMatches(citations.size())
                .matches(citations)
                .build();
    }

    private CitationDto maptoCitation(Document document) {
        Map<String, Object> meta = document.getMetadata();
        UUID docId = null;
        if (meta.get("documentId") != null) {
            try {
                docId = UUID.fromString(meta.get("documentId").toString());
            } catch (Exception ignore) {
            }
        }
        Integer chunkIndex = null;
        if (meta.get("chunkIndex") instanceof Number n) {
            chunkIndex = n.intValue();
        }
        Integer pageNumber = null;
        if (meta.get("page_number") instanceof Number n) {
            pageNumber = n.intValue();
        } else if (meta.get("pageNumber") instanceof Number n) {
            pageNumber = n.intValue();
        }
        Double score = null;
        if (meta.get("distance") instanceof Number n) {
            score = 1.0 - n.doubleValue();
        }
        return CitationDto.builder()
                .documentId(docId)
                .fileName((String) meta.getOrDefault("fileName", "unknown_file"))
                .chunkIndex(chunkIndex)
                .pageNumber(pageNumber)
                .snippet(document.getText())
                .similarityScore(score)
                .metadata(meta)
                .build();
    }

    private List<Document> retrieveRelevantDocument(String query, UUID documentId, Integer topK,
            Double similaritySearch) {

        int effectiveTopK = topK != null ? topK : appProperties.getRag().getTopK();
        double effectiveSimilaritySearch = similaritySearch != null ? similaritySearch
                : appProperties.getRag().getSimilarityThreshold();

        SearchRequest.Builder searchRequestBuilder = SearchRequest.builder()
                .query(query)
                .topK(effectiveTopK);

        if (effectiveSimilaritySearch > 0.0) {
            searchRequestBuilder.similarityThreshold(effectiveSimilaritySearch);
        }
        if (documentId != null) {
            FilterExpressionBuilder b = new FilterExpressionBuilder();
            searchRequestBuilder.filterExpression(b.eq("documentId", documentId.toString()).build());
        }

        try {
            List<Document> documents = vectorStore.similaritySearch(searchRequestBuilder.build());
            logger.info("Retrieved {} documents for query: {}", documents.size(), query);
            return documents;
        } catch (Exception ex) {
            logger.error("Failed to retrieve documents", ex);
            return Collections.emptyList();
        }
    }
}