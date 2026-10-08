package org.apache.tika.io;

import org.apache.tika.io.EndianUtils;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.io.InputStream;
import org.apache.tika.exception.TikaException;
import org.mockito.*;
import org.junit.jupiter.api.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class EndianUtils_getIntBE_22_0_Test {

    @Test
    public void testGetIntBE() {
        byte[] data = new byte[] { 0x00, 0x00, 0x00, 0x01 };
        int result = EndianUtils.getIntBE(data);
        assertEquals(1, result);
        data = new byte[] { 0x00, 0x00, 0x01, 0x00 };
        result = EndianUtils.getIntBE(data);
        assertEquals(256, result);
        data = new byte[] { (byte) 0x01, 0x00, 0x00, 0x00 };
        result = EndianUtils.getIntBE(data);
        assertEquals(16777216, result);
        data = new byte[] { 0x00, (byte) 0x01, 0x00, 0x00 };
        result = EndianUtils.getIntBE(data);
        assertEquals(65536, result);
        data = new byte[] { 0x00, 0x00, 0x00, 0x00 };
        result = EndianUtils.getIntBE(data);
        assertEquals(0, result);
        data = new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };
        result = EndianUtils.getIntBE(data);
        assertEquals(-1, result);
    }
}
