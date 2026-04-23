package com.ai.docreader.controller;

import com.ai.docreader.service.DocumentIngestionService;
import com.ai.docreader.service.DocumentQAService;
import com.ai.docreader.service.DocumentQAService.QAResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DocumentController {

	@Autowired
    private DocumentIngestionService ingestionService;
	
	@Autowired
    private  DocumentQAService qaService;
	
    private Logger log = LoggerFactory.getLogger(DocumentController.class);

    // ── Upload Endpoint ──────────────────────────────────────────────────────

    /**
     * Accepts a PDF file, parses it, embeds it, and stores it in PGVector.
     *
     * Usage:
     *   curl -X POST http://localhost:8080/api/documents/upload \
     *        -F "file=@/path/to/document.pdf"
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadDocument(
            @RequestParam("file") MultipartFile file) {

        // Validate file type
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "File is empty"));
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Only PDF files are supported. Got: " + filename));
        }

        try {
            int chunksStored = ingestionService.ingestPdf(file);

            return ResponseEntity.ok(Map.of(
                    "message", "Document ingested successfully!",
                    "filename", filename,
                    "chunksStored", chunksStored,
                    "status", "ready"
            ));

        } catch (IOException e) {
            log.error("Failed to ingest PDF: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to process PDF: " + e.getMessage()));
        }
    }

    // ── Ask Endpoint ─────────────────────────────────────────────────────────

    /**
     * Accepts a question, retrieves relevant chunks, and returns a grounded answer.
     *
     * Usage:
     *   curl -X POST http://localhost:8080/api/documents/ask \
     *        -H "Content-Type: application/json" \
     *        -d '{"question": "What is this document about?"}'
     */
    @PostMapping("/ask")
    public ResponseEntity<Map<String, Object>> askQuestion(
            @RequestBody Map<String, String> request) {

        String question = request.get("question");

        if (question == null || question.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Question cannot be empty"));
        }

        QAResponse response = qaService.askQuestion(question);

        // Build source citations list (filename + chunk preview)
        List<Map<String, Object>> sources = response.sourceChunks().stream()
                .map(doc -> Map.<String, Object>of(
                        "filename", doc.getMetadata().getOrDefault("filename", "unknown"),
                        "preview",  doc.getText().substring(0, Math.min(150, doc.getText().length())) + "..."
                ))
                .toList();

        return ResponseEntity.ok(Map.of(
                "question", question,
                "answer",   response.answer(),
                "sources",  sources,         // Which chunks were used to generate the answer
                "chunksUsed", sources.size()
        ));
    }

    // ── Health Check ──────────────────────────────────────────────────────────

    /**
     * Simple health check to verify the service is running.
     *
     * Usage:
     *   curl http://localhost:8080/api/documents/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "DocMind Phase 1",
                "description", "RAG-powered Document Q&A"
        ));
    }

}