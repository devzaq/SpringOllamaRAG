package com.dev.rag_gem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.reader.pdf.ParagraphPdfDocumentReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

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

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM vector_store", Integer.class
        );

        if (count != null && count > 0) {
            log.info("Vector Store already has {} records, skipping ingestion.", count);
            return;
        }

        log.info("Vector Store is empty, starting ingestion...");
        ParagraphPdfDocumentReader pdfReader = new ParagraphPdfDocumentReader(pdfResource);

        log.info("PDF loaded, splitting into chunks...");
        TextSplitter textSplitter = new TokenTextSplitter();
        var chunks = textSplitter.apply(pdfReader.get());

        log.info("Split into {} chunks, storing in vector store...", chunks.size());
        vectorStore.accept(chunks);

        Integer finalCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM vector_store", Integer.class
        );
        log.info("Ingestion complete! Vector Store now has {} records.", finalCount);
    }
}