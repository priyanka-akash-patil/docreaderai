package com.ai.docreader.service;

import com.ai.docreader.model.ChatSession;
import com.ai.docreader.model.ChatSession.ChatTurn;
import com.ai.docreader.model.ChatSession.SourceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
 
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
 
/**
 * Phase 2 service — multi-turn chat memory + source citations.
 *
 * Spring AI GA API (1.0.0+):
 *   InMemoryChatMemory     → replaced by MessageWindowChatMemory
 *   InMemoryChatMemoryRepository → the actual storage backend
 *   CHAT_MEMORY_CONVERSATION_ID_KEY → use .conversationId() directly on advisor
 */
@Service
public class ChatService {
 
    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
 
    private final ChatClient          chatClient;
    private final VectorStore         vectorStore;
    private final MessageWindowChatMemory chatMemory;
 
    private final Map<String, ChatSession> sessions = new HashMap<>();
 
    private static final int    TOP_K                = 4;
    private static final double SIMILARITY_THRESHOLD = 0.0;
 
    public ChatService(ChatClient chatClient, VectorStore vectorStore) {
        this.chatClient  = chatClient;
        this.vectorStore = vectorStore;
 
        // InMemoryChatMemoryRepository is the storage backend (ConcurrentHashMap internally)
        // MessageWindowChatMemory wraps it and keeps the last N messages per conversation
        this.chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)   // keep last 20 messages per session
                .build();
    }
 
    // ── Session management ────────────────────────────────────────────────────
 
    public ChatSession createSession() {
        ChatSession session = new ChatSession();
        sessions.put(session.getSessionId(), session);
        log.info("Created new session: {}", session.getSessionId());
        return session;
    }
 
    public ChatSession getSession(String sessionId) {
        ChatSession session = sessions.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException(
                "Session not found: " + sessionId + ". Call POST /api/chat/start first.");
        }
        return session;
    }
 
    // ── RAG + Memory Q&A ──────────────────────────────────────────────────────
 
    public ChatResponse askWithMemory(String sessionId, String question) {
 
        ChatSession session = getSession(sessionId);
        session.incrementTurn();
        log.debug("[Session {}] Turn {}: {}", sessionId, session.getTurnNumber(), question);
 
        // Step 1: Retrieve relevant chunks from PGVector
        List<Document> relevantChunks = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(question)
                        .topK(TOP_K)
                        .similarityThreshold(SIMILARITY_THRESHOLD)
                        .build()
        );
        log.debug("[Session {}] Retrieved {} chunks", sessionId, relevantChunks.size());
 
        // Step 2: Build context from chunks
        String context = relevantChunks.isEmpty()
                ? "No document has been uploaded yet."
                : relevantChunks.stream()
                        .map(Document::getText)
                        .collect(Collectors.joining("\n\n---\n\n"));
 
        // Step 3: Build RAG prompt
        String ragPrompt = """
                Use the document context below to answer the question.
                If the answer isn't in the context, say so clearly.
                Keep answers concise and accurate.
 
                DOCUMENT CONTEXT:
                %s
 
                QUESTION: %s
                """.formatted(context, question);
 
        // Step 4: Call Groq with MessageChatMemoryAdvisor
        // .conversationId(sessionId) links this call to the right memory bucket
        // The advisor automatically injects history + saves new exchange
        String answer = chatClient.prompt()
                .user(ragPrompt)
                .advisors(MessageChatMemoryAdvisor.builder(chatMemory)
                        .conversationId(sessionId)
                        .build())
                .call()
                .content();
 
        // Step 5: Build source citations with page numbers
        List<SourceReference> sources = relevantChunks.stream()
                .map(doc -> {
                    String filename = (String) doc.getMetadata()
                            .getOrDefault("filename", "unknown");
                    Object pageObj = doc.getMetadata().get("page_number");
                    int page = (pageObj instanceof Number n) ? n.intValue() : 1;
                    String preview = doc.getText()
                            .substring(0, Math.min(150, doc.getText().length())) + "...";
                    return new SourceReference(filename, page, preview);
                })
                .toList();
 
        // Step 6: Save turn to session history
        session.addTurn(new ChatTurn(
                session.getTurnNumber(), question, answer, sources, LocalDateTime.now()));
 
        return new ChatResponse(answer, sessionId, sources, session.getTurnNumber());
    }
 
    public record ChatResponse(
            String               answer,
            String               sessionId,
            List<SourceReference> sources,
            int                  turnNumber
    ) {}
}