package simulator.tomasulo.issue;

import simulator.tomasulo.models.Instruction;  

/**
 * ReservationStation.java
 * Represents a single reservation station in Tomasulo's algorithm
 * Structure: {Name, Busy, Op, Vj, Vk, Qj, Qk, A}
 */

 public class ReservationStation {
    // Station identification
    private String name;           // Name of the reservation station (e.g., "Add1", "Mult1")
    private String type;           // Type: FP_ADD, FP_MUL, FP_DIV, LOAD, STORE, INTEGER_ADD, BRANCH
    
    // Status fields
    private boolean busy;          // Is the station occupied?
    
    // Instruction information
    private String op;             // Operation to perform
    private Instruction instruction; // Reference to the actual instruction
    
    // Source operand values
    private Double vj;             // Value of source operand 1
    private Double vk;             // Value of source operand 2
    
    // Source operand tags (for register renaming)
    private String qj;             // Reservation station producing source operand 1
    private String qk;             // Reservation station producing source operand 2
    
    // Address/Immediate field
    private Integer a;             // Immediate value or memory offset
    
    // Execution tracking
    private int remainingCycles;   // Cycles remaining for execution
    private Double result;         // Computed result
    private boolean resultReady;   // Is result ready to broadcast?
    private int issueCycle = -1;   // Cycle when instruction was issued (-1 means not issued yet)
    private boolean executionStarted = false; // Whether execution has started
    
    /**
     * Constructor
     */
    public ReservationStation(String name, String type) {
        this.name = name;
        this.type = type;
        this.busy = false;
        this.op = null;
        this.vj = null;
        this.vk = null;
        this.qj = null;
        this.qk = null;
        this.a = null;
        this.remainingCycles = 0;
        this.result = null;
        this.resultReady = false;
        this.instruction = null;
    }
    
    // Getters
    public String getName() { return name; }
    public String getType() { return type; }
    public boolean isBusy() { return busy; }
    public String getOp() { return op; }
    public Double getVj() { return vj; }
    public Double getVk() { return vk; }
    public String getQj() { return qj; }
    public String getQk() { return qk; }
    public Integer getA() { return a; }
    public int getRemainingCycles() { return remainingCycles; }
    public Double getResult() { return result; }
    public boolean isResultReady() { return resultReady; }
    public Instruction getInstruction() { return instruction; }
    public int getIssueCycle() { return issueCycle; }
    public boolean isExecutionStarted() { return executionStarted; }
    
    // Setters
    public void setBusy(boolean busy) { this.busy = busy; }
    public void setOp(String op) { this.op = op; }
    public void setVj(Double vj) { this.vj = vj; }
    public void setVk(Double vk) { this.vk = vk; }
    public void setQj(String qj) { this.qj = qj; }
    public void setQk(String qk) { this.qk = qk; }
    public void setA(Integer a) { this.a = a; }
    public void setRemainingCycles(int cycles) { this.remainingCycles = cycles; }
    public void setResult(Double result) { this.result = result; }
    public void setResultReady(boolean ready) { this.resultReady = ready; }
    public void setInstruction(Instruction instruction) { this.instruction = instruction; }
    public void setIssueCycle(int cycle) { this.issueCycle = cycle; }
    public void setExecutionStarted(boolean started) { this.executionStarted = started; }
    
    /**
     * TOMASULO READY CHECK
     * An instruction can execute when BOTH Q tags are null (all operands ready)
     * 
     * Rule: Ready = (Qj == null) AND (Qk == null)
     * 
     * Special cases:
     * - LOADS: only need Qj == null (base address ready)
     * - STORES: need both Qj == null (address) AND Qk == null (data)
     * - COMPUTE: need both Qj == null AND Qk == null
     */
    public boolean isReadyToExecute() {
        if (!busy) {
            return false;
        }
        
        // For memory operations:
        // - LOADS: only need base address ready (Qj null)
        // - STORES: need both base address (Qj null) AND source data (Qk null)
        if (isMemoryOperation()) {
            if (type.equals("STORE")) {
                // Stores need both base address (Qj) and source data (Qk) ready
                return (qj == null || qj.equals("")) && (qk == null || qk.equals(""));
            } else {
                // Loads only need base address ready
                return qj == null || qj.equals("");
            }
        }
        
        // For compute operations (ADD, MUL, DIV, SUB, branches), both operands must be ready
        // This is the classic Tomasulo ready condition!
        return (qj == null || qj.equals("")) && (qk == null || qk.equals(""));
    }
    
    /**
     * Checks if this is a memory operation (load/store)
     */
    public boolean isMemoryOperation() {
        return type.equals("LOAD") || type.equals("STORE");
    }
    
    /**
     * TOMASULO OPERAND UPDATE
     * Called when a result is broadcast on the CDB
     * Updates V and clears Q when producer matches
     * 
     * Example: If Qj == "Mult1" and Mult1 broadcasts value 6.0:
     *   → Vj = 6.0, Qj = null (operand now ready!)
     */
    public void updateOperand(String producer, Double value) {
        if (producer.equals(qj)) {
            vj = value;   // Capture the value
            qj = null;    // Clear the dependency tag
        }
        if (producer.equals(qk)) {
            vk = value;   // Capture the value
            qk = null;    // Clear the dependency tag
        }
    }
    
    /**
     * Clears the reservation station (makes it available)
     */
    public void clear() {
        this.busy = false;
        this.op = null;
        this.vj = null;
        this.vk = null;
        this.qj = null;
        this.qk = null;
        this.a = null;
        this.remainingCycles = 0;
        this.result = null;
        this.resultReady = false;
        this.instruction = null;
        this.issueCycle = -1;
        this.executionStarted = false;
    }
    
    /**
     * Starts execution of the instruction
     */
    public void startExecution(int latency) {
        this.remainingCycles = latency;
        this.executionStarted = true;
    }
    
    /**
     * Advances execution by one cycle
     * Returns true if execution completed this cycle
     */
    public boolean executeOneCycle() {
        if (remainingCycles > 0) {
            remainingCycles--;
            if (remainingCycles == 0) {
                resultReady = true;
                return true;
            }
        }
        return false;
    }
    
    /**
     * Returns a string representation of the reservation station
     */
    @Override
    public String toString() {
        if (!busy) {
            return String.format("%-8s | Free", name);
        }
        
        String vjStr = (qj != null && !qj.equals("")) ? qj : (vj != null ? vj.toString() : "-");
        String vkStr = (qk != null && !qk.equals("")) ? qk : (vk != null ? vk.toString() : "-");
        String aStr = (a != null) ? a.toString() : "-";
        
        return String.format("%-8s | Busy | %-8s | Vj:%-8s | Vk:%-8s | Qj:%-8s | Qk:%-8s | A:%-8s | Cycles:%d",
                           name, op, vjStr, vkStr, 
                           (qj != null ? qj : "-"), 
                           (qk != null ? qk : "-"), 
                           aStr, remainingCycles);
    }
    
    /**
     * Returns a detailed state for GUI display
     */
    public String[] getStateArray() {
        return new String[] {
            name,
            busy ? "Yes" : "No",
            op != null ? op : "",
            formatValue(vj, qj),
            formatValue(vk, qk),
            qj != null ? qj : "",
            qk != null ? qk : "",
            a != null ? a.toString() : "",
            String.valueOf(remainingCycles)
        };
    }
    
    /**
     * Helper method to format value/tag for display
     */
    private String formatValue(Double value, String tag) {
        if (tag != null && !tag.equals("")) {
            return tag;
        } else if (value != null) {
            return String.format("%.2f", value);
        } else {
            return "";
        }
    }
}
