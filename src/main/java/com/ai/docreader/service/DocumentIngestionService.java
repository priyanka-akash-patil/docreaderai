package com.ai.docreader.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.ai.docreader.controller.DocumentController;

import java.io.IOException;
import java.util.List;

/**
 * Handles the ingestion pipeline:
 *
 *   PDF file  →  parse pages  →  split into chunks  →  embed  →  store in PGVector
 *
 * Key concepts:
 *   - PagePdfDocumentReader: Spring AI's built-in PDF reader (uses PDFBox internally)
 *   - TokenTextSplitter: splits text into overlapping chunks for better retrieval
 *   - VectorStore: Spring AI abstraction over PGVector (embed + store in one call)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentIngestionService {
	private Logger log = LoggerFactory.getLogger(DocumentIngestionService.class);

	@Autowired
    private  VectorStore vectorStore;

    /**
     * Full ingestion pipeline for a PDF file.
     *
     * @param file the uploaded PDF (MultipartFile from the controller)
     * @return number of chunks stored
     */
    public int ingestPdf(MultipartFile file) throws IOException {

        log.info("Starting ingestion for: {}", file.getOriginalFilename());

        // ── Step 1: Read the PDF ────────────────────────────────────────────
        // We convert the MultipartFile to a ByteArrayResource so Spring AI
        // can read it without needing to save it to disk first.
        ByteArrayResource pdfResource = new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename(); // needed for metadata
            }
        };

        PdfDocumentReaderConfig readerConfig = PdfDocumentReaderConfig.builder()
                .withPagesPerDocument(1)   // One Document object per PDF page
                .build();

        PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(pdfResource, readerConfig);
        List<Document> pages = pdfReader.get();
        log.info("Parsed {} pages from PDF", pages.size());

        // ── Step 2: Split into chunks ───────────────────────────────────────
        // Why chunk? LLMs have a token limit. We can't send 50 pages at once.
        // Chunking also improves retrieval precision — smaller, focused chunks
        // are more relevant than huge page-dumps.
        //
        // TokenTextSplitter parameters:
        //   defaultChunkSize     = 500 tokens per chunk (good balance)
        //   minChunkSizeChars    = 350 minimum chars (avoid tiny useless chunks)
        //   minChunkLengthToEmbed= 5   (skip very short fragments)
        //   maxNumChunks         = 10000
        //   keepSeparator        = true (preserve sentence boundaries)
        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(500)
                .withMinChunkSizeChars(350)
                .withMinChunkLengthToEmbed(5)
                .withMaxNumChunks(10000)
                .withKeepSeparator(true)
                .build();
        List<Document> chunks = splitter.apply(pages);
        log.info("Split into {} chunks", chunks.size());

        // ── Step 3: Attach metadata to every chunk ──────────────────────────
        // Metadata is stored alongside the vector in PGVector.
        // We can use it later to filter by filename or show source citations.
        String filename = file.getOriginalFilename();
        chunks.forEach(chunk -> {
            chunk.getMetadata().put("filename", filename);
            chunk.getMetadata().put("filesize", file.getSize());
        });

        // ── Step 4: Embed + Store ───────────────────────────────────────────
        // vectorStore.add() does two things automatically:
        //   1. Calls OpenAI embedding API to get a vector for each chunk
        //   2. Stores (chunk text + vector + metadata) in PGVector
        vectorStore.add(chunks);
        log.info("Successfully stored {} chunks for file: {}", chunks.size(), filename);

        return chunks.size();
    }

}