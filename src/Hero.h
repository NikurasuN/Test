#pragma once

#include "Item.h"

#include <algorithm>
#include <map>
#include <string>
#include <vector>

namespace herolinewars {

class Hero {
public:
    enum class PrimaryAttribute {
        Strength,
        Dexterity,
        Intelligence
    };

    Hero(std::string name,
         int baseMaxHealth,
         int baseAttack,
         int baseDefense,
         int strength,
         int dexterity,
         int intelligence,
         PrimaryAttribute primaryAttribute,
         int startingGold,
         int startingIncome,
         int attackRangeUnits);

    const std::string& getName() const { return name_; }
    PrimaryAttribute getPrimaryAttribute() const { return primaryAttribute_; }

    int getStrength() const;
    int getBaseStrength() const { return strength_; }
    int getDexterity() const;
    int getBaseDexterity() const { return dexterity_; }
    int getIntelligence() const;
    int getBaseIntelligence() const { return intelligence_; }

    int getItemStrengthBonus() const { return itemStrengthBonus_; }
    int getItemDexterityBonus() const { return itemDexterityBonus_; }
    int getItemIntelligenceBonus() const { return itemIntelligenceBonus_; }

    int getMaxHealth() const { return maxHealth_; }
    int getCurrentHealth() const { return currentHealth_; }
    int getCurrentShield() const { return currentShield_; }
    int getAttackPower() const { return attack_; }
    int getDefense() const { return defense_; }
    int getAttackRangeUnits() const { return attackRangeUnits_; }
    int getAttackRangePixels() const { return attackRangePixels_; }
    double getAttackSpeedMultiplier() const;
    double getEvasionChance() const;
    double getCriticalChance() const;
    int getMaxEnergyShield() const;

    int rollAttackDamage() const;

    void resetHealth();

    int getGold() const { return gold_; }
    int getIncome() const { return income_; }
    void addIncome(int amount) { income_ += amount; }
    void earnIncome();
    void addGold(int amount) { gold_ += amount; }
    bool spendGold(int amount);

    bool applyItem(const Item& item);

    bool upgradeAttribute(PrimaryAttribute attribute, int cost);

    bool takeDamage(int amount);

    int getLevel() const { return level_; }
    int getExperience() const { return experience_; }
    int getExperienceToNextLevel() const { return experienceToNextLevel_; }
    int gainExperience(int amount);

    std::string summary() const;
    const std::vector<Item>& getInventory() const { return inventory_; }

    void developerSetGold(int amount) { gold_ = std::max(0, amount); }

private:
    static constexpr int HEALTH_PER_STRENGTH = 4;
    static constexpr double DEFENSE_PER_STRENGTH = 1.0 / 3.0;
    static constexpr double PRIMARY_DAMAGE_RATIO = 0.5;
    static constexpr double ATTACK_SPEED_PER_DEX = 0.03;
    static constexpr double EVASION_PER_DEX = 0.01;
    static constexpr double MAX_EVASION = 0.35;
    static constexpr double CRITICAL_PER_INT = 0.015;
    static constexpr double MAX_CRITICAL = 0.4;
    static constexpr int SHIELD_PER_INT = 8;
    static constexpr int RANGE_UNIT_PIXELS = 24;
    static constexpr int PRIMARY_ATTRIBUTE_LEVEL_GAIN = 3;
    static constexpr int SECONDARY_ATTRIBUTE_LEVEL_GAIN = 1;

    struct SlotComparator {
        bool operator()(EquipmentSlot lhs, EquipmentSlot rhs) const {
            return static_cast<int>(lhs) < static_cast<int>(rhs);
        }
    };

    void recalculateStats();
    int calculatePrimaryDamageBonus() const;
    int getPrimaryAttributeValue() const;
    void applyLevelUp();
    int computeExperienceForLevel(int newLevel) const;

    std::string name_;
    PrimaryAttribute primaryAttribute_;
    std::vector<Item> inventory_;
    std::map<EquipmentSlot, std::size_t, SlotComparator> equippedUniqueItems_;

    const int baseMaxHealth_;
    const int baseAttack_;
    const int baseDefense_;
    const int attackRangeUnits_;
    const int attackRangePixels_;

    int itemAttackBonus_ = 0;
    int itemDefenseBonus_ = 0;
    int itemStrengthBonus_ = 0;
    int itemDexterityBonus_ = 0;
    int itemIntelligenceBonus_ = 0;

    int strength_;
    int dexterity_;
    int intelligence_;

    int maxHealth_ = 0;
    int currentHealth_ = 0;
    int currentShield_ = 0;
    int attack_ = 0;
    int defense_ = 0;
    int gold_ = 0;
    int income_ = 0;
    int level_ = 1;
    int experience_ = 0;
    int experienceToNextLevel_ = 0;
};

}  // namespace herolinewars
