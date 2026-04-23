package com.ai.docreader;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgDistanceType;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIndexType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class AppConfig {
		@Bean
		ChatClient chatClient(OpenAiChatModel openAiChatModel) {
	        return ChatClient.builder(openAiChatModel)
	                .defaultSystem("""
	                        You are DocReader, a helpful AI assistant that answers questions
	                        strictly based on the provided document context.
	                        If the answer is not in the context, say:
	                        "I couldn't find this information in the uploaded document."
	                        Be concise and factual.
	                        """)
	                .build();
	    }
		
		@Bean
	    public VectorStore vectorStore(JdbcTemplate jdbcTemplate,
	                                   OllamaEmbeddingModel ollamaEmbeddingModel) {
	        return PgVectorStore.builder(jdbcTemplate, ollamaEmbeddingModel)
	                .dimensions(768)
	                .distanceType(PgDistanceType.COSINE_DISTANCE)
	                .indexType(PgIndexType.HNSW)
	                .initializeSchema(true)
	                .build();
	    }
	 

}
