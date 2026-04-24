package com.fly.uloganalyzer.business;

import com.fly.uloganalyzer.domain.*;
import com.fly.uloganalyzer.infrastructure.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AnalysisService {
    
    private ULogParser ulogParser;
    private CSVFilter csvFilter;
    private OpenAIClient openAIClient;
    private final AudioPlayer audioPlayer;
    private ExecutorService executor;
    
    public AnalysisService(ULogParser ulogParser, CSVFilter csvFilter, 
                          OpenAIClient openAIClient, AudioPlayer audioPlayer) {
        this.ulogParser = ulogParser;
        this.csvFilter = csvFilter;
        this.openAIClient = openAIClient;
        this.audioPlayer = audioPlayer;
        this.executor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
    }
    
    public CompletableFuture<List<CSVData>> processULogFile(ULogFile ulogFile) {
        return CompletableFuture.supplyAsync(() -> {
            audioPlayer.playSound(SoundType.FILE_LOADED);
            
            List<CSVData> allCsvData = ulogParser.parseULog(ulogFile);
            List<CSVData> filteredData = csvFilter.filterEssentialCSV(allCsvData, ulogFile.getDisplayName());
            
            audioPlayer.playSound(SoundType.FILTER_COMPLETE);
            return filteredData;
            
        }, executor);
    }
    
    public CompletableFuture<AnalysisResult> analyzeData(List<CSVData> csvData, 
                                                        String customPrompt, 
                                                        boolean useCustomPrompt) {
        return CompletableFuture.supplyAsync(() -> {
            
            try {
                String prompt = useCustomPrompt ? customPrompt : buildDefaultPrompt();
                AnalysisRequest request = new AnalysisRequest(csvData, prompt, useCustomPrompt);
                AnalysisResult result = openAIClient.analyzeData(request);
                
                if (result.isSuccess()) {
                    audioPlayer.playSound(SoundType.ANALYSIS_SUCCESS);
                } else {
                    audioPlayer.playSound(SoundType.ANALYSIS_ERROR);
                }
                
                return result;
                
            } catch (Exception e) {
                audioPlayer.playSound(SoundType.ANALYSIS_ERROR);
                return AnalysisResult.error("Analysis service error: " + e.getMessage());
            }
            
        }, executor);
    }
    
    private String buildDefaultPrompt() {
        return "Bu drone uçuş verilerini kapsamlı şekilde analiz et ve Türkçe detaylı rapor hazırla.";
    }
    
    public void updateDependencies(ULogParser ulogParser, CSVFilter csvFilter, OpenAIClient openAIClient) {
        this.ulogParser = ulogParser;
        this.csvFilter = csvFilter;
        this.openAIClient = openAIClient;
        System.out.println("AnalysisService bağımlılıkları güncellendi.");
    }
    
    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            System.out.println("AnalysisService shutdown completed");
        }
    }
}