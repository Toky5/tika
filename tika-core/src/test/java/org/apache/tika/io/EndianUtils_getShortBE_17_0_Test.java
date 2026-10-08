/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tika.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;


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
        assertThrows(NullPointerException.class, () -> EndianUtils.getShortBE(data, offset));
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
        assertThrows(IndexOutOfBoundsException.class, () -> EndianUtils.getShortBE(data, offset));
    }
}
