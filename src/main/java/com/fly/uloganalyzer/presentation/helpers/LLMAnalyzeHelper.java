package com.fly.uloganalyzer.presentation.helpers;

import java.util.List;
import java.util.function.Consumer;
import com.fly.uloganalyzer.business.AnalysisService;
import com.fly.uloganalyzer.domain.AnalysisResult;
import com.fly.uloganalyzer.domain.CSVData;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;

public class LLMAnalyzeHelper {
    
    private final AnalysisService analysisService;
    private final CSVProcessHelper csvProcessHelper;
    
    // UI Components
    private TextArea txtResults;
    private TextArea txtFollowup;
    private TextArea txtCustomPrompt;
    private ProgressIndicator progressIndicator;
    private Button btnSendFollowup;
    
    // Callbacks
    private Consumer<Boolean> onUIStateUpdate;
    private Runnable onFollowupEnabled;
    
    // Constructor injection
    public LLMAnalyzeHelper(
        AnalysisService analysisService,
        CSVProcessHelper csvProcessHelper
    ) {
        this.analysisService = analysisService;
        this.csvProcessHelper = csvProcessHelper;
    }
    
    // UI Components setter
    public void setUIComponents(
        TextArea txtResults,
        TextArea txtFollowup,
        TextArea txtCustomPrompt,
        ProgressIndicator progressIndicator,
        Button btnSendFollowup
    ) {
        this.txtResults = txtResults;
        this.txtFollowup = txtFollowup;
        this.txtCustomPrompt = txtCustomPrompt;
        this.progressIndicator = progressIndicator;
        this.btnSendFollowup = btnSendFollowup;
    }
    
    // Callbacks setter
    public void setOnUIStateUpdate(Consumer<Boolean> callback) {
        this.onUIStateUpdate = callback;
    }
    
    public void setOnFollowupEnabled(Runnable callback) {
        this.onFollowupEnabled = callback;
    }
    
    // Public API
    public void analyzeWithDefaultPrompt() {
        performAnalysis("", false);
    }
    
    public void analyzeWithCustomPrompt() {
        String customPrompt = txtCustomPrompt.getText().trim();
        if (!UIHelper.validateNotEmpty(customPrompt, "Özel prompt")) {
            return;
        }
        performAnalysis(customPrompt, true);
    }
    
    public void sendFollowupQuestion() {
        String question = txtFollowup.getText().trim();
        if (question.isEmpty()) return;
        
        UIHelper.appendToTextArea(txtResults, "\n👤 SORU: " + question);
        txtFollowup.clear();
        performAnalysis(question, true);
    }
    
    // Private helper
    private void performAnalysis(String customPrompt, boolean useCustomPrompt) {
        List<CSVData> selectedData = csvProcessHelper.getSelectedCSVData();
        if (selectedData.isEmpty()) {
            UIHelper.showAlert("Uyarı", "Lütfen analiz için en az bir CSV dosyası seçin!");
            return;
        }
        
        String analysisMessage = useCustomPrompt ? 
            "\n📝 ÖZEL PROMPT İLE ANALİZ BAŞLATILIYOR..." :
            "\n🚀 VARSAYILAN PROMPT İLE ANALİZ BAŞLATILIYOR...";
        UIHelper.appendToTextArea(txtResults, analysisMessage);
        
        if (onUIStateUpdate != null) {
            onUIStateUpdate.accept(false);
        }
        UIHelper.showProgressIndicator(progressIndicator, true);
        
        analysisService.analyzeData(selectedData, customPrompt, useCustomPrompt)
            .thenAcceptAsync(this::handleAnalysisResult)
            .exceptionally(this::handleAnalysisError);
    }
    
    private void handleAnalysisResult(AnalysisResult result) {
        Platform.runLater(() -> {
            UIHelper.showProgressIndicator(progressIndicator, false);
            
            if (onUIStateUpdate != null) {
                onUIStateUpdate.accept(true);
            }
            
            if (result.isSuccess()) {
                UIHelper.appendToTextArea(txtResults, "\n🤖 ANALİZ SONUÇLARI:\n" + result.content());
                UIHelper.appendToTextArea(txtResults, "\n✅ Analiz tamamlandı! Aşağıdan takip sorusu sorabilirsiniz.");

                enableFollowup();
            } else {
                UIHelper.appendToTextArea(txtResults, "\n❌ ANALİZ HATASI:\n" + result.errorMessage());
            }
        });
    }
    
    private Void handleAnalysisError(Throwable throwable) {
        Platform.runLater(() -> {
            UIHelper.showProgressIndicator(progressIndicator, false);
            
            if (onUIStateUpdate != null) {
                onUIStateUpdate.accept(true);
            }
            
            UIHelper.appendToTextArea(txtResults, "\n❌ ANALİZ HATASI: " + throwable.getMessage());
        });
        return null;
    }
    
    private void enableFollowup() {
        if (txtFollowup != null) {
            txtFollowup.setDisable(false);
        }
        if (btnSendFollowup != null) {
            btnSendFollowup.setDisable(false);
        }
        if (txtFollowup != null) {
            txtFollowup.requestFocus();
        }
        if (onFollowupEnabled != null) {
            onFollowupEnabled.run();
        }
    }
}