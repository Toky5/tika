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

import org.junit.jupiter.api.Test;


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
