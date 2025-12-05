package simulator.gui;

import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Panel for displaying simulation logs.
 */
public class LogPanel {
    private SimulationController controller;
    private VBox panel;
    private TextArea logArea;
    private ScrollPane scrollPane;
    
    public LogPanel(SimulationController controller) {
        this.controller = controller;
        createPanel();
    }
    
    private void createPanel() {
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));
        vbox.setStyle("-fx-background-color: #f9f9f9;");
        
        // Title
        HBox titleBox = new HBox(10);
        Label title = new Label("Simulation Log");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        
        Button clearButton = new Button("Clear");
        clearButton.setOnAction(e -> clearLog());
        
        Button exportButton = new Button("Export");
        exportButton.setOnAction(e -> exportLog());
        
        titleBox.getChildren().addAll(title, clearButton, exportButton);
        vbox.getChildren().add(titleBox);
        
        // Log text area
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setFont(Font.font("Consolas", 12));
        logArea.setStyle("-fx-background-color: #1e1e1e; -fx-text-fill: #d4d4d4;");
        
        scrollPane = new ScrollPane(logArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        vbox.getChildren().add(scrollPane);
        
        this.panel = vbox;
    }
    
    public void update() {
        StringBuilder sb = new StringBuilder();
        for (String message : controller.getLogMessages()) {
            sb.append(message).append("\n");
        }
        logArea.setText(sb.toString());
        
        // Auto-scroll to bottom
        logArea.setScrollTop(Double.MAX_VALUE);
    }
    
    private void clearLog() {
        logArea.clear();
        controller.log("Log cleared.");
    }
    
    private void exportLog() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Export Log");
        fileChooser.getExtensionFilters().add(
            new javafx.stage.FileChooser.ExtensionFilter("Text Files", "*.txt")
        );
        
        javafx.stage.Stage stage = (javafx.stage.Stage) panel.getScene().getWindow();
        java.io.File file = fileChooser.showSaveDialog(stage);
        
        if (file != null) {
            try {
                java.nio.file.Files.writeString(file.toPath(), logArea.getText());
                controller.log("Log exported to: " + file.getName());
            } catch (Exception e) {
                showError("Export Failed", "Could not export log: " + e.getMessage());
            }
        }
    }
    
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    public VBox getPanel() {
        return panel;
    }
}
