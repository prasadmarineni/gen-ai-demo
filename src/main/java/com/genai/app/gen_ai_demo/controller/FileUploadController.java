package com.genai.app.gen_ai_demo.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.genai.app.gen_ai_demo.entity.FileEntity;
import com.genai.app.gen_ai_demo.service.FileProcessingService;
import com.genai.app.gen_ai_demo.service.FileService;

@RestController
public class FileUploadController {

	@Autowired
	private FileProcessingService fileProcessingService;

	@Autowired
	private FileService fileService;

	@Autowired
    private ResourceLoader resourceLoader;

	private static String uploadDir = null;;
	
    public String getResourcesPath() {
        Resource resource = resourceLoader.getResource("classpath:application.properties");
        String resourcePath;
		try {
			resourcePath = resource.getFile().getAbsolutePath();
			Path absolutePath = Paths.get(resourcePath).toAbsolutePath();

	        // Navigate back two directories to get to the root of the project
	        Path projectRootPath = absolutePath
	                .getParent() // classes directory
	                .getParent() // target directory
	                .getParent(); // project root directory

	        // Add src/main/resources to the path
	        Path resourcesPath = projectRootPath.resolve("src/main/resources/files/");

	        System.out.println("Absolute Path to resources: " + resourcesPath);
	        
	        return resourcesPath.toString();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return "./";
    }
    
    
	public String getUploadDir() {
		if(uploadDir == null)
			uploadDir = getResourcesPath() + File.separator; 
		return uploadDir;
	}


	@GetMapping("/files")
	public List<String> listUploadedFiles() {

		List<FileEntity> files = fileService.getFiles();

		return files != null ? files.stream().map(FileEntity::getFileName) // Adjust this method based on your actual
																			// implementation
				.collect(Collectors.toList()) : List.of();
	}

	@PostMapping("/upload")
	public ResponseEntity<?> handleFileUpload(@RequestParam("file") MultipartFile file) {

		if (file.isEmpty()) {
			return ResponseEntity.badRequest().body("Please select a file to upload.");
		}

		String fileName = file.getOriginalFilename();
		System.out.println("File upload START: " + fileName);

		try {
			// Save the uploaded file or process it directly
			String filePath = getUploadDir() + fileName;

			// Create the directory if it doesn't exist
			File dir = new File(getUploadDir());
			if (!dir.exists()) {
				dir.mkdirs(); // This will create the directory and any necessary parent directories
			}

			// Construct the file path
			File serverFile = new File(dir, file.getOriginalFilename());
			file.transferTo(serverFile); // Save the file

			// Process the uploaded PDF file
			fileProcessingService.processPdf(filePath, fileName);

			System.out.println("File upload END: " + fileName);

			return ResponseEntity.ok().body("File uploaded successfully: " + fileName);
		} catch (Exception e) {
			System.err.println(e);
			return ResponseEntity.status(500).body("File upload failed for " + fileName + ", error: " + e.getMessage());
		}
	}
}
