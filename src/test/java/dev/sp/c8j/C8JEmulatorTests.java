package dev.sp.c8j;
import org.junit.jupiter.api.Test;

import dev.sp.c8j.C8JEmulator.EMU_STATE;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Field;
import java.util.HexFormat;

public class C8JEmulatorTests {
    // private final C8JEmulator emulator;

    // public C8JEmulatorTests() throws IOException {
    //     this.emulator = new C8JEmulator();
    // }

    //private final static BytesFor = HexFormat.ofDelimiter(":").withUpperCase().withPrefix("0x");
    @Test
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
    void testEmulatorInitProgBytes(){
        byte[] prog = HexFormat.of().parseHex("0123456789ABCDEF");

        C8JEmulator emulator = assertDoesNotThrow(() -> new C8JEmulator(prog));
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
    void test1NNNJump() throws Exception{
        byte[] prog = HexFormat.of().parseHex("1DEA");

        C8JEmulator emulator = new C8JEmulator(prog);
        
        emulator.step();
        assertEquals(0xDEA, emulator.getProgramCounter());

    }

    @Test()
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

    @Test()
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

}
