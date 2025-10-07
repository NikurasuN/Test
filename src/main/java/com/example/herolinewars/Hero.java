package com.example.herolinewars;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A controllable hero on the upper lane.
 */
public class Hero {
    public enum PrimaryAttribute {
        STRENGTH("Strength"),
        DEXTERITY("Dexterity"),
        INTELLIGENCE("Intelligence");

        private final String displayName;

        PrimaryAttribute(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private static final int HEALTH_PER_STRENGTH = 5;
    private static final double ARMOR_PER_STRENGTH = 0.2;
    private static final double ATTACK_SPEED_PER_DEXTERITY = 0.015;
    private static final double EVASION_PER_DEXTERITY = 0.01;
    private static final double CRIT_PER_INTELLIGENCE = 0.015;
    private static final int SHIELD_PER_INTELLIGENCE = 4;
    private static final double CRIT_DAMAGE_MULTIPLIER = 1.5;

    private final String name;
    private final int strength;
    private final int dexterity;
    private final int intelligence;
    private final PrimaryAttribute primaryAttribute;
    private final int baseHealth;
    private final int baseAttack;
    private final int baseDefense;
    private int maxHealth;
    private int currentHealth;
    private int attack;
    private int defense;
    private int maxEnergyShield;
    private int currentEnergyShield;
    private int gold;
    private int income;
    private final List<Item> inventory = new ArrayList<>();

    public Hero(String name, int strength, int dexterity, int intelligence,
                PrimaryAttribute primaryAttribute, int baseHealth, int baseAttack,
                int baseDefense, int startingGold, int startingIncome) {
        this.name = name;
        this.strength = strength;
        this.dexterity = dexterity;
        this.intelligence = intelligence;
        this.primaryAttribute = primaryAttribute;
        this.baseHealth = baseHealth;
        this.baseAttack = baseAttack;
        this.baseDefense = baseDefense;
        this.gold = startingGold;
        this.income = startingIncome;
        initializeDerivedStats();
    }

    private void initializeDerivedStats() {
        this.maxHealth = baseHealth + strength * HEALTH_PER_STRENGTH;
        this.currentHealth = maxHealth;
        this.attack = baseAttack + getPrimaryAttributeValue();
        this.defense = baseDefense + (int) Math.floor(strength * ARMOR_PER_STRENGTH);
        this.maxEnergyShield = Math.max(0, intelligence * SHIELD_PER_INTELLIGENCE);
        this.currentEnergyShield = maxEnergyShield;
    }

    public String getName() {
        return name;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public int getCurrentHealth() {
        return currentHealth;
    }

    public int getCurrentEnergyShield() {
        return currentEnergyShield;
    }

    public int getMaxEnergyShield() {
        return maxEnergyShield;
    }

    public void resetHealth() {
        this.currentHealth = maxHealth;
        this.currentEnergyShield = maxEnergyShield;
    }

    public int getAttack() {
        return attack;
    }

    public int getDefense() {
        return defense;
    }

    public int getGold() {
        return gold;
    }

    public int getIncome() {
        return income;
    }

    public void addIncome(int amount) {
        this.income += amount;
    }

    public void earnIncome() {
        this.gold += income;
    }

    public void addGold(int amount) {
        this.gold += amount;
    }

    public boolean spendGold(int amount) {
        if (gold < amount) {
            return false;
        }
        gold -= amount;
        return true;
    }

    public void applyItem(Item item) {
        inventory.add(item);
        attack += item.getAttackBonus();
        defense += item.getDefenseBonus();
    }

    public List<Item> getInventory() {
        return Collections.unmodifiableList(inventory);
    }

    public boolean takeDamage(int amount) {
        if (amount <= 0) {
            return false;
        }
        if (ThreadLocalRandom.current().nextDouble() < getEvasionChance()) {
            return false;
        }
        int remaining = amount;
        if (currentEnergyShield > 0) {
            int absorbed = Math.min(currentEnergyShield, remaining);
            currentEnergyShield -= absorbed;
            remaining -= absorbed;
        }
        if (remaining <= 0) {
            return false;
        }
        currentHealth -= remaining;
        return currentHealth <= 0;
    }

    public int rollAttackDamage(Random random) {
        int damage = attack;
        if (random.nextDouble() < getCriticalChance()) {
            damage = (int) Math.round(damage * CRIT_DAMAGE_MULTIPLIER);
        }
        return damage;
    }

    public double getAttackSpeedMultiplier() {
        return 1.0 + dexterity * ATTACK_SPEED_PER_DEXTERITY;
    }

    public int getAttackDelayTicks(int baseTicks) {
        int adjusted = (int) Math.round(baseTicks / getAttackSpeedMultiplier());
        return Math.max(6, adjusted);
    }

    public double getEvasionChance() {
        return Math.min(0.35, Math.max(0, dexterity * EVASION_PER_DEXTERITY));
    }

    public double getCriticalChance() {
        return Math.min(0.5, Math.max(0, intelligence * CRIT_PER_INTELLIGENCE));
    }

    public int getStrength() {
        return strength;
    }

    public int getDexterity() {
        return dexterity;
    }

    public int getIntelligence() {
        return intelligence;
    }

    public PrimaryAttribute getPrimaryAttribute() {
        return primaryAttribute;
    }

    public int getPrimaryAttributeValue() {
        switch (primaryAttribute) {
            case STRENGTH:
                return strength;
            case DEXTERITY:
                return dexterity;
            case INTELLIGENCE:
                return intelligence;
            default:
                return 0;
        }
    }

    @Override
    public String toString() {
        return String.format(
                "%s - HP: %d/%d, Shield: %d/%d, ATK: %d, DEF: %d, Gold: %d, Income: %d",
                name,
                currentHealth,
                maxHealth,
                currentEnergyShield,
                maxEnergyShield,
                attack,
                defense,
                gold,
                income);
    }
}
