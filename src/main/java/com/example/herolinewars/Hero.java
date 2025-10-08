package com.example.herolinewars;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
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
    private static final int RANGE_UNIT_PIXELS = 24;
    private static final int PRIMARY_ATTRIBUTE_LEVEL_GAIN = 3;
    private static final int SECONDARY_ATTRIBUTE_LEVEL_GAIN = 1;
    private static final Random RANDOM = new Random();

    private static final int MAX_RINGS = 2;

    private final String name;
    private final PrimaryAttribute primaryAttribute;
    private final List<Item> inventory = new ArrayList<>();
    private final Map<Item.EquipmentSlot, Item> equippedUniqueItems = new EnumMap<>(Item.EquipmentSlot.class);

    private final int baseMaxHealth;
    private final int baseAttack;
    private final int baseDefense;
    private final int attackRangeUnits;
    private final int attackRangePixels;
    private int itemAttackBonus;
    private int itemDefenseBonus;
    private int itemStrengthBonus;
    private int itemDexterityBonus;
    private int itemIntelligenceBonus;

    private int strength;
    private int dexterity;
    private int intelligence;

    private int maxHealth;
    private int currentHealth;
    private int currentShield;
    private int attack;
    private int defense;
    private int gold;
    private int income;
    private int level = 1;
    private int experience;
    private int experienceToNextLevel;

    public Hero(String name, int baseMaxHealth, int baseAttack, int baseDefense,
                int strength, int dexterity, int intelligence, PrimaryAttribute primaryAttribute,
                int startingGold, int startingIncome, int attackRangeUnits) {
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
        this.attackRangeUnits = Math.max(1, attackRangeUnits);
        this.attackRangePixels = this.attackRangeUnits * RANGE_UNIT_PIXELS;
        this.experienceToNextLevel = computeExperienceForLevel(level);
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
        return strength + itemStrengthBonus;
    }

    public int getBaseStrength() {
        return strength;
    }

    public int getDexterity() {
        return dexterity + itemDexterityBonus;
    }

    public int getBaseDexterity() {
        return dexterity;
    }

    public int getIntelligence() {
        return intelligence + itemIntelligenceBonus;
    }

    public int getBaseIntelligence() {
        return intelligence;
    }

    public int getItemStrengthBonus() {
        return itemStrengthBonus;
    }

    public int getItemDexterityBonus() {
        return itemDexterityBonus;
    }

    public int getItemIntelligenceBonus() {
        return itemIntelligenceBonus;
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

    public int getAttackRangeUnits() {
        return attackRangeUnits;
    }

    public int getAttackRangePixels() {
        return attackRangePixels;
    }

    public int getLevel() {
        return level;
    }

    public int getExperience() {
        return experience;
    }

    public int getExperienceToNextLevel() {
        return experienceToNextLevel;
    }

    /**
     * Legacy accessor retained for backwards compatibility with older game logic.
     * Prefer {@link #getAttackPower()} for clarity.
     */
    public int getAttack() {
        return getAttackPower();
    }

    public int getDefense() {
        return defense;
    }

    public int getMaxRings() {
        return MAX_RINGS;
    }

    public double getAttackSpeedMultiplier() {
        return 1.0 + getDexterity() * ATTACK_SPEED_PER_DEX;
    }

    public double getEvasionChance() {
        return Math.min(MAX_EVASION, getDexterity() * EVASION_PER_DEX);
    }

    public double getCriticalChance() {
        return Math.min(MAX_CRITICAL, getIntelligence() * CRITICAL_PER_INT);
    }

    public int getMaxEnergyShield() {
        return getIntelligence() * SHIELD_PER_INT;
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

    public boolean applyItem(Item item) {
        Item.EquipmentSlot slot = item.getSlot();
        if (slot == Item.EquipmentSlot.RING && getEquippedCount(Item.EquipmentSlot.RING) >= MAX_RINGS) {
            return false;
        }
        if (slot != null && slot.isUnique()) {
            Item replaced = equippedUniqueItems.put(slot, item);
            if (replaced != null) {
                inventory.remove(replaced);
                itemAttackBonus -= replaced.getAttackBonus();
                itemDefenseBonus -= replaced.getDefenseBonus();
                itemStrengthBonus -= replaced.getStrengthBonus();
                itemDexterityBonus -= replaced.getDexterityBonus();
                itemIntelligenceBonus -= replaced.getIntelligenceBonus();
            }
        }
        inventory.add(item);
        itemAttackBonus += item.getAttackBonus();
        itemDefenseBonus += item.getDefenseBonus();
        itemStrengthBonus += item.getStrengthBonus();
        itemDexterityBonus += item.getDexterityBonus();
        itemIntelligenceBonus += item.getIntelligenceBonus();
        recalculateStats();
        return true;
    }

    public int getEquippedCount(Item.EquipmentSlot slot) {
        if (slot == null) {
            return 0;
        }
        int count = 0;
        for (Item item : inventory) {
            if (slot.equals(item.getSlot())) {
                count++;
            }
        }
        return count;
    }

    public Map<Item.EquipmentSlot, List<Item>> getEquippedItemsBySlot() {
        Map<Item.EquipmentSlot, List<Item>> grouped = new EnumMap<>(Item.EquipmentSlot.class);
        for (Item item : inventory) {
            Item.EquipmentSlot slot = item.getSlot();
            if (slot == null) {
                continue;
            }
            grouped.computeIfAbsent(slot, key -> new ArrayList<>()).add(item);
        }
        Map<Item.EquipmentSlot, List<Item>> immutable = new EnumMap<>(Item.EquipmentSlot.class);
        for (Map.Entry<Item.EquipmentSlot, List<Item>> entry : grouped.entrySet()) {
            immutable.put(entry.getKey(), Collections.unmodifiableList(entry.getValue()));
        }
        return Collections.unmodifiableMap(immutable);
    }

    public Item getEquippedItem(Item.EquipmentSlot slot) {
        if (slot == null || !slot.isUnique()) {
            return null;
        }
        return equippedUniqueItems.get(slot);
    }

    public List<Item> getInventory() {
        return Collections.unmodifiableList(inventory);
    }

    public boolean upgradeAttribute(PrimaryAttribute attribute, int cost) {
        if (attribute == null || cost < 0 || !spendGold(cost)) {
            return false;
        }
        switch (attribute) {
            case STRENGTH:
                strength++;
                break;
            case DEXTERITY:
                dexterity++;
                break;
            case INTELLIGENCE:
                intelligence++;
                break;
            default:
                break;
        }
        recalculateStats();
        currentHealth = maxHealth;
        currentShield = getMaxEnergyShield();
        return true;
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

    public int gainExperience(int amount) {
        if (amount <= 0) {
            return 0;
        }
        experience += amount;
        int levelsGained = 0;
        while (experience >= experienceToNextLevel) {
            experience -= experienceToNextLevel;
            level++;
            levelsGained++;
            applyLevelUp();
            experienceToNextLevel = computeExperienceForLevel(level);
        }
        return levelsGained;
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

    public void developerSetBaseAttributes(int newStrength, int newDexterity, int newIntelligence) {
        this.strength = Math.max(0, newStrength);
        this.dexterity = Math.max(0, newDexterity);
        this.intelligence = Math.max(0, newIntelligence);
        recalculateStats();
    }

    public void developerSetGold(int amount) {
        this.gold = Math.max(0, amount);
    }

    public void developerSetIncome(int amount) {
        this.income = Math.max(0, amount);
    }

    public void developerSetCurrentHealth(int amount) {
        this.currentHealth = Math.max(0, Math.min(amount, maxHealth));
    }

    public void developerSetCurrentShield(int amount) {
        this.currentShield = Math.max(0, Math.min(amount, getMaxEnergyShield()));
    }

    private void recalculateStats() {
        int totalStrength = getStrength();
        this.maxHealth = baseMaxHealth + (int) Math.round(totalStrength * HEALTH_PER_STRENGTH);
        this.attack = baseAttack + itemAttackBonus + calculatePrimaryDamageBonus();
        this.defense = baseDefense + itemDefenseBonus + (int) Math.round(totalStrength * DEFENSE_PER_STRENGTH);
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
                return getStrength();
            case DEXTERITY:
                return getDexterity();
            case INTELLIGENCE:
            default:
                return getIntelligence();
        }
    }

    private void applyLevelUp() {
        switch (primaryAttribute) {
            case STRENGTH:
                strength += PRIMARY_ATTRIBUTE_LEVEL_GAIN;
                dexterity += SECONDARY_ATTRIBUTE_LEVEL_GAIN;
                intelligence += SECONDARY_ATTRIBUTE_LEVEL_GAIN;
                break;
            case DEXTERITY:
                dexterity += PRIMARY_ATTRIBUTE_LEVEL_GAIN;
                strength += SECONDARY_ATTRIBUTE_LEVEL_GAIN;
                intelligence += SECONDARY_ATTRIBUTE_LEVEL_GAIN;
                break;
            case INTELLIGENCE:
            default:
                intelligence += PRIMARY_ATTRIBUTE_LEVEL_GAIN;
                strength += SECONDARY_ATTRIBUTE_LEVEL_GAIN;
                dexterity += SECONDARY_ATTRIBUTE_LEVEL_GAIN;
                break;
        }
        recalculateStats();
        currentHealth = maxHealth;
        currentShield = getMaxEnergyShield();
    }

    private int computeExperienceForLevel(int newLevel) {
        return 120 + Math.max(0, newLevel - 1) * 60;
    }
}
