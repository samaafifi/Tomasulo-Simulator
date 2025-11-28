package com.tomasulo.hazards;

import com.tomasulo.models.Instruction;
import com.tomasulo.registerfile.IRegisterFile;

/**
 * Detects data hazards (RAW, WAR, WAW) in Tomasulo's algorithm.
 * 
 * Note: While this class provides explicit detection methods for logging/reporting,
 * the actual hazard handling is performed by RegisterFile and DataHazardHandler:
 * - RAW hazards are handled via Qi field tracking (waiting for write-back)
 * - WAR/WAW hazards are eliminated via register renaming (Register Alias Table)
 * 
 * This class is useful for:
 * - GUI display of detected hazards
 * - Logging and debugging
 * - Educational purposes (showing which hazards would occur without Tomasulo)
 */
public class HazardDetection {
    private final IRegisterFile registerFile;

    public HazardDetection(IRegisterFile registerFile) {
        this.registerFile = registerFile;
    }

    /**
     * Detects a RAW (Read After Write) hazard.
     * RAW occurs when an instruction reads from a register that a previous
     * instruction is still writing to (register has pending write, Qi != null).
     * 
     * Resolution in Tomasulo: The reading instruction waits for the Qi tag
     * to be broadcast on the CDB. Handled automatically via DataHazardHandler.
     * 
     * @param registerName the register to check
     * @return true if RAW hazard exists (register is busy), false otherwise
     */
    public boolean detectRAW(String registerName) {
        return !registerFile.isRegisterReady(registerName);
    }

    /**
     * Detects a WAR (Write After Read) hazard.
     * WAR occurs when an instruction writes to a register that a previous
     * instruction is still reading from.
     * 
     * Resolution in Tomasulo: Register renaming eliminates WAR hazards.
     * The write is tracked via Qi field, and the old value remains available
     * for the reading instruction. Handled automatically via Register Alias Table.
     * 
     * @param registerName the register being written to
     * @return true if WAR hazard would exist (register was previously read),
     *         false otherwise. Note: In Tomasulo, this is always false after
     *         renaming, but this method can detect the potential hazard.
     */
    public boolean detectWAR(String registerName) {
        // In Tomasulo, WAR is eliminated by renaming, so this would only
        // be true if we're checking before renaming occurs.
        // After renaming, the register can be written without affecting
        // previous reads. This method checks if register has a value
        // (indicating it might have been read), but renaming handles it.
        return registerFile.isRegisterReady(registerName);
    }

    /**
     * Detects a WAW (Write After Write) hazard.
     * WAW occurs when two instructions write to the same register.
     * 
     * Resolution in Tomasulo: Register renaming eliminates WAW hazards.
     * The latest write overwrites the Qi field, and only the latest result
     * is written back. Handled automatically via Register Alias Table.
     * 
     * @param registerName the register being written to
     * @return true if WAW hazard exists (register already has pending write),
     *         false otherwise
     */
    public boolean detectWAW(String registerName) {
        return !registerFile.isRegisterReady(registerName);
    }

    /**
     * Detects all hazards for a given instruction.
     * Checks source registers for RAW hazards and destination register for WAW hazards.
     * 
     * @param instruction the instruction to check
     * @return a HazardReport containing detected hazards
     */
    public HazardReport detectHazards(Instruction instruction) {
        boolean rawSrc1 = false;
        boolean rawSrc2 = false;
        boolean waw = false;

        // Check source registers for RAW hazards
        if (instruction.getSourceReg1() != null) {
            rawSrc1 = detectRAW(instruction.getSourceReg1());
        }
        if (instruction.getSourceReg2() != null) {
            rawSrc2 = detectRAW(instruction.getSourceReg2());
        }

        // Check destination register for WAW hazard
        if (instruction.getDestRegister() != null) {
            waw = detectWAW(instruction.getDestRegister());
        }

        return new HazardReport(rawSrc1, rawSrc2, waw);
    }

    /**
     * Report of detected hazards for an instruction.
     */
    public static class HazardReport {
        private final boolean rawSrc1;
        private final boolean rawSrc2;
        private final boolean waw;

        public HazardReport(boolean rawSrc1, boolean rawSrc2, boolean waw) {
            this.rawSrc1 = rawSrc1;
            this.rawSrc2 = rawSrc2;
            this.waw = waw;
        }

        public boolean hasRAWSrc1() {
            return rawSrc1;
        }

        public boolean hasRAWSrc2() {
            return rawSrc2;
        }

        public boolean hasAnyRAW() {
            return rawSrc1 || rawSrc2;
        }

        public boolean hasWAW() {
            return waw;
        }

        public boolean hasAnyHazard() {
            return hasAnyRAW() || hasWAW();
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("HazardReport{");
            if (rawSrc1) sb.append("RAW(src1), ");
            if (rawSrc2) sb.append("RAW(src2), ");
            if (waw) sb.append("WAW, ");
            if (!hasAnyHazard()) sb.append("none");
            else sb.setLength(sb.length() - 2); // Remove trailing ", "
            sb.append("}");
            return sb.toString();
        }
    }
}
