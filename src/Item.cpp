#include "Item.h"

#include <sstream>

namespace herolinewars {

namespace {
std::string buildStatList(const Item& item) {
    std::ostringstream stream;
    auto appendStat = [&stream](const std::string& text) {
        if (!text.empty()) {
            if (stream.tellp() > 0) {
                stream << ", ";
            }
            stream << text;
        }
    };

    if (item.getAttackBonus() != 0) {
        appendStat("+" + std::to_string(item.getAttackBonus()) + " ATK");
    }
    if (item.getDefenseBonus() != 0) {
        appendStat("+" + std::to_string(item.getDefenseBonus()) + " DEF");
    }
    if (item.getStrengthBonus() != 0) {
        appendStat("+" + std::to_string(item.getStrengthBonus()) + " STR");
    }
    if (item.getDexterityBonus() != 0) {
        appendStat("+" + std::to_string(item.getDexterityBonus()) + " DEX");
    }
    if (item.getIntelligenceBonus() != 0) {
        appendStat("+" + std::to_string(item.getIntelligenceBonus()) + " INT");
    }

    if (stream.tellp() <= 0) {
        stream << "No bonuses";
    }

    return stream.str();
}
}  // namespace

Item::Item(std::string name,
           int attackBonus,
           int defenseBonus,
           int cost,
           std::string description,
           EquipmentSlot slot,
           int strengthBonus,
           int dexterityBonus,
           int intelligenceBonus)
    : name_(std::move(name)),
      attackBonus_(attackBonus),
      defenseBonus_(defenseBonus),
      cost_(cost),
      description_(std::move(description)),
      slot_(slot),
      strengthBonus_(strengthBonus),
      dexterityBonus_(dexterityBonus),
      intelligenceBonus_(intelligenceBonus) {}

std::string Item::summary() const {
    std::ostringstream builder;
    builder << name_ << " (Cost: " << cost_ << ") " << buildStatList(*this);
    if (slot_ != EquipmentSlot::None) {
        builder << " [" << slotDisplayName(slot_) << "]";
    }
    builder << " - " << description_;
    return builder.str();
}

bool isUniqueSlot(EquipmentSlot slot) {
    switch (slot) {
        case EquipmentSlot::Helmet:
        case EquipmentSlot::Chestplate:
        case EquipmentSlot::Weapon:
        case EquipmentSlot::Shield:
            return true;
        case EquipmentSlot::Accessory:
        case EquipmentSlot::Ring:
        case EquipmentSlot::None:
        default:
            return false;
    }
}

std::string slotDisplayName(EquipmentSlot slot) {
    switch (slot) {
        case EquipmentSlot::Helmet:
            return "Helmet";
        case EquipmentSlot::Chestplate:
            return "Chestplate";
        case EquipmentSlot::Weapon:
            return "Weapon";
        case EquipmentSlot::Shield:
            return "Shield";
        case EquipmentSlot::Accessory:
            return "Accessory";
        case EquipmentSlot::Ring:
            return "Ring";
        case EquipmentSlot::None:
        default:
            return "None";
    }
}

std::vector<Item> defaultShopItems() {
    return {
        Item("Sharpened Arrows", 6, 0, 85,
             "Lightweight arrowheads that increase ranged damage.", EquipmentSlot::None, 0, 0, 0),
        Item("Bulwark Shield", 0, 6, 90,
             "Sturdy shield that absorbs blows.", EquipmentSlot::Shield, 1, 0, 0),
        Item("War Banner", 4, 3, 110,
             "Rallying banner granting balanced power.", EquipmentSlot::Accessory, 1, 1, 0),
        Item("Arcane Tome", 9, 0, 140,
             "Magical tome that empowers offensive spells.", EquipmentSlot::Weapon, 0, 0, 2),
        Item("Guardian Armor", 0, 9, 150,
             "Heavy armor that keeps you standing longer.", EquipmentSlot::Chestplate, 1, 0, 0),
        Item("Heroic Relic", 6, 6, 185,
             "Relic of old heroes granting all-around strength.", EquipmentSlot::Accessory, 2, 2, 2),
        Item("Steel Helm", 0, 4, 160,
             "Fortified helmet that hardens resolve.", EquipmentSlot::Helmet, 2, 0, 0),
        Item("Knight's Chestplate", 0, 7, 210,
             "Immovable armor that protects the torso.", EquipmentSlot::Chestplate, 3, 0, 0),
        Item("Ring of Fortitude", 0, 0, 125,
             "Enchanted band that bolsters physical might.", EquipmentSlot::Ring, 3, 0, 0),
        Item("Ring of Swiftness", 0, 0, 125,
             "A nimble ring that heightens agility.", EquipmentSlot::Ring, 0, 3, 0),
        Item("Ring of Insight", 0, 0, 125,
             "A crystalline ring that sharpens arcane focus.", EquipmentSlot::Ring, 0, 0, 3),
    };
}

}  // namespace herolinewars
