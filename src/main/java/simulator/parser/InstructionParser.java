// src/main/java/simulator/parser/InstructionParser.java
import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

public class InstructionParser {
    private final BranchHandler branchHandler;

    public InstructionParser() {
        this.branchHandler = new BranchHandler();
    }

    /** Load and parse from a .asm file */
    public List<ParsedInstruction> parseFile(String filepath) throws IOException {
        List<String> lines = readLines(filepath);
        return parseInstructions(lines);
    }

    /** Main parsing method – takes raw lines of assembly */
    public List<ParsedInstruction> parseInstructions(List<String> rawLines) {
        List<String> cleaned = cleanLines(rawLines);
        List<ParsedInstruction> instructions = new ArrayList<>();

        int lineNumber = 0;
        for (String line : cleaned) {
            ParsedInstruction instr = parseSingleLine(line, lineNumber++);
            if (instr != null) {
                instructions.add(instr);
            }
        }

        // First pass: extract all labels
        branchHandler.extractLabels(instructions);

        // Second pass: resolve branch targets (LOOP → instruction index)
        branchHandler.resolveBranchTargets(instructions);

        // Remove pure label lines (keep only real instructions + labeled instructions)
        return filterOutPureLabelLines(instructions);
    }

    private List<String> readLines(String filepath) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filepath))) {
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }

    private List<String> cleanLines(List<String> lines) {
        List<String> result = new ArrayList<>();
        Pattern commentPattern = Pattern.compile("//.*$|;.*$");

        for (String line : lines) {
            String cleaned = commentPattern.matcher(line).replaceAll("").trim();
            if (!cleaned.isEmpty()) {
                result.add(cleaned);
            }
        }
        return result;
    }

    private ParsedInstruction parseSingleLine(String line, int lineNumber) {
        if (line == null || line.isBlank()) return null;

        // Case 1: Pure label definition → "LOOP:"
        if (line.endsWith(":") && line.indexOf(' ') == -1) {
            String label = line.substring(0, line.length() - 1).trim();
            ParsedInstruction dummy = new ParsedInstruction(null, new String[0], line, lineNumber);
            dummy.setLabel(label);
            return dummy;
        }

        // Split into mnemonic + rest
        String[] parts = line.split("\\s+", 2);
        String mnemonic = parts[0];
        String rest = parts.length > 1 ? parts[1] : "";

        String label = null;

        // Case 2: Label + instruction on same line → "LOOP: L.D F0, 8(R1)"
        if (mnemonic.endsWith(":")) {
            label = mnemonic.substring(0, mnemonic.length() - 1).trim();
            String[] restParts = rest.split("\\s+", 2);
            mnemonic = restParts[0];
            rest = restParts.length > 1 ? restParts[1] : "";
        }

        // Parse the actual instruction
        InstructionType type;
        try {
            type = InstructionType.fromAssemblyName(mnemonic);
        } catch (IllegalArgumentException e) {
            // Might be a label followed by nothing valid → treat as label only
            if (label != null) {
                ParsedInstruction dummy = new ParsedInstruction(null, new String[0], line, lineNumber);
                dummy.setLabel(label);
                return dummy;
            }
            throw new IllegalArgumentException(
                "Unknown instruction at line " + (lineNumber + 1) + ": " + line);
        }

        String[] operands = splitOperands(rest);
        ParsedInstruction instr = new ParsedInstruction(type, operands, line.trim(), lineNumber);

        if (label != null) {
            instr.setLabel(label);
        }

        return instr;
    }

    /** Smart operand splitter – respects parentheses in memory operands */
    private String[] splitOperands(String operandStr) {
        if (operandStr == null || operandStr.trim().isEmpty()) {
            return new String[0];
        }

        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean insideParen = false;

        for (char c : operandStr.toCharArray()) {
            if (c == '(') insideParen = true;
            if (c == ')') insideParen = false;

            if (c == ',' && !insideParen) {
                result.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) {
            result.add(current.toString().trim());
        }

        return result.toArray(new String[0]);
    }

    /** Remove lines that contain ONLY a label (no instruction) */
    private List<ParsedInstruction> filterOutPureLabelLines(List<ParsedInstruction> list) {
        List<ParsedInstruction> filtered = new ArrayList<>();
        for (ParsedInstruction instr : list) {
            if (instr.getType() != null || instr.isLabelDefinition()) {
                filtered.add(instr);
            }
        }
        return filtered;
    }

    public BranchHandler getBranchHandler() {
        return branchHandler;
    }

    // ===================================================================
    // TESTING – RUN THIS TO VERIFY EVERYTHING WORKS
    // ===================================================================
    public static void main(String[] args) {
        InstructionParser parser = new InstructionParser();

        List<String> testCase3 = Arrays.asList(
            "DADDI R1, R1, 24",
            "DADDI R2, R2, 0",
            "LOOP: L.D F0, 8(R1)",
            "MUL.D F4, F0, F2",
            "S.D F4, 8(R1)",
            "DSUBI R1, R1, 8",
            "BNE R1, R2, LOOP"
        );

        try {
            List<ParsedInstruction> program = parser.parseInstructions(testCase3);

            System.out.println("SUCCESS: Parsed " + program.size() + " instructions\n");
            for (ParsedInstruction i : program) {
                System.out.println(i.toDebugString());
            }

            System.out.println("\nLabel Resolution:");
            parser.getBranchHandler().printLabels();

        } catch (Exception e) {
            System.err.println("Parsing failed:");
            e.printStackTrace();
        }
    }
}