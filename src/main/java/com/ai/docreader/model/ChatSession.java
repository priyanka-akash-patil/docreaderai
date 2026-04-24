package com.ai.docreader.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
 
/**
 * Represents one conversation session.
 *
 * Stored in memory (Map) — in Phase 3 we will persist this in PostgreSQL.
 * Each session has its own chat history so multiple users don't bleed into each other.
 */
public class ChatSession {
 
    private final String sessionId;
    private final LocalDateTime createdAt;
    private int turnNumber;
    private final List<ChatTurn> history;
 
    public ChatSession() {
        this.sessionId  = UUID.randomUUID().toString();
        this.createdAt  = LocalDateTime.now();
        this.turnNumber = 0;
        this.history    = new ArrayList<>();
    }
 
    public String getSessionId()       { return sessionId; }
    public LocalDateTime getCreatedAt(){ return createdAt; }
    public int getTurnNumber()         { return turnNumber; }
    public List<ChatTurn> getHistory() { return history; }
 
    public void incrementTurn() { this.turnNumber++; }
 
    public void addTurn(ChatTurn turn) { this.history.add(turn); }
 
    /**
     * One Q&A exchange in the conversation.
     */
    public record ChatTurn(
            int turn,
            String question,
            String answer,
            List<SourceReference> sources,
            LocalDateTime timestamp
    ) {}
 
    /**
     * A document chunk that was used to generate an answer.
     */
    public record SourceReference(
            String filename,
            int    page,
            String preview
    ) {}
}