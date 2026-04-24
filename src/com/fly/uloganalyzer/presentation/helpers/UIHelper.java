// com/fly/uloganalyzer/presentation/helpers/UIHelper.java
package com.fly.uloganalyzer.presentation.helpers;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Pane;

public class UIHelper {
    
    private UIHelper() {
        // Utility class - no instantiation
    }
    
    // ========== ALERT DIALOGS ==========
    public static void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    public static void showSuccess(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Başarılı");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    public static void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    // ========== DRAG & DROP ==========
    public static void setupDragAndDrop(Pane dragDropArea, 
                                        Consumer<File> onFileDropped, 
                                        String... validExtensions) {
        dragDropArea.setOnDragOver(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasFiles() && isValidFile(db, validExtensions)) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });
        
        dragDropArea.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasFiles()) {
                onFileDropped.accept(db.getFiles().get(0));
                event.setDropCompleted(true);
            }
            event.consume();
        });
    }
    
    private static boolean isValidFile(Dragboard db, String... extensions) {
        return db.getFiles().stream()
                .anyMatch(file -> {
                    String fileName = file.getName().toLowerCase();
                    for (String ext : extensions) {
                        if (fileName.endsWith(ext.toLowerCase())) {
                            return true;
                        }
                    }
                    return false;
                });
    }
    
    public static boolean isULogFile(Dragboard db) {
        return isValidFile(db, ".ulg");
    }
    

    // for param drag drop
    public static boolean isParamsFile(Dragboard db) {
        return isValidFile(db, ".params", ".txt"); 
    }
    /**/
    
    // ========== TEXT AREA OPERATIONS ==========
    public static void appendToTextArea(TextArea textArea, String text) {
        Platform.runLater(() -> {
            textArea.appendText(text + "\n");
            textArea.setScrollTop(Double.MAX_VALUE);
        });
    }
    
    public static void clearTextArea(TextArea textArea) {
        Platform.runLater(() -> textArea.clear());
    }
    
    public static void setTextAreaPrompt(TextArea textArea, String prompt) {
        Platform.runLater(() -> textArea.setPromptText(prompt));
    }
    
    // ========== BUTTON OPERATIONS ==========
    public static void updateButtonState(Button button, boolean enabled) {
        Platform.runLater(() -> button.setDisable(!enabled));
    }
    
    public static void updateButtonText(Button button, String text) {
        Platform.runLater(() -> button.setText(text));
    }
    
    // ========== LIST VIEW OPERATIONS ==========
    public static void updateListView(ListView<String> listView, java.util.List<String> items) {
        Platform.runLater(() -> {
            listView.getItems().clear();
            if (items != null && !items.isEmpty()) {
                listView.getItems().addAll(items);
            }
        });
    }
    
    public static List<String> getSelectedListViewItems(ListView<String> listView) {
        if (listView == null || listView.getSelectionModel() == null) {
            return Collections.emptyList();
        }
        
        ObservableList<String> selectedItems = listView.getSelectionModel().getSelectedItems();
        return new ArrayList<>(selectedItems);
    }
    
    public static void selectAllInListView(ListView<String> listView) {
        Platform.runLater(() -> listView.getSelectionModel().selectAll());
    }
    
    // ========== PROGRESS INDICATOR ==========
    public static void showProgressIndicator(javafx.scene.control.ProgressIndicator indicator, boolean show) {
        Platform.runLater(() -> indicator.setVisible(show));
    }
    
    // ========== LABEL OPERATIONS ==========
    public static void updateLabelText(javafx.scene.control.Label label, String text) {
        Platform.runLater(() -> label.setText(text));
    }
    
    public static void updateLabelStyle(javafx.scene.control.Label label, String style) {
        Platform.runLater(() -> label.setStyle(style));
    }
    
    // ========== VALIDATION ==========
    public static boolean validateNotEmpty(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            showAlert("Hata", fieldName + " boş olamaz!");
            return false;
        }
        return true;
    }
    
    public static boolean validateNumber(String value, String fieldName) {
        try {
            Integer.parseInt(value.trim());
            return true;
        } catch (NumberFormatException e) {
            showAlert("Hata", fieldName + " geçerli bir sayı olmalı!");
            return false;
        }
    }
}