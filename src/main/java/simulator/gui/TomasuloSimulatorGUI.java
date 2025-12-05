package simulator.gui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.geometry.Insets;

/**
 * Main JavaFX Application for Tomasulo Algorithm Simulator
 * 
 * Features:
 * - Step-by-step cycle execution
 * - Visual tables for all components
 * - Configurable parameters
 * - Instruction input and file loading
 * - Comprehensive logging
 */
public class TomasuloSimulatorGUI extends Application {
    
    private SimulationController controller;
    private InstructionInputPanel instructionPanel;
    private ControlPanel controlPanel;
    private VisualizationPanel visualizationPanel;
    
    @Override
    public void start(Stage primaryStage) {
        try {
            primaryStage.setTitle("Tomasulo Algorithm Simulator - MIPS Instruction Execution");
            
            // Initialize controller
            controller = new SimulationController();
            
            // Create main layout
            BorderPane root = new BorderPane();
            root.setPadding(new Insets(10));
            
            // Top: Control Panel
            controlPanel = new ControlPanel(controller);
            root.setTop(controlPanel.getPanel());
            
            // Center: Main content area with split (no log panel)
            SplitPane centerSplit = new SplitPane();
            
            // Left: Instruction Input Panel (narrow)
            instructionPanel = new InstructionInputPanel(controller);
            VBox leftPanel = new VBox(10, instructionPanel.getPanel());
            leftPanel.setPrefWidth(300);
            leftPanel.setMinWidth(250);
            
            // Center: Visualization Panel (takes all remaining space)
            visualizationPanel = new VisualizationPanel(controller);
            VBox centerPanel = new VBox(5, visualizationPanel.getPanel());
            centerPanel.setPadding(new Insets(5));
            
            centerSplit.getItems().addAll(leftPanel, centerPanel);
            centerSplit.setDividerPositions(0.20); // 20% left, 80% center
            
            root.setCenter(centerSplit);
            
            // Wrap everything in ScrollPane for full page scrolling
            ScrollPane mainScrollPane = new ScrollPane(root);
            mainScrollPane.setFitToWidth(true);
            mainScrollPane.setFitToHeight(true);
            mainScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            mainScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            mainScrollPane.setPannable(true);
            
            // Set up controller callbacks
            controller.setUpdateCallback(() -> {
                controlPanel.updateCycleCounter();
                visualizationPanel.update();
            });
            
            // Create scene with scrollable root
            Scene scene = new Scene(mainScrollPane, 1600, 900);
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1200);
            primaryStage.setMinHeight(700);
            primaryStage.setMaximized(true); // Start maximized to use full screen
            primaryStage.show();
            
            // Initialize log
            controller.log("Tomasulo Simulator initialized. Load instructions to begin.");
            
        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to initialize GUI", e.getMessage());
        }
    }
    
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
