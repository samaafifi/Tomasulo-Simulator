package com.tomasulo.registerfile;

import java.util.HashMap;
import java.util.Map;

/**
 * Main Register File implementation for Tomasulo's algorithm.
 * Manages all 64 registers (32 floating-point F0-F31, 32 integer R0-R31).
 * 
 * Key responsibilities:
 * - Store register values
 * - Track Qi fields (which station will produce each register's value)
 * - Coordinate with Register Alias Table for register renaming
 * - Handle write-back operations from Common Data Bus
 */
public class RegisterFile implements IRegisterFile {
    private final Map<String, Register> registers;
    private final RegisterAliasTable rat;
    
    public RegisterFile() {
        this.registers = new HashMap<>();
        this.rat = new RegisterAliasTable();
    }
    
    @Override
    public void initializeRegisters() {
        // Create 32 floating-point registers (F0-F31)
        for (int i = 0; i < 32; i++) {
            String name = "F" + i;
            registers.put(name, new Register(name, RegisterType.FLOATING_POINT));
        }
        
        // Create 32 integer registers (R0-R31)
        for (int i = 0; i < 32; i++) {
            String name = "R" + i;
            registers.put(name, new Register(name, RegisterType.INTEGER));
        }
    }
    
    @Override
    public void reset() {
        registers.clear();
        rat.clearAll();
        initializeRegisters();
    }
    
    @Override
    public void preloadValues(Map<String, Double> initialValues) {
        for (Map.Entry<String, Double> entry : initialValues.entrySet()) {
            String regName = entry.getKey();
            if (registers.containsKey(regName)) {
                registers.get(regName).setValue(entry.getValue());
            } else {
                throw new IllegalArgumentException("Register not found: " + regName);
            }
        }
    }
    
    @Override
    public boolean isRegisterReady(String registerName) {
        Register reg = registers.get(registerName);
        if (reg == null) {
            throw new IllegalArgumentException("Register not found: " + registerName);
        }
        return reg.isReady();
    }
    
    @Override
    public String getQi(String registerName) {
        Register reg = registers.get(registerName);
        if (reg == null) {
            throw new IllegalArgumentException("Register not found: " + registerName);
        }
        return reg.getQi();
    }
    
    @Override
    public double readValue(String registerName) {
        Register reg = registers.get(registerName);
        if (reg == null) {
            throw new IllegalArgumentException("Register not found: " + registerName);
        }
        if (reg.isBusy()) {
            throw new IllegalStateException("Cannot read busy register: " + registerName + 
                " (waiting for " + reg.getQi() + ")");
        }
        return reg.getValue();
    }
    
    @Override
    public Register getRegister(String registerName) {
        return registers.get(registerName);
    }
    
    @Override
    public void setQi(String registerName, String stationTag) {
        Register reg = registers.get(registerName);
        if (reg == null) {
            throw new IllegalArgumentException("Register not found: " + registerName);
        }
        reg.setQi(stationTag);
        rat.bind(registerName, stationTag);
    }
    
    @Override
    public void writeValue(String registerName, double value) {
        Register reg = registers.get(registerName);
        if (reg == null) {
            throw new IllegalArgumentException("Register not found: " + registerName);
        }
        reg.setValue(value);
        rat.clear(registerName);
    }
    
    @Override
    public void clearQi(String registerName) {
        Register reg = registers.get(registerName);
        if (reg == null) {
            throw new IllegalArgumentException("Register not found: " + registerName);
        }
        reg.clearQi();
        rat.clear(registerName);
    }
    
    @Override
    public void writeBackFromStation(String stationTag, double value) {
        // Find all registers waiting for this station and update them
        for (Register reg : registers.values()) {
            if (stationTag.equals(reg.getQi())) {
                reg.setValue(value);
            }
        }
        // Clear all RAT bindings for this station
        rat.clearByStation(stationTag);
    }
    
    @Override
    public Map<String, RegisterStatus> getAllRegisterStatus(int currentCycle) {
        Map<String, RegisterStatus> statusMap = new HashMap<>();
        for (Map.Entry<String, Register> entry : registers.entrySet()) {
            statusMap.put(entry.getKey(), 
                RegisterStatus.fromRegister(entry.getValue(), currentCycle));
        }
        return statusMap;
    }
    
    @Override
    public Map<String, Register> getAllRegisters() {
        return new HashMap<>(registers);
    }
    
    /**
     * Returns the Register Alias Table for advanced operations.
     * Generally not needed by other members - they should use the interface methods.
     */
    public RegisterAliasTable getRAT() {
        return rat;
    }
}
