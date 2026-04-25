// SoundType.java
package com.fly.uloganalyzer.domain;

public enum SoundType {
    FILE_LOADED("sounds/FILE_LOADED.wav"),
    ANALYSIS_SUCCESS("sounds/ANALYSIS_SUCCESS.wav"),
    ANALYSIS_ERROR("sounds/ANALYSIS_ERROR.wav"),
    FILTER_COMPLETE("sounds/ses.mp3");
    
    private final String defaultPath;
    
    SoundType(String defaultPath) {
        this.defaultPath = defaultPath;
    }
    
    public String getDefaultPath() {
        return defaultPath;
    }
}