package com.fly.uloganalyzer.presentation.helpers;

import com.fly.uloganalyzer.business.AnalysisService;
import com.fly.uloganalyzer.domain.SoundType;
import com.fly.uloganalyzer.infrastructure.AudioPlayer;
import com.fly.uloganalyzer.infrastructure.ConfigLoader;
import com.fly.uloganalyzer.infrastructure.CSVFilter;
import com.fly.uloganalyzer.infrastructure.OpenAIClient;
import com.fly.uloganalyzer.infrastructure.ULogParser;
import com.fly.uloganalyzer.infrastructure.impl.EssentialCSVFilter;
import com.fly.uloganalyzer.infrastructure.impl.HttpOpenAIClient;
import com.fly.uloganalyzer.infrastructure.impl.RealULogParser;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class SaveSettingsHelper {
    
    private final ConfigLoader configLoader;
    private final AudioPlayer audioPlayer;
    private final APIKeyHelper apiKeyHelper;
    
    // UI Components
    private PasswordField txtApiKey;
    private TextField txtModelName;
    private TextField txtMaxTokens;
    private TextField txtTimeout;
    private TextArea txtDefaultPromptSetting;
    private TextArea txtFilterListSetting;
    private Label lblSettingsStatus;
    
    private AnalysisService analysisService;
    
    public SaveSettingsHelper(
        ConfigLoader configLoader,
        AudioPlayer audioPlayer,
        APIKeyHelper apiKeyHelper
    ) {
        this.configLoader = configLoader;
        this.audioPlayer = audioPlayer;
        this.apiKeyHelper = apiKeyHelper;
    }
    
    public void setUIComponents(
        PasswordField txtApiKey,
        TextField txtModelName,
        TextField txtMaxTokens,
        TextField txtTimeout,
        TextArea txtDefaultPromptSetting,
        TextArea txtFilterListSetting,
        Label lblSettingsStatus
    ) {
        this.txtApiKey = txtApiKey;
        this.txtModelName = txtModelName;
        this.txtMaxTokens = txtMaxTokens;
        this.txtTimeout = txtTimeout;
        this.txtDefaultPromptSetting = txtDefaultPromptSetting;
        this.txtFilterListSetting = txtFilterListSetting;
        this.lblSettingsStatus = lblSettingsStatus;
    }
    
    public void setAnalysisService(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }
    
    public void saveAllSettings() {
        try {
            // Validation
            if (!UIHelper.validateNotEmpty(txtApiKey.getText(), "API Key")) return;
            if (!UIHelper.validateNotEmpty(txtModelName.getText(), "Model Adı")) return;
            if (!UIHelper.validateNumber(txtMaxTokens.getText().trim(), "Max Tokens")) return;
            if (!UIHelper.validateNumber(txtTimeout.getText().trim(), "Timeout")) return;
            
            // Save settings
            configLoader.saveApiKey(txtApiKey.getText());
            
            int timeout = Integer.parseInt(txtTimeout.getText().trim());
            int maxTokens = Integer.parseInt(txtMaxTokens.getText().trim());
            configLoader.saveSettings(txtModelName.getText().trim(), timeout, maxTokens, 5);
            
            configLoader.saveDefaultPrompt(txtDefaultPromptSetting.getText());
            configLoader.saveFilters(txtFilterListSetting.getText());
            

            
            // Update UI
            txtDefaultPromptSetting.setText(configLoader.getDefaultPrompt());
            if (apiKeyHelper != null) {
                apiKeyHelper.checkApiKey();
            }
            
            String updatedFilters = String.join("\n", configLoader.getCsvFilterPatterns());
            txtFilterListSetting.setText(updatedFilters);
            
            // Show success message
            UIHelper.updateLabelText(lblSettingsStatus, "✅ Ayarlar kaydedildi ve uygulama güncellendi!");
            audioPlayer.playSound(SoundType.FILE_LOADED);
            
            // Clear status message after 3 seconds
            clearStatusMessage();
            
            // Reinitialize services with new settings
            reinitializeServices();
            
        } catch (Exception e) {
            UIHelper.showError("Kritik Hata", "Ayarlar kaydedilirken sorun oluştu: " + e.getMessage());
        }
    }
    
    private void reinitializeServices() {
        if (analysisService != null) {
            ULogParser newParser = new RealULogParser();
            CSVFilter newFilter = new EssentialCSVFilter(configLoader);
            OpenAIClient newClient = new HttpOpenAIClient(configLoader);
            
            analysisService.updateDependencies(newParser, newFilter, newClient);
        }
    }
    
    private void clearStatusMessage() {
        new java.util.Timer().schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                javafx.application.Platform.runLater(() -> 
                    UIHelper.updateLabelText(lblSettingsStatus, ""));
            }
        }, 3000);
    }
}