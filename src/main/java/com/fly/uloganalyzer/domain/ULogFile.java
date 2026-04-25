package com.fly.uloganalyzer.domain;

import java.nio.file.Path;

public record ULogFile(Path filePath, String fileName) {
    public ULogFile {
        if (filePath == null || fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("File path and name cannot be null or empty");
        }
    }
    
    public String getDisplayName() {
        return fileName;
    }
}