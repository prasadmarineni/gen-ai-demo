package com.genai.app.gen_ai_demo.service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
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

			// Spring AI PDF Reader
			List<Document> documents = getPdfDocuments(pdfFilePath);

			// Assuming Document has a method to create from content only
			List<Document> cleanedDocuments = documents.stream().map(doc ->
			getDocument(doc, fileName, pdfFilePath)).collect(Collectors.toList());

			// Apache PDFBox PDF Reader
			// List<Document> cleanedDocuments = getPdfBoxDocuments(pdfFilePath);

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
/*
	private List<Document> getPdfBoxDocuments(String pdfFilePath) throws Exception {

		List<Document> documentDataList = new ArrayList<>();
		Resource pdfResource;
		try {
			pdfResource = new UrlResource("file:" + pdfFilePath);
			File pdfFile = pdfResource.getFile();

			try (PDDocument document = PDDocument.load(pdfFile)) {
				PDFTextStripper textStripper = new PDFTextStripper();

				for (int pageNumber = 0; pageNumber < document.getNumberOfPages(); pageNumber++) {
					PDPage page = document.getPage(pageNumber);
					String text = textStripper.getText(document);

					text = removeDuplicateSpecialCharsFromText(text);
					// Extract images
					List<String> imagePaths = extractImagesFromPage(page, pageNumber);

					List<Media> media = new ArrayList<>();

					for (String imagePath : imagePaths) {
						
						URL url = convertToURL(imagePath);
						MimeType mimeType = MimeTypeUtils.IMAGE_PNG;
						
					    System.out.println("MIME Type: " + mimeType);
					    
						Media m = new Media(mimeType, url);
						media.add(m);
					}

					Map<String, Object> metadata = new HashMap<>();

					metadata.put("page_number", pageNumber);
					metadata.put("file_name", pdfFile.getName());
					metadata.put("path", pdfFile.getAbsolutePath());

					Document doc = new Document(text, media, metadata);

					// Store text and images in Document
					documentDataList.add(doc);
				}

			}
			
			// Read the PDF and apply the text splitter
			var textSplitter = new TokenTextSplitter();
			List<Document> documents = textSplitter.apply(documentDataList);

			return documents;

		} catch (MalformedURLException e) {
			e.printStackTrace();
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}

	}

*/
	
	public List<String> extractImagesFromPage(PDPage page, int pageNumber) throws IOException {
		List<String> imagePaths = new ArrayList<>();
		PDResources resources = page.getResources();

		for (COSName name : resources.getXObjectNames()) {
			if (resources.getXObject(name) instanceof PDImageXObject) {
				PDImageXObject imageXObject = (PDImageXObject) resources.getXObject(name);
				BufferedImage bufferedImage = imageXObject.getImage();

				// Save the image to a file
				String imagePath = "extracted_image_page_" + pageNumber + "_" + name.getName() + ".png";
				File imageFile = new File(imagePath);
				ImageIO.write(bufferedImage, "PNG", imageFile);

				imagePaths.add(imageFile.getAbsolutePath());
			}
		}

		return imagePaths;
	}

	public List<Document> getPdfDocuments(String pdfFilePath) throws MalformedURLException {
		Resource pdfResource = new UrlResource("file:" + pdfFilePath); // Dynamically pass the file path
		// If you want custom config then pass the PdfDocumentReaderConfig

		PagePdfDocumentReader reader = new PagePdfDocumentReader(pdfResource);

		List<Document> pages = reader.get();

		// Read the PDF and apply the text splitter
		var textSplitter = new TokenTextSplitter();
		List<Document> documents = textSplitter.apply(pages);
		return documents;
	}

	public Document getDocument(Document doc, String fileName, String pdfFilePath) {

		Map<String, Object> metaData = doc.getMetadata();
		if (metaData == null) {
			metaData = new HashMap<>();
		}

		metaData.put("path", pdfFilePath);

		return new Document(doc.getId(), removeDuplicateSpecialCharsFromText(doc.getContent()), List.of(), metaData);
	}

	public String removeDuplicateSpecialCharsFromText(String rawText) {
		// Step 1: Remove excessive dots, dashes, or other patterns
		String cleanedText = rawText;

		// Example: Replace multiple dots with a single dot
		cleanedText = cleanedText.replaceAll("\\.{2,}", ".");

		// Example: Replace multiple dashes with a single dash
		cleanedText = cleanedText.replaceAll("-{2,}", "-");

		// Step 2: Replace long sequences of dots or dashes around headers (e.g.,
		// "Purpose ...........")
		cleanedText = cleanedText.replaceAll("\\.{2,}\\s*\\d+", ""); // Remove dots followed by numbers
		cleanedText = cleanedText.replaceAll("-{2,}\\s*\\d+", ""); // Remove dashes followed by numbers

		// Step 3: Remove excessive whitespaces
		cleanedText = cleanedText.trim().replaceAll("\\s{2,}", " "); // Normalize multiple spaces to one

		// Step 4: Remove unwanted characters or patterns (you can customize this)
		cleanedText = cleanedText.replaceAll("[^\\x20-\\x7E]", ""); // Remove non-printable ASCII characters

		// Step 5: Use replaceAll with regex to replace multiple "x" with a single "x"
		//cleanedText = cleanedText.replaceAll("\\b(x)\\s*", "$1 ").replaceAll(" +", " ").trim();
		
		return cleanedText;
	}
	
	public static URL convertToURL(String imagePath) {
		try {
			// Create a File object from the image path
			File imageFile = new File(imagePath);
			// Convert the file to a URL
			return imageFile.toURI().toURL();
		} catch (MalformedURLException e) {
			e.printStackTrace(); // Handle the exception as needed
			return null;
		}
	}
}
