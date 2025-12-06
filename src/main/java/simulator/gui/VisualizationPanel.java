package simulator.gui;

import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.paint.Color;
import simulator.tomasulo.issue.*;
import simulator.tomasulo.registerfile.*;
import simulator.memory.*;
import simulator.tomasulo.models.Instruction;
import java.util.*;

/**
 * Panel for visualizing all simulation components in tables.
 * Layout matches the reference design with all tables visible.
 */
public class VisualizationPanel {
    private SimulationController controller;
    private VBox panel;
    
    // Tables
    private TableView<InstructionStatusRow> instructionStatusTable;
    private TableView<RegisterRow> registerTable;
    private TableView<LSBRow> loadBufferTable;
    private TableView<LSBRow> storeBufferTable;
    // Note: rsTablesByType was intended for future use but is currently unused
    // Keeping commented out for potential future enhancement
    // private Map<String, TableView<ReservationStationRow>> rsTablesByType;
    
    public VisualizationPanel(SimulationController controller) {
        this.controller = controller;
        // this.rsTablesByType = new HashMap<>();
        createPanel();
    }
    
    private void createPanel() {
        // Main container with horizontal split
        HBox mainContainer = new HBox(5);
        mainContainer.setPadding(new Insets(3));
        
        // LEFT SIDE: Instruction Status Table (the main text box from image)
        VBox leftPanel = new VBox(3);
        leftPanel.setPadding(new Insets(3));
        leftPanel.setPrefWidth(420);
        leftPanel.setMinWidth(380);
        
        Label instructionLabel = new Label("Instruction Status");
        instructionLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        instructionStatusTable = createInstructionStatusTable();
        VBox.setVgrow(instructionStatusTable, Priority.ALWAYS);
        leftPanel.getChildren().addAll(instructionLabel, instructionStatusTable);
        
        // RIGHT SIDE: All other tables in a grid
        VBox rightPanel = new VBox(15); // Increased spacing
        rightPanel.setPadding(new Insets(5));
        
        // Top row: Register File
        HBox topRow = new HBox(10); // Increased spacing
        VBox registerBox = createRegisterFileSection();
        HBox.setHgrow(registerBox, Priority.ALWAYS);
        topRow.getChildren().add(registerBox);
        
        // Middle rows: Reservation Stations (grouped by type)
        rsSectionContainer = createReservationStationsSection();
        VBox.setVgrow(rsSectionContainer, Priority.ALWAYS);
        
        // Bottom row: Load and Store Buffers
        HBox bufferRow = new HBox(10); // Increased spacing
        VBox loadBufferBox = createLoadBufferSection();
        VBox storeBufferBox = createStoreBufferSection();
        HBox.setHgrow(loadBufferBox, Priority.ALWAYS);
        HBox.setHgrow(storeBufferBox, Priority.ALWAYS);
        bufferRow.getChildren().addAll(loadBufferBox, storeBufferBox);
        
        rightPanel.getChildren().addAll(topRow, rsSectionContainer, bufferRow);
        
        // Combine left and right
        mainContainer.getChildren().addAll(leftPanel, rightPanel);
        HBox.setHgrow(rightPanel, Priority.ALWAYS);
        mainContainer.setSpacing(15); // Increased spacing between left and right panels
        
        // Wrap in ScrollPane for scrolling
        ScrollPane scrollPane = new ScrollPane(mainContainer);
        scrollPane.setFitToWidth(false); // Allow horizontal scrolling if needed
        scrollPane.setFitToHeight(false); // Allow vertical scrolling if needed
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPannable(true); // Allow panning with mouse drag
        scrollPane.setPadding(new Insets(5));
        
        // Container that fills available space
        VBox container = new VBox();
        container.getChildren().add(scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        
        this.panel = container;
    }
    
    private TableView<InstructionStatusRow> createInstructionStatusTable() {
        TableView<InstructionStatusRow> table = new TableView<>();
        
        TableColumn<InstructionStatusRow, String> instrCol = new TableColumn<>("Instruction");
        instrCol.setCellValueFactory(data -> data.getValue().instructionProperty());
        instrCol.setPrefWidth(150);
        instrCol.setMinWidth(120);
        
        TableColumn<InstructionStatusRow, String> jCol = new TableColumn<>("j");
        jCol.setCellValueFactory(data -> data.getValue().jProperty());
        jCol.setPrefWidth(40);
        jCol.setMinWidth(35);
        
        TableColumn<InstructionStatusRow, String> kCol = new TableColumn<>("k");
        kCol.setCellValueFactory(data -> data.getValue().kProperty());
        kCol.setPrefWidth(40);
        kCol.setMinWidth(35);
        
        TableColumn<InstructionStatusRow, String> issueCol = new TableColumn<>("Issue");
        issueCol.setCellValueFactory(data -> data.getValue().issueProperty());
        issueCol.setPrefWidth(50);
        issueCol.setMinWidth(45);
        
        TableColumn<InstructionStatusRow, String> execCol = new TableColumn<>("Exec Complete");
        execCol.setCellValueFactory(data -> data.getValue().execCompleteProperty());
        execCol.setPrefWidth(100);
        execCol.setMinWidth(90);
        
        TableColumn<InstructionStatusRow, String> writeCol = new TableColumn<>("Write Result");
        writeCol.setCellValueFactory(data -> data.getValue().writeResultProperty());
        writeCol.setPrefWidth(80);
        writeCol.setMinWidth(70);
        
        table.getColumns().addAll(instrCol, jCol, kCol, issueCol, execCol, writeCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        
        return table;
    }
    
    private VBox createRegisterFileSection() {
        VBox vbox = new VBox(3);
        vbox.setPadding(new Insets(3));
        vbox.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1; -fx-border-radius: 3;");
        
        Label label = new Label("Reg. file");
        label.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        registerTable = createRegisterTable();
        registerTable.setPrefHeight(150);
        registerTable.setMinHeight(120);
        VBox.setVgrow(registerTable, Priority.SOMETIMES);
        
        vbox.getChildren().addAll(label, registerTable);
        return vbox;
    }
    
    private VBox createLoadBufferSection() {
        VBox vbox = new VBox(3);
        vbox.setPadding(new Insets(3));
        vbox.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1; -fx-border-radius: 3;");
        
        Label label = new Label("Load buffers");
        label.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        loadBufferTable = createLSBTable(true);
        loadBufferTable.setPrefHeight(120);
        loadBufferTable.setMinHeight(100);
        VBox.setVgrow(loadBufferTable, Priority.SOMETIMES);
        
        vbox.getChildren().addAll(label, loadBufferTable);
        return vbox;
    }
    
    private VBox createStoreBufferSection() {
        VBox vbox = new VBox(3);
        vbox.setPadding(new Insets(3));
        vbox.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1; -fx-border-radius: 3;");
        
        Label label = new Label("Store buffers");
        label.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        storeBufferTable = createLSBTable(false);
        storeBufferTable.setPrefHeight(120);
        storeBufferTable.setMinHeight(100);
        VBox.setVgrow(storeBufferTable, Priority.SOMETIMES);
        
        vbox.getChildren().addAll(label, storeBufferTable);
        return vbox;
    }
    
    private VBox rsSectionContainer; // Store reference to RS section
    
    private VBox createReservationStationsSection() {
        VBox vbox = new VBox(8);
        vbox.setPadding(new Insets(8));
        vbox.setStyle("-fx-border-color: #0066cc; -fx-border-width: 3; -fx-border-radius: 5; -fx-background-color: #e6f2ff;");
        
        Label title = new Label("Reservation stations");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        title.setTextFill(Color.web("#003366"));
        title.setPadding(new Insets(0, 0, 5, 0));
        vbox.getChildren().add(title);
        
        // Container for RS tables
        HBox rsTablesContainer = new HBox(10);
        rsTablesContainer.setPadding(new Insets(5));
        vbox.getChildren().add(rsTablesContainer);
        VBox.setVgrow(rsTablesContainer, Priority.ALWAYS);
        
        this.rsSectionContainer = vbox;
        return vbox;
    }
    
    private TableView<RegisterRow> createRegisterTable() {
        TableView<RegisterRow> table = new TableView<>();
        
        TableColumn<RegisterRow, String> regCol = new TableColumn<>("Register");
        regCol.setCellValueFactory(data -> data.getValue().registerProperty());
        regCol.setPrefWidth(70);
        regCol.setMinWidth(60);
        
        TableColumn<RegisterRow, String> valueCol = new TableColumn<>("Value");
        valueCol.setCellValueFactory(data -> data.getValue().valueProperty());
        valueCol.setPrefWidth(100);
        valueCol.setMinWidth(80);
        
        TableColumn<RegisterRow, String> qiCol = new TableColumn<>("Qi");
        qiCol.setCellValueFactory(data -> data.getValue().qiProperty());
        qiCol.setPrefWidth(80);
        qiCol.setMinWidth(70);
        
        table.getColumns().addAll(regCol, valueCol, qiCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        
        // Color coding
        table.setRowFactory(tv -> new TableRow<RegisterRow>() {
            @Override
            protected void updateItem(RegisterRow item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                } else if (item.isBusy()) {
                    setStyle("-fx-background-color: #ccccff;");
                }
            }
        });
        
        return table;
    }
    
    private TableView<LSBRow> createLSBTable(boolean isLoad) {
        TableView<LSBRow> table = new TableView<>();
        
        TableColumn<LSBRow, String> nameCol = new TableColumn<>("Buffer");
        nameCol.setCellValueFactory(data -> data.getValue().nameProperty());
        nameCol.setPrefWidth(50);
        nameCol.setMinWidth(45);
        
        TableColumn<LSBRow, String> busyCol = new TableColumn<>("Busy");
        busyCol.setCellValueFactory(data -> data.getValue().busyProperty());
        busyCol.setPrefWidth(40);
        busyCol.setMinWidth(35);
        
        TableColumn<LSBRow, String> addressCol = new TableColumn<>("Address");
        addressCol.setCellValueFactory(data -> data.getValue().addressProperty());
        addressCol.setPrefWidth(80);
        addressCol.setMinWidth(70);
        
        table.getColumns().addAll(nameCol, busyCol, addressCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        
        return table;
    }
    
    public void update() {
        updateInstructionStatusTable();
        updateRegisterTable();
        updateLoadBufferTable();
        updateStoreBufferTable();
        updateReservationStationsTables();
    }
    
    private void updateInstructionStatusTable() {
        instructionStatusTable.getItems().clear();
        
        List<Instruction> instructions = controller.getInstructions();
        Map<Integer, Integer> issueCycles = controller.getIssueCycles();
        Map<Integer, Integer> execStartCycles = controller.getExecStartCycles();
        Map<Integer, Integer> execEndCycles = controller.getExecEndCycles();
        Map<Integer, Integer> completeCycles = controller.getCompleteCycles();
        
        for (Instruction instr : instructions) {
            InstructionStatusRow row = new InstructionStatusRow();
            
            // Format instruction string
            String instrStr = formatInstruction(instr);
            row.setInstruction(instrStr);
            
            // Get j and k (source registers or immediate values)
            if (instr.isMemoryOperation()) {
                // For memory ops, j is offset, k is base register
                row.setJ(instr.getOffset() != 0 ? String.valueOf(instr.getOffset()) : "");
                row.setK(instr.getBaseRegister() != null ? instr.getBaseRegister() : "");
            } else {
                row.setJ(instr.getSourceReg1() != null ? instr.getSourceReg1() : "");
                row.setK(instr.getSourceReg2() != null ? instr.getSourceReg2() : "");
            }
            
            // Issue cycle
            Integer issueCycle = issueCycles.get(instr.getId());
            row.setIssue(issueCycle != null ? String.valueOf(issueCycle) : "");
            
            // Execution complete range - show only for this specific instruction
            Integer execStart = execStartCycles.get(instr.getId());
            Integer execEnd = execEndCycles.get(instr.getId());
            if (execStart != null && execEnd != null && execStart > 0 && execEnd >= execStart) {
                if (execStart.equals(execEnd)) {
                    row.setExecComplete(String.valueOf(execStart));
                } else {
                    row.setExecComplete(execStart + "..." + execEnd);
                }
            } else {
                // Show empty if execution hasn't started yet
                row.setExecComplete("");
            }
            
            // Write result cycle
            Integer writeCycle = completeCycles.get(instr.getId());
            row.setWriteResult(writeCycle != null ? String.valueOf(writeCycle) : "");
            
            instructionStatusTable.getItems().add(row);
        }
    }
    
    private String formatInstruction(Instruction instr) {
        StringBuilder sb = new StringBuilder();
        sb.append(instr.getOperation());
        
        if (instr.isMemoryOperation()) {
            // Format: L.D F6, 32(R2) or S.D F6, 32(R2)
            if (instr.getDestRegister() != null) {
                sb.append(" ").append(instr.getDestRegister());
            }
            if (instr.getOffset() != 0 || instr.getBaseRegister() != null) {
                sb.append(" ");
                if (instr.getOffset() != 0) {
                    sb.append(instr.getOffset());
                }
                if (instr.getBaseRegister() != null && !instr.getBaseRegister().isEmpty()) {
                    sb.append("(").append(instr.getBaseRegister()).append(")");
                }
            }
        } else if (instr.getOperation().equals("BEQ") || instr.getOperation().equals("BNE")) {
            // Branch: BEQ R1, R2, label
            if (instr.getSourceReg1() != null) {
                sb.append(" ").append(instr.getSourceReg1());
            }
            if (instr.getSourceReg2() != null) {
                sb.append(" ").append(instr.getSourceReg2());
            }
        } else {
            // Arithmetic: ADD.D F6, F8, F2
            if (instr.getDestRegister() != null) {
                sb.append(" ").append(instr.getDestRegister());
            }
            if (instr.getSourceReg1() != null) {
                sb.append(" ").append(instr.getSourceReg1());
            }
            if (instr.getSourceReg2() != null) {
                sb.append(" ").append(instr.getSourceReg2());
            }
        }
        
        return sb.toString();
    }
    
    private void updateRegisterTable() {
        registerTable.getItems().clear();
        
        RegisterFile registerFile = controller.getRegisterFile();
        Map<String, Register> registers = registerFile.getAllRegisters();
        
        // Show all registers (F0-F31, R0-R31) - show all, including zero values
        for (int i = 0; i < 32; i++) {
            String fpName = "F" + i;
            Register fpReg = registers.get(fpName);
            if (fpReg != null) {
                RegisterRow row = new RegisterRow();
                row.setRegister(fpName);
                double value = fpReg.getValue();
                // Always show value (including 0.0 for preloaded registers)
                row.setValue(String.format("%.2f", value));
                row.setQi(fpReg.getQi() != null ? fpReg.getQi() : "");
                row.setBusy(fpReg.isBusy());
                registerTable.getItems().add(row);
            }
        }
        
        for (int i = 0; i < 32; i++) {
            String intName = "R" + i;
            Register intReg = registers.get(intName);
            if (intReg != null) {
                RegisterRow row = new RegisterRow();
                row.setRegister(intName);
                double value = intReg.getValue();
                // Always show value (including 0.0 for preloaded registers)
                row.setValue(String.format("%.2f", value));
                row.setQi(intReg.getQi() != null ? intReg.getQi() : "");
                row.setBusy(intReg.isBusy());
                registerTable.getItems().add(row);
            }
        }
    }
    
    private void updateLoadBufferTable() {
        loadBufferTable.getItems().clear();
        
        // Note: LSB entries can be accessed via controller.getMemorySystem().getLSBEntries() if needed
        
        // Get all load stations from RS pool
        ReservationStationPool rsPool = controller.getRsPool();
        List<ReservationStation> loadStations = rsPool.getStationsByType("LOAD");
        if (loadStations == null) loadStations = new ArrayList<>();
        
        // Create rows for all load stations
        for (int i = 0; i < loadStations.size(); i++) {
            ReservationStation rs = loadStations.get(i);
            LSBRow row = new LSBRow();
            row.setName("L" + (i + 1));
            row.setBusy(rs.isBusy() ? "1" : "0");
            
            if (rs.isBusy() && rs.getA() != null) {
                row.setAddress(String.valueOf(rs.getA()));
            } else {
                row.setAddress("");
            }
            
            loadBufferTable.getItems().add(row);
        }
    }
    
    private void updateStoreBufferTable() {
        storeBufferTable.getItems().clear();
        
        ReservationStationPool rsPool = controller.getRsPool();
        List<ReservationStation> storeStations = rsPool.getStationsByType("STORE");
        if (storeStations == null) storeStations = new ArrayList<>();
        
        // Create rows for all store stations
        for (int i = 0; i < storeStations.size(); i++) {
            ReservationStation rs = storeStations.get(i);
            LSBRow row = new LSBRow();
            row.setName("S" + (i + 1));
            row.setBusy(rs.isBusy() ? "1" : "0");
            
            if (rs.isBusy() && rs.getA() != null) {
                row.setAddress(String.valueOf(rs.getA()));
            } else {
                row.setAddress("");
            }
            
            storeBufferTable.getItems().add(row);
        }
    }
    
    private void updateReservationStationsTables() {
        if (rsSectionContainer == null) return;
        
        ReservationStationPool rsPool = controller.getRsPool();
        
        // Group stations by type - 4 reservation station types
        String[] stationTypes = {"FP_ADD", "FP_MUL", "FP_DIV", "INTEGER_ADD"};
        String[] typeLabels = {"FP Add/Sub", "FP Mul/Div", "Integer Add/Sub", "Integer Mul/Div"};
        
        // Find and clear existing tables container (keep title at index 0)
        HBox rsTablesContainer = null;
        if (rsSectionContainer.getChildren().size() > 1) {
            // Remove old container
            rsSectionContainer.getChildren().remove(1);
        }
        
        rsTablesContainer = new HBox(15); // Increased spacing
        rsTablesContainer.setPadding(new Insets(5));
        
        for (int i = 0; i < stationTypes.length; i++) {
            String type = stationTypes[i];
            List<ReservationStation> stations = new ArrayList<>();
            
            // Handle combined types
            if (i == 1) {
                // FP Mul/Div - combine FP_MUL and FP_DIV
                List<ReservationStation> mulStations = rsPool.getStationsByType("FP_MUL");
                List<ReservationStation> divStations = rsPool.getStationsByType("FP_DIV");
                if (mulStations != null) stations.addAll(mulStations);
                if (divStations != null) stations.addAll(divStations);
            } else if (i == 3) {
                // Integer Mul/Div - check if this type exists, otherwise show empty
                List<ReservationStation> intMulStations = rsPool.getStationsByType("INTEGER_MUL");
                List<ReservationStation> intDivStations = rsPool.getStationsByType("INTEGER_DIV");
                if (intMulStations != null) stations.addAll(intMulStations);
                if (intDivStations != null) stations.addAll(intDivStations);
            } else {
                // Regular types
                List<ReservationStation> typeStations = rsPool.getStationsByType(type);
                if (typeStations != null) stations.addAll(typeStations);
            }
            
            // Always show the table, even if empty
            VBox typeBox = new VBox(5);
            typeBox.setPadding(new Insets(8));
            typeBox.setStyle("-fx-border-color: #0066cc; -fx-border-width: 2; -fx-border-radius: 4; -fx-background-color: #ffffff;");
            
            Label typeLabel = new Label(typeLabels[i]);
            typeLabel.setFont(Font.font("Arial", FontWeight.BOLD, 13));
            typeLabel.setTextFill(Color.web("#003366"));
            typeLabel.setPadding(new Insets(0, 0, 3, 0));
            
            TableView<ReservationStationRow> table = createRSTable();
            table.setPrefHeight(150);
            table.setMinHeight(120);
            VBox.setVgrow(table, Priority.SOMETIMES);
            updateRSTableForType(table, stations);
            
            typeBox.getChildren().addAll(typeLabel, table);
            rsTablesContainer.getChildren().add(typeBox);
        }
        
        // Add the container (title is at index 0)
        rsSectionContainer.getChildren().add(rsTablesContainer);
    }
    
    private TableView<ReservationStationRow> createRSTable() {
        TableView<ReservationStationRow> table = new TableView<>();
        
        // Add name column first (like in the image)
        TableColumn<ReservationStationRow, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(data -> data.getValue().nameProperty());
        nameCol.setPrefWidth(50);
        nameCol.setMinWidth(45);
        
        TableColumn<ReservationStationRow, String> busyCol = new TableColumn<>("busy");
        busyCol.setCellValueFactory(data -> data.getValue().busyProperty());
        busyCol.setPrefWidth(40);
        busyCol.setMinWidth(35);
        
        TableColumn<ReservationStationRow, String> opCol = new TableColumn<>("op");
        opCol.setCellValueFactory(data -> data.getValue().operationProperty());
        opCol.setPrefWidth(55);
        opCol.setMinWidth(50);
        
        TableColumn<ReservationStationRow, String> vjCol = new TableColumn<>("Vj");
        vjCol.setCellValueFactory(data -> data.getValue().vjProperty());
        vjCol.setPrefWidth(60);
        vjCol.setMinWidth(55);
        
        TableColumn<ReservationStationRow, String> vkCol = new TableColumn<>("Vk");
        vkCol.setCellValueFactory(data -> data.getValue().vkProperty());
        vkCol.setPrefWidth(60);
        vkCol.setMinWidth(55);
        
        TableColumn<ReservationStationRow, String> qjCol = new TableColumn<>("Qj");
        qjCol.setCellValueFactory(data -> data.getValue().qjProperty());
        qjCol.setPrefWidth(45);
        qjCol.setMinWidth(40);
        
        TableColumn<ReservationStationRow, String> qkCol = new TableColumn<>("Qk");
        qkCol.setCellValueFactory(data -> data.getValue().qkProperty());
        qkCol.setPrefWidth(45);
        qkCol.setMinWidth(40);
        
        TableColumn<ReservationStationRow, String> aCol = new TableColumn<>("A");
        aCol.setCellValueFactory(data -> data.getValue().aProperty());
        aCol.setPrefWidth(50);
        aCol.setMinWidth(45);
        
        table.getColumns().addAll(nameCol, busyCol, opCol, vjCol, vkCol, qjCol, qkCol, aCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        
        // Color coding
        table.setRowFactory(tv -> new TableRow<ReservationStationRow>() {
            @Override
            protected void updateItem(ReservationStationRow item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                } else if (item.isBusy()) {
                    setStyle("-fx-background-color: #ffcccc;");
                }
            }
        });
        
        return table;
    }
    
    private void updateRSTableForType(TableView<ReservationStationRow> table, List<ReservationStation> stations) {
        table.getItems().clear();
        
        for (ReservationStation rs : stations) {
            ReservationStationRow row = new ReservationStationRow();
            row.setName(rs.getName());
            row.setBusy(rs.isBusy() ? "1" : "0");
            row.setOperation(rs.getOp() != null ? rs.getOp() : "");
            
            // V/Q CONTRACT: Vj and Qj are MUTUALLY EXCLUSIVE
            // Vj column: Show VALUE only (empty if waiting on Qj)
            // Qj column: Show TAG only (empty if value ready in Vj)
            if (rs.getVj() != null) {
                // Value is ready - show it in Vj
                row.setVj(formatRSValue(rs.getVj()));
            } else {
                // No value yet (waiting on Qj) - Vj column is EMPTY
                row.setVj("");
            }
            
            if (rs.getVk() != null) {
                // Value is ready - show it in Vk
                row.setVk(formatRSValue(rs.getVk()));
            } else {
                // No value yet (waiting on Qk) - Vk column is EMPTY
                row.setVk("");
            }
            
            row.setQj(rs.getQj() != null ? rs.getQj() : "");
            row.setQk(rs.getQk() != null ? rs.getQk() : "");
            row.setA(rs.getA() != null ? String.valueOf(rs.getA()) : "");
            
            table.getItems().add(row);
        }
    }
    
    private String formatRSValue(Double value) {
        if (value == null) return "";
        // Check if it's a register reference (integer value that could be a register)
        // For now, just format as number
        return String.format("%.2f", value);
    }
    
    // Note: formatValue method was intended for future use but is currently unused
    // Keeping commented out for potential future enhancement
    /*
    private String formatValue(Double value, String tag) {
        if (tag != null && !tag.isEmpty()) {
            return tag;
        } else if (value != null) {
            return String.format("%.2f", value);
        } else {
            return "";
        }
    }
    */
    
    public VBox getPanel() {
        return panel;
    }
    
    // Row data classes
    public static class InstructionStatusRow {
        private javafx.beans.property.SimpleStringProperty instruction = new javafx.beans.property.SimpleStringProperty();
        private javafx.beans.property.SimpleStringProperty j = new javafx.beans.property.SimpleStringProperty();
        private javafx.beans.property.SimpleStringProperty k = new javafx.beans.property.SimpleStringProperty();
        private javafx.beans.property.SimpleStringProperty issue = new javafx.beans.property.SimpleStringProperty();
        private javafx.beans.property.SimpleStringProperty execComplete = new javafx.beans.property.SimpleStringProperty();
        private javafx.beans.property.SimpleStringProperty writeResult = new javafx.beans.property.SimpleStringProperty();
        
        public String getInstruction() { return instruction.get(); }
        public void setInstruction(String value) { instruction.set(value); }
        public javafx.beans.property.StringProperty instructionProperty() { return instruction; }
        
        public String getJ() { return j.get(); }
        public void setJ(String value) { j.set(value); }
        public javafx.beans.property.StringProperty jProperty() { return j; }
        
        public String getK() { return k.get(); }
        public void setK(String value) { k.set(value); }
        public javafx.beans.property.StringProperty kProperty() { return k; }
        
        public String getIssue() { return issue.get(); }
        public void setIssue(String value) { issue.set(value); }
        public javafx.beans.property.StringProperty issueProperty() { return issue; }
        
        public String getExecComplete() { return execComplete.get(); }
        public void setExecComplete(String value) { execComplete.set(value); }
        public javafx.beans.property.StringProperty execCompleteProperty() { return execComplete; }
        
        public String getWriteResult() { return writeResult.get(); }
        public void setWriteResult(String value) { writeResult.set(value); }
        public javafx.beans.property.StringProperty writeResultProperty() { return writeResult; }
    }
    
    public static class RegisterRow {
        private javafx.beans.property.SimpleStringProperty register = new javafx.beans.property.SimpleStringProperty();
        private javafx.beans.property.SimpleStringProperty value = new javafx.beans.property.SimpleStringProperty();
        private javafx.beans.property.SimpleStringProperty qi = new javafx.beans.property.SimpleStringProperty();
        private boolean isBusy;
        
        public String getRegister() { return register.get(); }
        public void setRegister(String value) { register.set(value); }
        public javafx.beans.property.StringProperty registerProperty() { return register; }
        
        public String getValue() { return value.get(); }
        public void setValue(String val) { value.set(val); }
        public javafx.beans.property.StringProperty valueProperty() { return value; }
        
        public String getQi() { return qi.get(); }
        public void setQi(String val) { qi.set(val); }
        public javafx.beans.property.StringProperty qiProperty() { return qi; }
        
        public void setBusy(boolean val) { isBusy = val; }
        public boolean isBusy() { return isBusy; }
    }
    
    public static class LSBRow {
        private javafx.beans.property.SimpleStringProperty name = new javafx.beans.property.SimpleStringProperty();
        private javafx.beans.property.SimpleStringProperty busy = new javafx.beans.property.SimpleStringProperty();
        private javafx.beans.property.SimpleStringProperty address = new javafx.beans.property.SimpleStringProperty();
        
        public String getName() { return name.get(); }
        public void setName(String value) { name.set(value); }
        public javafx.beans.property.StringProperty nameProperty() { return name; }
        
        public String getBusy() { return busy.get(); }
        public void setBusy(String value) { busy.set(value); }
        public javafx.beans.property.StringProperty busyProperty() { return busy; }
        
        public String getAddress() { return address.get(); }
        public void setAddress(String value) { address.set(value); }
        public javafx.beans.property.StringProperty addressProperty() { return address; }
    }
    
    public static class ReservationStationRow {
        private javafx.beans.property.SimpleStringProperty name = new javafx.beans.property.SimpleStringProperty();
        private javafx.beans.property.SimpleStringProperty busy = new javafx.beans.property.SimpleStringProperty();
        private javafx.beans.property.SimpleStringProperty operation = new javafx.beans.property.SimpleStringProperty();
        private javafx.beans.property.SimpleStringProperty vj = new javafx.beans.property.SimpleStringProperty();
        private javafx.beans.property.SimpleStringProperty vk = new javafx.beans.property.SimpleStringProperty();
        private javafx.beans.property.SimpleStringProperty qj = new javafx.beans.property.SimpleStringProperty();
        private javafx.beans.property.SimpleStringProperty qk = new javafx.beans.property.SimpleStringProperty();
        private javafx.beans.property.SimpleStringProperty a = new javafx.beans.property.SimpleStringProperty();
        private boolean isBusy;
        
        public String getName() { return name.get(); }
        public void setName(String value) { name.set(value); }
        public javafx.beans.property.StringProperty nameProperty() { return name; }
        
        public String getBusy() { return busy.get(); }
        public void setBusy(String value) { 
            busy.set(value);
            isBusy = "1".equals(value);
        }
        public javafx.beans.property.StringProperty busyProperty() { return busy; }
        public boolean isBusy() { return isBusy; }
        
        public String getOperation() { return operation.get(); }
        public void setOperation(String value) { operation.set(value); }
        public javafx.beans.property.StringProperty operationProperty() { return operation; }
        
        public String getVj() { return vj.get(); }
        public void setVj(String value) { vj.set(value); }
        public javafx.beans.property.StringProperty vjProperty() { return vj; }
        
        public String getVk() { return vk.get(); }
        public void setVk(String value) { vk.set(value); }
        public javafx.beans.property.StringProperty vkProperty() { return vk; }
        
        public String getQj() { return qj.get(); }
        public void setQj(String value) { qj.set(value); }
        public javafx.beans.property.StringProperty qjProperty() { return qj; }
        
        public String getQk() { return qk.get(); }
        public void setQk(String value) { qk.set(value); }
        public javafx.beans.property.StringProperty qkProperty() { return qk; }
        
        public String getA() { return a.get(); }
        public void setA(String value) { a.set(value); }
        public javafx.beans.property.StringProperty aProperty() { return a; }
    }
}
