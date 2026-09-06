package com.nebulabyte.agent.apollo_backend.Services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import com.nebulabyte.agent.apollo_backend.Entity.DocumentMetaData;
import com.nebulabyte.agent.apollo_backend.Entity.DocumentStatus;
import com.nebulabyte.agent.apollo_backend.Exception.DocumentProcessingException;
import com.nebulabyte.agent.apollo_backend.Repositories.DocumentMetadataRepo;
import com.nebulabyte.agent.apollo_backend.config.AppProperties;

@Service
public class IngestionService {

    private static final Logger logger = LoggerFactory.getLogger(IngestionService.class);
    private final VectorStore vectorStore;
    private final DocumentMetadataRepo documentMetadataRepo;
    private final AppProperties appProperties;

    public IngestionService(VectorStore vectorStore, DocumentMetadataRepo documentMetadataRepo,AppProperties appProperties) {
        this.vectorStore = vectorStore;
        this.documentMetadataRepo = documentMetadataRepo;
        this.appProperties = appProperties;
    }

    public int ingest(DocumentMetaData savedDoc, List<Document> parsedDoc) {
        try{
            savedDoc.setStatus(DocumentStatus.PROCESSING);
            savedDoc.setTotalPages(parsedDoc.size());

            TokenTextSplitter tokenTextSplitter = TokenTextSplitter.builder()
            .withChunkSize(appProperties.getRag().getChunkSize())
            .withMinChunkSizeChars(appProperties.getRag().getMinChunkSizeChars())
            .withMinChunkLengthToEmbed(appProperties.getRag().getMinChunkLengthToEmbed())
            .withMaxNumChunks(appProperties.getRag().getMaxNumChunks())
            .withKeepSeparator(true)
            .build();

            List<Document> chunks = tokenTextSplitter.apply(parsedDoc);

            if(chunks.isEmpty()){
                savedDoc.setStatus(DocumentStatus.FAILED);
                savedDoc.setErrorMessage("No chunks generated from the parsed documents");
                documentMetadataRepo.save(savedDoc);
                return 0;
            }

            List<Document> enrichedChunks = new ArrayList<>();
            for(int i=0; i<chunks.size();i++){
                Document chunk = chunks.get(i);
                Map<String, Object> enrichedMetadata = new HashMap<>(chunk.getMetadata());
                enrichedMetadata.put("documentId",savedDoc.getId().toString());
                enrichedMetadata.put("fileName",savedDoc.getFilename());
                enrichedMetadata.put("contentType", savedDoc.getContentType());

                enrichedMetadata.put("chunkIndex",i);
                
                Object pageNumber = chunk.getMetadata().get("page_number");
                if(pageNumber != null){
                    enrichedMetadata.put("pageNumber",pageNumber);
                }else{
                    pageNumber = chunk.getMetadata().get("pageNumber");
                    if(pageNumber != null){
                        enrichedMetadata.put("pageNumber",pageNumber);
                    }
                }
                Document enrichedDoc = new Document(chunk.getText(), enrichedMetadata);
                enrichedChunks.add(enrichedDoc);
            }

            logger.info("Adding {} chunks to vector store for document {}",enrichedChunks.size(),savedDoc.getId());
            vectorStore.add(enrichedChunks);
            savedDoc.setStatus(DocumentStatus.INDEXED);
            savedDoc.setTotalChunks(enrichedChunks.size());
            savedDoc.setErrorMessage(null);
            documentMetadataRepo.save(savedDoc);
            logger.info("Successfully ingested {} chunks from document {}",enrichedChunks.size(),savedDoc.getId());
            return enrichedChunks.size();
            

        }catch(Exception e)
        {
            logger.error("Error in ingesting documents",e);
            savedDoc.setStatus(DocumentStatus.FAILED);
            savedDoc.setErrorMessage("Error in ingesting documents");
            documentMetadataRepo.save(savedDoc);
            throw new DocumentProcessingException("Error in ingesting documents",e);
        }
        
    }

}
