#include "Team.h"

#include <algorithm>

namespace herolinewars {

Team::Team(std::string name, Hero hero)
    : name_(std::move(name)), hero_(std::move(hero)) {}

void Team::damageBase(int amount) {
    baseHealth_ = std::max(0, baseHealth_ - std::max(0, amount));
}

void Team::queueUnit(UnitId id) { nextWaveUnits_.push_back(id); }

std::vector<UnitId> Team::drainQueuedUnits() {
    std::vector<UnitId> copy = nextWaveUnits_;
    nextWaveUnits_.clear();
    return copy;
}

}  // namespace herolinewars
