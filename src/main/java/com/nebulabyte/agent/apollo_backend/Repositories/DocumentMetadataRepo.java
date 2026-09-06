package com.nebulabyte.agent.apollo_backend.Repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nebulabyte.agent.apollo_backend.Entity.DocumentMetaData;

public interface DocumentMetadataRepo extends JpaRepository<DocumentMetaData, UUID> {

    DocumentMetaData save(DocumentMetaData doc);

    List<DocumentMetaData> findAllByOrderByCreatedAtDesc();

}
