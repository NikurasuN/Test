# Hero Line Wars (Java Edition)

This repository contains a small console game inspired by the classic Hero Line Wars custom maps from Warcraft III and StarCraft II. You control a hero on the upper lane, purchase items from the shop, and send units to attack the opposing base while defending your own.

## Gameplay overview

- Choose between three hero archetypes (Ranger, Knight, or Battle Mage), each with different stats.
- Earn gold from your base income at the start of every round.
- Purchase items to improve your hero's attack and defense.
- Send units to the enemy lane to pressure their base and increase your income for future rounds.
- Survive the enemy waves on your own lane. If your hero falls, the remaining units damage your base.
- The game ends when either base falls or after 30 rounds (the higher income wins the tiebreaker).

## Building and running

This project is a plain Java application without external dependencies.

```bash
# Compile all sources
javac -d out $(find src -name "*.java")

# Run the game
java -cp out com.example.herolinewars.HeroLineWarsGame
```

When running the game, follow the prompts in the terminal to plan your economy, buy items, and send units every round.
