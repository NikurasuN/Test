package com.example.herolinewars;

/**
 * Represents an item that can be purchased from the shop.
 */
public class Item {
    /**
     * Equipment slots restrict how many pieces of a certain type can be equipped at once.
     */
    public enum EquipmentSlot {
        HELMET("Helmet", true),
        CHESTPLATE("Chestplate", true),
        WEAPON("Weapon", true),
        SHIELD("Shield", true),
        ACCESSORY("Accessory", false),
        RING("Ring", false);

        private final String displayName;
        private final boolean unique;

        EquipmentSlot(String displayName, boolean unique) {
            this.displayName = displayName;
            this.unique = unique;
        }

        public String getDisplayName() {
            return displayName;
        }

        public boolean isUnique() {
            return unique;
        }
    }

    private final String name;
    private final int attackBonus;
    private final int defenseBonus;
    private final int cost;
    private final String description;
    private final EquipmentSlot slot;
    private final int strengthBonus;
    private final int dexterityBonus;
    private final int intelligenceBonus;

    public Item(String name, int attackBonus, int defenseBonus, int cost, String description,
                EquipmentSlot slot, int strengthBonus, int dexterityBonus, int intelligenceBonus) {
        this.name = name;
        this.attackBonus = attackBonus;
        this.defenseBonus = defenseBonus;
        this.cost = cost;
        this.description = description;
        this.slot = slot;
        this.strengthBonus = strengthBonus;
        this.dexterityBonus = dexterityBonus;
        this.intelligenceBonus = intelligenceBonus;
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

    public EquipmentSlot getSlot() {
        return slot;
    }

    public int getStrengthBonus() {
        return strengthBonus;
    }

    public int getDexterityBonus() {
        return dexterityBonus;
    }

    public int getIntelligenceBonus() {
        return intelligenceBonus;
    }

    @Override
    public String toString() {
        StringBuilder stats = new StringBuilder();
        if (attackBonus != 0) {
            stats.append(String.format("+%d ATK", attackBonus));
        }
        if (defenseBonus != 0) {
            appendStat(stats, String.format("+%d DEF", defenseBonus));
        }
        if (strengthBonus != 0) {
            appendStat(stats, String.format("+%d STR", strengthBonus));
        }
        if (dexterityBonus != 0) {
            appendStat(stats, String.format("+%d DEX", dexterityBonus));
        }
        if (intelligenceBonus != 0) {
            appendStat(stats, String.format("+%d INT", intelligenceBonus));
        }
        String slotText = slot != null ? String.format(" [%s]", slot.getDisplayName()) : "";
        String statSummary = stats.length() > 0 ? stats.toString() : "No bonuses";
        return String.format("%s (Cost: %d) %s%s - %s", name, cost, statSummary, slotText, description);
    }

    private void appendStat(StringBuilder builder, String stat) {
        if (builder.length() > 0) {
            builder.append(", ");
        }
        builder.append(stat);
    }
}
