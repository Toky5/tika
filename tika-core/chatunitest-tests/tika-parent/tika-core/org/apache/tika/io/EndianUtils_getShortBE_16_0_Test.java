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

public class EndianUtils_getShortBE_16_0_Test {

    @Test
    public void testGetShortBE() {
        // Test with a valid byte array
        final byte[] data = new byte[] { 0, 1 };
        short result = EndianUtils.getShortBE(data);
        assertEquals(1, result);
        // Test with an invalid byte array
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> EndianUtils.getShortBE(data));
    }
}
