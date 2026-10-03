package com.oneshot.rules.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.random.RandomGeneratorFactory;

import org.junit.jupiter.api.Test;

public class GenerateurScenesTest {

    PolitiqueNombreScenes politiqueNombreScenes = new PolitiqueNombreScenes();
    PolitiqueRepartitionScenes politiqueRepartitionScenes = new PolitiqueRepartitionScenes();
    AssembleurScenes assembleurScenes = new AssembleurScenes();

    GenerateurScenes generateurScenes = new GenerateurScenes(politiqueNombreScenes, politiqueRepartitionScenes, assembleurScenes);

    long grain = RandomGeneratorFactory.of("L64X128MixRandom").create(42L).nextLong();

    List<TypeScene> attendu1 = List.of(TypeScene.COMBAT, TypeScene.COMBAT, TypeScene.COMBAT, TypeScene.COMBAT);
    List<TypeScene> attendu2 = List.of(TypeScene.EXPLORATION, TypeScene.EXPLORATION, TypeScene.EXPLORATION, TypeScene.EXPLORATION, TypeScene.EXPLORATION);
    List<TypeScene> attendu3 = List.of(TypeScene.ROLEPLAY, TypeScene.ROLEPLAY, TypeScene.ROLEPLAY, TypeScene.ROLEPLAY, TypeScene.ROLEPLAY, TypeScene.ROLEPLAY);
    List<TypeScene> attendu4 = List.of(TypeScene.COMBAT, TypeScene.COMBAT, TypeScene.EXPLORATION, TypeScene.EXPLORATION, TypeScene.ROLEPLAY);

    @Test
    void accepteIfListGenereEqualsRepetition() {
        verifierGeneration(attendu1, new DureeSession(120), new Repartition(100, 0, 0), grain);
        verifierGeneration(attendu2, new DureeSession(180), new Repartition(0, 100, 0), grain);
        verifierGeneration(attendu3, new DureeSession(240), new Repartition(0, 0, 100), grain);
    }

    @Test
    void accepteIfEqualList() {
        List<TypeScene> listTypeScenes1 = generateurScenes.generer(new DureeSession(180), new Repartition(40, 30, 30), grain);
        List<TypeScene> listTypeScenes2 = generateurScenes.generer(new DureeSession(180), new Repartition(40, 30, 30), grain);

        assertEquals(listTypeScenes1, listTypeScenes2);
    }
 
    @Test
    void refuseSiModificationListe() {
        List<TypeScene> listTypeScenes = generateurScenes.generer(new DureeSession(120), new Repartition(0, 100, 0), grain);

        assertThrows(UnsupportedOperationException.class, () -> listTypeScenes.add(TypeScene.ROLEPLAY));
    }

    private void verifierGeneration(List<TypeScene> listTypeScenesAttendue, DureeSession dureeSession, Repartition repartition, long grain) {
        List<TypeScene> listTypeScenes = generateurScenes.generer(dureeSession, repartition, grain);

        assertEquals(listTypeScenesAttendue, listTypeScenes);
    }
}
