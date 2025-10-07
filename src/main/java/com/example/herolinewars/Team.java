package com.example.herolinewars;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a team with a hero, base and queued units for the next wave.
 */
public class Team {
    private final String name;
    private final Hero hero;
    private final List<UnitType> nextWaveUnits = new ArrayList<>();
    private int baseHealth = 100;

    public Team(String name, Hero hero) {
        this.name = name;
        this.hero = hero;
    }

    public String getName() {
        return name;
    }

    public Hero getHero() {
        return hero;
    }

    public int getBaseHealth() {
        return baseHealth;
    }

    public void damageBase(int amount) {
        baseHealth = Math.max(0, baseHealth - amount);
    }

    public boolean isDefeated() {
        return baseHealth <= 0;
    }

    public void queueUnit(UnitType type) {
        nextWaveUnits.add(type);
    }

    public List<UnitType> getQueuedUnitsSnapshot() {
        return Collections.unmodifiableList(nextWaveUnits);
    }

    public List<UnitType> drainQueuedUnits() {
        List<UnitType> copy = new ArrayList<>(nextWaveUnits);
        nextWaveUnits.clear();
        return copy;
    }

    public boolean hasQueuedUnits() {
        return !nextWaveUnits.isEmpty();
    }
}
