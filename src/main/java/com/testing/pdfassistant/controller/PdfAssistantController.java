package com.testing.pdfassistant.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pdf")
public class PdfAssistantController {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public PdfAssistantController(ChatClient.Builder builder, VectorStore vectorStore) {
        this.vectorStore = vectorStore;

        SearchRequest searchRequest = SearchRequest.builder()
                .topK(5)
                .similarityThresholdAll()
                .build();

        // Template personnalisé en français pour guider llama3.2 (3B) sans le bloquer
        PromptTemplate customRagTemplate = new PromptTemplate("""
                Voici des extraits d'un document PDF contenant des sujets de projets :
                ---------------------
                {question_answer_context}
                ---------------------
                En utilisant uniquement les extraits ci-dessus, réponds de manière complète à la question suivante.
                Note : Un même projet peut être découpé sur plusieurs extraits, combine les informations si nécessaire.
                
                Question : {query}
                """);

        this.chatClient = builder
                .defaultAdvisors(
                        QuestionAnswerAdvisor.builder(vectorStore)
                                .searchRequest(searchRequest)
                                .promptTemplate(customRagTemplate)
                                .build(),
                        new SimpleLoggerAdvisor()
                )
                .build();
    }

    @GetMapping("/debug")
    public List<Document> debugSearch(
            @RequestParam(
                    value = "question",
                    defaultValue = "Quel est l'objectif du projet Super Loopi et qui est le client ?"
            ) String question) {

        return vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(question)
                        .topK(5)
                        .similarityThresholdAll()
                        .build()
        );
    }

    @GetMapping("/ask")
    public String ask(
            @RequestParam(
                    value = "question",
                    defaultValue = "Quel est l'objectif du projet Super Loopi et qui est le client ?"
            ) String question) {

        return chatClient.prompt()
                .user(question)
                .call()
                .content();
    }
}