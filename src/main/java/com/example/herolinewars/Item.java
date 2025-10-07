package com.example.herolinewars;

/**
 * Represents an item that can be purchased from the shop.
 */
public class Item {
    private final String name;
    private final int attackBonus;
    private final int defenseBonus;
    private final int cost;
    private final String description;

    public Item(String name, int attackBonus, int defenseBonus, int cost, String description) {
        this.name = name;
        this.attackBonus = attackBonus;
        this.defenseBonus = defenseBonus;
        this.cost = cost;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public int getAttackBonus() {
        return attackBonus;
    }

    public int getDefenseBonus() {
        return defenseBonus;
    }

    public int getCost() {
        return cost;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return String.format("%s (Cost: %d, +%d ATK, +%d DEF) - %s", name, cost, attackBonus, defenseBonus, description);
    }
}
