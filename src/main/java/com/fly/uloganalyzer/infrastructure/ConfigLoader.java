package com.fly.uloganalyzer.infrastructure;

import java.util.List;
import com.fly.uloganalyzer.domain.SoundType;

public interface ConfigLoader {
    String getApiKey();
    String getSoundPath(SoundType type);
    void saveApiKey(String apiKey);
    String getDefaultPrompt();
    List<String> getCsvFilterPatterns();
    
    String getOpenAIModel();      
    int getOpenAITimeout();       
    int getOpenAIMaxTokens();     
    int getCsvFileLimit();       
    
    
    void saveSettings(String model, int timeout, int maxTokens, int csvLimit);
    void saveDefaultPrompt(String promptContent);
    void saveFilters(String filterContent);
}