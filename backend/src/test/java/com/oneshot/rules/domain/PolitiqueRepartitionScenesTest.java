package com.oneshot.rules.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class PolitiqueRepartitionScenesTest {

    @Test
    void accepteIfComptageEqualsRepartitionRule() {
        verifierRepartition(5, new Repartition(40, 30, 30), new ComptageScenes(2, 2, 1));
        verifierRepartition(6, new Repartition(40, 30, 30), new ComptageScenes(2, 2, 2));
        verifierRepartition(7, new Repartition(33, 33, 34), new ComptageScenes(2, 2, 3));
        verifierRepartition(3, new Repartition(34, 33, 33), new ComptageScenes(1, 1, 1));
        verifierRepartition(5, new Repartition(100, 0, 0), new ComptageScenes(5, 0, 0));
        verifierRepartition(5, new Repartition(0, 100, 0), new ComptageScenes(0, 5, 0));
        verifierRepartition(5, new Repartition(0, 0, 100), new ComptageScenes(0, 0, 5));
        verifierRepartition(3, new Repartition(1, 1, 98), new ComptageScenes(0, 0, 3));
    }

    @Test
    void refuseSiNombreSceneInferieurOrEqualsZero() {
        assertThrows(IllegalArgumentException.class, () -> 
            verifierRepartition(0, new Repartition(40, 30, 30), new ComptageScenes(2, 2, 1)));
        assertThrows(IllegalArgumentException.class, () -> 
            verifierRepartition(-1, new Repartition(40, 30, 30), new ComptageScenes(2, 2, 1)));
        assertThrows(IllegalArgumentException.class, () -> 
            verifierRepartition(Integer.MIN_VALUE, new Repartition(40, 30, 30), new ComptageScenes(2, 2, 1)));
    }

    private void verifierRepartition(int scenes, Repartition repartition, ComptageScenes comptageScenesReference) {
        PolitiqueRepartitionScenes politique = new PolitiqueRepartitionScenes();
        ComptageScenes comptageScenes = politique.repartir(scenes, repartition);

        assertEquals(comptageScenesReference, comptageScenes);
    }
}
