package com.oneshot.rules.domain;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class RepartitionTest {
    
    @Test
    void accepteUneSommeDe100() {
        assertDoesNotThrow(() -> new Repartition(40, 30, 30));
    }

    @Test
    void acceptSiSomme100ButOneOrMoreIsZero() {
        assertDoesNotThrow(() -> new Repartition(100, 0, 0));
        assertDoesNotThrow(() -> new Repartition(0, 100, 0));
        assertDoesNotThrow(() -> new Repartition(0, 0, 100));
    }
    
    @Test
    void refuseUneSommeDe99() {
        assertThrows(IllegalArgumentException.class,
            () -> new Repartition(40, 30, 29));
    }

    @Test
    void refuseUneSommeDe101() {
        assertThrows(IllegalArgumentException.class,
            () -> new Repartition(40, 30, 31));
    }

    @Test
    void refuseSiSomme100ButOneIsNegatif() {
        assertThrows(IllegalArgumentException.class,
            () -> new Repartition(-1, 50, 51));
        assertThrows(IllegalArgumentException.class,
            () -> new Repartition(50, -1, 51));
        assertThrows(IllegalArgumentException.class,
            () -> new Repartition(50, 51, -1));
    }

    @Test
    void refuseSiMore100ButOneIsZero() {
        assertThrows(IllegalArgumentException.class,
            () -> new Repartition(101, 0, 0));
        assertThrows(IllegalArgumentException.class,
            () -> new Repartition(0, 101, 0));
        assertThrows(IllegalArgumentException.class,
            () -> new Repartition(0, 0, 101));
    }
}
