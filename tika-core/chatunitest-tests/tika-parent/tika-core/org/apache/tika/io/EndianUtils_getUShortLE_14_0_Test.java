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

public class EndianUtils_getUShortLE_14_0_Test {

    @Test
    public void testGetUShortLE() throws Exception {
        // Create a byte array with two bytes
        byte[] data = new byte[] { 0x12, 0x34 };
        // Invoke the getUShortLE method using reflection
        Method method = EndianUtils.class.getDeclaredMethod("getUShortLE", byte[].class, int.class);
        method.setAccessible(true);
        int result = (int) method.invoke(null, data, 0);
        // Check that the result is correct
        assertEquals(0x3412, result);
    }
}
