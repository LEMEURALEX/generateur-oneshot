package com.oneshot.rules.domain;

import java.util.random.RandomGenerator;

public class PolitiqueNombreScenes {

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
