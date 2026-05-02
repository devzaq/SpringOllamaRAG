package com.dev.rag_gem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

@Component
public class IngestionService implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);
    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;

    @Value("classpath:/docs/javabook.pdf")
    private Resource pdfResource;

    public IngestionService(VectorStore vectorStore, JdbcTemplate jdbcTemplate) {
        this.vectorStore = vectorStore;
        this.jdbcTemplate = jdbcTemplate;
    }

    private String clean(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (c != '\u0000') {
                sb.append(c);
            }
        }
        return sb.toString().trim();
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM vector_store", Integer.class
            );

            if (count != null && count > 0) {
                log.info("Vector Store already has {} records, skipping ingestion.", count);
                return;
            }

            log.info("Vector Store is empty, starting ingestion...");
            PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(
                    pdfResource,
                    PdfDocumentReaderConfig.builder()
                            .withPagesPerDocument(1)
                            .build()
            );

            log.info("PDF loaded, splitting into chunks...");
            TextSplitter textSplitter = new TokenTextSplitter();
            var chunks = textSplitter.apply(pdfReader.get());
            log.info("Split into {} chunks, cleaning...", chunks.size());

            var cleanedChunks = chunks.stream()
                    .map(doc -> {
                        String cleanContent = clean(doc.getText());
                        Map<String, Object> cleanedMetadata = doc.getMetadata().entrySet().stream()
                                .collect(Collectors.toMap(
                                        Map.Entry::getKey,
                                        e -> e.getValue() instanceof String s
                                                ? clean(s)
                                                : e.getValue() != null ? e.getValue() : ""
                                ));
                        return new Document(cleanContent, cleanedMetadata);
                    })
                    .filter(doc -> !doc.getText().isEmpty())
                    .toList();

            log.info("Cleaned {} chunks, storing in vector store...", cleanedChunks.size());

            int batchSize = 50;
            for (int i = 0; i < cleanedChunks.size(); i += batchSize) {
                int end = Math.min(i + batchSize, cleanedChunks.size());
                try {
                    vectorStore.accept(cleanedChunks.subList(i, end));
                    log.info("Inserted {}/{}", end, cleanedChunks.size());
                } catch (Exception e) {
                    log.error("Failed batch {}-{}: {}", i, end, e.getMessage());
                }
            }

            Integer finalCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM vector_store", Integer.class
            );
            log.info("Ingestion complete! Vector Store now has {} records.", finalCount);

        } catch (Exception e) {
            log.error("Ingestion failed: {}", e.getMessage(), e);
        }
    }
}