package com.genai.app.gen_ai_demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.genai.app.gen_ai_demo.entity.DocEmbedding;

public interface DocEmbeddingRepository extends JpaRepository<DocEmbedding, Long> {

}
