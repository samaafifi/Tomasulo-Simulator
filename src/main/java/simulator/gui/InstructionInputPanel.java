package simulator.gui;

import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/**
 * Panel for inputting instructions either manually or from a file.
 */
public class InstructionInputPanel {
    private SimulationController controller;
    private TextArea instructionTextArea;
    private TextArea registerPreloadArea;
    private VBox panel;
    
    public InstructionInputPanel(SimulationController controller) {
        this.controller = controller;
        createPanel();
    }
    
    private void createPanel() {
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));
        vbox.setStyle("-fx-background-color: #f5f5f5;");
        
        // Title
        Label title = new Label("Instruction Input");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        vbox.getChildren().add(title);
        
        // Instruction text area
        Label instructionLabel = new Label("Enter MIPS Instructions:");
        instructionTextArea = new TextArea();
        instructionTextArea.setPrefRowCount(20);
        instructionTextArea.setPrefColumnCount(30);
        instructionTextArea.setWrapText(true);
        instructionTextArea.setPromptText(
            "Enter instructions, one per line:\n" +
            "L.D F6, 0(R2)\n" +
            "L.D F2, 8(R2)\n" +
            "MUL.D F0, F2, F4\n" +
            "ADD.D F6, F8, F2\n" +
            "..."
        );
        
        // Buttons
        HBox buttonBox = new HBox(10);
        buttonBox.setPadding(new Insets(5, 0, 5, 0));
        
        Button loadFileButton = new Button("Load Instructions");
        loadFileButton.setOnAction(e -> loadFromFile());
        
        Button loadButton = new Button("Load");
        loadButton.setOnAction(e -> loadInstructions());
        
        Button clearButton = new Button("Clear");
        clearButton.setOnAction(e -> clearInstructions());
        
        buttonBox.getChildren().addAll(loadFileButton, loadButton, clearButton);
        
        vbox.getChildren().addAll(instructionLabel, instructionTextArea, buttonBox);
        
        // Register Pre-loading Section
        Separator separator = new Separator();
        separator.setPadding(new Insets(10, 0, 10, 0));
        vbox.getChildren().add(separator);
        
        Label registerLabel = new Label("Register Pre-loading (Optional):");
        registerLabel.setStyle("-fx-font-weight: bold;");
        
        registerPreloadArea = new TextArea();
        registerPreloadArea.setPrefRowCount(8);
        registerPreloadArea.setPrefColumnCount(30);
        registerPreloadArea.setWrapText(true);
        registerPreloadArea.setPromptText(
            "Enter register values:\n" +
            "Format: REGISTER=VALUE (comma-separated or one per line)\n" +
            "Examples:\n" +
            "R2=1000, F4=1.5, F6=3.14\n" +
            "or\n" +
            "R2=1000\n" +
            "F4=1.5\n" +
            "F6=3.14"
        );
        
        HBox registerButtonBox = new HBox(10);
        registerButtonBox.setPadding(new Insets(5, 0, 5, 0));
        
        Button loadRegistersButton = new Button("Load Registers");
        loadRegistersButton.setOnAction(e -> loadRegisters());
        
        Button clearRegistersButton = new Button("Clear Registers");
        clearRegistersButton.setOnAction(e -> clearRegisters());
        
        registerButtonBox.getChildren().addAll(loadRegistersButton, clearRegistersButton);
        
        vbox.getChildren().addAll(registerLabel, registerPreloadArea, registerButtonBox);
        
        this.panel = vbox;
    }
    
    private void loadFromFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load Instructions from File");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Text Files", "*.txt", "*.asm", "*.s")
        );
        
        // Get the stage from any node
        Stage stage = (Stage) panel.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);
        
        if (file != null) {
            try {
                String content = Files.readString(file.toPath());
                instructionTextArea.setText(content);
                controller.log("Loaded instructions from: " + file.getName());
            } catch (Exception e) {
                showError("Error loading file", e.getMessage());
            }
        }
    }
    
    private void loadInstructions() {
        String code = instructionTextArea.getText().trim();
        if (code.isEmpty()) {
            showError("No Instructions", "Please enter instructions in the text area.");
            return;
        }
        
        if (controller.loadInstructions(code)) {
            controller.log("Instructions loaded successfully!");
        } else {
            showError("Load Failed", "Failed to parse instructions. Check syntax.");
        }
    }
    
    private void clearInstructions() {
        instructionTextArea.clear();
        controller.log("Instruction area cleared.");
    }
    
    private void loadRegisters() {
        String text = registerPreloadArea.getText().trim();
        if (text.isEmpty()) {
            controller.log("No register values to load.");
            return;
        }
        
        Map<String, Double> registerValues = new HashMap<>();
        int loaded = 0;
        int errors = 0;
        
        // First, try to split by comma (comma-separated format)
        String[] entries = text.split(",");
        
        // If no commas found, treat as line-separated
        if (entries.length == 1 && !text.contains(",")) {
            entries = text.split("\n");
        }
        
        for (String entry : entries) {
            entry = entry.trim();
            if (entry.isEmpty() || entry.startsWith("#")) continue; // Skip empty entries and comments
            
            try {
                // Parse format: REGISTER=VALUE
                int equalsIndex = entry.indexOf('=');
                if (equalsIndex == -1) {
                    errors++;
                    continue;
                }
                
                String regName = entry.substring(0, equalsIndex).trim();
                String valueStr = entry.substring(equalsIndex + 1).trim();
                
                // Validate register name (F0-F31 or R0-R31)
                if (!regName.matches("^[FR]\\d+$")) {
                    errors++;
                    controller.log("Invalid register name: " + regName);
                    continue;
                }
                
                double value = Double.parseDouble(valueStr);
                registerValues.put(regName, value);
                loaded++;
                
            } catch (NumberFormatException e) {
                errors++;
                controller.log("Invalid value format: " + entry);
            }
        }
        
        if (loaded > 0) {
            controller.preloadRegisters(registerValues);
            controller.log("Loaded " + loaded + " register values.");
        }
        
        if (errors > 0) {
            showError("Parse Errors", errors + " entries had errors and were skipped.");
        }
    }
    
    private void clearRegisters() {
        registerPreloadArea.clear();
        controller.clearRegisterPreloads();
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
    
    public void setInstructions(String instructions) {
        instructionTextArea.setText(instructions);
    }
}
