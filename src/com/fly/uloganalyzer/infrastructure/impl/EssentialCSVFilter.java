package com.fly.uloganalyzer.infrastructure.impl;

import com.fly.uloganalyzer.infrastructure.CSVFilter;
import com.fly.uloganalyzer.infrastructure.ConfigLoader;
import com.fly.uloganalyzer.domain.CSVData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Collectors;

public class EssentialCSVFilter implements CSVFilter {
    
    private final List<String> activePatterns;
    
    public EssentialCSVFilter(ConfigLoader configLoader) {
        this.activePatterns = configLoader.getCsvFilterPatterns();
        System.out.println("📊 [Filter Config] Loaded rules: " + activePatterns);
    }
    
    @Override
    public List<CSVData> filterEssentialCSV(List<CSVData> allCsvData, String nameOfULG) {
        System.out.println("--------------------------------------------------");
        System.out.println("🔍 FILTERING STARTED: Total input files: " + allCsvData.size());
        
        List<CSVData> filtered = allCsvData.stream()
                .filter(csv -> {
                    boolean isMatch = isEssentialCSV(csv.filename());
                    if (isMatch) {
                        System.out.println("✅ KEPT: " + csv.filename());
                    }
                    return isMatch;
                })
                .collect(Collectors.toList());
        
        System.out.println("📉 FILTERING ENDED: Kept " + filtered.size() + " files.");
        System.out.println("--------------------------------------------------");

        saveFilesToDisk(filtered, nameOfULG);

        return filtered;
    }
    
    private boolean isEssentialCSV(String filename) {
        return activePatterns.stream().anyMatch(filename::contains);
    }

    private void saveFilesToDisk(List<CSVData> dataList, String logFileName) {
        try {
            String projectPath = System.getProperty("user.dir");
            String timeStamp = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd__HH-mm-ss")
                                        .format(java.time.LocalDateTime.now());
            
            String cleanLogName = logFileName.replace(".ulg", "").replace(".ulog", "");

            Path sessionFolder = Paths.get(projectPath, "CSVs", cleanLogName + "_" + timeStamp);
            Files.createDirectories(sessionFolder);

            System.out.println("💾 [DISK] Dosyalar kaydediliyor: " + sessionFolder.toAbsolutePath());

            for (CSVData csv : dataList) {
                Path filePath = sessionFolder.resolve(csv.filename());
                Files.writeString(filePath, csv.content(), 
                    StandardOpenOption.CREATE, 
                    StandardOpenOption.TRUNCATE_EXISTING);
            }
            
            System.out.println("✅ [DISK] " + dataList.size() + " adet bağımsız CSV dosyası başarıyla yazıldı.");

        } catch (IOException e) {
            System.err.println("⚠️ [DISK] Dosya yazma hatası: " + e.getMessage());
        }
    }
}