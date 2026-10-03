package com.oneshot.rules.domain;

public class PolitiqueRepartitionScenes {

    public ComptageScenes repartir(int nombreScenes, Repartition repartition) {
        
        if (nombreScenes <= 0) {
           throw new IllegalArgumentException(
            "Le nombre de scène doit être supérieur à 0"
            ); 
        }

        int[] pourcentages = {repartition.combat(), repartition.exploration(), repartition.roleplay()};
        int[] compteurs = new int[3];
        int[] restes = new int[3];
        int scenesAttribuees = 0;

        for (int categorie = 0; categorie < pourcentages.length; categorie++) {
            long produit = (long) nombreScenes * pourcentages[categorie];

            compteurs[categorie] = (int) (produit / 100);
            restes[categorie] = (int) (produit % 100);
            scenesAttribuees += compteurs[categorie];
        }

        int scenesRestantes = nombreScenes - scenesAttribuees;

        for (int scene = 0; scene < scenesRestantes; scene++) {
            int categorieChoisie = 0;

            for (int categorie = 1; categorie < restes.length; categorie++) {
                if (restes[categorie] > restes[categorieChoisie]) {
                    categorieChoisie = categorie;
                }
            }

            compteurs[categorieChoisie]++;
            restes[categorieChoisie] = -1;
        }

        return new ComptageScenes(compteurs[0], compteurs[1], compteurs[2]);
    }
}
