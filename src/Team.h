#pragma once

#include "Hero.h"
#include "UnitType.h"

#include <string>
#include <vector>

namespace herolinewars {

class Team {
public:
    Team(std::string name, Hero hero);

    const std::string& getName() const { return name_; }
    Hero& getHero() { return hero_; }
    const Hero& getHero() const { return hero_; }

    int getBaseHealth() const { return baseHealth_; }
    void damageBase(int amount);
    bool isDefeated() const { return baseHealth_ <= 0; }

    void queueUnit(UnitId id);
    std::vector<UnitId> drainQueuedUnits();
    std::vector<UnitId> getQueuedUnitsSnapshot() const { return nextWaveUnits_; }
    bool hasQueuedUnits() const { return !nextWaveUnits_.empty(); }

private:
    std::string name_;
    Hero hero_;
    std::vector<UnitId> nextWaveUnits_;
    int baseHealth_ = 100;
};

}  // namespace herolinewars
