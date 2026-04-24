// Dosya: com/fly/uloganalyzer/infrastructure/impl/MarkdownReportService.java

package com.fly.uloganalyzer.infrastructure.impl;

import com.fly.uloganalyzer.business.ReportService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MarkdownReportService implements ReportService {

    private static final String TEMPLATE_FILE = "report-template.md";
    private static final String REPORTS_DIR = "reports";

    @Override
    public String saveReport(String originalFilename, String aiContent, String modelName) {
        try {
            String template = loadTemplate();

            String reportContent = template
                    .replace("{{DATE}}", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .replace("{{FILENAME}}", originalFilename)
                    .replace("{{MODEL_NAME}}", modelName)
                    .replace("{{AI_ANALYSIS}}", aiContent);

            Path reportsPath = Paths.get(REPORTS_DIR);
            if (!Files.exists(reportsPath)) {
                Files.createDirectories(reportsPath);
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String safeFilename = originalFilename.replaceAll("[^a-zA-Z0-9.-]", "_");
            String newFilename = "Report_" + safeFilename + "_" + timestamp + ".md";
            
            Path finalPath = reportsPath.resolve(newFilename);

            Files.writeString(finalPath, reportContent, StandardCharsets.UTF_8);
            
            System.out.println("✅ Rapor kaydedildi: " + finalPath.toAbsolutePath());
            return finalPath.toAbsolutePath().toString();

        } catch (IOException e) {
            System.err.println("❌ Rapor kaydetme hatası: " + e.getMessage());
            return null;
        }
    }

    private String loadTemplate() throws IOException {
        Path path = Paths.get(TEMPLATE_FILE);
        if (Files.exists(path)) {
            return Files.readString(path, StandardCharsets.UTF_8);
        }
        return "# Rapor\n\nTarih: {{DATE}}\n\n{{AI_ANALYSIS}}";
    }
}