package com.oneshot.implementation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.oneshot.model.ComptageScenes;
import com.oneshot.model.TypeScene;

public class AssembleurScenesImplTest {

    AssembleurScenesImpl assembleurScenes = new AssembleurScenesImpl();

    List<TypeScene> attendu1 = List.of(TypeScene.COMBAT, TypeScene.COMBAT, TypeScene.EXPLORATION, TypeScene.EXPLORATION, TypeScene.ROLEPLAY);
    List<TypeScene> attendu2 = List.of(TypeScene.COMBAT, TypeScene.COMBAT, TypeScene.COMBAT);
    List<TypeScene> attendu3 = List.of(TypeScene.EXPLORATION, TypeScene.EXPLORATION);
    List<TypeScene> attendu4 = List.of(TypeScene.ROLEPLAY);

    @Test
    void accepteIfComptageEqualsRepartitionRule() {
        verifierAssemblage(attendu1, new ComptageScenes(2, 2, 1));
        verifierAssemblage(attendu2, new ComptageScenes(3, 0, 0));
        verifierAssemblage(attendu3, new ComptageScenes(0, 2, 0));
        verifierAssemblage(attendu4, new ComptageScenes(0, 0, 1));
    }

    @Test
    void refuseSiModificationListe() {
        List<TypeScene> listTypeScenes = assembleurScenes.assembler(new ComptageScenes(2, 2, 1));

        assertThrows(UnsupportedOperationException.class, () -> listTypeScenes.add(TypeScene.ROLEPLAY));
    }

    private void verifierAssemblage(List<TypeScene> listTypeScenesAttendue, ComptageScenes comptageScenes) {
        List<TypeScene> listTypeScenes = assembleurScenes.assembler(comptageScenes);

        assertEquals(listTypeScenesAttendue, listTypeScenes);
    }
}
