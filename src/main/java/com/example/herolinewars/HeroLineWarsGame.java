package com.example.herolinewars;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

/**
 * A small console adaptation of the classic Hero Line Wars custom map.
 */
public class HeroLineWarsGame {
    private final Scanner scanner = new Scanner(System.in);
    private final Random random = new Random();

    private final List<Item> shopItems = List.of(
            new Item("Sharpened Arrows", 6, 0, 75, "Improves your hero's ranged attacks."),
            new Item("Steel Shield", 0, 5, 70, "Extra protection against incoming waves."),
            new Item("Mystic Tome", 4, 2, 65, "Knowledge that increases both attack and defense."),
            new Item("War Banner", 3, 3, 55, "A balanced boost for any hero."),
            new Item("Champion's Blade", 9, 0, 110, "High risk, high reward damage upgrade.")
    );

    public static void main(String[] args) {
        new HeroLineWarsGame().run();
    }

    private void run() {
        printIntro();

        Hero playerHero = chooseHero();
        Hero aiHero = createAiHero();

        Team playerTeam = new Team("Player", playerHero);
        Team aiTeam = new Team("Legion", aiHero);

        int round = 0;
        while (!playerTeam.isDefeated() && !aiTeam.isDefeated() && round < 30) {
            round++;
            System.out.println();
            System.out.println("==============================");
            System.out.println("Round " + round);
            System.out.println("==============================");

            startOfRoundIncome(playerTeam, aiTeam);
            playerPlanningPhase(playerTeam);
            aiPlanningPhase(aiTeam, round);

            List<UnitType> playerWave = playerTeam.drainQueuedUnits();
            List<UnitType> aiWave = aiTeam.drainQueuedUnits();

            resolveWave("Player", playerWave, aiTeam);
            resolveWave(aiTeam.getName(), aiWave, playerTeam);

            printRoundSummary(playerTeam, aiTeam);
        }

        if (playerTeam.isDefeated() && aiTeam.isDefeated()) {
            System.out.println("Both bases fell at the same time! It's a draw.");
        } else if (aiTeam.isDefeated()) {
            System.out.println("Congratulations! You defended your base and destroyed the opposing legion.");
        } else if (playerTeam.isDefeated()) {
            System.out.println("Your base was overwhelmed. Better luck next time!");
        } else {
            System.out.println("Time is up! The stronger economy wins...");
            if (playerTeam.getHero().getIncome() >= aiTeam.getHero().getIncome()) {
                System.out.println("Your team generated more income and is declared the winner!");
            } else {
                System.out.println("The legion amassed the greater economy. They win this time.");
            }
        }
    }

    private void printIntro() {
        System.out.println("Welcome to Hero Line Wars (Java Edition)!");
        System.out.println("You control a single hero on the upper lane, purchase items from the shop, \n" +
                "and send units to the opposing base on the lower lane. Every unit you send increases\n" +
                "your income, which is paid out at the start of each round.");
        System.out.println("Hold your lane, build a stronger economy than your opponent, and destroy their base!");
    }

    private void startOfRoundIncome(Team playerTeam, Team aiTeam) {
        playerTeam.getHero().earnIncome();
        aiTeam.getHero().earnIncome();
        System.out.printf("Income received! You now have %d gold (Income: %d).%n", playerTeam.getHero().getGold(), playerTeam.getHero().getIncome());
    }

    private Hero chooseHero() {
        System.out.println();
        System.out.println("Choose your hero:");
        System.out.println("1) Ranger - Balanced stats and reliable damage.");
        System.out.println("2) Knight - Heavily armored and built to tank waves.");
        System.out.println("3) Battle Mage - Fragile but deals heavy attacks.");

        while (true) {
            int choice = readInt("Enter hero number: ", 1, 3);
            switch (choice) {
                case 1:
                    return new Hero("Ranger", 95, 16, 4, 120, 50);
                case 2:
                    return new Hero("Knight", 125, 12, 7, 120, 50);
                case 3:
                    return new Hero("Battle Mage", 80, 20, 3, 120, 55);
                default:
                    System.out.println("Invalid option. Try again.");
            }
        }
    }

    private Hero createAiHero() {
        int roll = random.nextInt(3);
        switch (roll) {
            case 0:
                System.out.println("The opposing legion fields a disciplined Sentinel.");
                return new Hero("Sentinel", 110, 14, 6, 120, 52);
            case 1:
                System.out.println("The opposing legion fields a ferocious Berserker.");
                return new Hero("Berserker", 85, 19, 4, 120, 54);
            default:
                System.out.println("The opposing legion fields an arcane Warlock.");
                return new Hero("Warlock", 90, 17, 5, 120, 50);
        }
    }

    private void playerPlanningPhase(Team playerTeam) {
        Hero hero = playerTeam.getHero();
        boolean planning = true;
        while (planning) {
            System.out.println();
            System.out.println("--- Planning Phase ---");
            System.out.println(hero);
            System.out.println("1) View inventory");
            System.out.println("2) Buy item from shop");
            System.out.println("3) Send units to the enemy base");
            System.out.println("4) Review next wave composition");
            System.out.println("5) Finish planning for this round");

            int choice = readInt("Choose an action: ", 1, 5);
            switch (choice) {
                case 1:
                    displayInventory(hero);
                    break;
                case 2:
                    buyItem(hero);
                    break;
                case 3:
                    sendUnits(playerTeam);
                    break;
                case 4:
                    previewWave(playerTeam);
                    break;
                case 5:
                    planning = false;
                    break;
                default:
                    // Should not happen
                    break;
            }
        }
    }

    private void displayInventory(Hero hero) {
        System.out.println();
        System.out.println(hero.getName() + "'s Inventory:");
        if (hero.getInventory().isEmpty()) {
            System.out.println("  (empty)");
        } else {
            for (Item item : hero.getInventory()) {
                System.out.println("  - " + item);
            }
        }
    }

    private void buyItem(Hero hero) {
        System.out.println();
        System.out.println("--- Shop ---");
        for (int i = 0; i < shopItems.size(); i++) {
            Item item = shopItems.get(i);
            System.out.printf("%d) %s%n", i + 1, item);
        }
        System.out.println("0) Leave the shop");

        int choice = readInt("Select an item to purchase: ", 0, shopItems.size());
        if (choice == 0) {
            return;
        }
        Item selected = shopItems.get(choice - 1);
        if (hero.spendGold(selected.getCost())) {
            hero.applyItem(selected);
            System.out.printf("Purchased %s! New stats -> ATK: %d, DEF: %d%n", selected.getName(), hero.getAttack(), hero.getDefense());
        } else {
            System.out.println("Not enough gold for that item.");
        }
    }

    private void sendUnits(Team team) {
        Hero hero = team.getHero();
        System.out.println();
        System.out.println("--- Send Units ---");
        UnitType[] options = UnitType.values();
        for (int i = 0; i < options.length; i++) {
            UnitType type = options[i];
            System.out.printf("%d) %s - Cost: %d, HP: %d, DMG: %d, Income +%d%n   %s%n",
                    i + 1, type.getDisplayName(), type.getCost(), type.getHealth(), type.getDamage(),
                    type.getIncomeBonus(), type.getDescription());
        }
        System.out.println("0) Finish sending units");

        while (true) {
            int choice = readInt("Select a unit to send: ", 0, options.length);
            if (choice == 0) {
                break;
            }
            UnitType selected = options[choice - 1];
            int quantity = readInt("How many " + selected.getDisplayName() + "s? ", 1, 20);
            int totalCost = selected.getCost() * quantity;
            if (hero.getGold() < totalCost) {
                System.out.println("Not enough gold to send that many units.");
                continue;
            }
            hero.spendGold(totalCost);
            hero.addIncome(selected.getIncomeBonus() * quantity);
            for (int i = 0; i < quantity; i++) {
                team.queueUnit(selected);
            }
            System.out.printf("Queued %d %s for the next wave. Income is now %d.%n", quantity, selected.getDisplayName(), hero.getIncome());
        }
    }

    private void previewWave(Team team) {
        List<UnitType> queuedUnits = team.getQueuedUnitsSnapshot();
        if (queuedUnits.isEmpty()) {
            System.out.println("No units queued yet for the next wave.");
            return;
        }

        Map<UnitType, Integer> counts = new EnumMap<>(UnitType.class);
        for (UnitType type : UnitType.values()) {
            counts.put(type, 0);
        }
        for (UnitType queued : queuedUnits) {
            counts.put(queued, counts.get(queued) + 1);
        }

        System.out.println("Units ready for the next wave:");
        for (Map.Entry<UnitType, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > 0) {
                System.out.printf("  %s x%d%n", entry.getKey().getDisplayName(), entry.getValue());
            }
        }
    }

    private void aiPlanningPhase(Team aiTeam, int round) {
        Hero hero = aiTeam.getHero();
        System.out.println();
        System.out.println("The opposing legion is preparing its strategy...");

        // Simple AI: occasionally buy an item
        if (hero.getGold() >= 80 && hero.getInventory().size() < 4 && random.nextBoolean()) {
            Item chosen = shopItems.get(random.nextInt(shopItems.size()));
            if (hero.spendGold(chosen.getCost())) {
                hero.applyItem(chosen);
                System.out.printf("The legion equips %s for their hero.%n", chosen.getName());
            }
        }

        int minCost = getMinimumUnitCost();
        int unitsToSend = Math.max(1, round / 2);
        while (hero.getGold() >= minCost && unitsToSend > 0) {
            UnitType type = chooseUnitForAi(hero, round);
            if (!hero.spendGold(type.getCost())) {
                break;
            }
            aiTeam.queueUnit(type);
            hero.addIncome(type.getIncomeBonus());
            unitsToSend--;
            System.out.printf("The legion queues a %s for the coming wave.%n", type.getDisplayName());
        }
    }

    private UnitType chooseUnitForAi(Hero hero, int round) {
        UnitType[] types = UnitType.values();
        if (round < 4) {
            return types[random.nextInt(Math.min(types.length, 3))];
        }
        if (hero.getIncome() > 120) {
            return types[types.length - 1];
        }
        return types[random.nextInt(types.length)];
    }

    private int getMinimumUnitCost() {
        int min = Integer.MAX_VALUE;
        for (UnitType type : UnitType.values()) {
            min = Math.min(min, type.getCost());
        }
        return min;
    }

    private void resolveWave(String attackerName, List<UnitType> wave, Team defenderTeam) {
        Hero defender = defenderTeam.getHero();
        if (wave.isEmpty()) {
            System.out.printf("%s sends no units this round.%n", attackerName);
            return;
        }

        defender.resetHealth();
        int goldEarned = 0;
        boolean heroFell = false;
        int baseDamage = 0;
        int unitsProcessed = 0;

        for (int i = 0; i < wave.size(); i++) {
            UnitType unit = wave.get(i);
            int unitHealth = unit.getHealth();
            while (unitHealth > 0) {
                unitHealth -= defender.getAttack();
                if (unitHealth <= 0) {
                    goldEarned += 8;
                    break;
                }
                int damageToHero = Math.max(1, unit.getDamage() - defender.getDefense());
                if (defender.takeDamage(damageToHero)) {
                    heroFell = true;
                    baseDamage += Math.max(6, unit.getDamage());
                    unitsProcessed = i + 1;
                    break;
                }
            }
            if (heroFell) {
                break;
            }
            unitsProcessed = i + 1;
        }

        if (heroFell) {
            for (int i = unitsProcessed; i < wave.size(); i++) {
                UnitType remaining = wave.get(i);
                baseDamage += Math.max(6, remaining.getDamage());
            }
            defenderTeam.damageBase(baseDamage);
            System.out.printf("%s's hero was overwhelmed! The base takes %d damage (Base HP: %d).%n",
                    defenderTeam.getName(), baseDamage, Math.max(0, defenderTeam.getBaseHealth()));
        } else {
            int survivingHealth = defender.getCurrentHealth();
            System.out.printf("%s defended the wave with %d HP remaining and earned %d bonus gold.%n",
                    defender.getName(), survivingHealth, goldEarned);
            defender.addGold(goldEarned);
        }
        defender.resetHealth();
    }

    private void printRoundSummary(Team playerTeam, Team aiTeam) {
        System.out.println();
        System.out.println("--- Round Summary ---");
        System.out.printf("Your base HP: %d%n", Math.max(0, playerTeam.getBaseHealth()));
        System.out.printf("Enemy base HP: %d%n", Math.max(0, aiTeam.getBaseHealth()));
        System.out.printf("Your hero income: %d, Gold: %d%n", playerTeam.getHero().getIncome(), playerTeam.getHero().getGold());
        System.out.printf("Enemy hero income: %d%n", aiTeam.getHero().getIncome());
    }

    private int readInt(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine();
            try {
                int value = Integer.parseInt(line.trim());
                if (value < min || value > max) {
                    System.out.printf("Please enter a value between %d and %d.%n", min, max);
                } else {
                    return value;
                }
            } catch (NumberFormatException ex) {
                System.out.println("Please enter a valid number.");
            }
        }
    }
}
