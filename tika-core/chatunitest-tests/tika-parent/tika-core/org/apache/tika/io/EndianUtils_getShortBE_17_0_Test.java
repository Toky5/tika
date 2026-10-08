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

public class EndianUtils_getShortBE_17_0_Test {

    @Test
    public void testGetShortBE() throws Exception {
        // Arrange
        byte[] data = new byte[] { 0x00, 0x01 };
        int offset = 0;
        // Act
        Method method = EndianUtils.class.getDeclaredMethod("getShortBE", byte[].class, int.class);
        method.setAccessible(true);
        short result = (short) method.invoke(null, data, offset);
        // Assert
        assertEquals(1, result);
    }

    @Test
    public void testGetShortBEWithNegativeByte() throws Exception {
        // Arrange
        byte[] data = new byte[] { (byte) 0xFF, (byte) 0xFF };
        int offset = 0;
        // Act
        Method method = EndianUtils.class.getDeclaredMethod("getShortBE", byte[].class, int.class);
        method.setAccessible(true);
        short result = (short) method.invoke(null, data, offset);
        // Assert
        assertEquals(-1, result);
    }

    @Test
    public void testGetShortBEWithNullData() throws Exception {
        // Arrange
        byte[] data = null;
        int offset = 0;
        // Act
        Method method = EndianUtils.class.getDeclaredMethod("getShortBE", byte[].class, int.class);
        method.setAccessible(true);
        // Assert
        assertThrows(NullPointerException.class, () -> method.invoke(null, data, offset));
    }

    @Test
    public void testGetShortBEWithOffsetOutOfBounds() throws Exception {
        // Arrange
        byte[] data = new byte[] { 0x00, 0x01 };
        int offset = 2;
        // Act
        Method method = EndianUtils.class.getDeclaredMethod("getShortBE", byte[].class, int.class);
        method.setAccessible(true);
        // Assert
        assertThrows(IndexOutOfBoundsException.class, () -> method.invoke(null, data, offset));
    }
}
