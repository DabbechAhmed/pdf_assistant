package com.testing.pdfassistant.rag;

import com.testing.pdfassistant.dto.ProjectSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Component
public class PdfIngestionService implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(PdfIngestionService.class);

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;
    private final ChatModel chatModel;

    @Value("classpath:/docs/liste_sujets_2026-2027.pdf")
    private Resource pdfResource;

    public PdfIngestionService(VectorStore vectorStore, JdbcTemplate jdbcTemplate, ChatModel chatModel) {
        this.vectorStore = vectorStore;
        this.jdbcTemplate = jdbcTemplate;
        this.chatModel = chatModel;
    }

    @Override
    public void run(String... args) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM vector_store", Integer.class);

        if (count != null && count > 0) {
            log.info("Database already contains {} vector chunks. Skipping PDF ingestion.", count);
            return;
        }

        log.info("Starting PDF ingestion for: {}", pdfResource.getFilename());

        PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(pdfResource);
        List<Document> pages = pdfReader.get();
        log.info("Extracted {} pages from PDF", pages.size());

        List<Document> cleanedPages = pages.stream()
                .map(doc -> new Document(doc.getText().replaceAll("\\s+", " "), doc.getMetadata()))
                .toList();

        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(400)
                .withMinChunkSizeChars(50)
                .build();

        List<Document> chunks = splitter.apply(cleanedPages);

        log.info("Split PDF into {} chunks. Sending to Ollama (mxbai-embed-large)...", chunks.size());

        // 4. Load: Generate embeddings via Ollama and INSERT into PostgreSQL pgvector
        vectorStore.add(chunks);

        log.info("PDF ingestion complete! Stored {} chunks in PostgreSQL.", chunks.size());


    }


}
