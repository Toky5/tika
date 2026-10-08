package org.apache.tika.io;

import org.apache.tika.io.EndianUtils;
import java.lang.reflect.Method;
import org.mockito.*;
import org.junit.jupiter.api.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.io.InputStream;
import org.apache.tika.exception.TikaException;

public class EndianUtils_getUIntLE_25_0_Test {

    @Test
    public void testGetUIntLE() throws Exception {
        // Create a byte array with a little-endian unsigned integer
        byte[] data = new byte[] { 0x00, 0x00, 0x00, 0x01 };
        // Get the getUIntLE method using reflection
        Method getUIntLE = EndianUtils.class.getDeclaredMethod("getUIntLE", byte[].class, int.class);
        // Set the method to be accessible
        getUIntLE.setAccessible(true);
        // Invoke the method with the byte array and offset
        long result = (long) getUIntLE.invoke(null, data, 0);
        // Check that the result is correct
        assertEquals(1, result);
    }
}
