package com.oneshot.implementation;

import java.util.ArrayList;
import java.util.List;

import com.oneshot.model.ComptageScenes;
import com.oneshot.model.TypeScene;

public class AssembleurScenesImpl {

    public List<TypeScene> assembler(ComptageScenes comptageScenes) {

        List<TypeScene> listTypeScene = new ArrayList<>();

        addTypeScene(listTypeScene, TypeScene.COMBAT, comptageScenes.combat());
        addTypeScene(listTypeScene, TypeScene.EXPLORATION, comptageScenes.exploration());
        addTypeScene(listTypeScene, TypeScene.ROLEPLAY, comptageScenes.roleplay());

        return List.copyOf(listTypeScene);
    }
    
    private void addTypeScene(List<TypeScene> listTypeScene, TypeScene typeScene, int numberTypeScene) {
        for (int categorie = 0; categorie < numberTypeScene; categorie++) {
            listTypeScene.add(typeScene);
        }
    }    
}
