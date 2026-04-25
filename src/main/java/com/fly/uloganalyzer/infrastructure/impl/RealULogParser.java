package com.fly.uloganalyzer.infrastructure.impl;

import com.fly.uloganalyzer.domain.CSVData;
import com.fly.uloganalyzer.domain.ULogFile;
import com.fly.uloganalyzer.infrastructure.ULogParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class RealULogParser implements ULogParser {

    private static final long TARGET_INTERVAL_MS = 100; 

    private static final int DECIMAL_PRECISION = 3; 

    @Override
    public List<CSVData> parseULog(ULogFile ulogFile) {
        Path inputPath = ulogFile.filePath();
        String displayName = ulogFile.getDisplayName();

        System.out.println("🚀 SMART ULOG parsing started for: " + displayName);
        
        List<CSVData> result = new ArrayList<>();
        Path tempDir = null;

        try {
            tempDir = Files.createTempDirectory("ulog_analysis_");
            System.out.println("📂 Temp folder: " + tempDir.toAbsolutePath());

            ProcessBuilder pb = new ProcessBuilder(
                "cmd.exe", "/c", "ulog2csv", 
                inputPath.toAbsolutePath().toString(), 
                "-o", 
                tempDir.toAbsolutePath().toString()
            );
            
            pb.redirectErrorStream(true); 
            Process process = pb.start();


            int exitCode = process.waitFor();
            if (exitCode != 0) {
                if (exitCode == 9009) {
                     throw new RuntimeException("Komut bulunamadı (Hata: 9009). 'ulog2csv' yüklü mü?");
                }
                throw new RuntimeException("Dönüştürme işlemi başarısız oldu. Hata kodu: " + exitCode);
            }

            try (Stream<Path> paths = Files.list(tempDir)) {
                paths.filter(p -> p.toString().toLowerCase().endsWith(".csv"))
                     .forEach(path -> {
                         try {
                             String filename = path.getFileName().toString();  
                         
                             String processedContent = readAndTimeSampleCSV(path);
                             
                             result.add(new CSVData(filename, processedContent));
                         } catch (IOException e) {
                             System.err.println("❌ CSV okunamadı: " + path);
                         }
                     });
            }

            System.out.println("✅ Parsing completed. CSV Count: " + result.size());

        } catch (Exception e) {
            throw new RuntimeException("ULOG parsing error: " + e.getMessage(), e);
        } finally {
            deleteTempDirectory(tempDir);
        }

  
        
        return result;
    }

    /**
     * CSV dosyasını satır satır okur.
     * 1. Zaman aralığına göre (Downsampling) satır eler.
     * 2. Sayıları kısaltır (Precision reduction).
     */
    private String readAndTimeSampleCSV(Path path) throws IOException {
        StringBuilder sb = new StringBuilder();
        
        Pattern decimalPattern = Pattern.compile("(\\d+\\.\\d{" + DECIMAL_PRECISION + "})\\d+");

        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            
            String header = br.readLine();
            if (header != null) {
                sb.append(header).append("\n");
            }
            
            long targetIntervalUs = TARGET_INTERVAL_MS * 1000; 
            long lastSavedTimestamp = -1; 
            
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                
                try {
                    int firstCommaIndex = line.indexOf(',');
                    if (firstCommaIndex == -1) continue;
                    
                    String timestampStr = line.substring(0, firstCommaIndex);
                    long currentTimestamp = Long.parseLong(timestampStr);
                    if (lastSavedTimestamp == -1 || (currentTimestamp - lastSavedTimestamp) >= targetIntervalUs) {
                        
                        Matcher matcher = decimalPattern.matcher(line);
                        String optimizedLine = matcher.replaceAll("$1");
                        
                        sb.append(optimizedLine).append("\n");
                        
                        lastSavedTimestamp = currentTimestamp;
                    }
                    
                } catch (Exception e) {
                    continue;
                }
            }
        }
        return sb.toString();
    }

    private void deleteTempDirectory(Path path) {
        if (path == null || !Files.exists(path)) return;
        try (Stream<Path> walk = Files.walk(path)) {
            walk.sorted((a, b) -> b.compareTo(a))
                .forEach(p -> {
                    try { Files.delete(p); } catch (IOException ignored) {}
                });
        } catch (IOException e) {
            System.err.println("⚠️ Temp silinemedi: " + e.getMessage());
        }
    }
}