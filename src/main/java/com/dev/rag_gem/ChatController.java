package com.dev.rag_gem;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {
    private final ChatClient chatClient;

    public ChatController(ChatClient.Builder builder, VectorStore vectorStore ){
        this.chatClient = builder.defaultAdvisors(QuestionAnswerAdvisor.builder(vectorStore).build())
                .build();
    }

    @PostMapping("/java")
    public ResponseEntity<String> chat(@RequestBody MessageDto messageDto){
        return ResponseEntity.ok(chatClient
                .prompt()
                .user(messageDto.getMessage())
                .call()
                .content());
    }
}
