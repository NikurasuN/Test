# Hero Line Wars (Swing Arena)

This project contains a lightweight real-time interpretation of the classic Hero Line Wars custom maps. It now ships with a small Swing battlefield so you can move your hero with the mouse and watch the duel unfold without relying on console prompts.

## Gameplay overview

- Choose between three hero archetypes (Ranger, Berserker, or Mage), each with distinct attack ranges and play styles.
- Click anywhere on the lane to command your hero to move. Heroes can only change lanes by returning to the illuminated zone in front of their base, where fortified walls open briefly to let them cross.
- Earn experience by defeating units and heroes to level up, gaining larger boosts to your primary attribute than your secondary stats.
- Defeat the enemy hero to create openings that let you damage their base. Be careful—if you fall, the enemy will march toward yours!
- Bases lose health when exposed; the match ends when one base is destroyed.

## Building and running

This project is a plain Java application without external dependencies.

```bash
# Compile all sources
javac -d out $(find src -name "*.java")

# Run the game
java -cp out com.example.herolinewars.HeroLineWarsGame
```

When the application launches, a hero selection dialog appears. After choosing your hero, the battle begins instantly inside the Swing window.

### Creating a Windows `.exe`

If you have access to a Windows machine with JDK 17 or later installed, you can produce a native launcher using the provided PowerShell helper:

```powershell
pwsh scripts/create-windows-exe.ps1
```

By default the script expects `JAVA_HOME` to point to your JDK. It compiles the sources, packages them into a runnable JAR, and then invokes `jpackage` to emit `HeroLineWarsGame.exe` inside `dist\windows`. You can override the build or output directories via the script parameters (`-BuildDir`, `-OutputDir`, `-AppVersion`) if required.
