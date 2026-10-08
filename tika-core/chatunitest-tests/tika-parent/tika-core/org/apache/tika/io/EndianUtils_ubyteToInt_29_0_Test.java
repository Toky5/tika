package org.apache.tika.io;

import org.apache.tika.io.EndianUtils;
import org.mockito.*;
import org.junit.jupiter.api.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.io.InputStream;
import org.apache.tika.exception.TikaException;

public class EndianUtils_ubyteToInt_29_0_Test {

    @Test
    public void testUbyteToInt() {
        // Test with positive byte
        assertEquals(0x12, EndianUtils.ubyteToInt((byte) 0x12));
        // Test with negative byte
        assertEquals(0xF2, EndianUtils.ubyteToInt((byte) -34));
        // Test with zero byte
        assertEquals(0x00, EndianUtils.ubyteToInt((byte) 0x00));
    }
}
