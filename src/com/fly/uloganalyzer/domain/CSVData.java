package com.fly.uloganalyzer.domain;

public record CSVData(String filename, String content) {
    public CSVData {
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("Filename cannot be null or empty");
        }
        if (content == null) {
            content = "";
        }
    }
    
    public String getFullContent() {
        return content;
    }

    public String getShortContent() {
        return content;
    }
}