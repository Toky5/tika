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

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

// Tache 2 : tests ajoutes pour les mutants qui survivaient (mesure 3, voir le README).
// Faits rapidement a l'aide de l'IA generative, puis verifies avec PIT.
public class EndianUtilsMutationTest {

    // flux avec les octets donnes
    private static InputStream flux(int... octets) {
        byte[] donnees = new byte[octets.length];
        for (int i = 0; i < octets.length; i++) {
            donnees[i] = (byte) octets[i];
        }
        return new ByteArrayInputStream(donnees);
    }

    // flux qui renvoie les valeurs telles quelles (meme -1), puis -1
    private static InputStream fluxScripte(int... valeurs) {
        return new InputStream() {
            private int position = 0;

            @Override
            public int read() {
                return position < valeurs.length ? valeurs[position++] : -1;
            }
        };
    }

    @Test
    public void quatreOctetsNulsNeSontPasUneFinDeFlux() throws Exception {
        // 4 octets a 0 : le OU donne 0, ca ne doit pas etre vu comme une fin de flux
        assertEquals(0L, EndianUtils.readUIntLE(flux(0, 0, 0, 0)));
        assertEquals(0L, EndianUtils.readUIntBE(flux(0, 0, 0, 0)));
        assertEquals(0, EndianUtils.readIntME(flux(0, 0, 0, 0)));
    }

    @Test
    public void uneFinDeFluxSuivieDOctetsEstDetectee() {
        // -1 au debut puis des octets valides -> exception quand meme
        // (testReadUIntBE de EndianUtilsTest appelle readUIntLE par erreur)
        assertThrows(EndianUtils.BufferUnderrunException.class,
                () -> EndianUtils.readUIntLE(fluxScripte(-1, 1, 2, 3)));
        assertThrows(EndianUtils.BufferUnderrunException.class,
                () -> EndianUtils.readUIntBE(fluxScripte(-1, 1, 2, 3)));
        assertThrows(EndianUtils.BufferUnderrunException.class,
                () -> EndianUtils.readIntME(fluxScripte(-1, 1, 2, 3)));
    }

    @Test
    public void readUE7AccepteUnDernierGroupeNul() throws Exception {
        // 0x81 puis 0x00 -> 1 * 128 + 0 = 128
        assertEquals(128L, EndianUtils.readUE7(flux(0x81, 0x00)));
    }

    @Test
    public void readUE7NeDecodePasPlusDeSixOctets() throws Exception {
        // 6 octets max, le 7e est ignore -> 2^35
        assertEquals(1L << 35,
                EndianUtils.readUE7(flux(0x81, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x00)));
    }

    @Test
    public void getIntLEAvecQuatreOctetsDistinctsNonNuls() {
        // avec {0, 0, 0, 1} (test genere) les mutants sur les decalages survivaient
        assertEquals(0x04030201, EndianUtils.getIntLE(new byte[] {0x01, 0x02, 0x03, 0x04}, 0));
    }
}
