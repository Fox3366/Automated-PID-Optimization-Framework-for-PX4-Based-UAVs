package com.fly.uloganalyzer.presentation.helpers;

import com.fly.uloganalyzer.infrastructure.ConfigLoader;

import javafx.scene.control.Label;

public class APIKeyHelper {
    
    private Label lblApiStatus;
    private final ConfigLoader configLoader;
    
    public APIKeyHelper(ConfigLoader configLoader) {
        this.configLoader = configLoader;
    }
    
    public void setApiStatusLabel(Label label) {
        this.lblApiStatus = label;
    }
    
    public void checkApiKey() {
        if (lblApiStatus == null) {
            System.err.println("⚠️ lblApiStatus null! FXML enjeksiyonunu kontrol edin.");
            return;
        }
        
        String apiKey = configLoader.getApiKey();
        
        if (apiKey == null || apiKey.trim().isEmpty()) {
            UIHelper.updateLabelText(lblApiStatus, "❌ API Key bulunamadı");
            UIHelper.updateLabelStyle(lblApiStatus, "-fx-text-fill: #ff6b6b;");
            System.out.println("🚫 API Key: NOT CONFIGURED");
        } else if (!apiKey.startsWith("sk-")) {
            UIHelper.updateLabelText(lblApiStatus, "❌ Geçersiz API Key formatı");
            UIHelper.updateLabelStyle(lblApiStatus, "-fx-text-fill: #ff6b6b;");
            System.out.println("🚫 API Key: INVALID FORMAT");
        } else {
            String maskedKey = maskApiKey(apiKey);
            UIHelper.updateLabelText(lblApiStatus, "✅ API Key yüklendi: " + maskedKey);
            UIHelper.updateLabelStyle(lblApiStatus, "-fx-text-fill: #51cf66;");
            System.out.println("✅ API Key: CONFIGURED (" + maskedKey + ")");
        }
    }
    
    private String maskApiKey(String apiKey) {
        return apiKey.length() > 8 ? 
            apiKey.substring(0, 4) + "***" + apiKey.substring(apiKey.length() - 4) : "***";
    }
}