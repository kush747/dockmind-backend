package com.nebulabyte.agent.apollo_backend.Controllers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.nebulabyte.agent.apollo_backend.Dto.ApiResponse;
import com.nebulabyte.agent.apollo_backend.Dto.DocumentMetaDataDto;
import com.nebulabyte.agent.apollo_backend.Dto.DocumentResponseDto;
import com.nebulabyte.agent.apollo_backend.Services.DocumentMetadataService;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private final DocumentMetadataService documentService;

    public DocumentController(DocumentMetadataService documentService) {
        this.documentService = documentService;
    }

    // upload single document
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DocumentResponseDto>> uploadDocument(
            @RequestParam("file") MultipartFile file) {
        // process the file

        DocumentResponseDto documentResponseDto = this.documentService.uploadAndProcess(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<DocumentResponseDto>builder()
                .data(documentResponseDto)
                .message("Document uploaded and processed successfully")
                .timestamp(LocalDateTime.now())
                .success(true)
                .build());
    }

    // upload multiple document
    @PostMapping(value = "/upload-multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<DocumentResponseDto>>> uploadDocuments(
            @RequestParam("files") List<MultipartFile> files) {
        // process the file
        List<DocumentResponseDto> documentResponseDto = this.documentService.uploadAndProcessMultipleDocs(files);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<List<DocumentResponseDto>>builder()
                .data(documentResponseDto)
                .message("Documents uploaded and processed successfully")
                .timestamp(LocalDateTime.now())
                .success(true)
                .build());
 
    }

    //fetch all documents
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<DocumentMetaDataDto>>> getAllDocuments() {
        List<DocumentMetaDataDto> documents = this.documentService.getAllDocuments();
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.<List<DocumentMetaDataDto>>builder()
                .data(documents)
                .message("Documents fetched successfully")
                .timestamp(LocalDateTime.now())
                .success(true)
                .build());
    }

    //get document by its id
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DocumentMetaDataDto>> getDocumentById(@PathVariable UUID id) {
        DocumentMetaDataDto document = this.documentService.getDocumentById(id);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.<DocumentMetaDataDto>builder()
                .data(document)
                .message("Document fetched successfully")
                .timestamp(LocalDateTime.now())
                .success(true)
                .build());
    }

    //delete document by id
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDocumentById(@PathVariable UUID id) {
        this.documentService.deleteDocumentById(id);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.<Void>builder()
                .message("Document deleted successfully")
                .timestamp(LocalDateTime.now())
                .success(true)
                .build());
    }


    // //delete all documents
    // @DeleteMapping("/all")
    // public ResponseEntity<ApiResponse<String>> deleteAllDocuments() {
    //     this.documentService.deleteAllDocuments();
    //     return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.<String>builder()
    //             .message("All documents deleted successfully")
    //             .timestamp(LocalDateTime.now())
    //             .success(true)
    //             .build());

    // }

    

}
