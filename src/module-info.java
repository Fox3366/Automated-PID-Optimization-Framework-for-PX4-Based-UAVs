module com.fly.uloganalyzer {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.media;
    
    opens com.fly.uloganalyzer to javafx.fxml;
    opens com.fly.uloganalyzer.presentation.controllers to javafx.fxml;
    opens com.fly.uloganalyzer.domain to javafx.base;
    
    exports com.fly.uloganalyzer;
}