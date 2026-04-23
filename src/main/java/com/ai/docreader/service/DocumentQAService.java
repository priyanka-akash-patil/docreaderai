package com.ai.docreader.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles the RAG (Retrieval-Augmented Generation) Q&A flow:
 *
 *   question  →  embed question  →  similarity search  →  build prompt  →  call Groq  →  answer
 *
 * Key concepts:
 *   - Similarity search: finds the top-K chunks most semantically similar to the question
 *   - Augmented prompt: we inject those chunks as "context" into the LLM prompt
 *   - Grounding: the LLM is instructed to answer ONLY from the context (no hallucination)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentQAService {
	private Logger log = LoggerFactory.getLogger(DocumentQAService.class);;
	
	@Autowired
	private ChatClient chatClient;
	
	@Autowired
    private VectorStore vectorStore;

    // How many document chunks to retrieve for context
    // 4 is a good default — enough context, not too many tokens
    private static final int TOP_K = 4;

    // Minimum similarity score (0.0 to 1.0)
    // Chunks below this threshold are filtered out (not relevant enough)
    private static final double SIMILARITY_THRESHOLD = 0.0;

    /**
     * Main RAG method: retrieves relevant chunks and generates a grounded answer.
     *
     * @param question the user's question
     * @return QAResponse containing the answer and source chunks used
     */
    public QAResponse askQuestion(String question) {

        log.info("Processing question: {}", question);

        // ── Step 1: Similarity Search ───────────────────────────────────────
        // Spring AI automatically embeds the question (OpenAI API call)
        // then searches PGVector for the most similar chunks
        List<Document> relevantChunks = vectorStore.similaritySearch(
                SearchRequest.builder().query(question)
                        .topK(TOP_K)
                        .similarityThreshold(SIMILARITY_THRESHOLD).build()
        );

        log.info("Retrieved {} relevant chunks", relevantChunks.size());

        if (relevantChunks.isEmpty()) {
            return new QAResponse(
                    "I couldn't find any relevant information in the uploaded document for your question.",
                    List.of(),
                    question
            );
        }

        // ── Step 2: Build Context String ────────────────────────────────────
        // Concatenate the retrieved chunk texts, separated by a divider.
        // This becomes the "context" in our RAG prompt.
        String context = relevantChunks.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n---\n\n"));

        // ── Step 3: Build RAG Prompt ────────────────────────────────────────
        // We explicitly tell the LLM:
        //   - What context it has
        //   - What question to answer
        //   - To NOT go beyond the context
        String ragPrompt = """
                Answer the following question using ONLY the context provided below.
                Do not use any outside knowledge. Be concise and accurate.
                If the answer is not found in the context, say exactly:
                "I couldn't find this information in the uploaded document."

                CONTEXT:
                %s

                QUESTION: %s
                """.formatted(context, question);

        // ── Step 4: Call Groq via Spring AI ─────────────────────────────────
        // ChatClient sends the prompt to Groq (llama3-8b-8192)
        // and returns the text response
        String answer = chatClient.prompt()
                .user(ragPrompt)
                .call()
                .content();

        log.info("Generated answer ({} chars)", answer.length());

        return new QAResponse(answer, relevantChunks, question);
    }

    /**
     * Simple response object carrying the answer + source chunks.
     * In Phase 2 we'll expand this with page numbers and confidence scores.
     */
    public record QAResponse(
            String answer,
            List<Document> sourceChunks,
            String question
    ) {}

}