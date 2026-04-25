package com.fly.uloganalyzer;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class ULogAnalyzerApp extends Application {
    
    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/fxml/main.fxml"));
            
            Parent root = loader.load();
            
            Scene scene = new Scene(root, 1200, 800);
            
            String css = getClass().getResource("/styles/modern-dark.css").toExternalForm();
            scene.getStylesheets().add(css);
            
            primaryStage.setTitle("LLM-Assisted PID Tuning Framework");
            
            try {
                Image logo = new Image(getClass().getResourceAsStream("/images/logo.png"));
                primaryStage.getIcons().add(logo);
            } catch (Exception e) {
                System.out.println("Logo yüklenemedi: " + e.getMessage());
            }
            
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1000);
            primaryStage.setMinHeight(700);
            
            primaryStage.setOnCloseRequest(event -> {
                Platform.exit();
            });
            
            primaryStage.show();
            
        } catch (Exception e) {
            System.err.println("Uygulama başlatılırken hata: " + e.getMessage());
            e.printStackTrace();

            showErrorDialog(e);
            Platform.exit();
        }
    }
    
    private void showErrorDialog(Exception e) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle("Başlatma Hatası");
        alert.setHeaderText("Uygulama başlatılamadı");
        alert.setContentText("Hata: " + e.getMessage() + 
                           "\n\nFXML dosyalarının resources klasöründe olduğundan emin olun.");
        alert.showAndWait();
    }
    
    public static void main(String[] args) {
    	launch(args);
    }
}