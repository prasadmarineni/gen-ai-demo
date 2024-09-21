package com.genai.app.gen_ai_demo.controller;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class ChatController {

	private final ChatModel chatModel;
	private final VectorStore vectorStore;

	private String prompt = """
			Use the information from the DOCUMENTS
			and provide accurate answers. If unsure or if the answer isn't found in the DOCUMENTS for the QUESTION,
			simply state that you don't know the answer, also add document citations from meta data in the new line

			QUESTION:
			{input}

			DOCUMENTS:
			{documents}

			""";

	public ChatController(ChatModel chatModel, VectorStore vectorStore) {
		this.chatModel = chatModel;
		this.vectorStore = vectorStore;
	}

	@GetMapping("/chat")
	public Map<String, String> simplify(@RequestParam(value = "question", required = false) String question) {

		String answer = "Ask something";

		if (question != null && !question.isBlank()) {
			PromptTemplate template = new PromptTemplate(prompt);
			Map<String, Object> promptsParameters = new HashMap<>();
			String documentStr = findSimilarData(question);
			
			promptsParameters.put("input", question);
			promptsParameters.put("documents", documentStr);

			ChatResponse chatResponse = chatModel.call(template.create(promptsParameters));
			
			answer = chatResponse.getResult().getOutput().getContent();

		}

		return Map.of("answer", answer, "question", question);
	}

	private String findSimilarData(String question) {
		List<Document> documents = vectorStore.similaritySearch(SearchRequest.query(question).withTopK(5));

		return documents.stream().map(document -> document.getContent().toString()).collect(Collectors.joining());

	}
}
