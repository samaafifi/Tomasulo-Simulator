package com.tomasulo.hazards;

import com.tomasulo.models.Instruction;
import com.tomasulo.registerfile.Register;
import com.tomasulo.registerfile.RegisterFile;

/**
 * Coordinates register operations during Issue and Write-Back stages.
 * This is the main class Member 1 and Member 2 will call.
 */
public class DataHazardHandler {
    private final RegisterFile registerFile;

    public DataHazardHandler(RegisterFile registerFile) {
        this.registerFile = registerFile;
    }

    /**
     * Called by Member 1 when issuing an instruction.
     * Returns ready values or Qi tags for source registers.
     * Also allocates the destination register (sets its Qi).
     */
    public SourceOperands handleIssue(Instruction instr) {
        String src1Value = null;
        String src1Qi = null;
        String src2Value = null;
        String src2Qi = null;

        if (instr.getSourceReg1() != null) {
            Register r = registerFile.getRegister(instr.getSourceReg1());
            if (r.isReady()) {
                src1Value = String.valueOf(r.getValue());
            } else {
                src1Qi = r.getQi();
            }
        }

        if (instr.getSourceReg2() != null) {
            Register r = registerFile.getRegister(instr.getSourceReg2());
            if (r.isReady()) {
                src2Value = String.valueOf(r.getValue());
            } else {
                src2Qi = r.getQi();
            }
        }

        // Allocate destination register (register renaming)
        if (instr.getDestRegister() != null) {
            registerFile.setQi(instr.getDestRegister(), instr.getStationTag());
        }

        return new SourceOperands(src1Value, src1Qi, src2Value, src2Qi);
    }

    /** Called by Member 2 when a result is broadcast on the CDB */
    public void handleWriteBack(String stationTag, double result) {
        registerFile.writeBackFromStation(stationTag, result);
    }

    /** Returned to the Issue stage so reservation stations know what to wait for */
    public static class SourceOperands {
        private final String src1Value;
        private final String src1Qi;
        private final String src2Value;
        private final String src2Qi;

        public SourceOperands(String src1Value, String src1Qi, String src2Value, String src2Qi) {
            this.src1Value = src1Value;
            this.src1Qi = src1Qi;
            this.src2Value = src2Value;
            this.src2Qi = src2Qi;
        }

        public boolean isSource1Ready() { return src1Qi == null; }
        public boolean isSource2Ready() { return src2Qi == null; }
        public boolean areBothReady() { return isSource1Ready() && isSource2Ready(); }

        public String getSrc1Value() { return src1Value; }
        public String getSrc1Qi() { return src1Qi; }
        public String getSrc2Value() { return src2Value; }
        public String getSrc2Qi() { return src2Qi; }
    }
}
