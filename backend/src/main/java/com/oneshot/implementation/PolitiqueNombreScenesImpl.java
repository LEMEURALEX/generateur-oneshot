package com.oneshot.implementation;

import java.util.random.RandomGenerator;

import com.oneshot.model.DureeSession;

public class PolitiqueNombreScenesImpl {

    public int choisir (DureeSession duree, RandomGenerator aleatoire) {
        return switch (duree.minutes()) {
            case 120 -> aleatoire.nextInt(3, 5);
            case 180 -> aleatoire.nextInt(4, 7);
            case 240 -> aleatoire.nextInt(5,8);
            default -> throw new IllegalArgumentException(
                "Durée de session non prise en charge");
        };
    };
}
