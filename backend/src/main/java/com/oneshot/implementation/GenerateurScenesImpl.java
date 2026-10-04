package com.oneshot.implementation;

import java.util.List;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

import com.oneshot.model.ComptageScenes;
import com.oneshot.model.DureeSession;
import com.oneshot.model.Repartition;
import com.oneshot.model.TypeScene;

import lombok.AllArgsConstructor;

@AllArgsConstructor 
public class GenerateurScenesImpl {

    private final PolitiqueNombreScenesImpl nombreScenes;
    private final PolitiqueRepartitionScenesImpl repartitionScenes;
    private final AssembleurScenesImpl assembleurScenes;

    public List<TypeScene> generer(DureeSession dureeSession, Repartition repartition, long graine) {
        RandomGenerator randomGenerator = RandomGeneratorFactory.of("L64X128MixRandom").create(graine);

        int nbrScenes = nombreScenes.choisir(dureeSession, randomGenerator);
        ComptageScenes comptageScenes = repartitionScenes.repartir(nbrScenes, repartition);
        List<TypeScene> listTypeScenes = assembleurScenes.assembler(comptageScenes);

        return List.copyOf(listTypeScenes);
    }
}
