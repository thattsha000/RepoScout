package com.reposcout.server;


import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@Service
public class GeminiClient {

   @Value("${gemini.api-key}")
private String apiKey;

    @Value("${gemini.models}")
private String[] models;

public String generate(String prompt) throws Exception {
ObjectMapper mapper = new ObjectMapper();
ObjectNode body = mapper.createObjectNode();
body.putArray("contents").addObject().putArray("parts").addObject().put("text", prompt);
body.putObject("generationConfig").put("responseMimeType", "application/json");
String json = mapper.writeValueAsString(body);
HttpClient client = HttpClient.newHttpClient();
HttpResponse<String> response = null;
// try each model in order; move to the next only if this one is overloaded or rate limited
for (String model : models) {
String url = "https://generativelanguage.googleapis.com/v1beta/models/"
+ model.trim() + ":generateContent";
HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).header("Content-Type", "application/json").header("x-goog-api-key", apiKey).POST(HttpRequest.BodyPublishers.ofString(json))  // <- json goes here
.build();
response = client.send(request, HttpResponse.BodyHandlers.ofString());
int status = response.statusCode();
if (status != 503 && status != 429) break;   // success or a real error: stop
System.out.println("Model " + model + " returned " + status + ", trying next");
        }
if (response.statusCode() != 200) {
//catch any errors caused by a invalid request
throw new RuntimeException("Gemini returned " + response.statusCode() + ": " + response.body());
        }
//parse the strsing as something you can read
JsonNode root = mapper.readTree(response.body());
String text = root.at("/candidates/0/content/parts/0/text").asText();
if (text.isEmpty()) {
throw new RuntimeException("Gemini returned no text: " + response.body());
        }
return text;

    }
}