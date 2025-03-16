package com.vishnu.mailwithai.app;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class EmailGeneratorService {
    @Value("${gemini.api.url}")
    private String geminiapiurl;

    @Value("${gemini.api.key}")
    private String geminiapikey;

    private final WebClient webClient;

    public EmailGeneratorService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public String generateEmailReply(EmailRequest emailRequest){
        // Build the prompt for ai
        String prompt = buildPrompt(emailRequest);
        // construct the format of the request
        Map<String, Object> requestBody = Map.of(
                "contents" , new Object[]{
                        Map.of("parts", new Object[]{
                                Map.of("text",prompt)
                        })
                }
        );
        // do request and get response
        String response = webClient.post()
                .uri(geminiapiurl + geminiapikey)
                .header("Content-Type","application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        //extract response and return
        return extractResponseContent(response);
    }

    private String extractResponseContent(String response) {
        try{
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(response);
            return rootNode.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();

        }catch(Exception e){
            return "Error processing request: " + e.getMessage();
        }
    }

    private String buildPrompt(EmailRequest emailRequest) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate a professional email reply for the following email content. Please dont generate a subject line ");
        if (emailRequest.getTone() != null && !emailRequest.getTone().isEmpty()){
            prompt.append("use a").append(emailRequest.getTone()).append("tone.");
        }
        prompt.append("\nOriginal email : \n").append(emailRequest.getEmailContent());
        return prompt.toString();
    }
}
