#include "Hero.h"
#include "HeroLineWarsGame.h"
#include "IconLibrary.h"
#include "Item.h"
#include "Team.h"
#include "UnitType.h"

#include <algorithm>
#include <cctype>
#include <iostream>
#include <map>
#include <random>
#include <sstream>
#include <string>

namespace herolinewars {
namespace {
int parseInt(const std::string& text, int fallback = -1) {
    try {
        return std::stoi(text);
    } catch (...) {
        return fallback;
    }
}

std::mt19937& globalRng() {
    static std::mt19937 rng(std::random_device{}());
    return rng;
}

struct WaveTotals {
    int health = 0;
    int damage = 0;
    int unitCount = 0;
};

WaveTotals aggregateWaveStats(Hero& hero, const std::vector<UnitId>& units) {
    WaveTotals totals;
    totals.health = hero.getMaxHealth();
    totals.damage = hero.getAttackPower();

    for (UnitId id : units) {
        const UnitBlueprint& blueprint = getUnitBlueprint(id);
        totals.health += blueprint.health;
        totals.damage += blueprint.damage;
        ++totals.unitCount;
    }

    return totals;
}

std::map<UnitId, int> countUnits(const std::vector<UnitId>& units) {
    std::map<UnitId, int> counts;
    for (UnitId id : units) {
        ++counts[id];
    }
    return counts;
}

}  // namespace

class HeroLineWarsGame {
public:
    void run();

private:
    static constexpr int ATTRIBUTE_UPGRADE_COST = 120;
    static constexpr int UNIT_KILL_REWARD = 6;
    static constexpr int EXPERIENCE_PER_UNIT_KILL = 18;
    static constexpr int EXPERIENCE_PER_HERO_KILL = 150;
    static constexpr int BASE_DAMAGE_MIN = 8;
    static constexpr int MAX_WAVES = 12;

    std::vector<Item> shopItems_ = defaultShopItems();

    Hero selectHero();
    Hero createRanger() const;
    Hero createBerserker() const;
    Hero createMage() const;
    Hero chooseOpponent(Hero::PrimaryAttribute preferred) const;

    void playerPreparationPhase(Team& team);
    void aiPreparationPhase(int waveNumber, Team& aiTeam);
    void purchaseItem(Team& team);
    void upgradeAttributeInteraction(Team& team);
    void queueUnitInteraction(Team& team);
    void showInventory(const Hero& hero) const;
    void summariseQueue(const Team& team) const;

    void resolveWave(int waveNumber, Team& playerTeam, Team& enemyTeam);
    void printBanner(const std::string& text) const;
};

void HeroLineWarsGame::run() {
    std::cout << "==============================\n";
    std::cout << "   Hero Line Wars - C++ Duel\n";
    std::cout << "==============================\n\n";
    std::cout << "Select your champion and march down the lane!\n";

    Hero playerHero = selectHero();
    Hero aiHero = chooseOpponent(playerHero.getPrimaryAttribute());

    Team playerTeam("Vanguard", std::move(playerHero));
    Team enemyTeam("Legion", std::move(aiHero));

    for (int wave = 1; wave <= MAX_WAVES; ++wave) {
        if (playerTeam.isDefeated() || enemyTeam.isDefeated()) {
            break;
        }

        std::cout << "\n=== Wave " << wave << " Preparation ===\n";

        playerTeam.getHero().earnIncome();
        enemyTeam.getHero().earnIncome();

        playerPreparationPhase(playerTeam);
        aiPreparationPhase(wave, enemyTeam);

        resolveWave(wave, playerTeam, enemyTeam);
    }

    std::cout << "\n==============================\n";
    if (playerTeam.isDefeated() && enemyTeam.isDefeated()) {
        std::cout << "Both bases collapsed. It's a draw!\n";
    } else if (enemyTeam.isDefeated()) {
        std::cout << "Victory! " << playerTeam.getName() << " routed the opposing forces.\n";
    } else if (playerTeam.isDefeated()) {
        std::cout << "Defeat. The enemy breached your citadel.\n";
    } else {
        std::cout << "The duel ends in a stalemate after " << MAX_WAVES << " waves.\n";
    }
}

Hero HeroLineWarsGame::selectHero() {
    std::vector<Hero> options;
    options.push_back(createRanger());
    options.push_back(createBerserker());
    options.push_back(createMage());

    while (true) {
        std::cout << "\nChoose your hero:\n";
        for (std::size_t i = 0; i < options.size(); ++i) {
            const Hero& hero = options[i];
            std::cout << (i + 1) << ") " << hero.getName() << " - " << hero.summary() << '\n';
            std::cout << IconLibrary::heroGlyph(hero.getName()) << "\n\n";
        }
        std::cout << "Enter a number between 1 and " << options.size() << ": ";
        std::string input;
        std::getline(std::cin, input);
        int choice = parseInt(input);
        if (choice >= 1 && static_cast<std::size_t>(choice) <= options.size()) {
            std::cout << "You chose " << options[choice - 1].getName() << "!\n";
            return options[choice - 1];
        }
        std::cout << "Invalid selection. Try again.\n";
    }
}

Hero HeroLineWarsGame::createRanger() const {
    return Hero("Ranger", 180, 40, 6, 12, 15, 9, Hero::PrimaryAttribute::Dexterity, 420, 24, 5);
}

Hero HeroLineWarsGame::createBerserker() const {
    return Hero("Berserker", 220, 48, 8, 16, 10, 6, Hero::PrimaryAttribute::Strength, 420, 24, 2);
}

Hero HeroLineWarsGame::createMage() const {
    return Hero("Battle Mage", 170, 44, 5, 11, 11, 16, Hero::PrimaryAttribute::Intelligence, 420, 24, 6);
}

Hero HeroLineWarsGame::chooseOpponent(Hero::PrimaryAttribute preferred) const {
    std::vector<Hero> options = {createRanger(), createBerserker(), createMage()};
    std::uniform_int_distribution<std::size_t> pick(0, options.size() - 1);
    for (int attempt = 0; attempt < 6; ++attempt) {
        const Hero& candidate = options[pick(globalRng())];
        if (candidate.getPrimaryAttribute() != preferred) {
            std::cout << "Your opponent selected the " << candidate.getName() << ".\n";
            return candidate;
        }
    }
    const Hero& fallback = options[pick(globalRng())];
    std::cout << "Your opponent mirrors your strategy!\n";
    return fallback;
}

void HeroLineWarsGame::playerPreparationPhase(Team& team) {
    bool ready = false;
    while (!ready) {
        std::cout << "\n" << team.getHero().summary() << '\n';
        summariseQueue(team);
        std::cout << "\nSelect an action:\n";
        std::cout << " 1) Buy item\n";
        std::cout << " 2) Train attribute (+1 for " << ATTRIBUTE_UPGRADE_COST << " gold)\n";
        std::cout << " 3) Recruit units\n";
        std::cout << " 4) Review inventory\n";
        std::cout << " 5) Ready for battle\n> ";
        std::string input;
        std::getline(std::cin, input);
        if (input == "1") {
            purchaseItem(team);
        } else if (input == "2") {
            upgradeAttributeInteraction(team);
        } else if (input == "3") {
            queueUnitInteraction(team);
        } else if (input == "4") {
            showInventory(team.getHero());
        } else if (input == "5") {
            ready = true;
        } else {
            std::cout << "Unknown option.\n";
        }
    }
}

void HeroLineWarsGame::aiPreparationPhase(int waveNumber, Team& aiTeam) {
    Hero& hero = aiTeam.getHero();
    std::uniform_real_distribution<double> chance(0.0, 1.0);

    if (hero.getGold() >= 140 && chance(globalRng()) < 0.35) {
        std::vector<Item> affordable;
        for (const Item& item : shopItems_) {
            if (item.getCost() <= hero.getGold()) {
                affordable.push_back(item);
            }
        }
        if (!affordable.empty()) {
            std::uniform_int_distribution<std::size_t> pick(0, affordable.size() - 1);
            const Item& selection = affordable[pick(globalRng())];
            if (hero.spendGold(selection.getCost())) {
                if (hero.applyItem(selection)) {
                    std::cout << "The enemy equips " << selection.getName() << ".\n";
                } else {
                    hero.addGold(selection.getCost());
                }
            }
        }
    }

    const auto& units = getAvailableUnits();
    int attempts = 0;
    while (attempts < 30) {
        ++attempts;
        std::uniform_int_distribution<std::size_t> pick(0, units.size() - 1);
        const UnitBlueprint& blueprint = units[pick(globalRng())];
        if (blueprint.cost > hero.getGold()) {
            continue;
        }
        hero.spendGold(blueprint.cost);
        hero.addIncome(blueprint.incomeBonus);
        aiTeam.queueUnit(blueprint.id);
    }

    if (!aiTeam.hasQueuedUnits()) {
        const UnitBlueprint& cheapest = units.front();
        if (cheapest.cost <= hero.getGold()) {
            hero.spendGold(cheapest.cost);
            hero.addIncome(cheapest.incomeBonus);
            aiTeam.queueUnit(cheapest.id);
        }
    }

    std::cout << "The enemy rallies " << aiTeam.getQueuedUnitsSnapshot().size() << " units for the wave.\n";
}

void HeroLineWarsGame::purchaseItem(Team& team) {
    Hero& hero = team.getHero();
    std::cout << "\n=== Armory ===\n";
    for (std::size_t i = 0; i < shopItems_.size(); ++i) {
        std::cout << (i + 1) << ") " << shopItems_[i].summary() << '\n';
    }
    std::cout << "Enter item number to purchase (0 to cancel): ";
    std::string input;
    std::getline(std::cin, input);
    int choice = parseInt(input);
    if (choice <= 0 || static_cast<std::size_t>(choice) > shopItems_.size()) {
        std::cout << "Leaving the armory.\n";
        return;
    }

    Item selection = shopItems_[choice - 1];
    if (!hero.spendGold(selection.getCost())) {
        std::cout << "Not enough gold for " << selection.getName() << ".\n";
        return;
    }
    if (!hero.applyItem(selection)) {
        std::cout << "Could not equip " << selection.getName() << ". Slot requirements not met.\n";
        hero.addGold(selection.getCost());
        return;
    }
    std::cout << "Equipped " << selection.getName() << "!\n";
}

void HeroLineWarsGame::upgradeAttributeInteraction(Team& team) {
    Hero& hero = team.getHero();
    if (hero.getGold() < ATTRIBUTE_UPGRADE_COST) {
        std::cout << "You need " << ATTRIBUTE_UPGRADE_COST << " gold to train.\n";
        return;
    }

    std::cout << "Train which attribute? (S)trength, (D)exterity, (I)ntelligence: ";
    std::string input;
    std::getline(std::cin, input);
    if (input.empty()) {
        std::cout << "Training cancelled.\n";
        return;
    }
    char c = static_cast<char>(std::toupper(input[0]));
    Hero::PrimaryAttribute attribute;
    switch (c) {
        case 'S':
            attribute = Hero::PrimaryAttribute::Strength;
            break;
        case 'D':
            attribute = Hero::PrimaryAttribute::Dexterity;
            break;
        case 'I':
            attribute = Hero::PrimaryAttribute::Intelligence;
            break;
        default:
            std::cout << "Unknown attribute.\n";
            return;
    }

    if (hero.upgradeAttribute(attribute, ATTRIBUTE_UPGRADE_COST)) {
        std::cout << "Attribute trained successfully.\n";
    } else {
        std::cout << "Training failed.\n";
    }
}

void HeroLineWarsGame::queueUnitInteraction(Team& team) {
    Hero& hero = team.getHero();
    const auto& units = getAvailableUnits();
    std::cout << "\n=== Recruitment ===\n";
    for (std::size_t i = 0; i < units.size(); ++i) {
        const auto& blueprint = units[i];
        std::cout << (i + 1) << ") " << blueprint.name << " - Cost: " << blueprint.cost
                  << ", Damage: " << blueprint.damage << ", Health: " << blueprint.health
                  << ", Income +" << blueprint.incomeBonus << '\n';
    }
    std::cout << "Enter unit number to recruit (0 to cancel): ";
    std::string input;
    std::getline(std::cin, input);
    int choice = parseInt(input);
    if (choice <= 0 || static_cast<std::size_t>(choice) > units.size()) {
        std::cout << "No units recruited.\n";
        return;
    }

    const UnitBlueprint& blueprint = units[choice - 1];
    std::cout << "How many " << blueprint.name << "? ";
    std::getline(std::cin, input);
    int quantity = std::max(1, parseInt(input, 1));
    int totalCost = blueprint.cost * quantity;
    if (hero.getGold() < totalCost) {
        std::cout << "Insufficient gold. You need " << totalCost << ".\n";
        return;
    }
    for (int i = 0; i < quantity; ++i) {
        hero.spendGold(blueprint.cost);
        hero.addIncome(blueprint.incomeBonus);
        team.queueUnit(blueprint.id);
    }
    std::cout << "Queued " << quantity << " " << blueprint.name << (quantity > 1 ? "s" : "") << ".\n";
}

void HeroLineWarsGame::showInventory(const Hero& hero) const {
    const auto& inventory = hero.getInventory();
    if (inventory.empty()) {
        std::cout << "Inventory is empty.\n";
        return;
    }
    std::cout << "\nEquipped gear:\n";
    for (const Item& item : inventory) {
        std::cout << " - " << item.summary() << '\n';
    }
}

void HeroLineWarsGame::summariseQueue(const Team& team) const {
    auto counts = countUnits(team.getQueuedUnitsSnapshot());
    if (counts.empty()) {
        std::cout << "No units queued.\n";
        return;
    }
    std::cout << "Queued units: ";
    bool first = true;
    for (const auto& entry : counts) {
        const UnitBlueprint& blueprint = getUnitBlueprint(entry.first);
        if (!first) {
            std::cout << ", ";
        }
        first = false;
        std::cout << blueprint.name << " x" << entry.second;
    }
    std::cout << '\n';
}

void HeroLineWarsGame::resolveWave(int waveNumber, Team& playerTeam, Team& enemyTeam) {
    printBanner("Wave " + std::to_string(waveNumber));
    auto playerUnits = playerTeam.drainQueuedUnits();
    auto enemyUnits = enemyTeam.drainQueuedUnits();

    Hero& playerHero = playerTeam.getHero();
    Hero& enemyHero = enemyTeam.getHero();

    playerHero.resetHealth();
    enemyHero.resetHealth();

    WaveTotals playerTotals = aggregateWaveStats(playerHero, playerUnits);
    WaveTotals enemyTotals = aggregateWaveStats(enemyHero, enemyUnits);

    int playerRemaining = playerTotals.health;
    int enemyRemaining = enemyTotals.health;

    int rounds = 0;
    while (playerRemaining > 0 && enemyRemaining > 0 && rounds < 80) {
        enemyRemaining -= playerTotals.damage;
        playerRemaining -= enemyTotals.damage;
        ++rounds;
    }

    bool playerVictory = playerRemaining > enemyRemaining;
    bool enemyVictory = enemyRemaining > playerRemaining;

    std::cout << playerTeam.getName() << " deploys " << playerUnits.size() << " units alongside the hero.\n";
    std::cout << enemyTeam.getName() << " deploys " << enemyUnits.size() << " units.\n";

    if (!playerVictory && !enemyVictory) {
        std::cout << "The wave ends in a brutal stalemate. Both bases hold.\n";
    } else if (playerVictory) {
        int damage = std::max(BASE_DAMAGE_MIN, playerTotals.damage / 10);
        enemyTeam.damageBase(damage);
        std::cout << "You push through the enemy lines and damage their base for " << damage << "!\n";
    } else {
        int damage = std::max(BASE_DAMAGE_MIN, enemyTotals.damage / 10);
        playerTeam.damageBase(damage);
        std::cout << "The enemy overwhelms your defenses for " << damage << " damage!\n";
    }

    int playerKills = static_cast<int>(enemyUnits.size());
    int enemyKills = static_cast<int>(playerUnits.size());

    playerHero.addGold(playerKills * UNIT_KILL_REWARD);
    enemyHero.addGold(enemyKills * UNIT_KILL_REWARD);

    if (playerVictory) {
        playerHero.addGold(90);
        playerHero.gainExperience(playerKills * EXPERIENCE_PER_UNIT_KILL + EXPERIENCE_PER_HERO_KILL);
        enemyHero.gainExperience(enemyKills * EXPERIENCE_PER_UNIT_KILL);
    } else if (enemyVictory) {
        enemyHero.addGold(90);
        enemyHero.gainExperience(enemyKills * EXPERIENCE_PER_UNIT_KILL + EXPERIENCE_PER_HERO_KILL);
        playerHero.gainExperience(playerKills * EXPERIENCE_PER_UNIT_KILL);
    } else {
        playerHero.gainExperience(playerKills * EXPERIENCE_PER_UNIT_KILL);
        enemyHero.gainExperience(enemyKills * EXPERIENCE_PER_UNIT_KILL);
    }

    std::cout << "Player base health: " << playerTeam.getBaseHealth() << ", Enemy base health: "
              << enemyTeam.getBaseHealth() << "\n";
}

void HeroLineWarsGame::printBanner(const std::string& text) const {
    std::cout << "\n------------------------------\n";
    std::cout << text << '\n';
    std::cout << "------------------------------\n";
}

void runGame() {
    HeroLineWarsGame game;
    game.run();
}

}  // namespace herolinewars
