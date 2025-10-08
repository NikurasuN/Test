#pragma once

#include <string>
#include <vector>

namespace herolinewars {

enum class EquipmentSlot {
    None,
    Helmet,
    Chestplate,
    Weapon,
    Shield,
    Accessory,
    Ring
};

bool isUniqueSlot(EquipmentSlot slot);
std::string slotDisplayName(EquipmentSlot slot);

class Item {
public:
    Item() = default;
    Item(std::string name,
         int attackBonus,
         int defenseBonus,
         int cost,
         std::string description,
         EquipmentSlot slot,
         int strengthBonus,
         int dexterityBonus,
         int intelligenceBonus);

    const std::string& getName() const { return name_; }
    int getAttackBonus() const { return attackBonus_; }
    int getDefenseBonus() const { return defenseBonus_; }
    int getCost() const { return cost_; }
    const std::string& getDescription() const { return description_; }
    EquipmentSlot getSlot() const { return slot_; }
    int getStrengthBonus() const { return strengthBonus_; }
    int getDexterityBonus() const { return dexterityBonus_; }
    int getIntelligenceBonus() const { return intelligenceBonus_; }

    std::string summary() const;

private:
    std::string name_;
    int attackBonus_ = 0;
    int defenseBonus_ = 0;
    int cost_ = 0;
    std::string description_;
    EquipmentSlot slot_ = EquipmentSlot::None;
    int strengthBonus_ = 0;
    int dexterityBonus_ = 0;
    int intelligenceBonus_ = 0;
};

std::vector<Item> defaultShopItems();

}  // namespace herolinewars
