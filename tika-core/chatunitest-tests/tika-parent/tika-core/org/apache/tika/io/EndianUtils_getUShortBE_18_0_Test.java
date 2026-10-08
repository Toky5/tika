package org.apache.tika.io;

import org.mockito.*;
import org.junit.jupiter.api.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.io.InputStream;
import org.apache.tika.exception.TikaException;

public class EndianUtils_getUShortBE_18_0_Test {

    @Test
    public void testGetUShortBE() {
        byte[] data = new byte[] { 0x12, 0x34 };
        int result = EndianUtils.getUShortBE(data);
        assertEquals(0x1234, result);
    }

    @Test
    public void testGetUShortBEWithOffset() {
        byte[] data = new byte[] { 0x00, 0x12, 0x34, 0x56 };
        int result = EndianUtils.getUShortBE(data, 1);
        assertEquals(0x1234, result);
    }

    @Test
    public void testGetUShortBEWithNullData() {
        byte[] data = null;
        assertThrows(NullPointerException.class, () -> EndianUtils.getUShortBE(data));
    }

    @Test
    public void testGetUShortBEWithEmptyData() {
        byte[] data = new byte[] {};
        assertThrows(IndexOutOfBoundsException.class, () -> EndianUtils.getUShortBE(data));
    }

    @Test
    public void testGetUShortBEWithShortData() {
        byte[] data = new byte[] { 0x12 };
        assertThrows(IndexOutOfBoundsException.class, () -> EndianUtils.getUShortBE(data));
    }
}
