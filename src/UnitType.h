#pragma once

#include <string>
#include <vector>

namespace herolinewars {

enum class UnitId {
    Scout,
    Soldier,
    Archer,
    Knight,
    SiegeGolem
};

struct UnitBlueprint {
    UnitId id;
    std::string name;
    int cost;
    int health;
    int damage;
    int incomeBonus;
    int range;
    std::string description;
};

const std::vector<UnitBlueprint>& getAvailableUnits();
const UnitBlueprint& getUnitBlueprint(UnitId id);

}  // namespace herolinewars
