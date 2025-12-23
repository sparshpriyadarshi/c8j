package dev.sp.c8j;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Random;
import java.util.Scanner;
import java.util.Stack;
import java.util.logging.Logger;
/**
 * 
 * A CHIP-8 emulator
 */
public class C8JEmulator {

    private static int MAX_ADDRESSIBLE_BYTES = 4096; // 0x1000
    private static int FONT_MEM_BASE_IDX = 80;       // 0x050
    private static int FONT_MEM_LAST_IDX = 159;      // 0x09F
    private static int PROGRAM_MEM_BASE_IDX = 512;   // 0x200
    private static int PROGRAM_MEM_LAST_IDX = 3743;  // 0xE9F
    private static int INSTRUCTION_WIDTH = 2;        // 2 bytes
    private static int DISP_W = 64;
    private static int DISP_H = 32;

    private byte[] program;                          // Just a program, not in memory...
    private byte[] memory;                           // 
    private byte[] vRegisters;                       // V registers 8 bit
    private short iRegister;                         // the I register 16 bit
    private Stack<Integer> stack;                    // 16-bit wide atleast 12 deep...
    private int programCounter;                      // the Program Counter
    private byte delayTimer;                         // another U8bit value, counts down at 60hz
    private byte soundTimer;                         // if >= 0x02 emit a tone

    private short instruction;                       // 2 byte instruction
    private byte[] decodedInstructions;              // intermediate use only

    private long[] displayLines;                     // monochrome display 64 x 32 pixels


    private byte keyPress;                           // valid keys are 0123456789abcdef
    //wonder if this keypress tracker should be gaurded....
    
    
    
    // This welcome screen program is always loaded by default
    //public static String DEFAULT_PROGRAM_SRC_FILEPATH = "src/main/resources/binaries/c8splash.ch8"; // TODO this path can be
    //public static String DEFAULT_PROGRAM_SRC_FILEPATH = "src/main/resources/binaries/ibmlogo.ch8"; // TODO this path can be
    //public static String DEFAULT_PROGRAM_SRC_FILEPATH = "src/main/resources/binaries/persontest.ch8"; // TODO this path can be
    public static String DEFAULT_PROGRAM_SRC_FILEPATH = "src/main/resources/binaries/kbtest.ch8"; // TODO this path can be
                                                                                                        // done better
    public static HexFormat HEX_LINEAR_FORMATTER = HexFormat.ofDelimiter(":").withUpperCase().withPrefix("0x");
    private static Logger logger = Logger.getLogger("dev.sp.c8j");
    public C8JEmulator() throws IOException {
        // initialize empty registers V0 to VF, I, Stack
        vRegisters = new byte[16];
        iRegister = 0;
        stack = new Stack<>();
        programCounter = 0;
        delayTimer = (byte)0xFF;//TODO set off timers
        soundTimer = (byte)0xFF;//TODO set of timers
        memory = new byte[MAX_ADDRESSIBLE_BYTES];

        loadFonts();
        // get hold of a "default" program to run
        readCh8Binary(Paths.get(DEFAULT_PROGRAM_SRC_FILEPATH));
        programCounter = loadProgram();

        instruction = 0;
        decodedInstructions = new byte[4];

        displayLines = new long[DISP_H]; //long's width = 64
        keyPress = 'x'; //keypad should be classed 
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
        programCounter = loadProgram();

    }

    private short fetch() {

        assert programCounter >= PROGRAM_MEM_BASE_IDX && programCounter <= PROGRAM_MEM_LAST_IDX : "Program Counter out of bounds: " + programCounter;

        // TODO: investigate this and idiomatic refactors, java has signed bytes
        instruction = 0x0000;
        instruction = (short) (instruction | memory[programCounter] & 0xFF);
        instruction = (short) (instruction << Byte.SIZE);
        instruction = (short) (instruction | memory[programCounter + 1] & 0xFF);

        //System.out.println("Fetched instruction: " + instruction + " = " + HEX_LINEAR_FORMATTER.toHexDigits(instruction));
        System.out.printf("Fetched instruction@%x =%x\n",programCounter, instruction);

        programCounter += INSTRUCTION_WIDTH;

        return instruction;
    }

    private byte[] decode(short instruction) { // fetch vs decode is irrelevant in this trivial use case of chip8...
        String instructionHexString = HEX_LINEAR_FORMATTER.toHexDigits(instruction);
        System.out.println("Decoding instruction: " + instructionHexString);
        
        //no reason
        decodedInstructions[0] = (byte) 0xDE; 
        decodedInstructions[1] = (byte) 0xAD; 
        decodedInstructions[2] = (byte) 0xBE; 
        decodedInstructions[3] = (byte) 0xAD; 

        decodedInstructions[0] = (byte)((instruction >>> 12) & 0xF); //shift to get the nibble, maskoff higher nibble, cast
        decodedInstructions[1] = (byte)((instruction >>>  8) & 0xF); 
        decodedInstructions[2] = (byte)((instruction >>>  4) & 0xF); 
        decodedInstructions[3] = (byte)((instruction >>>  0) & 0xF); 

        return decodedInstructions;
    }

    private void execute(byte[] decodedInstructions) throws Exception {
        assert decodedInstructions.length == 4 : "instruction doesn't have 4 nibbles" + decodedInstructions;

        System.out.printf("decoded instr = %x%x%x%x\n", decodedInstructions[0], decodedInstructions[1],
                decodedInstructions[2], decodedInstructions[3]);
        int x, y;
        byte n;
        switch (decodedInstructions[0]) {// TODO: refactor this big switch and dcoder better....
            case 0x0:
                if (decodedInstructions[1] == 0x0 && decodedInstructions[2] == 0xE && decodedInstructions[3] == 0x0) { // 00E0
                    System.out.println("00E0 clearing screen");
                    displayLines = new long[DISP_H];
                } else if (decodedInstructions[1] == 0x0 && decodedInstructions[2] == 0xE
                        && decodedInstructions[3] == 0xE) {// 00EE
                    programCounter = stack.pop();
                    System.out.printf("00EE ret from subrt to %x\n", programCounter);
                } else { // 0NNN calls machine language subroutine at NNN,not implemented here...
                    throw new Exception("0NNN exec mach lang subrt at NNN, this is unimplemented");
                }
                break;
            case 0x1:
                System.out.println("1NNN instr Jump to NNN");
                System.out.printf("from addr=%x\n", programCounter);
                programCounter = instruction & 0xFFF;
                // or
                // pc = instruction & ((1 << 12) - 1);
                System.out.printf("to addr=%x\n", programCounter);
                break;
            case 0x2:
                System.out.println("2NNN call subrt at NNN");
                System.out.printf("from addr=%x (ret addr)\n", programCounter);
                stack.add(programCounter); // used later for returning...
                programCounter = instruction & 0xFFF;
                System.out.printf("to addr=%x\n", programCounter);
                break;
            case 0x3:
                System.out.printf("3XNN: skip if VX is NN");
                x = (int) decodedInstructions[1];
                n = (byte) (instruction & 0xFF);
                System.out.printf("vx=%x ? n=%x \n", vRegisters[x], n);
                if (vRegisters[x] == n) {
                    System.out.printf("skipped\n");
                    programCounter += INSTRUCTION_WIDTH; // pc already advanced at fetch, advance again here.
                }
                break;
            case 0x4:
                System.out.printf("4XNN: skip if VX not NN");
                x = (int) decodedInstructions[1];
                n = (byte) (instruction & 0xFF);
                System.out.printf("vx=%x ? n=%x \n", vRegisters[x], n);
                if (vRegisters[x] != n) {
                    System.out.printf("skipped\n");
                    programCounter += INSTRUCTION_WIDTH;
                }
                break;
            case 0x5:
                System.out.printf("5XY0: skip if VX is VY");
                x = (int) decodedInstructions[1];
                y = (int) decodedInstructions[2];
                System.out.printf("vx=%x ? vy=%x \n", vRegisters[x], vRegisters[y]);
                if (vRegisters[x] == vRegisters[y]) {
                    System.out.printf("skipped\n");
                    programCounter += INSTRUCTION_WIDTH;
                }
                break;
            case 0x6:
                System.out.printf("6XNN: set VX = NN\n");
                x = (int) decodedInstructions[1];
                n = (byte) (instruction & 0xFF);
                vRegisters[x] = n;
                System.out.printf("v[%d] = %x now\n", x, n);
                break;
            case 0x7:
                System.out.printf("7XNN: VX += NN\n");
                x = (int) decodedInstructions[1];
                n = (byte) (instruction & 0xFF);
                vRegisters[x] += n;
                System.out.printf("v[%d] = %x now\n", x, n);
                break;
            case 0x8:
                if (decodedInstructions[3] == 0x0) {
                    System.out.println("8XY0: VX = value of VY");
                    x = (int) decodedInstructions[1];
                    y = (int) decodedInstructions[2];
                    vRegisters[x] = vRegisters[y];
                    System.out.printf("v[%d] = v[%d] now = %x %x\n", x, y, vRegisters[x], vRegisters[y]);

                } else if (decodedInstructions[3] == 0x1) {
                    System.out.println("8XY1: VX = VX | VY");
                    x = (int) decodedInstructions[1];
                    y = (int) decodedInstructions[2];
                    vRegisters[x] |= vRegisters[y];
                    System.out.printf("v[%x] = v[%x] or v[%x] = %x\n", x, x, y, vRegisters[x]);

                } else if (decodedInstructions[3] == 0x2) {
                    System.out.println("8XY1: VX = VX & VY");
                    x = (int) decodedInstructions[1];
                    y = (int) decodedInstructions[2];
                    vRegisters[x] &= vRegisters[y];
                    System.out.printf("v[%x] = v[%x] and v[%x] = %x\n", x, x, y, vRegisters[x]);

                } else if (decodedInstructions[3] == 0x3) {
                    System.out.println("8XY1: VX = VX xor VY");
                    x = (int) decodedInstructions[1];
                    y = (int) decodedInstructions[2];
                    vRegisters[x] ^= vRegisters[y];
                    System.out.printf("v[%x] = v[%x] xor v[%x] = %x\n", x, x, y, vRegisters[x]);
                } else if (decodedInstructions[3] == 0x4) {
                    System.out.println("8XY4: VX = VX + VY, carry");
                    x = (int) decodedInstructions[1];
                    y = (int) decodedInstructions[2];
                    int vx = vRegisters[x];
                    int vy = vRegisters[y];
                    int sum = vx + vy;
                    if (sum > 0xFF) { // 255
                        vRegisters[0xf] = 1;
                    } else {
                        vRegisters[0xf] = 0;
                    }
                    vRegisters[x] += vRegisters[y];
                    vRegisters[0xf] = 0;
                    System.out.printf("v[%x] = v[%x] + v[%x] = %x, carry = %x\n", x, x, y, vRegisters[x],
                            vRegisters[0xf]);

                } else if (decodedInstructions[3] == 0x5) {
                    System.out.println("8XY5: VX=VX-VY");
                    x = (int) decodedInstructions[1];
                    y = (int) decodedInstructions[2];
                    if (vRegisters[x] > vRegisters[y]) {
                        vRegisters[0xf] = 1;
                    } else {
                        vRegisters[0xf] = 0;
                    }
                    vRegisters[x] = (byte) (vRegisters[x] - vRegisters[y]);
                    System.out.printf("v[%x] = v[%x] - v[%x] = %x, carry = %x\n", x, x, y, vRegisters[x],
                            vRegisters[0xf]);

                    System.out.printf("\n");
                } else if (decodedInstructions[3] == 0x6) { // Ambiguous instruction, doing original!
                    System.out.println("8XY6: VX shiftRight");
                    x = (int) decodedInstructions[1];
                    y = (int) decodedInstructions[2];
                    vRegisters[x] = vRegisters[y]; // this doesn't happen in newer impls
                    if ((vRegisters[x] & 0x01) == 0x01) {
                        vRegisters[0xf] = 1;
                    } else {
                        vRegisters[0xf] = 0;
                    }
                    vRegisters[x] = (byte) (vRegisters[x] >> 1);
                    System.out.printf("VX now = %x, VF = %x\n", vRegisters[x], vRegisters[0xf]);
                } else if (decodedInstructions[3] == 0x7) {
                    System.out.println("8XY5: VX=VY-VX");
                    x = (int) decodedInstructions[1];
                    y = (int) decodedInstructions[2];
                    if (vRegisters[y] > vRegisters[x]) {
                        vRegisters[0xf] = 1;
                    } else {
                        vRegisters[0xf] = 0;
                    }
                    vRegisters[x] = (byte) (vRegisters[y] - vRegisters[x]);
                    System.out.printf("v[%x] = v[%x] - v[%x] = %x, carry = %x\n", x, y, x, vRegisters[x],
                            vRegisters[0xf]);

                } else if (decodedInstructions[3] == 0xE) {/// Ambiguous instruction, doing original!
                    System.out.println("8XYE: VX shiftLeft");
                    x = (int) decodedInstructions[1];
                    y = (int) decodedInstructions[2];
                    vRegisters[x] = vRegisters[y]; // this doesn't happen in newer impls
                    if ((vRegisters[x] & 0x80) == 0x80) {
                        vRegisters[0xf] = 1;
                    } else {
                        vRegisters[0xf] = 0;
                    }
                    vRegisters[x] = (byte) (vRegisters[x] << 1);
                    System.out.printf("VX now = %x, VF = %x\n", vRegisters[x], vRegisters[0xf]);
                } else {
                    throw new Exception(String.format("%x decoded intr unimplemented %n", instruction)); // TODO make
                                                                                                         // custom
                                                                                                         // exception

                }

                break;
            case 0x9:
                System.out.printf("9XY0: skip if VX not VY");
                x = (int) decodedInstructions[1];
                y = (int) decodedInstructions[2];
                System.out.printf("vx=%x ? vy=%x \n", vRegisters[x], vRegisters[y]);
                if (vRegisters[x] != vRegisters[y]) {
                    System.out.printf("skipped\n");
                    programCounter += INSTRUCTION_WIDTH;
                }
                break;
            case 0xA:
                System.out.printf("ANNN: Set I to NNN");
                iRegister = (short) (instruction & 0x0FFF);
                System.out.printf("I = %x now\n", iRegister);
                break;
            case 0xB:
                // Ambiguous instruction!
                System.out.printf("BNNN: jump w/ offset: V0 + NNN\n");
                byte v0 = vRegisters[0x0];
                short addr = (short) (instruction & 0x0FFF);
                programCounter = v0 + addr;
                System.out.println("pc is now =" + programCounter);
                break;
            case 0xC:
                System.out.printf("CXNN: random-num AND NN into VX\n");
                Random randomizer = new Random();
                int randInt = randomizer.nextInt();
                byte randValue = (byte) ((randInt & 0xFF) & (instruction & 0xFF));
                vRegisters[(int) decodedInstructions[1]] = randValue;
                System.out.printf("rand value in VX = %x\n", randValue);
                break;
            case 0xD:

                System.out.printf("DXYN: draw sprite VXVY, N bytes at I\n");
                // Draw a sprite at position VX, VY with N bytes of sprite data starting at the
                // address stored in I
                // Set VF to 01 if any set pixels are changed to unset, and 00 otherwise
                x = (int) decodedInstructions[1];
                y = (int) decodedInstructions[2];
                n = decodedInstructions[3];

                // sprite's start pos can wrap around...
                int posX = (vRegisters[x] & (DISP_W - 1));
                int posY = (vRegisters[y] % DISP_H); // vy & 31 is another way...
                short address = iRegister;

                vRegisters[0xf] = 0;

                int spriteLineIdx = 0;
                int displayLineIdx = posY + spriteLineIdx;
                while (spriteLineIdx < n && displayLineIdx < DISP_H) {
                    long spriteLine = (long) memory[address + spriteLineIdx];

                    spriteLine = (spriteLine & 0xFF) << 56; // &FF is to nullify sign extension that might have happened
                                                            // in the above case
                    spriteLine = spriteLine >>> posX; // again, prevent sign extension

                    long originalDisplayLine = displayLines[displayLineIdx];
                    displayLines[displayLineIdx] ^= spriteLine;

                    if (vRegisters[0xf] == 0 && (originalDisplayLine & spriteLine) != 0) {
                        vRegisters[0xf] = 1;
                    }
                    spriteLineIdx++;
                    displayLineIdx++;
                }

                System.out.println("display rendered");
                break;

            case 0xE:
                x = (int) (decodedInstructions[1]);
                if (decodedInstructions[2] == 0x9 && decodedInstructions[3] == 0xE) {
                    System.out.println("skip if key pressed EX9E");
                    byte keyByte = (byte) (vRegisters[x] & 0x0F);
                    if (keyByte == keyPress) {
                        programCounter += INSTRUCTION_WIDTH;
                        System.out.println("skipping next instr");
                    }

                } else if (decodedInstructions[2] == 0xA && decodedInstructions[3] == 0x1) {
                    System.out.println("skip if key pressed NOT in vx EXA1");
                    byte keyByte = (byte) (vRegisters[x] & 0x0F);
                    if (keyByte != keyPress) {
                        programCounter += INSTRUCTION_WIDTH;
                        System.out.println("skipping next instr");
                    }
                } else {
                    throw new Exception(String.format("%x decoded intr unimplemented %n", instruction)); // TODO make
                                                                                                         // custom
                                                                                                         // exception
                }
                keyPress = 'x'; // reset keypress !!!
                break;
            case 0xF:
                System.out.println("FXNN instr decoded");
                x = (int) (decodedInstructions[1]);
                if (decodedInstructions[2] == 0x0 && decodedInstructions[3] == 0x7) {
                    vRegisters[x] = (byte) (delayTimer & 0xFF);
                } else if (decodedInstructions[2] == 0x1 && decodedInstructions[3] == 0x5) {
                    delayTimer = (byte) (vRegisters[x] & 0xFF);
                } else if (decodedInstructions[2] == 0x1 && decodedInstructions[3] == 0x8) {
                    soundTimer = (byte) (vRegisters[x] & 0xFF);
                }else if (decodedInstructions[2] == 0x1 && decodedInstructions[3] == 0xE) {
                    iRegister += (short) (vRegisters[x] & 0xFF); //undocumented detail, vf not affected here ideally...
                }else if (decodedInstructions[2] == 0x0 && decodedInstructions[3] == 0xA) {//get key
                    //timers keep running, wait for key input.... 
                    // either BLOCK here, or decr pc and keep going.... 
                    //TODO finish me
                    // current decision: will go back on PC for now
                    programCounter -= INSTRUCTION_WIDTH;
                }else if(decodedInstructions[2] == 0x2 && decodedInstructions[3] == 0x9){
                    byte vx = vRegisters[x];
                    iRegister = (short) ((int) FONT_MEM_BASE_IDX + (5 * (int) vx));
                }else if(decodedInstructions[2] == 0x3 && decodedInstructions[3] == 0x3){
                    //BCD VX at I
                    int vx = (int)vRegisters[x];
                    memory[iRegister + 0] = (byte)(vx / 100);
                    memory[iRegister + 1] = (byte)((vx % 100) / 10);
                    memory[iRegister + 2] = (byte)(vx % 10);                    
                    
                }else if(decodedInstructions[2] == 0x5 && decodedInstructions[3] == 0x5){//ambi
                    System.out.println("store to memory[Iregister] from v0 to vx incl");
                    x = decodedInstructions[1] & 0xFF;
                    for(int idx = 0; idx <= x; idx++){
                        memory[iRegister + idx] = vRegisters[idx];
                    }

                }else if(decodedInstructions[2] == 0x6 && decodedInstructions[3] == 0x5){//ambi
                    System.out.println("load from memory from v0 to vx incl");
                    x = decodedInstructions[1] & 0xFF;
                    for(int idx = 0; idx <= x; idx++){
                       vRegisters[idx] = memory[iRegister + idx];
                    }
                    //modern impl, I register remains unchanged here.... legacy would require I to update with idx
                }  else {
                    throw new Exception(String.format("%x decoded intr unimplemented %n", instruction)); // TODO make
                                                                                                         // custom
                                                                                                         // exception
                }
                break;
            default:
                throw new Exception(String.format("%x decoded intr unimplemented %n", instruction));// TODO choose
                                                                                                    // approp. ex
        }
    }

    private void step() throws Exception {
       
        execute(decode(fetch()));
        
    }
    
   

    private void run() throws Exception {
        int counter = 0;
        while (counter++ < 4096) {
        //while (true) {
            step();
        }
    }
    
    public static void runConsole(C8JEmulator emu) throws Exception {
        
        throw new Exception("runConsole() unimplemented");
    }

    
    public String dumpString() {//todo:redo this... stick to 1 convention for hex and decimal printing... see egs online
        StringBuilder sb = new StringBuilder(); 

        sb.append(String.format("Emulator Object: %n"));
        sb.append(String.format("R- I Register: %d%n", iRegister));

        sb.append(String.format("R- V Registers: %n"));
        for (int i = 0; i < 16; i++) {
            sb.append(String.format("R- V%02d|V%X = %03d or #%s%n", i, i, vRegisters[i],
                    HEX_LINEAR_FORMATTER.toHexDigits(vRegisters[i])));
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
        sb.append(String.format("I = %d%n", iRegister));
        for (int i = 0; i < 16; i++) {
            sb.append(String.format("V%02d|V%X = %03d or #%s%n", i, i, vRegisters[i],
                    HEX_LINEAR_FORMATTER.toHexDigits(vRegisters[i])));
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

    public void consumeKeypress(String keyString){
        keyString = keyString.toLowerCase();
        keyString = keyString.substring(0,1);
        keyPress = (byte)Byte.parseByte(keyString);
        System.out.printf("keypress set = %x\n", keyPress);
    }

    public static void main(String[] args) {
        System.out.println("Running C8MJ... | Current time = " + LocalDateTime.now());
        
        //TODO these
        //logger.log(Level.ALL, "Running C8MJ... | Current time = " + LocalDateTime.now());
        //logger.fine("doing stuff");
        //var ex = "generic ex";
        //logger.log(Level.WARNING, "trouble sneezing", ex);

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
        char subChar = 'x';
        while (replLoop) {
            System.out.print("c8j>");
            String consoleInput = scanner.next();
            inChar = consoleInput.charAt(0);
            if(consoleInput.length() > 1){
                subChar = consoleInput.charAt(1);
            }
            //System.out.printf("read ch = %c \n", inChar);
            //System.out.printf("read ch2 = %c \n", subChar);

            switch (inChar) {
                case 'h':
                    System.out.println("q=quit,s=step,r=run,g=registers,m=memory,p=prog-counter,d=display,k[x]=key_in-x,f=run-till-next-draw-call");//TODO rethink these..
                    break;
                case 'k':
                    emu.consumeKeypress(String.valueOf(subChar));
                    break;
                case 'q': // quit
                    replLoop = false;
                    break;
                case 's': // step
                    try {
                        emu.step();
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    //System.out.println(emu);
                    break;
                case 'f': //reach next draw sprite instruction...
                    try {
                        byte currentInstruction = emu.decodedInstructions[0];
                        while(currentInstruction != 0xD){
                            emu.step();
                            currentInstruction = emu.decodedInstructions[0];
                        }
                        emu.step();//this is invoking the draw then...

                    } catch (Exception e) {
                        // TODO: handle exception
                        e.printStackTrace();
                    }
                    break;
                case 'r': // run (at the moment "resumes".... if you stepped a bit that is preserved state..)
                    try {
                        C8JEmulator.runConsole(emu);
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    break;
                case 'g': // show regs
                    System.out.println(emu.dumpRegs());
                    break;
                case 'm': // show mem
                    break;
                case 'p': // show pc
                    System.out.println(emu.programCounter);
                    break;
                case 'd': // show display
                    for(int ri = 0; ri < DISP_H; ri++){
                        System.out.println(String.format("%64s", Long.toBinaryString(emu.displayLines[ri])).replace(' ', '0'));
                    }
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
