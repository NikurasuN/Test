package com.example.herolinewars;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * A controllable hero on the upper lane.
 */
public class Hero {
    public enum PrimaryAttribute {
        STRENGTH,
        DEXTERITY,
        INTELLIGENCE
    }

    private static final int HEALTH_PER_STRENGTH = 4;
    private static final double DEFENSE_PER_STRENGTH = 1.0 / 3.0;
    private static final double PRIMARY_DAMAGE_RATIO = 0.5;
    private static final double ATTACK_SPEED_PER_DEX = 0.03;
    private static final double EVASION_PER_DEX = 0.01;
    private static final double MAX_EVASION = 0.35;
    private static final double CRITICAL_PER_INT = 0.015;
    private static final double MAX_CRITICAL = 0.4;
    private static final int SHIELD_PER_INT = 8;
    private static final Random RANDOM = new Random();

    private final String name;
    private final PrimaryAttribute primaryAttribute;
    private final List<Item> inventory = new ArrayList<>();

    private final int baseMaxHealth;
    private final int baseAttack;
    private final int baseDefense;
    private int itemAttackBonus;
    private int itemDefenseBonus;

    private final int strength;
    private final int dexterity;
    private final int intelligence;

    private int maxHealth;
    private int currentHealth;
    private int currentShield;
    private int attack;
    private int defense;
    private int gold;
    private int income;

    public Hero(String name, int baseMaxHealth, int baseAttack, int baseDefense,
                int strength, int dexterity, int intelligence, PrimaryAttribute primaryAttribute,
                int startingGold, int startingIncome) {
        this.name = name;
        this.primaryAttribute = primaryAttribute;
        this.baseMaxHealth = baseMaxHealth;
        this.baseAttack = baseAttack;
        this.baseDefense = baseDefense;
        this.strength = strength;
        this.dexterity = dexterity;
        this.intelligence = intelligence;
        this.gold = startingGold;
        this.income = startingIncome;
        recalculateStats();
        this.currentHealth = maxHealth;
        this.currentShield = getMaxEnergyShield();
    }

    public String getName() {
        return name;
    }

    public PrimaryAttribute getPrimaryAttribute() {
        return primaryAttribute;
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

    public int getMaxHealth() {
        return maxHealth;
    }

    public int getCurrentHealth() {
        return currentHealth;
    }

    public int getCurrentShield() {
        return currentShield;
    }

    public int getAttackPower() {
        return attack;
    }

    public int getDefense() {
        return defense;
    }

    public double getAttackSpeedMultiplier() {
        return 1.0 + dexterity * ATTACK_SPEED_PER_DEX;
    }

    public double getEvasionChance() {
        return Math.min(MAX_EVASION, dexterity * EVASION_PER_DEX);
    }

    public double getCriticalChance() {
        return Math.min(MAX_CRITICAL, intelligence * CRITICAL_PER_INT);
    }

    public int getMaxEnergyShield() {
        return intelligence * SHIELD_PER_INT;
    }

    public int rollAttackDamage() {
        int damage = Math.max(1, attack);
        if (RANDOM.nextDouble() < getCriticalChance()) {
            damage = (int) Math.round(damage * 1.75);
        }
        return damage;
    }

    public void resetHealth() {
        this.currentHealth = maxHealth;
        this.currentShield = getMaxEnergyShield();
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
        itemAttackBonus += item.getAttackBonus();
        itemDefenseBonus += item.getDefenseBonus();
        recalculateStats();
    }

    public List<Item> getInventory() {
        return Collections.unmodifiableList(inventory);
    }

    public boolean takeDamage(int amount) {
        if (amount <= 0) {
            return false;
        }
        if (RANDOM.nextDouble() < getEvasionChance()) {
            return false;
        }

        int remaining = amount;
        if (currentShield > 0) {
            int absorbed = Math.min(currentShield, remaining);
            currentShield -= absorbed;
            remaining -= absorbed;
        }

        if (remaining <= 0) {
            return false;
        }

        currentHealth -= remaining;
        return currentHealth <= 0;
    }

    @Override
    public String toString() {
        return String.format(
                "%s - HP: %d/%d (+%d shield), ATK: %d, DEF: %d, Gold: %d, Income: %d",
                name,
                currentHealth,
                maxHealth,
                currentShield,
                attack,
                defense,
                gold,
                income);
    }

    private void recalculateStats() {
        this.maxHealth = baseMaxHealth + (int) Math.round(strength * HEALTH_PER_STRENGTH);
        this.attack = baseAttack + itemAttackBonus + calculatePrimaryDamageBonus();
        this.defense = baseDefense + itemDefenseBonus + (int) Math.round(strength * DEFENSE_PER_STRENGTH);
        if (currentHealth > maxHealth) {
            currentHealth = maxHealth;
        }
        int maxShield = getMaxEnergyShield();
        if (currentShield > maxShield) {
            currentShield = maxShield;
        }
    }

    private int calculatePrimaryDamageBonus() {
        return (int) Math.round(getPrimaryAttributeValue() * PRIMARY_DAMAGE_RATIO);
    }

    private int getPrimaryAttributeValue() {
        switch (primaryAttribute) {
            case STRENGTH:
                return strength;
            case DEXTERITY:
                return dexterity;
            case INTELLIGENCE:
            default:
                return intelligence;
        }
    }
}
