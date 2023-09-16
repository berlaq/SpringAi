package com.springai.springaidemo;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.ai.client.AiClient;
import org.springframework.ai.client.AiResponse;
import org.springframework.ai.prompt.Prompt;
import org.springframework.ai.prompt.PromptTemplate;
import org.springframework.ai.prompt.messages.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

@RestController
public class DemoRestController {

    private final AiClient aiClient;
    @Value("${openai.api-key}")
    private String apiKey;

    public DemoRestController(AiClient aiClient) {
        this.aiClient = aiClient;
    }

    @GetMapping("/joke")
    public String getJoke(@RequestParam(name = "topic") String topic) {
        PromptTemplate promptTemplate = new PromptTemplate("""
                I'm bored with hello world apps. How about you give me a joke about {topic}? to get started?
                Include some programming terms in your joke to make it more fun.
                """);
        promptTemplate.add("topic", topic);
        return this.aiClient.generate(promptTemplate.create()).getGeneration().getText();
    }

    @GetMapping("/bestMovie")
    public String getBestMovie(@RequestParam(name = "genre") String genre, @RequestParam(name = "year") String year) {
        PromptTemplate promptTemplate = new PromptTemplate("""
                I'm bored with hello world apps. How about you give me a movie about {genre} in {year} to get started?
                But pick the best movie you can think of. I'm a movie critic, after all. IMDB ratings are a good place to start.
                And which actor or actress stars in it? And who directed it? And who wrote it? Can you give me a short plot summary and also it's name?
                But don't give me too much information. I want to be surprised.
                And please give me these details in the following JSON format: genre, year, movieName, actor, director, writer, plot.
                """);
        Map.of("genre", genre, "year", year).forEach(promptTemplate::add);
        AiResponse generate = this.aiClient.generate(promptTemplate.create());
        return generate.getGeneration().getText();
    }

    @GetMapping(value = "/image", produces = "image/jpeg")
    public ResponseEntity<InputStreamResource> getImage(@RequestParam(name = "topic") String topic) throws URISyntaxException {
        PromptTemplate promptTemplate = new PromptTemplate("""
                I'm bored with hello world apps. Can you create me a prompt about {topic}. Enhance the topic I gave you. Make it fancy.
                Make resolution 256x256 but in Json it needs to be string. I want only 1 creation. Give me as JSON format: prompt, n, size.
                Do not make any comments. Just JSON file. In JSON, n ise an integer
                """);
        promptTemplate.add("topic", topic);
        String imagePrompt = this.aiClient.generate(promptTemplate.create()).getGeneration().getText();

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + apiKey);
        headers.add("Content-Type", "application/json");
        HttpEntity<String> httpEntity = new HttpEntity<>(imagePrompt,headers);

        String imageUrl = restTemplate.exchange("https://api.openai.com/v1/images/generations", HttpMethod.POST, httpEntity, GeneratedImage.class)
                .getBody().getData().get(0).getUrl();
        byte[] imageBytes = restTemplate.getForObject(new URI(imageUrl), byte[].class);
        return ResponseEntity.ok().body(new InputStreamResource(new java.io.ByteArrayInputStream(imageBytes)));
    }

}
