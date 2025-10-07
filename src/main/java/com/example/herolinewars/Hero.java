package com.example.herolinewars;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A controllable hero on the upper lane.
 */
public class Hero {
    private final String name;
    private final int maxHealth;
    private int currentHealth;
    private int attack;
    private int defense;
    private int gold;
    private int income;
    private final List<Item> inventory = new ArrayList<>();

    public Hero(String name, int maxHealth, int attack, int defense, int startingGold, int startingIncome) {
        this.name = name;
        this.maxHealth = maxHealth;
        this.currentHealth = maxHealth;
        this.attack = attack;
        this.defense = defense;
        this.gold = startingGold;
        this.income = startingIncome;
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

    public void resetHealth() {
        this.currentHealth = maxHealth;
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
        currentHealth -= amount;
        return currentHealth <= 0;
    }

    @Override
    public String toString() {
        return String.format("%s - HP: %d/%d, ATK: %d, DEF: %d, Gold: %d, Income: %d", name, currentHealth, maxHealth, attack, defense, gold, income);
    }
}
