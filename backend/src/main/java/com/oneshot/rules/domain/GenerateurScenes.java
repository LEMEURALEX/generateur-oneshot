package com.oneshot.rules.domain;

import java.util.List;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

import lombok.AllArgsConstructor;

@AllArgsConstructor 
public class GenerateurScenes {

    private final PolitiqueNombreScenes nombreScenes;
    private final PolitiqueRepartitionScenes repartitionScenes;
    private final AssembleurScenes assembleurScenes;

    public List<TypeScene> generer(DureeSession dureeSession, Repartition repartition, long graine) {
        RandomGenerator randomGenerator = RandomGeneratorFactory.of("L64X128MixRandom").create(graine);

        int nbrScenes = nombreScenes.choisir(dureeSession, randomGenerator);
        ComptageScenes comptageScenes = repartitionScenes.repartir(nbrScenes, repartition);
        List<TypeScene> listTypeScenes = assembleurScenes.assembler(comptageScenes);

        return List.copyOf(listTypeScenes);
    }
}
