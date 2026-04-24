package com.fly.uloganalyzer.infrastructure.impl;

import com.fly.uloganalyzer.infrastructure.ConfigLoader;
import com.fly.uloganalyzer.infrastructure.OpenAIClient;
import com.fly.uloganalyzer.domain.AnalysisRequest;
import com.fly.uloganalyzer.domain.AnalysisResult;
import com.fly.uloganalyzer.domain.CSVData;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpOpenAIClient implements OpenAIClient {
    
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    
    private final ConfigLoader configLoader;
    private final String apiKey;
    
    public HttpOpenAIClient(ConfigLoader configLoader) {
        this.configLoader = configLoader;
        this.apiKey = configLoader.getApiKey();
    }
    
    @Override
    public AnalysisResult analyzeData(AnalysisRequest request) {
        System.out.println("OpenAI API call started");
        
        HttpURLConnection connection = null;
        
        try {
            URL url = new URL(API_URL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);
            
            int timeoutSeconds = configLoader.getOpenAITimeout();
            connection.setConnectTimeout(30000); 
            connection.setReadTimeout(timeoutSeconds * 1000); 
            
            String prompt = buildOptimizedPrompt(request);
            String requestBody = buildRequestBody(prompt);
            
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            
            int responseCode = connection.getResponseCode();
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                StringBuilder response = new StringBuilder();
                try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                }
                
                String content = extractContentWithRegex(response.toString());
                System.out.println("OpenAI API call successful");
                return AnalysisResult.success(content);
                
            } else {
                StringBuilder errorResponse = new StringBuilder();
                try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                    
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        errorResponse.append(responseLine.trim());
                    }
                }
                
                String errorMsg = "API Error: " + responseCode + " - " + errorResponse.toString();
                System.err.println(errorMsg);
                return AnalysisResult.error(errorMsg);
            }
            
        } catch (Exception e) {
            String errorMsg = "Request failed: " + e.getMessage();
            System.err.println(errorMsg);
            return AnalysisResult.error(errorMsg);
            
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    private String buildOptimizedPrompt(AnalysisRequest request) {
        if (request.isCustomPrompt()) {
            return request.prompt() + "\n\nCSV Data:\n" + formatCSVDataForPrompt(request.csvData());
        } else {
            return buildDefaultPrompt(request.csvData());
        }
    }
    
    private String buildDefaultPrompt(List<CSVData> csvData) {
        String filePrompt = configLoader.getDefaultPrompt();
        return filePrompt + "\n\nVERİLER:\n" + formatCSVDataForPrompt(csvData);
    }
    
    private String formatCSVDataForPrompt(List<CSVData> csvData) {
        StringBuilder sb = new StringBuilder();
        
        int limit = configLoader.getCsvFileLimit();
        int fileCount = Math.min(csvData.size(), limit);
        
        for (int i = 0; i < fileCount; i++) {
            CSVData data = csvData.get(i);
            sb.append("\n--- ").append(data.filename()).append(" ---\n");
            sb.append(data.getShortContent());
            sb.append("...\n");
        }
        
        if (csvData.size() > limit) {
            sb.append("\n... ve ").append(csvData.size() - limit).append(" dosya daha\n");
        }
        
        return sb.toString();
    }
    
    private String buildRequestBody(String prompt) {
        String escapedPrompt = prompt.replace("\\", "\\\\")
                                   .replace("\"", "\\\"")
                                   .replace("\n", "\\n")
                                   .replace("\r", "\\r")
                                   .replace("\t", "\\t");
        
        String modelName = configLoader.getOpenAIModel();
        int maxTokens = configLoader.getOpenAIMaxTokens();
        
  
        return String.format(
            "{" +
                "\"model\":\"%s\"," +
                "\"messages\":[" +
                    "{\"role\":\"system\",\"content\":\"Sen bir VTOL PID verisi analiz uzmanısın. Analizleri Türkçe ve yeteri kadar detaylı yap.\"}," +
                    "{\"role\":\"user\",\"content\":\"%s\"}" +
                "]," +
                "\"max_completion_tokens\":%d" + 
            "}",
            modelName, escapedPrompt, maxTokens
        );
    }
    
    private String extractContentWithRegex(String jsonResponse) {
        try {
            Pattern pattern = Pattern.compile("\"content\"\\s*:\\s*\"(.*?)\"(?=\\s*[,}])", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(jsonResponse);
            
            if (matcher.find()) {
                String rawContent = matcher.group(1);
                
                String formatted = rawContent
                    .replace("\\n", "\n")
                    .replace("\\r", "")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\")
                    .replace("\\t", "\t");

                formatted = formatted.replaceAll("\\*\\*(.*?)\\*\\*", "$1");
                formatted = formatted.replaceAll("###\\s?", "");
                formatted = formatted.replaceAll("##\\s?", "");
                formatted = formatted.replaceAll("#\\s?", "");
                
                return formatted.trim();
            }
            
            return "";
            
        } catch (Exception e) {
            System.err.println("Metin formatlama hatası: " + e.getMessage());
            return "Yanıt işlenirken hata oluştu.";
        }
    }
}