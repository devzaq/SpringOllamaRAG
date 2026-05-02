package com.dev.rag_gem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.reader.pdf.ParagraphPdfDocumentReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import java.util.List;

@Component
public class IngestionService implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);
    private final VectorStore vectorStore;

    public IngestionService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Value("classpath:/docs/javabook.pdf")
    private Resource marketPDF;

    @Override
    public void run(String... args) throws Exception {
        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder().query("test").topK(1).build()
        );

        if (!results.isEmpty()) {
            log.info("Vector Store already has data, skipping ingestion.");
            return;
        }

        ParagraphPdfDocumentReader pdfReader = new ParagraphPdfDocumentReader(marketPDF);
        TextSplitter textSplitter = new TokenTextSplitter();
        vectorStore.accept(textSplitter.apply(pdfReader.get()));
        log.info("Vector Store loaded with data!");
    }
}
