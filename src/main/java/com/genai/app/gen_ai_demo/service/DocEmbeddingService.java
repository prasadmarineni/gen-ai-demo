package com.genai.app.gen_ai_demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.genai.app.gen_ai_demo.entity.DocumentChunk;
import com.genai.app.gen_ai_demo.repository.DocEmbeddingRepository;

import java.util.List;

@Service
public class DocEmbeddingService {

    @Autowired
    private DocEmbeddingRepository docEmbeddingRepository;

    public List<DocumentChunk> searchDocuments(String question) {
        // Convert the question to an embedding (implement this logic)
        List<Float> queryEmbedding = convertQuestionToEmbedding(question);

        // Perform similarity search
        return this.similaritySearch(queryEmbedding, 5);
    }

    private List<Float> convertQuestionToEmbedding(String question) {
        // Implement your logic to convert the question to embeddings
        return List.of(0.1f, 0.2f, 0.3f); // Placeholder
    }
    
    public List<DocumentChunk> similaritySearch(List<Float> queryEmbedding, int topK) {
        // This is a hypothetical implementation. You would replace this with your actual
        // similarity search logic, perhaps using a native query or leveraging
        // your vector database's capabilities.

        // Example: Get all documents (not efficient for large datasets)
        List<DocumentChunk> allDocuments = docEmbeddingRepository.findAll();

        // Calculate similarities and sort to get the top K
        return allDocuments.stream()
                .sorted((doc1, doc2) -> {
                    // Replace with your similarity calculation
                    float sim1 = calculateSimilarity(queryEmbedding, doc1.getEmbeddings());
                    float sim2 = calculateSimilarity(queryEmbedding, doc2.getEmbeddings());
                    return Float.compare(sim2, sim1); // Descending order
                })
                .limit(topK)
                .toList();
    }

    private float calculateSimilarity(List<Float> query, List<Float> docEmbeddings) {
        // Implement your similarity calculation (e.g., cosine similarity)
        return 0.0f; // Replace with actual calculation
    }
}
