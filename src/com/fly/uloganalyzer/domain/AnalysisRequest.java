package com.fly.uloganalyzer.domain;

import java.util.List;

public record AnalysisRequest(List<CSVData> csvData, String prompt, boolean useCustomPrompt) {
    public AnalysisRequest {
        if (csvData == null || csvData.isEmpty()) {
            throw new IllegalArgumentException("CSV data cannot be null or empty");
        }
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("Prompt cannot be null or empty");
        }
    }
    
    public boolean isCustomPrompt() {
        return useCustomPrompt;
    }
}