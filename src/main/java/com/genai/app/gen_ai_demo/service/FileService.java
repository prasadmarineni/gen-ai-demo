package com.genai.app.gen_ai_demo.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.genai.app.gen_ai_demo.entity.FileEntity;
import com.genai.app.gen_ai_demo.repository.FileRepository;

@Service
public class FileService {

    @Autowired
    private FileRepository fileRepository;

    public List<FileEntity> getFiles() {
        return fileRepository.findAll();
    }
    
    public void saveFileName(String fileName) {
        FileEntity fileEntity = new FileEntity();
        fileEntity.setFileName(fileName);
        fileRepository.save(fileEntity);
    }

    public boolean doesFileExist(String fileName) {
        return fileRepository.existsByFileName(fileName);
    }
}
