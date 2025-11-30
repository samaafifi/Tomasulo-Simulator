package simulator.tomasulo.issue;

import simulator.parser.*;
import simulator.tomasulo.models.Instruction;
import simulator.tomasulo.registerfile.*;
import simulator.tomasulo.issue.*;

 import java.util.*;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
 public class InstructionQueue {
     private Queue<Instruction> queue;
     private List<Instruction> allInstructions; // For tracking and display
     private int instructionIdCounter;
     
     /**
      * Constructor
      */
     public InstructionQueue() {
         this.queue = new LinkedList<>();
         this.allInstructions = new ArrayList<>();
         this.instructionIdCounter = 0;
     }
     
     /**
      * Adds an instruction to the queue
      */
     public void enqueue(Instruction instruction) {
         instruction.setId(instructionIdCounter++);
         queue.offer(instruction);
         allInstructions.add(instruction);
     }
     
     /**
      * Adds multiple instructions to the queue
      */
     public void enqueueAll(List<Instruction> instructions) {
         for (Instruction instruction : instructions) {
             enqueue(instruction);
         }
     }
     
     /**
      * Removes and returns the next instruction from the queue
      */
     public Instruction dequeue() {
         return queue.poll();
     }
     
     /**
      * Returns the next instruction without removing it
      */
     public Instruction peek() {
         return queue.peek();
     }
     
     /**
      * Checks if the queue is empty
      */
     public boolean isEmpty() {
         return queue.isEmpty();
     }
     
     /**
      * Gets the number of instructions in the queue
      */
     public int size() {
         return queue.size();
     }
     
     /**
      * Gets all instructions (including those already issued)
      */
     public List<Instruction> getAllInstructions() {
         return new ArrayList<>(allInstructions);
     }
     
     /**
      * Gets instructions still waiting in the queue
      */
     public List<Instruction> getWaitingInstructions() {
         return new ArrayList<>(queue);
     }
     
     /**
      * Clears the queue
      */
     public void clear() {
         queue.clear();
         allInstructions.clear();
         instructionIdCounter = 0;
     }
     
     /**
      * Loads instructions from an array of strings (assembly code)
      */
     public void loadFromAssembly(String[] assemblyCode) throws InvalidInstructionException {
         clear();
         Map<String, Integer> labels = new HashMap<>();
         
         // First pass: identify labels
         int lineNumber = 0;
         for (String line : assemblyCode) {
             line = line.trim();
             if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) {
                 continue; // Skip empty lines and comments
             }
             
             // Check for label
             if (line.contains(":")) {
                 String[] parts = line.split(":");
                 String label = parts[0].trim();
                 labels.put(label, lineNumber);
                 
                 // Check if there's an instruction on the same line
                 if (parts.length > 1 && !parts[1].trim().isEmpty()) {
                     lineNumber++;
                 }
             } else {
                 lineNumber++;
             }
         }
         
         // Second pass: parse instructions
         for (String line : assemblyCode) {
             line = line.trim();
             
             // Skip empty lines and comments
             if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) {
                 continue;
             }
             
             // Remove label if present
             if (line.contains(":")) {
                 String[] parts = line.split(":", 2);
                 if (parts.length > 1) {
                     line = parts[1].trim();
                 } else {
                     continue; // Label only, no instruction
                 }
             }
             
             // Parse and enqueue the instruction
             Instruction instruction = parseInstruction(line, labels);
             enqueue(instruction);
         }
     }
     
     /**
      * Parses a single assembly instruction
      */
     private Instruction parseInstruction(String line, Map<String, Integer> labels) 
             throws InvalidInstructionException {
         // Remove comments
         int commentIndex = line.indexOf('#');
         if (commentIndex >= 0) {
             line = line.substring(0, commentIndex);
         }
         commentIndex = line.indexOf("//");
         if (commentIndex >= 0) {
             line = line.substring(0, commentIndex);
         }
         
         line = line.trim();
         
         // Split instruction into parts
         String[] parts = line.split("\\s+", 2);
         if (parts.length == 0) {
             throw new InvalidInstructionException("Empty instruction");
         }
         
         String operation = parts[0].toUpperCase();
         String operands = (parts.length > 1) ? parts[1] : "";
         
         // Parse operands
         String[] operandArray = operands.split(",");
         for (int i = 0; i < operandArray.length; i++) {
             operandArray[i] = operandArray[i].trim();
         }
         
         // Create instruction from parsed operands
         String dest = operandArray.length > 0 ? operandArray[0] : null;
         String src1 = operandArray.length > 1 ? operandArray[1] : null;
         String src2 = operandArray.length > 2 ? operandArray[2] : null;
         
         return new Instruction(operation, dest, src1, src2, "");
     }
     
     /**
      * Prints the current state of the queue
      */
     public void printQueue() {
         System.out.println("\n=== Instruction Queue ===");
         System.out.println("Waiting instructions: " + queue.size());
         
         int index = 0;
         for (Instruction instr : queue) {
             System.out.println("  " + index++ + ": " + instr.toString());
         }
         System.out.println();
     }
     
     /**
      * Gets the instruction at a specific position in the original list
      */
     public Instruction getInstructionById(int id) {
         for (Instruction instr : allInstructions) {
             if (instr.getId() == id) {
                 return instr;
             }
         }
         return null;
     }
     
     /**
      * Returns a string representation of the queue
      */
     @Override
     public String toString() {
         StringBuilder sb = new StringBuilder();
         sb.append("Instruction Queue (").append(queue.size()).append(" waiting):\n");
         
         int index = 0;
         for (Instruction instr : queue) {
             sb.append("  ").append(index++).append(": ").append(instr.toString()).append("\n");
         }
         
         return sb.toString();
     }
     
     /**
      * Gets statistics about the queue
      */
     public Map<String, Integer> getStatistics() {
         Map<String, Integer> stats = new HashMap<>();
         
         stats.put("Total Instructions", allInstructions.size());
         stats.put("Waiting in Queue", queue.size());
         stats.put("Issued Instructions", allInstructions.size() - queue.size());
         
         return stats;
     }
 }
 
 /**
  * Exception thrown when an invalid instruction is encountered
  */
 class InvalidInstructionException extends Exception {
     public InvalidInstructionException(String message) {
         super(message);
     }
 }
