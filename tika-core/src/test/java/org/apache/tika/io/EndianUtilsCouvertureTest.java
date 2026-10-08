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

/**
 * Tâche 2 (IFT3913) : tests écrits à la main pour le code de {@link EndianUtils} qu'aucun autre
 * test n'exécutait (ni les tests d'origine, ni les tests générés par ChatUniTest, ni
 * {@link EndianUtilsMutationTest}) : 14 méthodes jamais appelées et le cas du flux tronqué de
 * readUE7. Ils tuent les mutants que PIT signalait comme non couverts.
 * Chaque test est documenté dans le README.md (section « Tests écrits à la main pour le code
 * non couvert »).
 */
public class EndianUtilsCouvertureTest {

    /** Flux qui contient exactement les octets donnés. */
    private static InputStream flux(int... octets) {
        byte[] donnees = new byte[octets.length];
        for (int i = 0; i < octets.length; i++) {
            donnees[i] = (byte) octets[i];
        }
        return new ByteArrayInputStream(donnees);
    }

    /** Flux qui renvoie les valeurs données telles quelles, -1 compris, puis -1 indéfiniment. */
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
        // Des octets distincts et non nuls : chacun a un poids différent dans le résultat,
        // et la valeur attendue se lit directement en hexadécimal.
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
        // Le OU des octets lus vaut exactement 0 : c'est la limite entre une lecture valide
        // (>= 0) et une fin de flux (< 0).
        assertEquals(0, EndianUtils.readUShortLE(flux(0, 0)));
        assertEquals(0, EndianUtils.readUShortBE(flux(0, 0)));
        assertEquals(0, EndianUtils.readIntLE(flux(0, 0, 0, 0)));
        assertEquals(0, EndianUtils.readIntBE(flux(0, 0, 0, 0)));
        assertEquals(0L, EndianUtils.readLongLE(flux(0, 0, 0, 0, 0, 0, 0, 0)));
        assertEquals(0L, EndianUtils.readLongBE(flux(0, 0, 0, 0, 0, 0, 0, 0)));
    }

    @Test
    public void finDeFluxDetecteeMemeSiDesOctetsSuivent() {
        // La première lecture renvoie -1 et les suivantes des octets valides : la valeur est
        // incomplète dès qu'UNE des lectures échoue.
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
        // Un flux vide, puis un flux qui ne contient que 0x81 : le bit de poids fort de cet
        // octet annonce un octet suivant, qui n'arrive jamais. Dans les deux cas, la valeur est
        // incomplète.
        assertThrows(IOException.class, () -> EndianUtils.readUE7(flux()));
        assertThrows(IOException.class, () -> EndianUtils.readUE7(flux(0x81)));
    }

    @Test
    public void surchargesSansDecalageLisentDepuisLeDebut() {
        // Les surcharges sans décalage doivent lire à partir de l'indice 0.
        byte[] octets = {0x01, 0x02, 0x03, 0x04};
        assertEquals(0x0201, EndianUtils.getShortLE(octets));
        assertEquals(0x0201, EndianUtils.getUShortLE(octets));
        assertEquals(0x04030201, EndianUtils.getIntLE(octets));
        assertEquals(0x04030201L, EndianUtils.getUIntLE(octets));
        assertEquals(0x01020304L, EndianUtils.getUIntBE(octets));
    }

    @Test
    public void getLongLEAvecOctetsDistinctsEtDecalage() {
        // Huit octets distincts : chacun doit garder sa place. Avec un décalage de 2,
        // les deux premiers octets (0x7F) doivent être ignorés.
        assertEquals(0x0807060504030201L,
                EndianUtils.getLongLE(new byte[] {1, 2, 3, 4, 5, 6, 7, 8}, 0));
        assertEquals(0x0807060504030201L,
                EndianUtils.getLongLE(new byte[] {0x7F, 0x7F, 1, 2, 3, 4, 5, 6, 7, 8}, 2));
    }
}
