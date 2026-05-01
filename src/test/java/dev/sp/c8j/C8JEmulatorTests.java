package dev.sp.c8j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import dev.sp.c8j.C8JEmulator.EMU_STATE;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Field;
import java.util.EmptyStackException;
import java.util.HexFormat;
import java.util.Set;

public class C8JEmulatorTests {
    // private final C8JEmulator emulator;

    // public C8JEmulatorTests() throws IOException {
    //     this.emulator = new C8JEmulator();
    // }

    //private final static BytesFor = HexFormat.ofDelimiter(":").withUpperCase().withPrefix("0x");
    @Test
    @Tag("Initialization")
    void testEmulatorInitDefault(){
        C8JEmulator emulator = assertDoesNotThrow(() -> new C8JEmulator());
        //sanity check for nulls on emulator "public" fields...
        for(Field field : emulator.getClass().getFields()){
            Object thisField = assertDoesNotThrow(()-> field.get(emulator));
            assertNotNull(thisField);
        }
        assertEquals(4096, emulator.getMemory8().length);
        //program memory base
        assertEquals(0x200, emulator.getProgramCounter());
        //font base and end indexes should be populated
        assertNotEquals(0x00, emulator.getMemory8()[0x50]);
        assertNotEquals(0x00, emulator.getMemory8()[0x9F]);

        assertEquals(EMU_STATE.INITIALIZED, emulator.getState());
        
       
    }

    @Test
    @Tag("Initialization")
    void testEmulatorInitProgBytes(){
        byte[] prog = HexFormat.of().parseHex("0123456789ABCDEF");

        C8JEmulator emulator = assertDoesNotThrow(() -> new C8JEmulator(prog));
        for(Field field : emulator.getClass().getFields()){
            Object thisField = assertDoesNotThrow(()-> field.get(emulator));
            assertNotNull(thisField);
        }
        assertEquals(4096, emulator.getMemory8().length);
        //program memory base
        assertEquals(512, emulator.getProgramCounter());
        //font base and end indexes should be populated
        assertNotEquals(0x00, emulator.getMemory8()[0x50]);
        assertNotEquals(0x00, emulator.getMemory8()[0x9F]);

        assertEquals(EMU_STATE.INITIALIZED, emulator.getState());

    }

    @Test
    @Tag("Instruction")
    void test0NNNExecMachineLanguageRoutine(){
        byte[] prog = HexFormat.of().parseHex("0DDD");
        C8JEmulator emu = assertDoesNotThrow(() -> new C8JEmulator(prog));
        assertThrows(RuntimeException.class, () -> emu.step());
    }

    @Test
    @Tag("Instruction")
    void test00E0ClearScreen() throws Exception{
        byte[] prog = HexFormat.of().parseHex("00E0");

        C8JEmulator emulator = new C8JEmulator(prog);
        
        emulator.step();
        assertEquals(emulator.getProgramCounter(), 0x202);

        var actual = emulator.getDisplayLines();
        var expected = new long[actual.length];
        assertArrayEquals(expected, actual);
    }

    @Test
    @Tag("Instruction")
    void test1NNNJump() throws Exception{
        byte[] prog = HexFormat.of().parseHex("1DEA");

        C8JEmulator emulator = new C8JEmulator(prog);
        
        emulator.step();
        assertEquals(0xDEA, emulator.getProgramCounter());

    }

    @Test
    @Tag("Instruction")
    void test6XNNSetVx() throws Exception{
        byte[] prog = HexFormat.of().parseHex("601160FF6F5561666A77");
        C8JEmulator emu = new C8JEmulator(prog);
        emu.step();
        assertEquals(0x11,emu.getVRegisters()[0x0]);
        emu.step();
        assertEquals(0xFF,emu.getVRegisters()[0x0]);
        emu.step();
        assertEquals(0x55,emu.getVRegisters()[0xF]);
        emu.step();
        assertEquals(0x66,emu.getVRegisters()[0x1]);
        emu.step();
        assertEquals(0x77,emu.getVRegisters()[0xA]);

    }

    @Test
    @Tag("Instruction")
    void test7XNNAddValToVx() throws Exception{
        byte[] prog = HexFormat.of().parseHex("70007101720279097F0F7202720A7F017FFF");
       
        C8JEmulator emu = new C8JEmulator(prog);
        emu.step();
        assertEquals(0x00,emu.getVRegisters()[0x00]);
        emu.step();
        assertEquals(0x01,emu.getVRegisters()[0x01]);
        emu.step();
        assertEquals(0x02,emu.getVRegisters()[0x02]);
        emu.step();
        assertEquals(0x09,emu.getVRegisters()[0x09]);
        emu.step();
        assertEquals(0x0F,emu.getVRegisters()[0x0F]);//v15 = 15
        emu.step();
        assertEquals(0x04,emu.getVRegisters()[0x02]);
        emu.step();
        assertEquals(0x0E,emu.getVRegisters()[0x02]);
        emu.step();
        assertEquals(0x10,emu.getVRegisters()[0x0F]);//v15 = 16
        emu.step();
        assertEquals(0x0F,emu.getVRegisters()[0x0F]);//271 or 15 (271 - 255 - 1) 
    }

    @Test
    @Tag("Instruction")
    void testANNNSetIReg() throws Exception{
        byte[] prog = HexFormat.of().parseHex("A000A001AFFFA123");
       
        C8JEmulator emu = new C8JEmulator(prog);
        emu.step();
        assertEquals(0, emu.getIRegister());
        emu.step();
        assertEquals(1, emu.getIRegister());
        emu.step();
        assertEquals(4095, emu.getIRegister());//0xFFF
        emu.step();
        assertEquals(291, emu.getIRegister());
        assertEquals(0x123, emu.getIRegister());

    }

    @Test
    @Tag("Instruction")
    @Tag("Subroutines")
    void test2NNNand00EECallSubroutineAtNNN() throws Exception{
       
       
        byte[] prog = HexFormat.of().parseHex("2206670400E0220C670300EE6701670200EE");
        ///
        /// 
        /// | ADDR         | VALUE  | comments                                  |
        /// | ------------ | -----  | ----------------------------------------- |
        /// | 512 or 0x200 | 0x2206 | CALL   518 or 0x206, STK is 514 or 0x202  |
        /// | 514 or 0x202 | 0x6704 | SET v[7] 4                                |
        /// | 516 or 0x204 | 0x00E0 | CLS --------------------end of line       |
        /// | 518 or 0x206 | 0x220C | CALL   524 or 0x20C, STK is 0x202, 0x208  |
        /// | 520 or 0x208 | 0x6703 | SET V[7] 3                                |
        /// | 522 or 0x20A | 0x00EE | RET, STK is _, PC = 0x202                 |
        /// | 524 or 0x20C | 0x6701 | SET V[7] 1                                |
        /// | 526 or 0x20E | 0x6702 | SET V[7] 2                                |
        /// | 528 or 0x210 | 0x00EE | RET, STK is 0x202, PC = 0x208             |
        /// 
        
        C8JEmulator emu = new C8JEmulator(prog);
        assertEquals(512, emu.getProgramCounter());

        //call subroutine...
        emu.step();
        assertEquals(1, emu.getStack16().size());
        assertEquals(514, emu.getStack16().peek());
        assertEquals(518, emu.getProgramCounter());
        emu.step();
        assertEquals(2, emu.getStack16().size());
        assertEquals(520, emu.getStack16().peek());
        assertEquals(524, emu.getProgramCounter());

        emu.step();
        assertEquals(1, emu.getVRegisters()[7]);
        emu.step();
        assertEquals(2, emu.getVRegisters()[7]);

        emu.step();
        assertEquals(1, emu.getStack16().size());
        assertEquals(514, emu.getStack16().peek());
        assertEquals(520, emu.getProgramCounter());

        emu.step();
        assertEquals(3, emu.getVRegisters()[7]);
        assertEquals(514, emu.getStack16().peek());
        assertEquals(522, emu.getProgramCounter());

        emu.step();
        assertEquals(0, emu.getStack16().size());
        assertEquals(514, emu.getProgramCounter());

        emu.step();
        assertEquals(4, emu.getVRegisters()[7]);
        
        emu.step();
        assertThrows(EmptyStackException.class, ()->emu.getStack16().peek());

        
    }
    
    /*
     * Draw (XOR) N pixles tall sprite from iReg location at Vx Vy, Vf = 1 if any ON
     * screen pixel was turned OFF.
     * Sprite width is implied as 8.
     */
    @Disabled
    @Test
    @Tag("Instruction")
    @Tag("Display")
    void testDXYNDisplay() throws Exception {
        // TODO:
    }


/*
3XNN, 4XNN, 5XY0 and 9XY0: Skip conditionally

Logical and arithmetic instructions
8XY0: Set
8XY1: Binary OR
8XY2: Binary AND
8XY3: Logical XOR
8XY4: Add
8XY5 and 8XY7: Subtract
8XY6 and 8XYE: Shift

BNNN: Jump with offset
CXNN: Random
EX9E and EXA1: Skip if key
FX07, FX15 and FX18: Timers
FX1E: Add to index
FX0A: Get key
FX29: Font character
FX33: Binary-coded decimal conversion
FX55 and FX65: Store and load memo    
*/
}
