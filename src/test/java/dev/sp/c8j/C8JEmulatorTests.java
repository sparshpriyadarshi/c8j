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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.EmptyStackException;
import java.util.HexFormat;
import java.util.Set;

public class C8JEmulatorTests {
    // private final C8JEmulator emulator;

    // public C8JEmulatorTests() throws IOException {
    // this.emulator = new C8JEmulator();
    // }

    // private final static BytesFor =
    // HexFormat.ofDelimiter(":").withUpperCase().withPrefix("0x");
    @Test
    @Tag("Initialization")
    void testEmulatorInitDefault() {
        C8JEmulator emulator = assertDoesNotThrow(() -> new C8JEmulator());
        // sanity check for nulls on emulator "public" fields...
        for (Field field : emulator.getClass().getFields()) {
            Object thisField = assertDoesNotThrow(() -> field.get(emulator));
            assertNotNull(thisField);
        }
        assertEquals(4096, emulator.getMemory8().length);
        // program memory base
        assertEquals(0x200, emulator.getProgramCounter());
        // font base and end indexes should be populated
        assertNotEquals(0x00, emulator.getMemory8()[0x50]);
        assertNotEquals(0x00, emulator.getMemory8()[0x9F]);

        assertEquals(EMU_STATE.INITIALIZED, emulator.getState());

    }

    @Test
    @Tag("Initialization")
    void testEmulatorInitProgBytes() {
        byte[] prog = HexFormat.of().parseHex("0123456789ABCDEF");

        C8JEmulator emulator = assertDoesNotThrow(() -> new C8JEmulator(prog));
        for (Field field : emulator.getClass().getFields()) {
            Object thisField = assertDoesNotThrow(() -> field.get(emulator));
            assertNotNull(thisField);
        }
        assertEquals(4096, emulator.getMemory8().length);
        // program memory base
        assertEquals(512, emulator.getProgramCounter());
        // font base and end indexes should be populated
        assertNotEquals(0x00, emulator.getMemory8()[0x50]);
        assertNotEquals(0x00, emulator.getMemory8()[0x9F]);

        assertEquals(EMU_STATE.INITIALIZED, emulator.getState());

    }

    @Test
    @Tag("Instruction")
    void test0NNN() {
        // Execute machine language routine at NNN
        byte[] prog = HexFormat.of().parseHex("0DDD");
        C8JEmulator emu = assertDoesNotThrow(() -> new C8JEmulator(prog));
        assertThrows(RuntimeException.class, () -> emu.step());
    }

    @Test
    @Tag("Instruction")
    void test00E0() throws Exception {
        // clear screen
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
    void test1NNN() throws Exception {
        // Jump to NNN
        byte[] prog = HexFormat.of().parseHex("1DEA");

        C8JEmulator emulator = new C8JEmulator(prog);

        emulator.step();
        assertEquals(0xDEA, emulator.getProgramCounter());

    }

    @Test
    @Tag("Instruction")
    void test6XNN() throws Exception {
        // VX = NN
        byte[] prog = HexFormat.of().parseHex("601160FF6F5561666A77");
        C8JEmulator emu = new C8JEmulator(prog);
        emu.step();
        assertEquals(0x11, emu.getVRegisters()[0x0]);
        emu.step();
        assertEquals(0xFF, emu.getVRegisters()[0x0]);
        emu.step();
        assertEquals(0x55, emu.getVRegisters()[0xF]);
        emu.step();
        assertEquals(0x66, emu.getVRegisters()[0x1]);
        emu.step();
        assertEquals(0x77, emu.getVRegisters()[0xA]);

    }

    @Test
    @Tag("Instruction")
    void test7XNN() throws Exception {
        // VX += NN
        byte[] prog = HexFormat.of().parseHex("70007101720279097F0F7202720A7F017FFF");

        C8JEmulator emu = new C8JEmulator(prog);
        emu.step();
        assertEquals(0x00, emu.getVRegisters()[0x00]);
        emu.step();
        assertEquals(0x01, emu.getVRegisters()[0x01]);
        emu.step();
        assertEquals(0x02, emu.getVRegisters()[0x02]);
        emu.step();
        assertEquals(0x09, emu.getVRegisters()[0x09]);
        emu.step();
        assertEquals(0x0F, emu.getVRegisters()[0x0F]);// v15 = 15
        emu.step();
        assertEquals(0x04, emu.getVRegisters()[0x02]);
        emu.step();
        assertEquals(0x0E, emu.getVRegisters()[0x02]);
        emu.step();
        assertEquals(0x10, emu.getVRegisters()[0x0F]);// v15 = 16
        emu.step();
        assertEquals(0x0F, emu.getVRegisters()[0x0F]);// 271 or 15 (271 - 255 - 1)
    }

    @Test
    @Tag("Instruction")
    void testANNN() throws Exception {
        // set I to NNN
        byte[] prog = HexFormat.of().parseHex("A000A001AFFFA123");

        C8JEmulator emu = new C8JEmulator(prog);
        emu.step();
        assertEquals(0, emu.getIRegister());
        emu.step();
        assertEquals(1, emu.getIRegister());
        emu.step();
        assertEquals(4095, emu.getIRegister());// 0xFFF
        emu.step();
        assertEquals(291, emu.getIRegister());
        assertEquals(0x123, emu.getIRegister());

    }

    @Test
    @Tag("Instruction")
    void test2NNNAnd00EE() throws Exception {
        // 2NNN call subroutine at NNN
        // 00EE return from subroutine

        byte[] prog = HexFormat.of().parseHex("2206670400E0220C670300EE6701670200EE");
        ///
        ///
        /// | ADDR | VALUE | comments |
        /// | ------------ | ----- | ----------------------------------------- |
        /// | 512 or 0x200 | 0x2206 | CALL 518 or 0x206, STK is 514 or 0x202 |
        /// | 514 or 0x202 | 0x6704 | SET v[7] 4 |
        /// | 516 or 0x204 | 0x00E0 | CLS --------------------end of line |
        /// | 518 or 0x206 | 0x220C | CALL 524 or 0x20C, STK is 0x202, 0x208 |
        /// | 520 or 0x208 | 0x6703 | SET V[7] 3 |
        /// | 522 or 0x20A | 0x00EE | RET, STK is _, PC = 0x202 |
        /// | 524 or 0x20C | 0x6701 | SET V[7] 1 |
        /// | 526 or 0x20E | 0x6702 | SET V[7] 2 |
        /// | 528 or 0x210 | 0x00EE | RET, STK is 0x202, PC = 0x208 |
        ///

        C8JEmulator emu = new C8JEmulator(prog);
        assertEquals(512, emu.getProgramCounter());

        // call subroutine...
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
        assertThrows(EmptyStackException.class, () -> emu.getStack16().peek());

    }

    @Disabled
    @Test
    @Tag("Instruction")
    void testDXYN() throws Exception {
        /*
         * Draw (XOR) N pixles tall sprite from iReg location at Vx Vy, Vf = 1 if any ON
         * screen pixel was turned OFF.
         * Sprite width is implied as 8.
         */
    }

    @Test
    @Tag("Instruction")
    void testCXNN() throws Exception {
        /* generate random number AND with NN and set Vx to it */
        byte[] prog = HexFormat.of().parseHex("C000C707CFFF");

        C8JEmulator emu = new C8JEmulator(prog);

        emu.step();
        assertEquals(0, emu.getVRegisters()[0]);
        emu.step();
        assertTrue(7 >= emu.getVRegisters()[7]);
        emu.step();
        assertTrue(0xFF >= emu.getVRegisters()[0xF]);
    }

    @Test
    @Tag("Instruction")
    void test3XNN() throws Exception {
        // skip if vx == nn
        byte[] prog = HexFormat.of().parseHex(
                        "61AA" + //0x200
                        "310A" + //0x202
                        "61FF" + //0x204
                        "31FF" + //0x206
                        "6101" + //0x208 shouldn't happen
                        "00E0"   //0x20A
                    );
        C8JEmulator emu = new C8JEmulator(prog);

        emu.step();
        emu.step();
        emu.step();
        assertEquals(0x206, emu.getProgramCounter());
        assertEquals(0xFF, emu.getVRegisters()[1]);

        emu.step();
        assertEquals(0x20A, emu.getProgramCounter());
        assertNotEquals(0x01, emu.getVRegisters()[1]);

        emu.step();
        assertEquals(0xFF, emu.getVRegisters()[1]);
        

        
    }

    @Test
    @Tag("Instruction")
    void test4XNN() throws Exception {
        // skip if vx != nn
        byte[] prog = HexFormat.of().parseHex(
                        "61AA" + //0x200
                        "410A" + //0x202
                        "61FF" + //0x204 unreachable
                        "41AA" + //0x206
                        "6101" + //0x208
                        "00E0"   //0x20A
                    );
        C8JEmulator emu = new C8JEmulator(prog);

        emu.step();
        assertEquals(0xAA, emu.getVRegisters()[1]);
        emu.step();
        assertEquals(0x206, emu.getProgramCounter());
        emu.step();
        assertEquals(0x208, emu.getProgramCounter());
        emu.step();
        assertEquals(0x1, emu.getVRegisters()[1]);
        assertEquals(0x20A, emu.getProgramCounter());
        emu.step();
        
    }

    @Test
    @Tag("Instruction")
    void test5XY0() throws Exception {
        // skip if vx == vy
         byte[] prog = HexFormat.of().parseHex(
                        "6AAA" + //0x200
                        "6BAA" + //0x202
                        "5AB0" + //0x204
                        "6AFF" + //0x206 unreachable
                        "6BFF" + //0x208
                        "5AB0" + //0x20A
                        "6FFF"   //0x20C
                    );
        C8JEmulator emu = new C8JEmulator(prog);
        emu.step();
        emu.step();
        assertEquals(0xAA, emu.getVRegisters()[0xA]);
        assertEquals(0xAA, emu.getVRegisters()[0xB]);
        emu.step();
        assertEquals(0x208, emu.getProgramCounter());

        emu.step();
        emu.step();
        emu.step();
        assertEquals(0xAA, emu.getVRegisters()[0xA]);
        assertEquals(0xFF, emu.getVRegisters()[0xB]);
        assertEquals(0xFF, emu.getVRegisters()[0xF]);
        assertEquals(0x20E, emu.getProgramCounter());
    }

    @Test
    @Tag("Instruction")
    void test9XY0() throws Exception {
        // skip if vx != vy
        byte[] prog = HexFormat.of().parseHex(
                        "6A0A" + //0x200
                        "6B0B" + //0x202
                        "9AB0" + //0x204
                        "6AFF" + //0x206 unreachable
                        "6A0B" + //0x208
                        "9AB0" + //0x20A
                        "6FFF"   //0x20C, canary
                    );
        C8JEmulator emu = new C8JEmulator(prog);
        emu.step();
        emu.step();
        assertEquals(0x0A, emu.getVRegisters()[0xA]);
        assertEquals(0x0B, emu.getVRegisters()[0xB]);
        emu.step();
        assertEquals(0x208, emu.getProgramCounter());

        emu.step();
        emu.step();
        emu.step();
        assertEquals(0xB, emu.getVRegisters()[0xA]);
        assertEquals(0xB, emu.getVRegisters()[0xB]);
        assertEquals(0xFF, emu.getVRegisters()[0xF]);
        assertEquals(0x20E, emu.getProgramCounter());
    }

    @Test
    @Tag("Instruction")
    void testBNNN() throws Exception {
        // Jump to V0 + NNN; aka jump with offset
        // warning: quirks, doing original

    }

    @Test
    @Tag("Instruction")
    void testFX1E() throws Exception {
        // I += VX, Add VX to index register
        // warning: quirks, original didn't affect VF

    }

    @Test
    @Tag("Instruction")
    void test8XY0() throws Exception {
        // 8XY0: Set VX = VY
    }

    @Test
    @Tag("Instruction")
    void test8XY1() throws Exception {
        // 8XY1: VX = VX OR VY
    }

    @Test
    @Tag("Instruction")
    void test8XY2() throws Exception {
        // 8XY2: VX = VX AND VY
    }

    @Test
    @Tag("Instruction")
    void test8XY3() throws Exception {
        // 8XY3: VX = VX XOR VY
    }

    @Test
    @Tag("Instruction")
    void test8XY4() throws Exception {
        // 8XY4: VX = VX + VY, VF = 1 if overflow (>255)
    }

    @Test
    @Tag("Instruction")
    void test8XY5() throws Exception {
        // 8XY5: VX = VX - VY, VF = 1 if VX >= VY; yea it looks counter intuitive...
    }

    @Test
    @Tag("Instruction")
    void test8XY7() throws Exception {
        // 8XY7: VX = VY - VX, VF = 1 if VY >= VX, counter intuitive...

    }

    @Test
    @Tag("Instruction")
    void test8XY6() throws Exception {
        // 8XY6: 
        // VX = VY; warning quirk: newer didn't do this assignment
        // VX = VX >> 1; VF = 1 if bit shifted out was 1. 
    }

    @Test
    @Tag("Instruction")
    void test8XYE() throws Exception {
        // 8XYE: 
        // VX = VY; warning quirk: newer didn't do this assignment
        // VX = VX << 1; VF = 1 if bit shifted out was 1. 
    }


    @Test
    @Tag("Instruction")
    void testFX29() throws Exception{
        // FX29: set I = address of VX's hex character (thats the last nibble only)
    }

    @Test
    @Tag("Instruction")
    void testFX33() throws Exception{
        // FX33: 
        // M[I]   = BCD(VX)[0],
        // M[I+1] = BCD(VX)[1],
        // M[I+2] = BCD(VX)[2],


    }
 
    @Test
    @Tag("Instruction")
    void testFX55() throws Exception{
        // FX55: Store V0 to VX(inclusive) to M[I] to M[I+X] respectively
        // warning, quirk: originally I ended up at I + X + 1 since I itself was incremented each time at load or store.
        // modern versions don't modify I ... 

    }

    @Test
    @Tag("Instruction")
    void testFX65() throws Exception{
        // FX65: Load V0 to VX(inclusive) from M[I] to M[I+X] respectively
        // warning, quirk: originally I ended up at I + X + 1 since I itself was incremented each time at load or store.
        // modern versions don't modify I ... 

    }



    @Test
    @Tag("Instruction")
    void testFX07() throws Exception{
        // FX07: VX = delay-timer value
    }
    
    @Test
    @Tag("Instruction")
    void testFX15() throws Exception{
        // FX15: set delay-timer = VX
    }
    
    @Test
    @Tag("Instruction")
    void testFX18() throws Exception{
        // FX18: set sound-timer = VX
    }


    @Test
    @Tag("Instruction")
    void testFX0A() throws Exception{
        // FX0A: Get key
        // wait(indefinitely) for key input;
        // set VX = keypress once received; 
        // continue then...
        
    }

    @Test
    @Tag("Instruction")
    void testEX9E() throws Exception{
        // EX9E: skip 1 instruction if VX == keypress
        // note this doesn't wait for key input....
    }

    @Test
    @Tag("Instruction")
    void testEXA1() throws Exception{
        // EXA1: skip if VX != keypress...
        // doesn't wait for key input......

        
    }


}
