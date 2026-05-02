package com.dev.rag_gem;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {
    private final ChatClient chatClient;
    private static final String SYSTEM_PROMPT = """
            You are a helpful assistant that answers questions ONLY based on the provided context documents.
            If the answer is not found in the context, respond with "I don't have information about that in the provided documents."
            Do NOT use your internal training knowledge to answer questions.
            Do NOT make up or infer information beyond what is explicitly stated in the context.
            """;

    public ChatController(ChatClient.Builder builder, VectorStore vectorStore ){
        this.chatClient = builder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        QuestionAnswerAdvisor
                                .builder(vectorStore)
                                .searchRequest(
                                        SearchRequest
                                                .builder()
                                                .topK(6)
                                                .similarityThreshold(0.3)
                                                .build()
                                ).build()
                )
                .build();
    }

    @PostMapping("/java")
    public String chat(@RequestBody MessageDto messageDto){
        return chatClient
                .prompt()
                .user(messageDto.getMessage())
                .call()
                .content();
    }
}
