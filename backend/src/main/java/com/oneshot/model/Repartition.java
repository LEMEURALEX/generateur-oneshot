package com.oneshot.model;

public record Repartition (int combat, int exploration, int roleplay) {
    
    public Repartition {
        if ((long) combat + exploration + roleplay != 100 ||
            (combat < 0 || exploration < 0 || roleplay < 0)) {
            throw new IllegalArgumentException(
                "RM1: combat + exploration + roleplay doit être égale à 100"
            );
        }
    }
}
