#include "UnitType.h"

#include <stdexcept>

namespace herolinewars {

const std::vector<UnitBlueprint>& getAvailableUnits() {
    static const std::vector<UnitBlueprint> units = {
        {UnitId::Scout, "Scout", 35, 20, 6, 2, 36, "A cheap and fast unit that increases income slightly."},
        {UnitId::Soldier, "Soldier", 50, 35, 9, 3, 40, "Balanced melee fighter."},
        {UnitId::Archer, "Archer", 65, 30, 12, 4, 160, "Ranged attacker that deals reliable damage."},
        {UnitId::Knight, "Knight", 90, 55, 16, 6, 42, "Heavy unit with strong damage."},
        {UnitId::SiegeGolem, "Siege Golem", 120, 80, 25, 8, 50, "Slow but devastating against bases."},
    };
    return units;
}

const UnitBlueprint& getUnitBlueprint(UnitId id) {
    const auto& units = getAvailableUnits();
    for (const auto& blueprint : units) {
        if (blueprint.id == id) {
            return blueprint;
        }
    }
    throw std::out_of_range("Unknown unit identifier");
}

}  // namespace herolinewars
