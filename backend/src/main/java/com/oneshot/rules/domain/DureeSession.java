package com.oneshot.rules.domain;

public record DureeSession (int minutes) {
    
    public DureeSession {
        if (minutes != 120 && minutes != 180 && minutes != 240)  {
            throw new IllegalArgumentException(
                "La durée doit être égale à 120 ou 180 ou 240"
            );
        }
    }
}
