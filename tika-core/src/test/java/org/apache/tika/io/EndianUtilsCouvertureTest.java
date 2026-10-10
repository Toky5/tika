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
import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

// Tache 2 : tests pour les methodes qu'aucun test n'appelait (mesure 4, voir le README).
// Faits rapidement a l'aide de l'IA generative, puis verifies avec PIT et JaCoCo.
public class EndianUtilsCouvertureTest {

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
    public void lectureDOctetsDistinctsDepuisUnFlux() throws Exception {
        // variantes a verifier : short, ushort, int et long, chacune en LE et en BE
        // ex. avec 1, 2 : LE -> 0x0201, BE -> 0x0102
        assertEquals(0x0201, EndianUtils.readShortLE(flux(1, 2)));
        assertEquals(0x0102, EndianUtils.readShortBE(flux(1, 2)));
        assertEquals(0x0201, EndianUtils.readUShortLE(flux(1, 2)));
        assertEquals(0x0102, EndianUtils.readUShortBE(flux(1, 2)));
        assertEquals(0x04030201, EndianUtils.readIntLE(flux(1, 2, 3, 4)));
        assertEquals(0x01020304, EndianUtils.readIntBE(flux(1, 2, 3, 4)));
        assertEquals(0x0807060504030201L, EndianUtils.readLongLE(flux(1, 2, 3, 4, 5, 6, 7, 8)));
        assertEquals(0x0102030405060708L, EndianUtils.readLongBE(flux(1, 2, 3, 4, 5, 6, 7, 8)));
    }

    @Test
    public void octetsNulsLusSansFinDeFlux() throws Exception {
        // que des 0 : ce n'est pas une fin de flux
        assertEquals(0, EndianUtils.readUShortLE(flux(0, 0)));
        assertEquals(0, EndianUtils.readUShortBE(flux(0, 0)));
        assertEquals(0, EndianUtils.readIntLE(flux(0, 0, 0, 0)));
        assertEquals(0, EndianUtils.readIntBE(flux(0, 0, 0, 0)));
        assertEquals(0L, EndianUtils.readLongLE(flux(0, 0, 0, 0, 0, 0, 0, 0)));
        assertEquals(0L, EndianUtils.readLongBE(flux(0, 0, 0, 0, 0, 0, 0, 0)));
    }

    @Test
    public void finDeFluxDetecteeMemeSiDesOctetsSuivent() {
        // -1 au debut puis des octets valides -> exception
        assertThrows(EndianUtils.BufferUnderrunException.class,
                () -> EndianUtils.readUShortLE(fluxScripte(-1, 1)));
        assertThrows(EndianUtils.BufferUnderrunException.class,
                () -> EndianUtils.readUShortBE(fluxScripte(-1, 1)));
        assertThrows(EndianUtils.BufferUnderrunException.class,
                () -> EndianUtils.readIntLE(fluxScripte(-1, 1, 2, 3)));
        assertThrows(EndianUtils.BufferUnderrunException.class,
                () -> EndianUtils.readIntBE(fluxScripte(-1, 1, 2, 3)));
        assertThrows(EndianUtils.BufferUnderrunException.class,
                () -> EndianUtils.readLongLE(fluxScripte(-1, 1, 2, 3, 4, 5, 6, 7)));
        assertThrows(EndianUtils.BufferUnderrunException.class,
                () -> EndianUtils.readLongBE(fluxScripte(-1, 1, 2, 3, 4, 5, 6, 7)));
    }

    @Test
    public void readUE7SignaleUnFluxTronque() {
        // flux vide, puis 0x81 tout seul (il annonce un octet qui n'arrive pas)
        assertThrows(IOException.class, () -> EndianUtils.readUE7(flux()));
        assertThrows(IOException.class, () -> EndianUtils.readUE7(flux(0x81)));
    }

    @Test
    public void surchargesSansDecalageLisentDepuisLeDebut() {
        // les versions sans offset doivent lire a partir de l'indice 0
        byte[] octets = {0x01, 0x02, 0x03, 0x04};
        assertEquals(0x0201, EndianUtils.getShortLE(octets));
        assertEquals(0x0201, EndianUtils.getUShortLE(octets));
        assertEquals(0x04030201, EndianUtils.getIntLE(octets));
        assertEquals(0x04030201L, EndianUtils.getUIntLE(octets));
        assertEquals(0x01020304L, EndianUtils.getUIntBE(octets));
    }

    @Test
    public void getLongLEAvecOctetsDistinctsEtDecalage() {
        // offset 2 : les deux 0x7F du debut doivent etre ignores
        assertEquals(0x0807060504030201L,
                EndianUtils.getLongLE(new byte[] {1, 2, 3, 4, 5, 6, 7, 8}, 0));
        assertEquals(0x0807060504030201L,
                EndianUtils.getLongLE(new byte[] {0x7F, 0x7F, 1, 2, 3, 4, 5, 6, 7, 8}, 2));
    }
}
