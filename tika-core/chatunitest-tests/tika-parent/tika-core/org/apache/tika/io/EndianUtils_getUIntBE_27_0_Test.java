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

public class EndianUtils_getUIntBE_27_0_Test {

    @Test
    public void testGetUIntBE() throws Exception {
        // Create a test byte array
        byte[] data = new byte[] { 0x00, 0x00, 0x00, 0x01 };
        // Get the getUIntBE method using reflection
        Method method = EndianUtils.class.getDeclaredMethod("getUIntBE", byte[].class, int.class);
        method.setAccessible(true);
        // Invoke the method and get the result
        long result = (long) method.invoke(null, data, 0);
        // Check that the result is correct
        assertEquals(1, result);
    }
}
