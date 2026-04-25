package com.fly.uloganalyzer.presentation.controllers;

import java.io.File;

import com.fly.uloganalyzer.business.AnalysisService;
import com.fly.uloganalyzer.domain.ULogFile;
import com.fly.uloganalyzer.infrastructure.AudioPlayer;
import com.fly.uloganalyzer.infrastructure.CSVFilter;
import com.fly.uloganalyzer.infrastructure.ConfigLoader;
import com.fly.uloganalyzer.infrastructure.OpenAIClient;
import com.fly.uloganalyzer.infrastructure.ULogParser;
import com.fly.uloganalyzer.infrastructure.impl.EnvironmentConfigLoader;
import com.fly.uloganalyzer.infrastructure.impl.EssentialCSVFilter;
import com.fly.uloganalyzer.infrastructure.impl.HttpOpenAIClient;
import com.fly.uloganalyzer.infrastructure.impl.JavaFXAudioPlayer;
import com.fly.uloganalyzer.infrastructure.impl.RealULogParser;
import com.fly.uloganalyzer.presentation.helpers.APIKeyHelper;
import com.fly.uloganalyzer.presentation.helpers.CSVProcessHelper;
import com.fly.uloganalyzer.presentation.helpers.LLMAnalyzeHelper;
import com.fly.uloganalyzer.presentation.helpers.ReportSaveHelper;
import com.fly.uloganalyzer.presentation.helpers.SaveSettingsHelper;
import com.fly.uloganalyzer.presentation.helpers.UIHelper;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

public class MainController {
    

    private final ConfigLoader configLoader = new EnvironmentConfigLoader();
    private final AudioPlayer audioPlayer = new JavaFXAudioPlayer(configLoader);
    private AnalysisService analysisService;
    private APIKeyHelper apiKeyHelper = new APIKeyHelper(null);
    private ReportSaveHelper reportSaveHelper;
    

    @FXML private BorderPane root;
    @FXML private ImageView logoImage;
    @FXML private Label lblApiStatus; 
    @FXML private Label lblSelectedFile;
    @FXML private ListView<String> csvListView;
    @FXML private TextArea txtResults;
    @FXML private TextArea txtFollowup;
    @FXML private TextArea txtCustomPrompt;
    @FXML private Button btnLoadUlog;
    @FXML private Button btnAnalyzeDefault;
    @FXML private Button btnAnalyzeCustom;
    @FXML private Button btnSendFollowup;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private VBox dragDropArea;
    @FXML private Button btnClearResults;
    @FXML private Button btnSaveReport;
    
    @FXML private PasswordField txtApiKey;
    @FXML private TextField txtModelName;
    @FXML private TextField txtMaxTokens;
    @FXML private TextField txtTimeout;
    @FXML private TextArea txtDefaultPromptSetting;
    @FXML private TextArea txtFilterListSetting;
    @FXML private Label lblSettingsStatus;
    @FXML private Button btnSaveSettings;

    @FXML private Pane csvDropArea;
    @FXML private Button btnAddCSV;
    @FXML private Button btnClearCSV;
    @FXML private Label lblCSVCount;
    
    private ULogFile currentULogFile;
    private ObservableList<String> csvFileNames = FXCollections.observableArrayList();
    private SaveSettingsHelper saveSettingsHelper;
    private CSVProcessHelper csvProcessHelper;
    private LLMAnalyzeHelper llmAnalyzeHelper;

    
    public MainController() {
        ULogParser ulogParser = new RealULogParser();
        CSVFilter csvFilter = new EssentialCSVFilter(configLoader);
        OpenAIClient openAIClient = new HttpOpenAIClient(configLoader);
        this.analysisService = new AnalysisService(ulogParser, csvFilter, openAIClient, audioPlayer);
    }
    

    @FXML
    public void initialize() {
        try {
            System.out.println("🚀 MainController.initialize() başlıyor...");
            
            setupModernUI();
            setupListView();
            
            System.out.println("🔄 Helper'lar initialize ediliyor...");
            
            apiKeyHelper = new APIKeyHelper(configLoader);
            apiKeyHelper.setApiStatusLabel(lblApiStatus);
            
            reportSaveHelper = new ReportSaveHelper(txtResults, configLoader);
            
            initialize_SaveSettingsHelper();
            initialize_CSVHelper();
            initialize_LLMAnalyzeHelper();
            
       
            System.out.println("🔄 Event handler'lar bağlanıyor...");
            setupEventHandlers();
            setupDragAndDrop();
            
            apiKeyHelper.checkApiKey();
            
            loadSettingsToUI();
            
            System.out.println("✅ MainController.initialize() başarıyla tamamlandı");
            
        } catch (Exception e) {
            System.err.println("❌ MainController.initialize() HATASI: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("MainController initialize failed", e);
        }
    }
    
    private void initialize_LLMAnalyzeHelper() {
        llmAnalyzeHelper = new LLMAnalyzeHelper(
            analysisService,
            csvProcessHelper
        );
        
        // UI components set
        llmAnalyzeHelper.setUIComponents(
            txtResults,
            txtFollowup,
            txtCustomPrompt,
            progressIndicator,
            btnSendFollowup
        );
        
        // Callbacks set
        llmAnalyzeHelper.setOnUIStateUpdate(hasData -> {
            updateUIState(hasData);
        });
        
        llmAnalyzeHelper.setOnFollowupEnabled(() -> {
            txtFollowup.setDisable(false);
            btnSendFollowup.setDisable(false);
            txtFollowup.requestFocus();
        });
    }
    
    @FXML
    private void analyzeWithDefaultPrompt() {
        llmAnalyzeHelper.analyzeWithDefaultPrompt();
    }
    
    @FXML
    private void analyzeWithCustomPrompt() {
        llmAnalyzeHelper.analyzeWithCustomPrompt();
    }
    
    @FXML
    private void sendFollowupQuestion() {
        llmAnalyzeHelper.sendFollowupQuestion();
    }
    
    private void initialize_SaveSettingsHelper()
    {
        saveSettingsHelper = new SaveSettingsHelper(
                configLoader,
                audioPlayer,
                apiKeyHelper
            );
        
        saveSettingsHelper.setUIComponents(
                txtApiKey,
                txtModelName,
                txtMaxTokens,
                txtTimeout,
                txtDefaultPromptSetting,
                txtFilterListSetting,
                lblSettingsStatus
            );
        
        saveSettingsHelper.setAnalysisService(analysisService);
    }
    
    private void initialize_CSVHelper()
    {
        csvProcessHelper = new CSVProcessHelper(
                analysisService,
                csvFileNames,
                csvListView,
                progressIndicator,
                txtResults,
                txtFollowup,
                btnSendFollowup
            );
            
            // Callback'leri set et
            csvProcessHelper.setOnUIStateUpdate(() -> {
                boolean hasData = csvProcessHelper.hasCSVData();
                updateUIState(hasData);
            });
            
            csvProcessHelper.setOnFollowupStateChange(enabled -> {
                txtFollowup.setDisable(!enabled);
                btnSendFollowup.setDisable(!enabled);
            });
            
            
    }
    
    
    
    private void saveAllSettings() {
        saveSettingsHelper.saveAllSettings();
    }
    
   
    
    private void setupModernUI() {
        loadLogo();
        UIHelper.setTextAreaPrompt(txtFollowup, "💡 Takip sorusu için analizin gerçekleşmiş olması gerek...");
        updateUIState(!csvFileNames.isEmpty());
    }
    
    private void loadLogo() {
        try {
            var logoStream = getClass().getResourceAsStream("/images/logo.png");
            if (logoStream != null) {
                logoImage.setImage(new Image(logoStream));
            } else {
                System.err.println("❌ Logo bulunamadı (Stream null döndü)");
            }
        } catch (Exception e) {
            System.err.println("❌ Logo hatası: " + e.getMessage());
        }
    }
    
    private void setupListView() {
        csvListView.setItems(csvFileNames);
        csvListView.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);
        
        csvListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            updateUIState(!csvFileNames.isEmpty());
        });
    }
    
    private void setupEventHandlers() {
        btnLoadUlog.setOnAction(e -> loadULogFile());
        btnAnalyzeDefault.setOnAction(e -> analyzeWithDefaultPrompt());
        btnAnalyzeCustom.setOnAction(e -> analyzeWithCustomPrompt());
        btnSendFollowup.setOnAction(e -> sendFollowupQuestion());
        btnSaveReport.setOnAction(e -> reportSaveHelper.saveReport());
        btnClearResults.setOnAction(e -> clearResults());
        btnSaveSettings.setOnAction(e -> saveAllSettings());  
    }
    
    private void clearResults() {
        UIHelper.clearTextArea(txtResults);
        UIHelper.appendToTextArea(txtResults, "Ekran temizlendi. Yeni analiz için hazırsınız.\n");
        txtFollowup.setDisable(true);
        btnSendFollowup.setDisable(true);
    }
    

 private void setupDragAndDrop() {
     dragDropArea.setOnDragOver(event -> {
         if (event.getDragboard().hasFiles() && UIHelper.isULogFile(event.getDragboard())) {
             event.acceptTransferModes(javafx.scene.input.TransferMode.COPY);
         }
         event.consume();
     });
     
     dragDropArea.setOnDragDropped(event -> {
         var db = event.getDragboard();
         boolean success = false;
         if (db.hasFiles() && UIHelper.isULogFile(db)) {
             handleDroppedFile(db.getFiles().get(0));
             success = true;
         }
         event.setDropCompleted(success);
         event.consume();
     });
     
     setupParamsDragAndDrop(); 
 }

 private void setupParamsDragAndDrop() {
     javafx.event.EventHandler<javafx.scene.input.DragEvent> dragOverHandler = event -> {
         if (event.getDragboard().hasFiles() && UIHelper.isParamsFile(event.getDragboard())) {
             event.acceptTransferModes(javafx.scene.input.TransferMode.COPY);
         }
         event.consume(); 
     };

     javafx.event.EventHandler<javafx.scene.input.DragEvent> dragDroppedHandler = event -> {
         var db = event.getDragboard();
         boolean success = false;
         if (db.hasFiles() && UIHelper.isParamsFile(db)) {
             csvProcessHelper.addParamsFile(db.getFiles().get(0));
             success = true;
         }
         event.setDropCompleted(success);
         event.consume();
     };

     csvListView.setOnDragOver(dragOverHandler);
     csvListView.setOnDragDropped(dragDroppedHandler);
     
     if (csvDropArea != null) {
         csvDropArea.setOnDragOver(dragOverHandler);
         csvDropArea.setOnDragDropped(dragDroppedHandler);
     }
 }
    
    private void loadULogFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("ULOG Dosyası Seç");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("ULOG files", "*.ulg")
        );
        
        File file = fileChooser.showOpenDialog(root.getScene().getWindow());
        if (file != null) {
            handleSelectedFile(file);
        }
    }
    
    private void handleSelectedFile(File file) {
        currentULogFile = new ULogFile(file.toPath(), file.getName());
        reportSaveHelper.setCurrentULogFile(currentULogFile);
        UIHelper.updateLabelText(lblSelectedFile, "Seçilen: " + file.getName());
        UIHelper.appendToTextArea(txtResults, "✅ ULOG dosyası yüklendi: " + file.getAbsolutePath());
        csvProcessHelper.processULogFile(currentULogFile);
    }
    
    private void handleDroppedFile(File file) {
        currentULogFile = new ULogFile(file.toPath(), file.getName());
        reportSaveHelper.setCurrentULogFile(currentULogFile);
        UIHelper.updateLabelText(lblSelectedFile, "Sürüklenen: " + file.getName());
        UIHelper.appendToTextArea(txtResults, "✅ ULOG dosyası sürükle-bırak ile yüklendi");
        csvProcessHelper.processULogFile(currentULogFile);
    }
    
    private void loadSettingsToUI() {
        txtApiKey.setText(configLoader.getApiKey());
        txtModelName.setText(configLoader.getOpenAIModel());
        txtMaxTokens.setText(String.valueOf(configLoader.getOpenAIMaxTokens()));
        txtTimeout.setText(String.valueOf(configLoader.getOpenAITimeout()));
        txtDefaultPromptSetting.setText(configLoader.getDefaultPrompt());
        
        String filters = String.join("\n", configLoader.getCsvFilterPatterns());
        txtFilterListSetting.setText(filters);
    }
    
    private void updateUIState(boolean hasData) {
        Platform.runLater(() -> {
            boolean hasCSVData = csvProcessHelper != null && csvProcessHelper.hasCSVData();
            boolean hasSelection = csvListView.getSelectionModel().getSelectedItems().size() > 0;
            
            boolean analyzeEnabled = hasData && hasSelection && hasCSVData;
            UIHelper.updateButtonState(btnAnalyzeDefault, analyzeEnabled);
            UIHelper.updateButtonState(btnAnalyzeCustom, analyzeEnabled);
            
            if (btnClearCSV != null) {
                btnClearCSV.setDisable(!hasCSVData);
            }
            
            if (txtResults != null && txtResults.getText().contains("ANALİZ SONUÇLARI")) {
                if (txtFollowup != null) txtFollowup.setDisable(false);
                if (btnSendFollowup != null) btnSendFollowup.setDisable(false);
            }
        });
    }
    
    public void shutdown() {
        if (analysisService != null) analysisService.shutdown();
        if (audioPlayer != null) audioPlayer.stopAllSounds();
    }
}