package com.oneshot.rules.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

import org.junit.jupiter.api.Test;

public class PolitiqueNombreScenesTest {
    
    private final PolitiqueNombreScenes politique = new PolitiqueNombreScenes();

    @Test
    void acceptSiDuree120SceneBetween3And4() {
        verifierPlage(120, 3, 4);
    }

    @Test
    void acceptSiDuree180SceneBetween4And6() {
        verifierPlage(180, 4, 6);
    }

    @Test
    void acceptSiDuree240SceneBetween5And7() {
        verifierPlage(240, 5, 7);
    }

    @Test
    void acceptSiSameResultChoisir() {
        RandomGenerator aleatoire1 = RandomGeneratorFactory.of("L64X128MixRandom").create(42L);
        RandomGenerator aleatoire2 = RandomGeneratorFactory.of("L64X128MixRandom").create(42L);
        DureeSession duree = new DureeSession(180);
        
        int premierResultat = politique.choisir(duree, aleatoire1);
        int deuxiemeResultat = politique.choisir(duree, aleatoire2);

        assertEquals(premierResultat, deuxiemeResultat);
    }


    private void verifierPlage(int minutes, int min, int max) {
        DureeSession duree = new DureeSession(minutes);

        for (long graine = 0; graine < 100; graine++) {
            RandomGenerator aleatoire = RandomGeneratorFactory.of("L64X128MixRandom").create(graine);

            int result = politique.choisir(duree, aleatoire);

            assertTrue(result >= min && result <= max, "Graine " + graine + " : resultat hors plage = " + result);
        }
    }
}
