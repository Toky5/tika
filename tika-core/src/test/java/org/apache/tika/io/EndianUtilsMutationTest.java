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

/**
 * Tâche 2 (IFT3913) : tests écrits à la main pour tuer les mutants de {@link EndianUtils}
 * qui survivent aux tests d'origine et aux tests générés par ChatUniTest.
 * Chaque test est documenté dans le README.md (section « Tests écrits à la main »).
 */
public class EndianUtilsMutationTest {

    /** Flux qui contient exactement les octets donnés. */
    private static InputStream flux(int... octets) {
        byte[] donnees = new byte[octets.length];
        for (int i = 0; i < octets.length; i++) {
            donnees[i] = (byte) octets[i];
        }
        return new ByteArrayInputStream(donnees);
    }

    /**
     * Flux qui renvoie les valeurs données telles quelles, -1 compris, puis -1 indéfiniment.
     * Il imite un flux dont la fin n'est pas définitive, comme une console après Ctrl-Z.
     */
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
        // Le OU des quatre octets lus vaut 0 : c'est la limite exacte entre une lecture
        // valide (>= 0) et une fin de flux (< 0).
        assertEquals(0L, EndianUtils.readUIntLE(flux(0, 0, 0, 0)));
        assertEquals(0L, EndianUtils.readUIntBE(flux(0, 0, 0, 0)));
        assertEquals(0, EndianUtils.readIntME(flux(0, 0, 0, 0)));
    }

    @Test
    public void uneFinDeFluxSuivieDOctetsEstDetectee() {
        // La première lecture renvoie -1 et les trois suivantes des octets valides :
        // la valeur est incomplète dès qu'UNE des quatre lectures échoue. Pour readUIntBE,
        // c'est aussi le cas du flux trop court que EndianUtilsTest.testReadUIntBE voulait
        // vérifier, mais qu'il appliquait par erreur à readUIntLE.
        assertThrows(EndianUtils.BufferUnderrunException.class,
                () -> EndianUtils.readUIntLE(fluxScripte(-1, 1, 2, 3)));
        assertThrows(EndianUtils.BufferUnderrunException.class,
                () -> EndianUtils.readUIntBE(fluxScripte(-1, 1, 2, 3)));
        assertThrows(EndianUtils.BufferUnderrunException.class,
                () -> EndianUtils.readIntME(fluxScripte(-1, 1, 2, 3)));
    }

    @Test
    public void readUE7AccepteUnDernierGroupeNul() throws Exception {
        // 0x81 : groupe de valeur 1 avec le bit de continuation ; 0x00 : dernier groupe, nul.
        // Valeur attendue : 1 * 128 + 0 = 128.
        assertEquals(128L, EndianUtils.readUE7(flux(0x81, 0x00)));
    }

    @Test
    public void readUE7NeDecodePasPlusDeSixOctets() throws Exception {
        // Un groupe 1 suivi de cinq groupes nuls donne 2^35 ; le septième octet (0x80)
        // est lu mais ignoré, car readUE7 décode au plus six groupes de 7 bits.
        assertEquals(1L << 35,
                EndianUtils.readUE7(flux(0x81, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x00)));
    }

    @Test
    public void getIntLEAvecQuatreOctetsDistinctsNonNuls() {
        // Chaque octet est non nul et a un poids différent : un décalage ou une addition
        // faussé change le résultat, ce que les octets nuls du test généré masquaient.
        assertEquals(0x04030201, EndianUtils.getIntLE(new byte[] {0x01, 0x02, 0x03, 0x04}, 0));
    }
}
