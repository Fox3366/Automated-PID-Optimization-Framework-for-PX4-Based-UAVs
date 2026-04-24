package com.fly.uloganalyzer.presentation.helpers;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import com.fly.uloganalyzer.business.AnalysisService;
import com.fly.uloganalyzer.domain.CSVData;
import com.fly.uloganalyzer.domain.ULogFile;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;

public class CSVProcessHelper {
    
    private final AnalysisService analysisService;
    
    // UI Components
    private ObservableList<String> csvFileNames;
    private ListView<String> csvListView;
    private ProgressIndicator progressIndicator;
    private TextArea txtResults;
    private TextArea txtFollowup;
    private Button btnSendFollowup;
    
    // State
    private List<CSVData> currentCSVData;
    
    // Callbacks
    private Runnable onUIStateUpdate;
    private Consumer<Boolean> onFollowupStateChange;
    
    // Constructor injection
    public CSVProcessHelper(
        AnalysisService analysisService,
        ObservableList<String> csvFileNames,
        ListView<String> csvListView,
        ProgressIndicator progressIndicator,
        TextArea txtResults,
        TextArea txtFollowup,
        Button btnSendFollowup
    ) {
        this.analysisService = analysisService;
        this.csvFileNames = csvFileNames;
        this.csvListView = csvListView;
        this.progressIndicator = progressIndicator;
        this.txtResults = txtResults;
        this.txtFollowup = txtFollowup;
        this.btnSendFollowup = btnSendFollowup;
    }
    
    // Setter for callbacks
    public void setOnUIStateUpdate(Runnable callback) {
        this.onUIStateUpdate = callback;
    }
    
    public void setOnFollowupStateChange(Consumer<Boolean> callback) {
        this.onFollowupStateChange = callback;
    }
    
    public void setCurrentCSVData(List<CSVData> csvData) {
        this.currentCSVData = csvData;
    }
    
    // Public API
    public CompletableFuture<Void> processULogFile(ULogFile ulogFile) {
        UIHelper.showProgressIndicator(progressIndicator, true);
        
        if (onUIStateUpdate != null) {
            onUIStateUpdate.run();
        }
        
        return analysisService.processULogFile(ulogFile)
            .thenAcceptAsync(this::handleProcessedCSVData)
            .exceptionally(this::handleProcessingError);
    }
    
    public List<CSVData> getSelectedCSVData() {
        if (currentCSVData == null || csvListView == null) {
            return List.of();
        }
        
        return currentCSVData.stream()
            .filter(csv -> csvListView.getSelectionModel().getSelectedItems().contains(csv.filename()))
            .toList();
    }
    
    public List<CSVData> getCurrentCSVData() {
        return currentCSVData;
    }
    
    public boolean hasCSVData() {
        return currentCSVData != null && !currentCSVData.isEmpty();
    }
    
    // Private helpers
    private void handleProcessedCSVData(List<CSVData> csvData) {
        Platform.runLater(() -> {
            this.currentCSVData = csvData;
            updateCSVList(csvData);
            UIHelper.showProgressIndicator(progressIndicator, false);
            
            if (onUIStateUpdate != null) {
                onUIStateUpdate.run(); // UI state'i true yap
            }
            
            UIHelper.appendToTextArea(txtResults, "🎯 " + csvData.size() + " CSV dosyası filtrelendi");
        });
    }
    
    private Void handleProcessingError(Throwable throwable) {
        Platform.runLater(() -> {
            UIHelper.showProgressIndicator(progressIndicator, false);
            
            if (onUIStateUpdate != null) {
                onUIStateUpdate.run();
            }
            
            UIHelper.appendToTextArea(txtResults, "❌ Hata: " + throwable.getMessage());
        });
        return null;
    }
    
    private void updateCSVList(List<CSVData> csvData) {
        csvFileNames.clear();
        if (csvData != null && !csvData.isEmpty()) {
            csvData.forEach(csv -> csvFileNames.add(csv.filename()));
            System.out.println("📊 CSV List güncellendi: " + csvFileNames.size() + " dosya");
            
            UIHelper.updateListView(csvListView, 
                csvData.stream().map(CSVData::filename).toList());
            UIHelper.selectAllInListView(csvListView);
            
            int selectedCount = csvListView.getSelectionModel().getSelectedItems().size();
            System.out.println("✅ Otomatik seçim: " + selectedCount + " dosya seçildi");
        } else {
            System.out.println("❌ CSV List boş!");
        }
        
        if (onFollowupStateChange != null) {
            onFollowupStateChange.accept(false);
        }
        
        if (txtFollowup != null) {
            txtFollowup.clear();
        }
    }
    
    /* FOR PARAMS DRAG DROP */

    public void addParamsFile(File file) {
        try {
            String content = java.nio.file.Files.readString(file.toPath(), java.nio.charset.StandardCharsets.UTF_8);
            String filename = file.getName();
            
            if (currentCSVData == null) {
                currentCSVData = new java.util.ArrayList<>();
            }
            
            boolean exists = currentCSVData.stream().anyMatch(d -> d.filename().equals(filename));
            if (!exists) {
                CSVData paramsData = new CSVData(filename, content);
                currentCSVData.add(paramsData);
            }
            
            Platform.runLater(() -> {
                if (!csvFileNames.contains(filename)) {
                    csvFileNames.add(filename);
                }
                
                csvListView.getSelectionModel().select(filename); 
                
                UIHelper.appendToTextArea(txtResults, "➕ Parametre dosyası eklendi: " + filename);
                
                if (onUIStateUpdate != null) {
                    onUIStateUpdate.run();
                }
            });
            
        } catch (Exception e) {
            Platform.runLater(() -> 
                UIHelper.showError("Dosya Okuma Hatası", "Params dosyası eklenemedi: " + e.getMessage()));
        }
    }
}