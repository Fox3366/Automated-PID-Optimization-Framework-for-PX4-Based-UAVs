package com.fly.uloganalyzer.domain;

public record AnalysisResult(String content, boolean isSuccess, String errorMessage) {
    public static AnalysisResult success(String content) {
        return new AnalysisResult(content, true, null);
    }
    
    public static AnalysisResult error(String errorMessage) {
        return new AnalysisResult(null, false, errorMessage);
    }
    
    public boolean hasError() {
        return !isSuccess;
    }
}