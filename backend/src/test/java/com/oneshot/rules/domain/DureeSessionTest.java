package com.oneshot.rules.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class DureeSessionTest {
    
    DureeSession dureeSession120 = new DureeSession(120);
    DureeSession dureeSession180 = new DureeSession(180);
    DureeSession dureeSession240 = new DureeSession(240);

    @Test
    void accepteUneDuree120Or180Or240() {
        assertEquals(120, dureeSession120.minutes());
        assertEquals(180, dureeSession180.minutes());
        assertEquals(240, dureeSession240.minutes());
    }

    @Test
    void refuseDureeSiDifferentDe120Or180Or240() {
        assertThrows(IllegalArgumentException.class, () -> new DureeSession(119));
        assertThrows(IllegalArgumentException.class, () -> new DureeSession(121));
        assertThrows(IllegalArgumentException.class, () -> new DureeSession(179));
        assertThrows(IllegalArgumentException.class, () -> new DureeSession(181));
        assertThrows(IllegalArgumentException.class, () -> new DureeSession(239));
        assertThrows(IllegalArgumentException.class, () -> new DureeSession(241));
    }

    @Test
    void refuseDureeSiZeroOrMoinsUn() {
        assertThrows(IllegalArgumentException.class, () -> new DureeSession(0));
        assertThrows(IllegalArgumentException.class, () -> new DureeSession(-1));
    }
}
