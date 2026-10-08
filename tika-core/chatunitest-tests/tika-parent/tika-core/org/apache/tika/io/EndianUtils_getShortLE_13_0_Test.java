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

public class EndianUtils_getShortLE_13_0_Test {

    @Test
    public void testGetShortLE() {
        byte[] data = new byte[] { 0x12, 0x34 };
        int offset = 0;
        short expected = (short) 0x3412;
        short result = EndianUtils.getShortLE(data, offset);
        assertEquals(expected, result);
    }

    @Test
    public void testGetShortLEWithOffset() {
        byte[] data = new byte[] { 0x00, 0x12, 0x34, 0x56 };
        int offset = 1;
        short expected = (short) 0x3412;
        short result = EndianUtils.getShortLE(data, offset);
        assertEquals(expected, result);
    }

    @Test
    public void testGetShortLEWithNegativeOffset() {
        byte[] data = new byte[] { 0x12, 0x34 };
        int offset = -1;
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> EndianUtils.getShortLE(data, offset));
    }

    @Test
    public void testGetShortLEWithNullData() {
        byte[] data = null;
        int offset = 0;
        assertThrows(NullPointerException.class, () -> EndianUtils.getShortLE(data, offset));
    }
}
