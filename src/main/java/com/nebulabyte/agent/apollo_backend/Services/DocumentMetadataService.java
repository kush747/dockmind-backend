package com.nebulabyte.agent.apollo_backend.Services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.nebulabyte.agent.apollo_backend.Dto.DocumentMetaDataDto;
import com.nebulabyte.agent.apollo_backend.Dto.DocumentResponseDto;
import com.nebulabyte.agent.apollo_backend.Entity.DocumentMetaData;
import com.nebulabyte.agent.apollo_backend.Entity.DocumentStatus;
import com.nebulabyte.agent.apollo_backend.Exception.DocumentProcessingException;
import com.nebulabyte.agent.apollo_backend.Exception.ResourceNotFoundException;
import com.nebulabyte.agent.apollo_backend.Repositories.DocumentMetadataRepo;

@Service
public class DocumentMetadataService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentMetadataService.class);

    private final DocumentMetadataRepo documentMetadataRepo;
    private final JdbcTemplate jdbcTemplate;
    private final ParserService parserService;
    private final IngestionService ingestionService;
    private final ModelMapper modelMapper;

    public DocumentMetadataService(DocumentMetadataRepo documentMetadataRepo, JdbcTemplate jdbcTemplate,
            ParserService parserService, IngestionService ingestionService, ModelMapper modelMapper) {
        this.documentMetadataRepo = documentMetadataRepo;
        this.jdbcTemplate = jdbcTemplate;
        this.parserService = parserService;
        this.ingestionService = ingestionService;
        this.modelMapper = modelMapper;
    }

    public DocumentResponseDto uploadAndProcess(MultipartFile file) {
        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document";
        String contextType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";

        DocumentMetaData doc = new DocumentMetaData();
        doc.setFilename(fileName);
        doc.setContentType(contextType);
        doc.setFileSize(file.getSize());
        doc.setStatus(DocumentStatus.UPLOADING);
        doc.setCreatedAt(LocalDateTime.now());

        DocumentMetaData savedDoc = documentMetadataRepo.save(doc);

        List<Document> parsedDoc = null;
        int chunksCreated = 0;


        try {
            parsedDoc = parserService.parse(file);

            chunksCreated = ingestionService.ingest(savedDoc, parsedDoc);
}catch (DocumentProcessingException e) {
            documentMetadataRepo.delete(savedDoc);
            throw e;
        }

        return DocumentResponseDto.builder()
                .id(savedDoc.getId())
                .fileName(savedDoc.getFilename())
                .fileSize(savedDoc.getFileSize())
                .status(savedDoc.getStatus())
                .chunksCreated(chunksCreated)
                .message("Document uploaded and processed successfully")
                .build();

    }

    public List<DocumentResponseDto> uploadAndProcessMultipleDocs(List<MultipartFile> files) {

        List<DocumentResponseDto> dtoList = new ArrayList<>();
        for (MultipartFile file : files) {
            dtoList.add(uploadAndProcess(file));
        }
        return dtoList;
    }

    // get all documents using modalMapper
    public List<DocumentMetaDataDto> getAllDocuments() {
        List<DocumentMetaData> documents = this.documentMetadataRepo.findAllByOrderByCreatedAtDesc();
        return documents.stream()
                .map(doc -> modelMapper.map(doc, DocumentMetaDataDto.class))
                .toList();
    }

    public DocumentMetaDataDto getDocumentById(UUID id) {
        DocumentMetaData document = this.documentMetadataRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + id));
        return this.modelMapper.map(document, DocumentMetaDataDto.class);
    }

    // fetch and delete both from db and vector store
    public void deleteDocumentById(UUID id) {

        DocumentMetaData document = this.documentMetadataRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + id));

        try {
            String deleteVectorsSql = "DELETE FROM vector_store WHERE metadata ->>'documentId' = ? ";
            int deleteCount = this.jdbcTemplate.update(deleteVectorsSql, id.toString());
            this.documentMetadataRepo.deleteById(id);

        } catch (Exception e) {
            logger.warn("Failed to delete document with id: {} and vectors {} ", id, e);
        }
    }

}
