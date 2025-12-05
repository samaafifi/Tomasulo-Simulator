package simulator.gui;

import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.util.Map;
import java.util.HashMap;

/**
 * Control panel for simulation controls and configuration.
 */
public class ControlPanel {
    private SimulationController controller;
    private VBox panel;
    
    // Control buttons
    private Button startButton;
    private Button stepButton;
    private Button runAllButton;
    private Button pauseButton;
    private Button resetButton;
    
    // Configuration inputs
    private TextField cycleCounter;
    private Accordion configAccordion;
    
    // Station size fields
    private TextField fpAddSizeField;
    private TextField fpMulSizeField;
    private TextField fpDivSizeField;
    private TextField intAddSizeField;
    private TextField intMulSizeField;
    private TextField branchSizeField;
    private TextField loadSizeField;
    private TextField storeSizeField;
    private TextField lsbSizeField;
    
    // Cache configuration fields - CRITICAL: Store references to read current values
    private TextField cacheSizeField;
    private TextField blockSizeField;
    private TextField hitLatencyField;
    private TextField missPenaltyField;
    
    public ControlPanel(SimulationController controller) {
        this.controller = controller;
        createPanel();
    }
    
    private void createPanel() {
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));
        vbox.setStyle("-fx-background-color: #e8f4f8;");
        
        // Title
        Label title = new Label("Simulation Controls");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        vbox.getChildren().add(title);
        
        // Cycle counter - white banner style
        HBox cycleBanner = new HBox();
        cycleBanner.setAlignment(Pos.CENTER);
        cycleBanner.setPadding(new Insets(10));
        cycleBanner.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 5; -fx-border-color: #cccccc; -fx-border-width: 2; -fx-border-radius: 5;");
        Label cycleLabel = new Label("CYCLE ");
        cycleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        cycleLabel.setTextFill(javafx.scene.paint.Color.BLACK);
        cycleCounter = new TextField("0");
        cycleCounter.setEditable(false);
        cycleCounter.setPrefWidth(80);
        cycleCounter.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-alignment: center; -fx-background-color: #f0f0f0; -fx-text-fill: #000000; -fx-border-color: #999999; -fx-border-width: 1;");
        cycleBanner.getChildren().addAll(cycleLabel, cycleCounter);
        vbox.getChildren().add(cycleBanner);
        
        // Control buttons
        HBox buttonRow1 = new HBox(5);
        startButton = new Button("Start");
        startButton.setPrefWidth(100);
        startButton.setOnAction(e -> startSimulation());
        
        stepButton = new Button("Step Cycle");
        stepButton.setPrefWidth(100);
        stepButton.setOnAction(e -> stepCycle());
        
        buttonRow1.getChildren().addAll(startButton, stepButton);
        
        HBox buttonRow2 = new HBox(5);
        runAllButton = new Button("Run All");
        runAllButton.setPrefWidth(100);
        runAllButton.setOnAction(e -> runAll());
        
        pauseButton = new Button("Pause");
        pauseButton.setPrefWidth(100);
        pauseButton.setDisable(true);
        pauseButton.setOnAction(e -> pause());
        
        buttonRow2.getChildren().addAll(runAllButton, pauseButton);
        
        HBox buttonRow3 = new HBox(5);
        resetButton = new Button("Reset");
        resetButton.setPrefWidth(100);
        resetButton.setOnAction(e -> reset());
        
        buttonRow3.getChildren().add(resetButton);
        
        vbox.getChildren().addAll(buttonRow1, buttonRow2, buttonRow3);
        
        // Configuration accordion
        configAccordion = createConfigurationAccordion();
        vbox.getChildren().add(configAccordion);
        
        this.panel = vbox;
    }
    
    private Accordion createConfigurationAccordion() {
        Accordion accordion = new Accordion();
        
        // Instruction Latencies
        TitledPane latenciesPane = new TitledPane("Instruction Latencies", createLatenciesPane());
        latenciesPane.setExpanded(false);
        
        // Cache Configuration
        TitledPane cachePane = new TitledPane("Cache Configuration", createCachePane());
        cachePane.setExpanded(false);
        
        // Station and Buffer Sizes
        TitledPane bufferPane = new TitledPane("Station & Buffer Sizes", createBufferPane());
        bufferPane.setExpanded(false);
        
        accordion.getPanes().addAll(latenciesPane, cachePane, bufferPane);
        return accordion;
    }
    
    private VBox createLatenciesPane() {
        VBox vbox = new VBox(5);
        vbox.setPadding(new Insets(10));
        
        // Integer operations - User must enter values
        HBox intAddBox = createLatencyInput("DADDI:", "");
        HBox intSubBox = createLatencyInput("DSUBI:", "");
        
        // FP operations - User must enter values
        HBox fpAddBox = createLatencyInput("ADD.D:", "");
        HBox fpSubBox = createLatencyInput("SUB.D:", "");
        HBox fpMulBox = createLatencyInput("MUL.D:", "");
        HBox fpDivBox = createLatencyInput("DIV.D:", "");
        
        // Memory operations - User must enter values
        HBox loadBox = createLatencyInput("Load (L.D/LW):", "");
        HBox storeBox = createLatencyInput("Store (S.D/SW):", "");
        
        // Branch operations - User must enter values
        HBox branchBox = createLatencyInput("Branch (BEQ/BNE):", "");
        
        vbox.getChildren().addAll(
            new Label("Integer Operations:"),
            intAddBox, intSubBox,
            new Separator(),
            new Label("Floating-Point Operations:"),
            fpAddBox, fpSubBox, fpMulBox, fpDivBox,
            new Separator(),
            new Label("Memory Operations:"),
            loadBox, storeBox,
            new Separator(),
            new Label("Branch Operations:"),
            branchBox
        );
        
        return vbox;
    }
    
    private HBox createLatencyInput(String label, String defaultValue) {
        HBox hbox = new HBox(10);
        Label lbl = new Label(label);
        lbl.setPrefWidth(120);
        TextField field = new TextField(defaultValue);
        field.setPrefWidth(60);

        // Apply on Enter
        field.setOnAction(e -> updateLatency(label.replace(":", ""), field.getText()));

        // Also apply on focus loss so user input is not ignored if they don't press Enter
        field.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                String text = field.getText();
                if (text != null && !text.trim().isEmpty()) {
                    updateLatency(label.replace(":", ""), text);
                }
            }
        });

        hbox.getChildren().addAll(lbl, field);
        return hbox;
    }
    
    private void updateLatency(String instruction, String value) {
        try {
            if (value == null || value.trim().isEmpty()) {
                showError("Invalid Latency", "Please enter a latency value (must be at least 1 cycle).");
                return;
            }
            
            int latency = Integer.parseInt(value.trim());
            if (latency < 1) {
                showError("Invalid Latency", "Latency must be at least 1 cycle.");
                return;
            }
            
            // Handle load/store latencies specially - update MemorySystem
            if (instruction.contains("Load")) {
                controller.setLoadLatency(latency);
                // Also set for all load instruction types
                controller.setLatency("L.D", latency);
                controller.setLatency("L.S", latency);
                controller.setLatency("LW", latency);
                controller.setLatency("LD", latency);
                controller.log("Updated Load latency to " + latency + " cycles");
            } else if (instruction.contains("Store")) {
                controller.setStoreLatency(latency);
                // Also set for all store instruction types
                controller.setLatency("S.D", latency);
                controller.setLatency("S.S", latency);
                controller.setLatency("SW", latency);
                controller.setLatency("SD", latency);
                controller.log("Updated Store latency to " + latency + " cycles");
            } else {
                // Map display names to instruction types
                String instrType = mapInstructionName(instruction);
                controller.setLatency(instrType, latency);
                controller.log("Updated " + instrType + " latency to " + latency);
            }
        } catch (NumberFormatException e) {
            showError("Invalid Input", "Please enter a valid integer.");
        }
    }
    
    private String mapInstructionName(String displayName) {
        if (displayName.contains("DADDI")) return "DADDI";
        if (displayName.contains("DSUBI")) return "DSUBI";
        if (displayName.contains("ADD.D")) return "ADD.D";
        if (displayName.contains("SUB.D")) return "SUB.D";
        if (displayName.contains("MUL.D")) return "MUL.D";
        if (displayName.contains("DIV.D")) return "DIV.D";
        if (displayName.contains("Load")) return "L.D";
        if (displayName.contains("Store")) return "S.D";
        if (displayName.contains("Branch")) return "BEQ";
        return displayName;
    }
    
    private VBox createCachePane() {
        VBox vbox = new VBox(5);
        vbox.setPadding(new Insets(10));
        
        // CRITICAL FIX: Store references to text fields so we can read current values
        // User must enter all cache configuration values
        HBox cacheSizeBox = createCacheConfigInput("Cache Size (KB):", "", "cacheSize");
        cacheSizeField = extractTextField(cacheSizeBox);
        
        HBox blockSizeBox = createCacheConfigInput("Block Size (bytes):", "", "blockSize");
        blockSizeField = extractTextField(blockSizeBox);
        
        HBox hitLatencyBox = createCacheConfigInput("Hit Latency (cycles):", "", "hitLatency");
        hitLatencyField = extractTextField(hitLatencyBox);
        
        HBox missPenaltyBox = createCacheConfigInput("Miss Penalty (cycles):", "", "missPenalty");
        missPenaltyField = extractTextField(missPenaltyBox);
        
        vbox.getChildren().addAll(cacheSizeBox, blockSizeBox, hitLatencyBox, missPenaltyBox);
        
        return vbox;
    }
    
    // Helper method to extract TextField from HBox
    private TextField extractTextField(HBox hbox) {
        for (javafx.scene.Node node : hbox.getChildren()) {
            if (node instanceof TextField) {
                return (TextField) node;
            }
        }
        return null;
    }
    
    // Note: createConfigInput method is currently unused (replaced by createCacheConfigInput)
    // Keeping commented out for potential future use
    /*
    private HBox createConfigInput(String label, String defaultValue) {
        HBox hbox = new HBox(10);
        Label lbl = new Label(label);
        lbl.setPrefWidth(150);
        TextField field = new TextField(defaultValue);
        field.setPrefWidth(100);
        hbox.getChildren().addAll(lbl, field);
        return hbox;
    }
    */
    
    private HBox createCacheConfigInput(String label, String defaultValue, String configType) {
        HBox hbox = new HBox(10);
        Label lbl = new Label(label);
        lbl.setPrefWidth(150);
        TextField field = new TextField(defaultValue);
        field.setPrefWidth(100);
        
        // Apply on Enter
        field.setOnAction(e -> updateCacheConfig(configType, field.getText()));
        
        // CRITICAL FIX: Also apply on focus loss so user input is not ignored
        // This ensures values are applied immediately when user types and clicks away
        field.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                String text = field.getText();
                if (text != null && !text.trim().isEmpty()) {
                    updateCacheConfig(configType, text);
                }
            }
        });
        
        hbox.getChildren().addAll(lbl, field);
        return hbox;
    }
    
    private void updateCacheConfig(String configType, String value) {
        try {
            if (value == null || value.trim().isEmpty()) {
                showError("Invalid Value", "Please enter a value for " + configType + ".");
                return;
            }
            
            int intValue = Integer.parseInt(value.trim());
            if (intValue < 1) {
                showError("Invalid Value", "Value must be at least 1.");
                return;
            }
            
            switch (configType) {
                case "cacheSize":
                    controller.setCacheSize(intValue * 1024); // Convert KB to bytes
                    controller.log("Updated Cache Size to " + intValue + " KB");
                    break;
                case "blockSize":
                    controller.setBlockSize(intValue);
                    controller.log("Updated Block Size to " + intValue + " bytes");
                    break;
                case "hitLatency":
                    controller.setCacheHitLatency(intValue);
                    controller.log("Updated Cache Hit Latency to " + intValue + " cycles");
                    break;
                case "missPenalty":
                    controller.setCacheMissPenalty(intValue);
                    controller.log("Updated Cache Miss Penalty to " + intValue + " cycles");
                    break;
            }
        } catch (NumberFormatException e) {
            showError("Invalid Input", "Please enter a valid integer.");
        }
    }
    
    private VBox createBufferPane() {
        VBox vbox = new VBox(5);
        vbox.setPadding(new Insets(10));
        
        // FP Stations - User must enter values
        Label fpLabel = new Label("Floating-Point Stations:");
        fpLabel.setStyle("-fx-font-weight: bold;");
        HBox fpAddBox = createStationSizeInput("FP Add/Sub:", "", "FP_ADD");
        HBox fpMulBox = createStationSizeInput("FP Mul/Div:", "", "FP_MUL");
        HBox fpDivBox = createStationSizeInput("FP Mul/Div (Div):", "", "FP_DIV");
        
        // Integer Stations - User must enter values
        Label intLabel = new Label("Integer Stations:");
        intLabel.setStyle("-fx-font-weight: bold;");
        HBox intAddBox = createStationSizeInput("Integer Add/Sub:", "", "INTEGER_ADD");
        HBox intMulBox = createStationSizeInput("Integer Mul/Div:", "", "INTEGER_MUL");
        
        // Branch Stations - User must enter values
        Label branchLabel = new Label("Branch Stations:");
        branchLabel.setStyle("-fx-font-weight: bold;");
        HBox branchBox = createStationSizeInput("Branch:", "", "BRANCH");
        
        // Memory Stations - User must enter values
        Label memLabel = new Label("Memory Stations:");
        memLabel.setStyle("-fx-font-weight: bold;");
        HBox loadBox = createStationSizeInput("Load Buffer:", "", "LOAD");
        HBox storeBox = createStationSizeInput("Store Buffer:", "", "STORE");
        HBox lsbBox = createStationSizeInput("Load/Store Buffer Size:", "", "LSB");
        
        Button applyButton = new Button("Apply Configuration");
        applyButton.setOnAction(e -> applyStationConfiguration());
        applyButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        
        vbox.getChildren().addAll(
            fpLabel, fpAddBox, fpMulBox, fpDivBox,
            new Separator(),
            intLabel, intAddBox, intMulBox,
            new Separator(),
            branchLabel, branchBox,
            new Separator(),
            memLabel, loadBox, storeBox, lsbBox,
            new Separator(),
            applyButton
        );
        
        return vbox;
    }
    
    private HBox createStationSizeInput(String label, String defaultValue, String stationType) {
        HBox hbox = new HBox(10);
        Label lbl = new Label(label);
        lbl.setPrefWidth(150);
        TextField field = new TextField(defaultValue);
        field.setPrefWidth(60);
        field.setUserData(stationType); // Store station type in field
        
        // Store reference to field based on station type
        switch (stationType) {
            case "FP_ADD": fpAddSizeField = field; break;
            case "FP_MUL": fpMulSizeField = field; break;
            case "FP_DIV": fpDivSizeField = field; break;
            case "INTEGER_ADD": intAddSizeField = field; break;
            case "INTEGER_MUL": intMulSizeField = field; break;
            case "BRANCH": branchSizeField = field; break;
            case "LOAD": loadSizeField = field; break;
            case "STORE": storeSizeField = field; break;
            case "LSB": lsbSizeField = field; break;
        }
        
        hbox.getChildren().addAll(lbl, field);
        return hbox;
    }
    
    private void applyStationConfiguration() {
        try {
            // Validate all fields are filled
            if (fpAddSizeField.getText().trim().isEmpty() ||
                fpMulSizeField.getText().trim().isEmpty() ||
                fpDivSizeField.getText().trim().isEmpty() ||
                intAddSizeField.getText().trim().isEmpty() ||
                branchSizeField.getText().trim().isEmpty() ||
                loadSizeField.getText().trim().isEmpty() ||
                storeSizeField.getText().trim().isEmpty() ||
                lsbSizeField.getText().trim().isEmpty()) {
                showError("Missing Configuration", "Please fill in all station and buffer size fields.");
                return;
            }
            
            Map<String, Integer> stationCounts = new HashMap<>();
            
            stationCounts.put("FP_ADD", Integer.parseInt(fpAddSizeField.getText().trim()));
            stationCounts.put("FP_MUL", Integer.parseInt(fpMulSizeField.getText().trim()));
            stationCounts.put("FP_DIV", Integer.parseInt(fpDivSizeField.getText().trim()));
            stationCounts.put("INTEGER_ADD", Integer.parseInt(intAddSizeField.getText().trim()));
            if (intMulSizeField != null && !intMulSizeField.getText().trim().isEmpty()) {
                stationCounts.put("INTEGER_MUL", Integer.parseInt(intMulSizeField.getText().trim()));
            }
            stationCounts.put("BRANCH", Integer.parseInt(branchSizeField.getText().trim()));
            stationCounts.put("LOAD", Integer.parseInt(loadSizeField.getText().trim()));
            stationCounts.put("STORE", Integer.parseInt(storeSizeField.getText().trim()));
            
            int lsbSize = Integer.parseInt(lsbSizeField.getText().trim());
            
            // Validate all values are positive
            for (Map.Entry<String, Integer> entry : stationCounts.entrySet()) {
                if (entry.getValue() < 1) {
                    showError("Invalid Configuration", entry.getKey() + " size must be at least 1.");
                    return;
                }
            }
            if (lsbSize < 1) {
                showError("Invalid Configuration", "Load/Store Buffer size must be at least 1.");
                return;
            }
            
            controller.applyStationConfiguration(stationCounts, lsbSize);
            controller.log("Station and buffer configuration applied successfully.");
            
        } catch (NumberFormatException e) {
            showError("Invalid Input", "Please enter valid integers for all station sizes.");
        } catch (Exception e) {
            showError("Configuration Error", "Failed to apply configuration: " + e.getMessage());
        }
    }
    
    private void startSimulation() {
        // CRITICAL FIX: Apply all current GUI values before starting simulation
        // This ensures the simulation uses the values currently in the text fields,
        // not defaults or previously entered values
        applyAllCurrentConfigValues();
        
        // Check if stations are configured
        if (controller.getStationCounts().isEmpty()) {
            showError("Configuration Required", 
                "Please configure station and buffer sizes before starting the simulation.\n" +
                "Go to 'Station & Buffer Sizes' in the configuration panel and click 'Apply Configuration'.");
            return;
        }
        
        controller.log("Simulation started.");
        startButton.setDisable(true);
        stepButton.setDisable(false);
        runAllButton.setDisable(false);
    }
    
    /**
     * CRITICAL FIX: Apply all current values from GUI text fields to backend
     * This ensures simulation uses CURRENT user inputs, not defaults or stale values
     */
    private void applyAllCurrentConfigValues() {
        // Apply cache configuration from current text field values
        if (cacheSizeField != null && !cacheSizeField.getText().trim().isEmpty()) {
            updateCacheConfig("cacheSize", cacheSizeField.getText());
        }
        if (blockSizeField != null && !blockSizeField.getText().trim().isEmpty()) {
            updateCacheConfig("blockSize", blockSizeField.getText());
        }
        if (hitLatencyField != null && !hitLatencyField.getText().trim().isEmpty()) {
            updateCacheConfig("hitLatency", hitLatencyField.getText());
        }
        if (missPenaltyField != null && !missPenaltyField.getText().trim().isEmpty()) {
            updateCacheConfig("missPenalty", missPenaltyField.getText());
        }
        
        controller.log("Applied all current configuration values from GUI");
    }
    
    private void stepCycle() {
        controller.stepCycle();
        updateCycleCounter();
    }
    
    private void runAll() {
        controller.runAll();
        startButton.setDisable(true);
        stepButton.setDisable(true);
        runAllButton.setDisable(true);
        pauseButton.setDisable(false);
    }
    
    private void pause() {
        controller.pause();
        startButton.setDisable(false);
        stepButton.setDisable(false);
        runAllButton.setDisable(false);
        pauseButton.setDisable(true);
    }
    
    private void reset() {
        controller.reset();
        updateCycleCounter();
        startButton.setDisable(false);
        stepButton.setDisable(false);
        runAllButton.setDisable(false);
        pauseButton.setDisable(true);
    }
    
    public void updateCycleCounter() {
        cycleCounter.setText(String.valueOf(controller.getCurrentCycle()));
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
