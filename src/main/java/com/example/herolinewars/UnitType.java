package com.example.herolinewars;

/**
 * Describes a unit that can be sent to the enemy lane.
 */
public enum UnitType {
    SCOUT("Scout", 35, 20, 6, 2, 36, "A cheap and fast unit that increases income slightly."),
    SOLDIER("Soldier", 50, 35, 9, 3, 40, "Balanced melee fighter."),
    ARCHER("Archer", 65, 30, 12, 4, 160, "Ranged attacker that deals reliable damage."),
    KNIGHT("Knight", 90, 55, 16, 6, 42, "Heavy unit with strong damage."),
    SIEGE_GOLEM("Siege Golem", 120, 80, 25, 8, 50, "Slow but devastating against bases.");

    private final String displayName;
    private final int cost;
    private final int health;
    private final int damage;
    private final int incomeBonus;
    private final int range;
    private final String description;

    UnitType(String displayName, int cost, int health, int damage, int incomeBonus, int range, String description) {
        this.displayName = displayName;
        this.cost = cost;
        this.health = health;
        this.damage = damage;
        this.incomeBonus = incomeBonus;
        this.range = range;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getCost() {
        return cost;
    }

    public int getHealth() {
        return health;
    }

    public int getDamage() {
        return damage;
    }

    public int getIncomeBonus() {
        return incomeBonus;
    }

    public int getRange() {
        return range;
    }

    public String getDescription() {
        return description;
    }
}
