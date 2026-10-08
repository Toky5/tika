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

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;


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
