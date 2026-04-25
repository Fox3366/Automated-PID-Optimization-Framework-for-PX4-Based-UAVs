package com.fly.uloganalyzer.presentation.helpers;

import com.fly.uloganalyzer.business.ReportService;
import com.fly.uloganalyzer.domain.SoundType;
import com.fly.uloganalyzer.domain.ULogFile;
import com.fly.uloganalyzer.infrastructure.AudioPlayer;
import com.fly.uloganalyzer.infrastructure.ConfigLoader;
import com.fly.uloganalyzer.infrastructure.impl.JavaFXAudioPlayer;
import com.fly.uloganalyzer.infrastructure.impl.MarkdownReportService;

import javafx.scene.control.TextArea;

public class ReportSaveHelper {
    private TextArea txtResults;
    private ULogFile currentULogFile;
    private ConfigLoader configLoader;
    private final ReportService reportService;
    private final AudioPlayer audioPlayer;
    
    public ReportSaveHelper(TextArea txtResults, ConfigLoader configLoader) {
        this.txtResults = txtResults;
        this.configLoader = configLoader;
        this.reportService = new MarkdownReportService();
        this.audioPlayer = new JavaFXAudioPlayer(configLoader);
    }
    
    public void setCurrentULogFile(ULogFile currentULogFile) {
        this.currentULogFile = currentULogFile;
    }
    
    public void setTxtResults(TextArea txtResults) {
        this.txtResults = txtResults;
    }
    
    public void saveReport() {
        if (txtResults == null) {
            UIHelper.showAlert("Hata", "TextArea başlatılmamış!");
            return;
        }
        
        if (txtResults.getText().isEmpty()) {
            UIHelper.showAlert("Uyarı", "Kaydedilecek bir analiz sonucu yok!");
            return;
        }
        
        if (currentULogFile == null) {
            UIHelper.showAlert("Uyarı", "Önce bir ULOG dosyası yükleyin!");
            return;
        }

        String savedPath = reportService.saveReport(
            currentULogFile.getDisplayName(),
            txtResults.getText(),
            configLoader.getOpenAIModel()
        );

        if (savedPath != null) {
            UIHelper.appendToTextArea(txtResults, "\n\n✅ Rapor başarıyla kaydedildi:\n" + savedPath);
            audioPlayer.playSound(SoundType.ANALYSIS_SUCCESS);
        } else {
            UIHelper.showAlert("Hata", "Rapor kaydedilemedi. Logları kontrol edin.");
        }
    }
}