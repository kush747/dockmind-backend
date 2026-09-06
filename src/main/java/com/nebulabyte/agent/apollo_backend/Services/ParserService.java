package com.nebulabyte.agent.apollo_backend.Services;


import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import io.micrometer.common.lang.Nullable;

@Service
public class ParserService {

    private final static Logger logger = LoggerFactory.getLogger(ParserService.class);


    public List<Document> parse(MultipartFile file) {
        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document";
        String contentType = file.getContentType() != null ? file.getContentType() : "";

        logger.info("parsing file: {}, type {}", fileName, contentType);

        try {
            Resource resource = new ByteArrayResource(file.getBytes()){
            @Override
            public @Nullable String getFilename() {
                return fileName;
            }
        };

        if(fileName.toLowerCase().endsWith(".pdf") || contentType.contains(".pdf")){
            return parsePdf(resource);
        }else{
            return parseGenericFile(resource);
        }
            
            
        }
        catch(Exception e){
            logger.error("failed to parse file: {}", fileName,e);
            throw new RuntimeException("Failed to parse file", e);
        }


     
    }

    private List<Document> parsePdf(Resource resource){

        PdfDocumentReaderConfig config = PdfDocumentReaderConfig.builder()
            .withPageBottomMargin(0)
            .withPageTopMargin(0)
            .build();

        PagePdfDocumentReader documentReader = new PagePdfDocumentReader(resource,config);
        return documentReader.read();
        
    }

    private List<Document> parseGenericFile(Resource resource){

        TikaDocumentReader documentReader = new TikaDocumentReader(resource);
        return documentReader.read();
        
    }

}
