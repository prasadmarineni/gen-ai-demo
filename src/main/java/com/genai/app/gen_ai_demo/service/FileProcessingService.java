package com.genai.app.gen_ai_demo.service;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.chat.messages.Media;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class FileProcessingService {

	private final VectorStore vectorStore;

	private final JdbcClient jdbcClient;

	private final FileService fileService;
	
	

	@Value("classpath:/BNS.pdf")
	private Resource pdfResource;

	public FileProcessingService(VectorStore vectorStore, JdbcClient jdbcClient, FileService fileService) {
		this.vectorStore = vectorStore;
		this.jdbcClient = jdbcClient;
		this.fileService = fileService;
	}

	@PostConstruct
	public void init() {

		Integer count = jdbcClient.sql("select COUNT(*) from vector_store").query(Integer.class).single();

		System.out.println("No of Records in the PG Vector Store = " + count);

		System.out.println("Application is ready to Serve the Requests");
	}

	public void processPdf(String pdfFilePath, String fileName) throws Exception {

		try {

			boolean fileExist = fileService.doesFileExist(fileName);
			if (fileExist) {
				System.err.println("This file is already available in database, File Name: " + fileName);
				throw new Exception("This file is already available in database, File Name: " + fileName);
			}

			System.out.println("File process START: " + fileName);

			Resource pdfResource = new UrlResource("file:" + pdfFilePath); // Dynamically pass the file path
			// If you want custom config then pass the PdfDocumentReaderConfig

			PagePdfDocumentReader reader = new PagePdfDocumentReader(pdfResource);

			// Read the PDF and apply the text splitter
			var textSplitter = new TokenTextSplitter();
			List<Document> documents = textSplitter.apply(reader.get());

			// Assuming Document has a method to create from content only
			List<Document> cleanedDocuments = documents.stream().map(doc -> getDocument(doc, fileName, pdfFilePath))
					.collect(Collectors.toList());

			System.out.println("File processed document count: " + cleanedDocuments.size());

			// Accept the processed documents
			vectorStore.accept(cleanedDocuments);

			// Saving processed file name in DB
			fileService.saveFileName(fileName);

			System.out.println("File process END: " + fileName);

		} catch (MalformedURLException e) {
			System.err.println("Invalid PDF file path: " + e.getMessage());
		}
		
	}

	private Document getDocument(Document doc, String fileName, String pdfFilePath) {

		Map<String, Object> metaData = doc.getMetadata();
		if (metaData == null) {
			metaData = new HashMap<>();
		}

		metaData.put("path", pdfFilePath);

		Collection<Media> media = doc.getMedia();
		List<Media> mediaList = List.of();
		if (media != null && !media.isEmpty())
			mediaList = new ArrayList<>(media);

		return new Document(doc.getId(), removeDuplicateSpecialCharsFromText(doc.getContent()), mediaList, metaData);
	}
	
	public String removeDuplicateSpecialCharsFromText(String rawText) {
        // Step 1: Remove excessive dots, dashes, or other patterns
        String cleanedText = rawText;

        // Example: Replace multiple dots with a single dot
        cleanedText = cleanedText.replaceAll("\\.{2,}", ".");

        // Example: Replace multiple dashes with a single dash
        cleanedText = cleanedText.replaceAll("-{2,}", "-");

        // Step 2: Replace long sequences of dots or dashes around headers (e.g., "Purpose ...........")
        cleanedText = cleanedText.replaceAll("\\.{2,}\\s*\\d+", ""); // Remove dots followed by numbers
        cleanedText = cleanedText.replaceAll("-{2,}\\s*\\d+", ""); // Remove dashes followed by numbers

        // Step 3: Remove excessive whitespaces
        cleanedText = cleanedText.trim().replaceAll("\\s{2,}", " "); // Normalize multiple spaces to one

        // Step 4: Remove unwanted characters or patterns (you can customize this)
        cleanedText = cleanedText.replaceAll("[^\\x20-\\x7E]", ""); // Remove non-printable ASCII characters
        
        return cleanedText;
    }

}
