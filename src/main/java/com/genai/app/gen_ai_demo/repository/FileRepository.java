package com.genai.app.gen_ai_demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.genai.app.gen_ai_demo.entity.FileEntity;

public interface FileRepository extends JpaRepository<FileEntity, Long> {
    boolean existsByFileName(String fileName);
}
