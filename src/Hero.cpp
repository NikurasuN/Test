#include "Hero.h"

#include <algorithm>
#include <cmath>
#include <iomanip>
#include <random>
#include <sstream>

namespace herolinewars {

namespace {
std::mt19937& rng() {
    static std::mt19937 generator(std::random_device{}());
    return generator;
}

double randomUnit() {
    static std::uniform_real_distribution<double> distribution(0.0, 1.0);
    return distribution(rng());
}
}  // namespace

Hero::Hero(std::string name,
           int baseMaxHealth,
           int baseAttack,
           int baseDefense,
           int strength,
           int dexterity,
           int intelligence,
           PrimaryAttribute primaryAttribute,
           int startingGold,
           int startingIncome,
           int attackRangeUnits)
    : name_(std::move(name)),
      primaryAttribute_(primaryAttribute),
      baseMaxHealth_(baseMaxHealth),
      baseAttack_(baseAttack),
      baseDefense_(baseDefense),
      attackRangeUnits_(std::max(1, attackRangeUnits)),
      attackRangePixels_(attackRangeUnits_ * RANGE_UNIT_PIXELS),
      strength_(strength),
      dexterity_(dexterity),
      intelligence_(intelligence),
      gold_(startingGold),
      income_(startingIncome) {
    experienceToNextLevel_ = computeExperienceForLevel(level_);
    recalculateStats();
    currentHealth_ = maxHealth_;
    currentShield_ = getMaxEnergyShield();
}

int Hero::getStrength() const { return strength_ + itemStrengthBonus_; }
int Hero::getDexterity() const { return dexterity_ + itemDexterityBonus_; }
int Hero::getIntelligence() const { return intelligence_ + itemIntelligenceBonus_; }

double Hero::getAttackSpeedMultiplier() const {
    return 1.0 + getDexterity() * ATTACK_SPEED_PER_DEX;
}

double Hero::getEvasionChance() const {
    return std::min(MAX_EVASION, getDexterity() * EVASION_PER_DEX);
}

double Hero::getCriticalChance() const {
    return std::min(MAX_CRITICAL, getIntelligence() * CRITICAL_PER_INT);
}

int Hero::getMaxEnergyShield() const {
    return getIntelligence() * SHIELD_PER_INT;
}

int Hero::rollAttackDamage() const {
    int damage = std::max(1, attack_);
    if (randomUnit() < getCriticalChance()) {
        damage = static_cast<int>(std::round(damage * 1.75));
    }
    return damage;
}

void Hero::resetHealth() {
    currentHealth_ = maxHealth_;
    currentShield_ = getMaxEnergyShield();
}

void Hero::earnIncome() { gold_ += income_; }

bool Hero::spendGold(int amount) {
    if (amount < 0 || gold_ < amount) {
        return false;
    }
    gold_ -= amount;
    return true;
}

bool Hero::applyItem(const Item& item) {
    const EquipmentSlot slot = item.getSlot();

    if (slot == EquipmentSlot::Ring) {
        int ringCount = 0;
        for (const auto& owned : inventory_) {
            if (owned.getSlot() == EquipmentSlot::Ring) {
                ++ringCount;
            }
        }
        if (ringCount >= 2) {
            return false;
        }
    }

    if (slot != EquipmentSlot::None && isUniqueSlot(slot)) {
        auto found = equippedUniqueItems_.find(slot);
        if (found != equippedUniqueItems_.end()) {
            Item& replaced = inventory_.at(found->second);
            itemAttackBonus_ -= replaced.getAttackBonus();
            itemDefenseBonus_ -= replaced.getDefenseBonus();
            itemStrengthBonus_ -= replaced.getStrengthBonus();
            itemDexterityBonus_ -= replaced.getDexterityBonus();
            itemIntelligenceBonus_ -= replaced.getIntelligenceBonus();
            replaced = item;
            itemAttackBonus_ += item.getAttackBonus();
            itemDefenseBonus_ += item.getDefenseBonus();
            itemStrengthBonus_ += item.getStrengthBonus();
            itemDexterityBonus_ += item.getDexterityBonus();
            itemIntelligenceBonus_ += item.getIntelligenceBonus();
            recalculateStats();
            return true;
        }
    }

    inventory_.push_back(item);
    if (slot != EquipmentSlot::None && isUniqueSlot(slot)) {
        equippedUniqueItems_[slot] = inventory_.size() - 1;
    }
    itemAttackBonus_ += item.getAttackBonus();
    itemDefenseBonus_ += item.getDefenseBonus();
    itemStrengthBonus_ += item.getStrengthBonus();
    itemDexterityBonus_ += item.getDexterityBonus();
    itemIntelligenceBonus_ += item.getIntelligenceBonus();
    recalculateStats();
    return true;
}

bool Hero::upgradeAttribute(PrimaryAttribute attribute, int cost) {
    if (attribute != PrimaryAttribute::Strength && attribute != PrimaryAttribute::Dexterity &&
        attribute != PrimaryAttribute::Intelligence) {
        return false;
    }
    if (!spendGold(cost)) {
        return false;
    }

    switch (attribute) {
        case PrimaryAttribute::Strength:
            ++strength_;
            break;
        case PrimaryAttribute::Dexterity:
            ++dexterity_;
            break;
        case PrimaryAttribute::Intelligence:
            ++intelligence_;
            break;
    }

    recalculateStats();
    currentHealth_ = maxHealth_;
    currentShield_ = getMaxEnergyShield();
    return true;
}

bool Hero::takeDamage(int amount) {
    if (amount <= 0) {
        return false;
    }
    if (randomUnit() < getEvasionChance()) {
        return false;
    }

    int remaining = amount;
    if (currentShield_ > 0) {
        const int absorbed = std::min(currentShield_, remaining);
        currentShield_ -= absorbed;
        remaining -= absorbed;
    }

    if (remaining <= 0) {
        return false;
    }

    currentHealth_ -= remaining;
    return currentHealth_ <= 0;
}

int Hero::gainExperience(int amount) {
    if (amount <= 0) {
        return 0;
    }

    experience_ += amount;
    int levelsGained = 0;
    while (experience_ >= experienceToNextLevel_) {
        experience_ -= experienceToNextLevel_;
        ++level_;
        ++levelsGained;
        applyLevelUp();
        experienceToNextLevel_ = computeExperienceForLevel(level_);
    }
    return levelsGained;
}

std::string Hero::summary() const {
    std::ostringstream out;
    out << name_ << " - "
        << "HP: " << currentHealth_ << "/" << maxHealth_ << " (" << currentShield_ << " shield), "
        << "ATK: " << attack_ << ", DEF: " << defense_ << ", Gold: " << gold_ << ", Income: " << income_;
    return out.str();
}

void Hero::recalculateStats() {
    const int totalStrength = getStrength();
    maxHealth_ = baseMaxHealth_ + static_cast<int>(std::round(totalStrength * HEALTH_PER_STRENGTH));
    attack_ = baseAttack_ + itemAttackBonus_ + calculatePrimaryDamageBonus();
    defense_ = baseDefense_ + itemDefenseBonus_ + static_cast<int>(std::round(totalStrength * DEFENSE_PER_STRENGTH));
    if (currentHealth_ > maxHealth_) {
        currentHealth_ = maxHealth_;
    }
    const int maxShield = getMaxEnergyShield();
    if (currentShield_ > maxShield) {
        currentShield_ = maxShield;
    }
}

int Hero::calculatePrimaryDamageBonus() const {
    return static_cast<int>(std::round(getPrimaryAttributeValue() * PRIMARY_DAMAGE_RATIO));
}

int Hero::getPrimaryAttributeValue() const {
    switch (primaryAttribute_) {
        case PrimaryAttribute::Strength:
            return getStrength();
        case PrimaryAttribute::Dexterity:
            return getDexterity();
        case PrimaryAttribute::Intelligence:
        default:
            return getIntelligence();
    }
}

void Hero::applyLevelUp() {
    switch (primaryAttribute_) {
        case PrimaryAttribute::Strength:
            strength_ += PRIMARY_ATTRIBUTE_LEVEL_GAIN;
            dexterity_ += SECONDARY_ATTRIBUTE_LEVEL_GAIN;
            intelligence_ += SECONDARY_ATTRIBUTE_LEVEL_GAIN;
            break;
        case PrimaryAttribute::Dexterity:
            dexterity_ += PRIMARY_ATTRIBUTE_LEVEL_GAIN;
            strength_ += SECONDARY_ATTRIBUTE_LEVEL_GAIN;
            intelligence_ += SECONDARY_ATTRIBUTE_LEVEL_GAIN;
            break;
        case PrimaryAttribute::Intelligence:
        default:
            intelligence_ += PRIMARY_ATTRIBUTE_LEVEL_GAIN;
            strength_ += SECONDARY_ATTRIBUTE_LEVEL_GAIN;
            dexterity_ += SECONDARY_ATTRIBUTE_LEVEL_GAIN;
            break;
    }
    recalculateStats();
    currentHealth_ = maxHealth_;
    currentShield_ = getMaxEnergyShield();
}

int Hero::computeExperienceForLevel(int newLevel) const {
    return 120 + std::max(0, newLevel - 1) * 60;
}

}  // namespace herolinewars
