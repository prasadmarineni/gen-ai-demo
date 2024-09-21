package com.genai.app.gen_ai_demo.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Data
@Entity(name = "document-chunk")
public class DocumentChunk {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = true)
    private String fileName;
    
    @Column(nullable = true)
    private String path;

    @ElementCollection
    private List<Float> embeddings; // Store the vector embeddings

    @Column(nullable = true)
    private String version;

    @Column(nullable = true)
    private String tags;
    
    
}
