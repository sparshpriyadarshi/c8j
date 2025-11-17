package dev.sp.c8j;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Scanner;
import java.util.Stack;

/**
 * 
 * A CHIP-8 emulator
 */
public class C8JEmulator {

    private static int MAX_ADDRESSIBLE_BYTES = 4096;// 0x1000
    private static int FONT_MEM_BASE_IDX = 80;// 0x050
    private static int FONT_MEM_LAST_IDX = 159;// 0x09F
    private static int PROGRAM_MEM_BASE_IDX = 512;// 0x200
    private static int PROGRAM_MEM_LAST_IDX = 3743;// 0xE9F
    private static int INSTRUCTION_WIDTH = 2; // 2 bytes
    // This welcome screen program is always loaded by default
    public static String DEFAULT_PROGRAM_SRC_FILEPATH = "src/main/resources/binaries/c8splash.ch8"; // TODO this path can be
                                                                                             // done better
    public static HexFormat HEX_LINEAR_FORMATTER = HexFormat.ofDelimiter(":").withUpperCase().withPrefix("0x");

    private byte[] program; // Just a program, not in memory...
    private byte[] memory;
    private byte[] vRegs; // V registers 8 bit
    private short iReg; // the I register 16 bit
    private Stack<Integer> stack; // 16-bit wide atleast 12 deep...
    private int pc; // the Program Counter
    private byte delayTimer; // another U8bit value, counts down at 60hz
    private byte soundTimer; // if >= 0x02 emit a tone

    private short instruction; // 2 byte instruction

    public C8JEmulator() throws IOException {
        // initialize empty registers V0 to VF, I, Stack
        vRegs = new byte[16];
        iReg = 0;
        stack = new Stack<>();
        pc = 0;
        delayTimer = 0;
        soundTimer = 0;
        memory = new byte[MAX_ADDRESSIBLE_BYTES];

        loadFonts();
        // get hold of a "default" program to run
        readCh8Binary(Paths.get(DEFAULT_PROGRAM_SRC_FILEPATH));
        pc = loadProgram();

        instruction = 0;
    }

    private void readCh8Binary(Path path) throws IOException {
        File binFile = new File(path.toUri());
        FileInputStream fileInputStream = new FileInputStream(binFile);
        program = fileInputStream.readAllBytes();
        System.out.println("Read " + program.length + " bytes from file: " + path);
        System.out.println("Program:");
        System.out.println(HEX_LINEAR_FORMATTER.formatHex(program));
        fileInputStream.close();
    }
    private void loadFonts(){
        //load 0 to F's font sprites in memory, 
        byte[] fonts = new byte[]{
            (byte)0xF0, (byte)0x90, (byte)0x90, (byte)0x90, (byte)0xF0, // 0
            (byte)0x20, (byte)0x60, (byte)0x20, (byte)0x20, (byte)0x70, // 1
            (byte)0xF0, (byte)0x10, (byte)0xF0, (byte)0x80, (byte)0xF0, // 2
            (byte)0xF0, (byte)0x10, (byte)0xF0, (byte)0x10, (byte)0xF0, // 3
            (byte)0x90, (byte)0x90, (byte)0xF0, (byte)0x10, (byte)0x10, // 4
            (byte)0xF0, (byte)0x80, (byte)0xF0, (byte)0x10, (byte)0xF0, // 5
            (byte)0xF0, (byte)0x80, (byte)0xF0, (byte)0x90, (byte)0xF0, // 6
            (byte)0xF0, (byte)0x10, (byte)0x20, (byte)0x40, (byte)0x40, // 7
            (byte)0xF0, (byte)0x90, (byte)0xF0, (byte)0x90, (byte)0xF0, // 8
            (byte)0xF0, (byte)0x90, (byte)0xF0, (byte)0x10, (byte)0xF0, // 9
            (byte)0xF0, (byte)0x90, (byte)0xF0, (byte)0x90, (byte)0x90, // A
            (byte)0xE0, (byte)0x90, (byte)0xE0, (byte)0x90, (byte)0xE0, // B
            (byte)0xF0, (byte)0x80, (byte)0x80, (byte)0x80, (byte)0xF0, // C
            (byte)0xE0, (byte)0x90, (byte)0x90, (byte)0x90, (byte)0xE0, // D
            (byte)0xF0, (byte)0x80, (byte)0xF0, (byte)0x80, (byte)0xF0, // E
            (byte)0xF0, (byte)0x80, (byte)0xF0, (byte)0x80, (byte)0x80  // F
        };
        int fontIdx = 0;
        for (int idx = FONT_MEM_BASE_IDX; idx <= FONT_MEM_LAST_IDX; idx++) {
            memory[idx] = fonts[fontIdx]; fontIdx++;
        }
        
    }
    private int loadProgram() {
        // initialize memory with program bytes
        for (int idx = PROGRAM_MEM_BASE_IDX; idx < PROGRAM_MEM_BASE_IDX + program.length; idx++) {
            memory[idx] = program[idx - PROGRAM_MEM_BASE_IDX];
        }

        System.out.println("Loaded program into memory, head: ");
        System.out.println(HEX_LINEAR_FORMATTER.formatHex(memory, PROGRAM_MEM_BASE_IDX, PROGRAM_MEM_BASE_IDX + 12));

        return PROGRAM_MEM_BASE_IDX;
    }

    public void loadCh8Binary(Path path) throws IOException {
        System.out.println("loading binary...");
        // sanity check path and all
        readCh8Binary(path);
        pc = loadProgram();

    }

    private String fetch() {

        assert pc >= PROGRAM_MEM_BASE_IDX && pc <= PROGRAM_MEM_LAST_IDX : "Program Counter out of bounds: " + pc;

        // TODO: investigate this and idiomatic refactors, java has signed bytes
        instruction = 0x0;
        instruction = (short) (instruction | memory[pc] & 0xFF);
        instruction = (short) (instruction << Byte.SIZE);
        instruction = (short) (instruction | memory[pc + 1] & 0xFF);

        System.out
                .println("Fetched instruction: " + instruction + " = " + HEX_LINEAR_FORMATTER.toHexDigits(instruction));

        pc += 2;

        return HEX_LINEAR_FORMATTER.toHexDigits(instruction);
    }

    private byte[] decode() { // fetch vs decode is irrelevant in this trivial use case of chip8...
        String instructionHexString = HEX_LINEAR_FORMATTER.toHexDigits(instruction);
        System.out.println("Decoding instruction: " + instructionHexString);

        byte[] decodedInstruction = new byte[4]; //technically we need nibbles, but this will do.
        //TODO fill this...
        return decodedInstruction;
    }

    private void execute(byte[] decodedInstruction) {
        assert decodedInstruction.length == 4 : "instruction decoded not 4 bytes";

        switch (decodedInstruction[0]) {
            case 0x1:
                System.out.println("1NNN instr Jump");
                break;
            case 0x8:
                System.out.println("8XYZ instr decoded");
                break;
            case 0xf:
                System.out.println("FXNN instr decoded");
                break;
            default:
                System.out.printf("%x decoded intr unimplemented %n", instruction);
                break;
        }
    }

    private void step() {
        fetch();
        execute(decode());
    }

    public void run() {
        int counter = 0;
        while (counter++ < 13) {
            step();
        }
    }

    public String dumpString() {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("Emulator Object: %n"));
        sb.append(String.format("R- I Register: %d%n", iReg));

        sb.append(String.format("R- V Registers: %n"));
        for (int i = 0; i < 16; i++) {
            sb.append(String.format("R- V%02d|V%X = %03d or #%s%n", i, i, vRegs[i],
                    HEX_LINEAR_FORMATTER.toHexDigits(vRegs[i])));
        }
        sb.append(String.format("-- .:..%n"));
        sb.append(String.format("- Maximum addressible bytes are: %d%n", MAX_ADDRESSIBLE_BYTES));
        sb.append(String.format("-- loaded program size is %d bytes: ", program.length));
        sb.append(HEX_LINEAR_FORMATTER.formatHex(program));

        sb.append(String.format("%n"));
        sb.append(String.format("-- Peeking Memory at location 0x%s (index: %d): %n",
                HEX_LINEAR_FORMATTER.toHexDigits(PROGRAM_MEM_BASE_IDX), PROGRAM_MEM_BASE_IDX));
        for (int idx = 0; idx < 8; idx++) {
            sb.append(String.format("M- %d|0x%s = #%s%n", idx,
                    HEX_LINEAR_FORMATTER.toHexDigits(PROGRAM_MEM_BASE_IDX + idx),
                    HEX_LINEAR_FORMATTER.toHexDigits(memory[PROGRAM_MEM_BASE_IDX + idx])));
        }

        sb.append(String.format("-- .:..%n"));
        return sb.toString();
    }
    public String dumpRegs(){
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("I = %d%n", iReg));
        for (int i = 0; i < 16; i++) {
            sb.append(String.format("V%02d|V%X = %03d or #%s%n", i, i, vRegs[i],
                    HEX_LINEAR_FORMATTER.toHexDigits(vRegs[i])));
        }
        return sb.toString();
    }

    public String toString() {
        return dumpString();
    }
    public static void testBytes() {
        for (int i = 0; i < 256; i++) {
            System.out.println("i = " + i + " | " + Integer.toHexString(i)
                    + " | " + Integer.toBinaryString(i));
        }
        System.out.println();
    }
    public static void testFonts(C8JEmulator emu){
        for(int i = 0; i < 80; i++){
            System.out.printf("Font byte = %x %n", emu.memory[FONT_MEM_BASE_IDX + i]);
        }
    }

    public static void main(String[] args) {
        System.out.println("Running C8MJ... | Current time = " + LocalDateTime.now());
        C8JEmulator emu;
        try {
            emu = new C8JEmulator();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Abnormally exiting C8MJ !");
            return;
        }
        System.out.println(emu);

        Scanner scanner = new Scanner(System.in);
        boolean replLoop = true;
        char inChar = 'z';
        while (replLoop) {
            System.out.print("c8j>");
            inChar = scanner.next().charAt(0);
            
            //System.out.printf("read ch = %c \n", inChar);

            switch (inChar) {
                case 'h':
                    System.out.println("q=quit,s=step,r=run,g=registers,m=memory,p=program-counter,d=display");
                    break;
                case 'q': // quit
                    replLoop = false;
                    break;
                case 's': // step
                    emu.step();
                    System.out.println(emu);
                    break;
                case 'r': // run
                    emu.run();
                    break;
                case 'g': // show regs
                    System.out.println(emu.dumpRegs());
                    break;
                case 'm': // show mem
                    break;
                case 'p': // show pc
                    System.out.println(emu.pc);
                    break;
                case 'd': // show display
                    System.out.println("FAKE DISPLAY");
                    break;
                default:
                    System.out.println("Unimplemented...");
                    break;
            }
        }
        scanner.close();

        System.out.println("Exiting C8MJ... | time = " + LocalDateTime.now());
    }
}


