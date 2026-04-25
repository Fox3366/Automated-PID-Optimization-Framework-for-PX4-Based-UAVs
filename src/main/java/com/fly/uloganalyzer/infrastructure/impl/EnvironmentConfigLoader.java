package com.fly.uloganalyzer.infrastructure.impl;

import com.fly.uloganalyzer.infrastructure.ConfigLoader;
import com.fly.uloganalyzer.domain.SoundType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

public class EnvironmentConfigLoader implements ConfigLoader {
    
    private final Properties envProps; 
    private final Properties configProps;
    
    private final Path envFile;
    private final Path promptFile;
    private final Path filterFile;
    private final Path configFile;

    private static final String DEFAULT_CONFIG_CONTENT = """
            # --- ULOG ANALYZER GENEL AYARLARI ---
            
            # OpenAI Model Seçimi
            # Önerilenler: gpt-4o, gpt-4-turbo, gpt-3.5-turbo, o1-mini
            openai.model=gpt-5.1
            
            # Bağlantı Zaman Aşımı (Saniye cinsinden)
            # Büyük dosyalar için süreyi artırabilirsiniz.
            openai.timeout=120
            
            # Maksimum Token Sayısı (Cevap uzunluğu + Düşünme payı)
            # 'o1' serisi modeller için yüksek tutulmalı (en az 5000)
            openai.max_tokens=10000
            
            # Analiz Edilecek Maksimum CSV Dosya Sayısı
            # Token limitini aşmamak için sınırlandırılır.
            analysis.csv_limit=15
            """;

    private static final String DEFAULT_PROMPT_TEMPLATE = """
            PID Tuning uzmanı olarak hareket et.
            
            GÖREVLERİN:
            1. Adım Tepkisi Analizi (Setpoint vs Actual): Tepki süresi nasıl? Gecikme var mı?
            2. Hata Analizi:
               - Overshoot (Aşım): P çok mu yüksek?
               - Oscillation (Salınım): D yetersiz mi?
               - Steady State Error: I kazancı artırılmalı mı?
            3. Gürültü Analizi: Sinyalde çok gürültü var mı? Filtre gerekir mi?
            4. Net Tuning Tavsiyesi: Hangi eksende (Roll/Pitch/Yaw) hangi P, I, D değerini artırmalı veya azaltmalıyım?
            
            Lütfen analizini madde madde ve anlaşılır bir mühendis diliyle fazla uzatmadan (max 200 kelime) yap.
            """;

    private static final String DEFAULT_FILTER_CONTENT = """
            # --- ULOG ANALYZER CSV FİLTRELERİ ---
            # Başında # olan satırlar okunmaz.
            
            # Temel Uçuş Verileri
            vehicle_attitude
            vehicle_rates_setpoint
            vehicle_local_position
            vehicle_angular_velocity
            
            # Sensör ve Kontrol
            sensor_combined
            actuator_controls
            actuator_outputs
            RateCtrlStatus
            """;

    public EnvironmentConfigLoader() {
        this.envProps = new Properties();
        this.configProps = new Properties();
        
        this.envFile = Paths.get(".env");
        this.promptFile = Paths.get("default-prompt.txt");
        this.filterFile = Paths.get("csv-filters.txt");
        this.configFile = Paths.get("config.properties");
        
        loadAllConfigs();
    }
    
    private void loadAllConfigs() {
        try {
            if (Files.exists(envFile)) {
                envProps.load(Files.newInputStream(envFile));
                System.out.println("✓ Secrets loaded from .env");
            }
        } catch (IOException e) {
            System.err.println("❌ .env load error: " + e.getMessage());
        }

        try {
            if (!Files.exists(configFile)) {
                System.out.println("ℹ Config file not found. Creating default: " + configFile);
                Files.writeString(configFile, DEFAULT_CONFIG_CONTENT, StandardCharsets.UTF_8);
                configProps.load(Files.newInputStream(configFile));
            } else {
                configProps.load(Files.newInputStream(configFile));
                System.out.println("✓ Settings loaded from config.properties");
            }
        } catch (IOException e) {
            System.err.println("❌ Config properties error: " + e.getMessage());
        }
    }


    @Override
    public String getOpenAIModel() {
        return configProps.getProperty("openai.model", "gpt-5.1").trim();
    }

    @Override
    public int getOpenAITimeout() {
        return parseIntSafe(configProps.getProperty("openai.timeout"), 120);
    }

    @Override
    public int getOpenAIMaxTokens() {
        return parseIntSafe(configProps.getProperty("openai.max_tokens"), 12000);
    }

    @Override
    public int getCsvFileLimit() {
        return parseIntSafe(configProps.getProperty("analysis.csv_limit"), 15);
    }

    private int parseIntSafe(String value, int defaultValue) {
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }


    @Override
    public String getApiKey() {
        return envProps.getProperty("OPENAI_API_KEY", "").trim();
    }

    @Override
    public String getDefaultPrompt() {
        try {
             if (Files.exists(promptFile)) {
                 System.out.println("✓ Prompt loaded from file.");
                 return Files.readString(promptFile, StandardCharsets.UTF_8);
             } else {
                 System.out.println("ℹ Creating default prompt file.");
                 Files.writeString(promptFile, DEFAULT_PROMPT_TEMPLATE, StandardCharsets.UTF_8);
                 return DEFAULT_PROMPT_TEMPLATE;
             }
        } catch (IOException e) { return DEFAULT_PROMPT_TEMPLATE; }
    }
    
    @Override
    public List<String> getCsvFilterPatterns() {
        try {
            if (!Files.exists(filterFile)) {
                System.out.println("ℹ Creating default filter file.");
                Files.writeString(filterFile, DEFAULT_FILTER_CONTENT, StandardCharsets.UTF_8);
            }
            return Files.readAllLines(filterFile, StandardCharsets.UTF_8).stream()
                .map(String::trim)
                .filter(l -> !l.startsWith("#") && !l.isEmpty())
                .toList();
        } catch (Exception e) { return List.of("vehicle_attitude"); }
    }

    @Override
    public String getSoundPath(SoundType type) {
        return type.getDefaultPath();
    }

    @Override
    public void saveApiKey(String apiKey) {
        envProps.setProperty("OPENAI_API_KEY", apiKey.trim());
        try {
            envProps.store(Files.newOutputStream(envFile), "ULog Analyzer Secrets");
        } catch (IOException e) { e.printStackTrace(); }
    }
    
    @Override
    public void saveSettings(String model, int timeout, int maxTokens, int csvLimit) {
        configProps.setProperty("openai.model", model);
        configProps.setProperty("openai.timeout", String.valueOf(timeout));
        configProps.setProperty("openai.max_tokens", String.valueOf(maxTokens));
        configProps.setProperty("analysis.csv_limit", String.valueOf(csvLimit));
        
        try {
            configProps.store(Files.newOutputStream(configFile), "ULog Analyzer General Settings");
            System.out.println("✅ Settings saved to config.properties");
        } catch (IOException e) {
            System.err.println("❌ Failed to save settings: " + e.getMessage());
        }
    }

    @Override
    public void saveDefaultPrompt(String promptContent) {
        try {
            Files.writeString(promptFile, promptContent, StandardCharsets.UTF_8);
            System.out.println("✅ Prompt saved to file.");
        } catch (IOException e) {
            System.err.println("❌ Failed to save prompt: " + e.getMessage());
        }
    }

    @Override
    public void saveFilters(String filterContent) {
        try {
            Files.writeString(filterFile, filterContent, StandardCharsets.UTF_8);
            System.out.println("✅ Filters saved to file.");
        } catch (IOException e) {
            System.err.println("❌ Failed to save filters: " + e.getMessage());
        }
    }	
}