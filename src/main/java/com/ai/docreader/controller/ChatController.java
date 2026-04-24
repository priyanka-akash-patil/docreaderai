package com.ai.docreader.controller;
 
import com.ai.docreader.model.ChatSession;
import com.ai.docreader.service.ChatService;
import com.ai.docreader.service.ChatService.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
 
import java.util.Map;
 
/**
 * Phase 2 Controller — chat endpoints with memory + citations.
 *
 * Endpoints:
 *   POST /api/chat/start              → create a new session
 *   POST /api/chat/ask                → ask a question (with memory)
 *   GET  /api/chat/history/{sessionId} → get full conversation history
 */
@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {
 
    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
 
    private final ChatService chatService;
 
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }
 
    // ── POST /api/chat/start ──────────────────────────────────────────────────
 
    /**
     * Creates a new conversation session.
     * Always call this first before asking questions.
     *
     * Response: { "sessionId": "abc-123", "message": "Session started" }
     *
     * Usage:
     *   curl -X POST http://localhost:8080/api/chat/start
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startSession() {
        ChatSession session = chatService.createSession();
        log.info("New chat session started: {}", session.getSessionId());
 
        return ResponseEntity.ok(Map.of(
                "sessionId", session.getSessionId(),
                "message",   "Session started! Upload a PDF then start asking questions.",
                "createdAt", session.getCreatedAt().toString()
        ));
    }
 
    // ── POST /api/chat/ask ────────────────────────────────────────────────────
 
    /**
     * Ask a question in an existing session.
     * Groq remembers all previous questions in this session.
     *
     * Request body:
     * {
     *   "sessionId": "abc-123",
     *   "question":  "What is the story about?"
     * }
     *
     * Response:
     * {
     *   "answer":     "The story is about...",
     *   "sessionId":  "abc-123",
     *   "turnNumber": 1,
     *   "sources": [
     *     { "filename": "story.pdf", "page": 1, "preview": "Once upon a time..." }
     *   ]
     * }
     */
    @PostMapping("/ask")
    public ResponseEntity<?> askQuestion(@RequestBody Map<String, String> request) {
 
        String sessionId = request.get("sessionId");
        String question  = request.get("question");
 
        if (sessionId == null || sessionId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "sessionId is required. Call POST /api/chat/start first."
            ));
        }
        if (question == null || question.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "question cannot be empty."
            ));
        }
 
        try {
            ChatResponse response = chatService.askWithMemory(sessionId, question);
 
            return ResponseEntity.ok(Map.of(
                    "answer",      response.answer(),
                    "sessionId",   response.sessionId(),
                    "turnNumber",  response.turnNumber(),
                    "sources",     response.sources()
            ));
 
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
 
    // ── GET /api/chat/history/{sessionId} ─────────────────────────────────────
 
    /**
     * Returns the full conversation history for a session.
     *
     * Usage:
     *   curl http://localhost:8080/api/chat/history/abc-123
     */
    @GetMapping("/history/{sessionId}")
    public ResponseEntity<?> getHistory(@PathVariable String sessionId) {
        try {
            ChatSession session = chatService.getSession(sessionId);
 
            return ResponseEntity.ok(Map.of(
                    "sessionId",  session.getSessionId(),
                    "createdAt",  session.getCreatedAt().toString(),
                    "totalTurns", session.getTurnNumber(),
                    "history",    session.getHistory()
            ));
 
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}