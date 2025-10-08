using System;
using System.Collections.Generic;
using System.Globalization;
using System.Linq;

namespace HeroLineWars;

internal sealed class Game
{
    private readonly List<Item> _shopItems;
    private readonly IUserInterface _ui;
    private readonly Random _random;

    public Game(IUserInterface ui, Random random)
    {
        _ui = ui;
        _random = random;
        _shopItems = Item.CreateDefaultShopItems().ToList();
    }

    public void Run()
    {
        _ui.WriteLine("==============================");
        _ui.WriteLine("   Hero Line Wars - Terminal Duel");
        _ui.WriteLine("==============================\n");
        _ui.WriteLine("Select your champion and march down the lane!");

        var playerHero = SelectHero();
        var aiHero = ChooseOpponent(playerHero.Primary);

        var playerTeam = new Team("Vanguard", playerHero);
        var enemyTeam = new Team("Legion", aiHero);

        const int maxWaves = 12;
        for (var wave = 1; wave <= maxWaves; wave++)
        {
            if (playerTeam.IsDefeated() || enemyTeam.IsDefeated())
            {
                break;
            }

            _ui.WriteLine($"\n=== Wave {wave} Preparation ===");

            playerTeam.Hero.EarnIncome();
            enemyTeam.Hero.EarnIncome();

            PlayerPreparationPhase(playerTeam);
            AiPreparationPhase(wave, enemyTeam);

            ResolveWave(wave, playerTeam, enemyTeam);
        }

        _ui.WriteLine("\n==============================");
        if (playerTeam.IsDefeated() && enemyTeam.IsDefeated())
        {
            _ui.WriteLine("Both bases collapsed. It's a draw!");
        }
        else if (enemyTeam.IsDefeated())
        {
            _ui.WriteLine($"Victory! {playerTeam.Name} routed the opposing forces.");
        }
        else if (playerTeam.IsDefeated())
        {
            _ui.WriteLine("Defeat. The enemy breached your citadel.");
        }
        else
        {
            _ui.WriteLine($"The duel ends in a stalemate after {maxWaves} waves.");
        }
    }

    private Hero SelectHero()
    {
        var options = new List<Hero> { CreateRanger(), CreateBerserker(), CreateMage() };
        while (true)
        {
            _ui.WriteLine("\nChoose your hero:");
            for (var i = 0; i < options.Count; i++)
            {
                var hero = options[i];
                _ui.WriteLine($" {i + 1}) {hero.Name} - {hero.Summary()}");
                _ui.WriteLine(Glyphs.HeroGlyph(hero.Name));
                _ui.WriteLine();
            }

            _ui.Write($"Enter a number between 1 and {options.Count}: ");
            var choice = ReadInt();
            if (choice >= 1 && choice <= options.Count)
            {
                _ui.WriteLine($"You chose {options[choice - 1].Name}!");
                return options[choice - 1];
            }

            _ui.WriteLine("Invalid selection. Try again.");
        }
    }

    private Hero CreateRanger()
    {
        return new Hero("Ranger", 180, 40, 6, 12, 15, 9, PrimaryAttribute.Dexterity, 420, 24, 5);
    }

    private Hero CreateBerserker()
    {
        return new Hero("Berserker", 220, 48, 8, 16, 10, 6, PrimaryAttribute.Strength, 420, 24, 2);
    }

    private Hero CreateMage()
    {
        return new Hero("Battle Mage", 170, 44, 5, 11, 11, 16, PrimaryAttribute.Intelligence, 420, 24, 6);
    }

    private Hero ChooseOpponent(PrimaryAttribute preferred)
    {
        var options = new[] { CreateRanger(), CreateBerserker(), CreateMage() };
        for (var attempt = 0; attempt < 6; attempt++)
        {
            var candidate = options[_random.Next(options.Length)];
            if (candidate.Primary != preferred)
            {
                _ui.WriteLine($"Your opponent selected the {candidate.Name}.");
                return candidate;
            }
        }

        _ui.WriteLine("Your opponent mirrors your strategy!");
        return options[_random.Next(options.Length)];
    }

    private void PlayerPreparationPhase(Team team)
    {
        const int attributeUpgradeCost = 120;
        while (true)
        {
            _ui.WriteLine($"\n{team.Hero.Summary()}");
            SummariseQueue(team);
            _ui.WriteLine("\nSelect an action:");
            _ui.WriteLine(" 1) Buy item");
            _ui.WriteLine($" 2) Train attribute (+1 for {attributeUpgradeCost} gold)");
            _ui.WriteLine(" 3) Recruit units");
            _ui.WriteLine(" 4) Review inventory");
            _ui.WriteLine(" 5) Ready for battle");
            _ui.Write("> ");
            var input = ReadLine();
            switch (input)
            {
                case "1":
                    PurchaseItem(team);
                    break;
                case "2":
                    UpgradeAttributeInteraction(team, attributeUpgradeCost);
                    break;
                case "3":
                    QueueUnitInteraction(team);
                    break;
                case "4":
                    ShowInventory(team.Hero);
                    break;
                case "5":
                    return;
                default:
                    _ui.WriteLine("Unknown option.");
                    break;
            }
        }
    }

    private void AiPreparationPhase(int waveNumber, Team aiTeam)
    {
        var hero = aiTeam.Hero;
        if (hero.Gold >= 140 && _random.NextDouble() < 0.35)
        {
            var affordable = _shopItems.Where(item => item.Cost <= hero.Gold).ToList();
            if (affordable.Count > 0)
            {
                var selection = affordable[_random.Next(affordable.Count)];
                if (hero.SpendGold(selection.Cost))
                {
                    if (hero.ApplyItem(selection))
                    {
                        _ui.WriteLine($"The enemy equips {selection.Name}.");
                    }
                    else
                    {
                        hero.Gold += selection.Cost;
                    }
                }
            }
        }

        var units = UnitCatalog.All();
        var attempts = 0;
        while (attempts < 30)
        {
            attempts++;
            var blueprint = units[_random.Next(units.Count)];
            if (blueprint.Cost > hero.Gold)
            {
                continue;
            }

            hero.SpendGold(blueprint.Cost);
            hero.Income += blueprint.IncomeBonus;
            aiTeam.QueueUnit(blueprint.Id);
        }

        if (!aiTeam.HasQueuedUnits())
        {
            var cheapest = units[0];
            if (cheapest.Cost <= hero.Gold)
            {
                hero.SpendGold(cheapest.Cost);
                hero.Income += cheapest.IncomeBonus;
                aiTeam.QueueUnit(cheapest.Id);
            }
        }

        _ui.WriteLine($"The enemy rallies {aiTeam.QueuedUnitsSnapshot().Count} units for the wave.");
    }

    private void PurchaseItem(Team team)
    {
        var hero = team.Hero;
        _ui.WriteLine("\n=== Armory ===");
        for (var i = 0; i < _shopItems.Count; i++)
        {
            _ui.WriteLine($" {i + 1}) {_shopItems[i].Summary()}");
        }

        _ui.Write("Enter item number to purchase (0 to cancel): ");
        var choice = ReadInt();
        if (choice <= 0 || choice > _shopItems.Count)
        {
            _ui.WriteLine("Leaving the armory.");
            return;
        }

        var selection = _shopItems[choice - 1];
        if (!hero.SpendGold(selection.Cost))
        {
            _ui.WriteLine($"Not enough gold for {selection.Name}.");
            return;
        }

        if (!hero.ApplyItem(selection))
        {
            _ui.WriteLine($"Could not equip {selection.Name}. Slot requirements not met.");
            hero.Gold += selection.Cost;
            return;
        }

        _ui.WriteLine($"Equipped {selection.Name}!");
    }

    private void UpgradeAttributeInteraction(Team team, int cost)
    {
        var hero = team.Hero;
        if (hero.Gold < cost)
        {
            _ui.WriteLine($"You need {cost} gold to train.");
            return;
        }

        _ui.Write("Train which attribute? (S)trength, (D)exterity, (I)ntelligence: ");
        var input = ReadLine();
        if (string.IsNullOrWhiteSpace(input))
        {
            _ui.WriteLine("Training cancelled.");
            return;
        }

        switch (input.Trim().ToUpper(CultureInfo.InvariantCulture)[0])
        {
            case 'S':
                hero.UpgradeAttribute(PrimaryAttribute.Strength, cost);
                _ui.WriteLine("Attribute trained successfully.");
                break;
            case 'D':
                hero.UpgradeAttribute(PrimaryAttribute.Dexterity, cost);
                _ui.WriteLine("Attribute trained successfully.");
                break;
            case 'I':
                hero.UpgradeAttribute(PrimaryAttribute.Intelligence, cost);
                _ui.WriteLine("Attribute trained successfully.");
                break;
            default:
                _ui.WriteLine("Unknown attribute.");
                break;
        }
    }

    private void QueueUnitInteraction(Team team)
    {
        var hero = team.Hero;
        var units = UnitCatalog.All();
        _ui.WriteLine("\n=== Recruitment ===");
        for (var i = 0; i < units.Count; i++)
        {
            var blueprint = units[i];
            _ui.WriteLine(
                $" {i + 1}) {blueprint.Name} - Cost: {blueprint.Cost}, Damage: {blueprint.Damage}, Health: {blueprint.Health}, Income +{blueprint.IncomeBonus}");
        }

        _ui.Write("Enter unit number to recruit (0 to cancel): ");
        var choice = ReadInt();
        if (choice <= 0 || choice > units.Count)
        {
            _ui.WriteLine("No units recruited.");
            return;
        }

        var selected = units[choice - 1];
        _ui.Write($"How many {selected.Name}? ");
        var quantity = ReadIntWithDefault(1);
        if (quantity < 1)
        {
            quantity = 1;
        }

        var totalCost = selected.Cost * quantity;
        if (hero.Gold < totalCost)
        {
            _ui.WriteLine($"Insufficient gold. You need {totalCost}.");
            return;
        }

        for (var i = 0; i < quantity; i++)
        {
            hero.SpendGold(selected.Cost);
            hero.Income += selected.IncomeBonus;
            team.QueueUnit(selected.Id);
        }

        var plural = quantity > 1 ? "s" : string.Empty;
        _ui.WriteLine($"Queued {quantity} {selected.Name}{plural}.");
    }

    private void ShowInventory(Hero hero)
    {
        if (hero.Inventory.Count == 0)
        {
            _ui.WriteLine("Inventory is empty.");
            return;
        }

        _ui.WriteLine("\nEquipped gear:");
        foreach (var item in hero.Inventory)
        {
            _ui.WriteLine($" - {item.Summary()}");
        }
    }

    private void SummariseQueue(Team team)
    {
        var counts = new Dictionary<UnitId, int>();
        foreach (var id in team.QueuedUnitsSnapshot())
        {
            counts[id] = counts.TryGetValue(id, out var existing) ? existing + 1 : 1;
        }

        if (counts.Count == 0)
        {
            _ui.WriteLine("No units queued.");
            return;
        }

        var entries = counts
            .Select(pair => new { Blueprint = UnitCatalog.ById(pair.Key), Count = pair.Value })
            .OrderBy(entry => entry.Blueprint.Name)
            .Select(entry => $"{entry.Blueprint.Name} x{entry.Count}")
            .ToList();

        _ui.WriteLine($"Queued units: {string.Join(", ", entries)}");
    }

    private void ResolveWave(int waveNumber, Team playerTeam, Team enemyTeam)
    {
        _ui.WriteLine("\n------------------------------");
        _ui.WriteLine($"Wave {waveNumber}");
        _ui.WriteLine("------------------------------");

        var playerUnits = playerTeam.DrainQueuedUnits();
        var enemyUnits = enemyTeam.DrainQueuedUnits();

        var playerHero = playerTeam.Hero;
        var enemyHero = enemyTeam.Hero;

        playerHero.ResetHealth();
        enemyHero.ResetHealth();

        var playerTotals = AggregateWaveStats(playerHero, playerUnits);
        var enemyTotals = AggregateWaveStats(enemyHero, enemyUnits);

        var playerRemaining = playerTotals.Health;
        var enemyRemaining = enemyTotals.Health;

        var rounds = 0;
        while (playerRemaining > 0 && enemyRemaining > 0 && rounds < 80)
        {
            enemyRemaining -= playerTotals.Damage;
            playerRemaining -= enemyTotals.Damage;
            rounds++;
        }

        var playerVictory = playerRemaining > enemyRemaining;
        var enemyVictory = enemyRemaining > playerRemaining;

        _ui.WriteLine($"{playerTeam.Name} deploys {playerUnits.Count} units alongside the hero.");
        _ui.WriteLine($"{enemyTeam.Name} deploys {enemyUnits.Count} units.");

        const int baseDamageMin = 8;
        const int unitKillReward = 6;
        const int expPerUnitKill = 18;
        const int expPerHeroKill = 150;

        if (!playerVictory && !enemyVictory)
        {
            _ui.WriteLine("The wave ends in a brutal stalemate. Both bases hold.");
        }
        else if (playerVictory)
        {
            var damage = Math.Max(baseDamageMin, playerTotals.Damage / 10);
            enemyTeam.DamageBase(damage);
            _ui.WriteLine($"You push through the enemy lines and damage their base for {damage}!");
        }
        else
        {
            var damage = Math.Max(baseDamageMin, enemyTotals.Damage / 10);
            playerTeam.DamageBase(damage);
            _ui.WriteLine($"The enemy overwhelms your defenses for {damage} damage!");
        }

        var playerKills = enemyUnits.Count;
        var enemyKills = playerUnits.Count;

        playerHero.Gold += playerKills * unitKillReward;
        enemyHero.Gold += enemyKills * unitKillReward;

        if (playerVictory)
        {
            playerHero.Gold += 90;
            playerHero.GainExperience(playerKills * expPerUnitKill + expPerHeroKill);
            enemyHero.GainExperience(enemyKills * expPerUnitKill);
        }
        else if (enemyVictory)
        {
            enemyHero.Gold += 90;
            enemyHero.GainExperience(enemyKills * expPerUnitKill + expPerHeroKill);
            playerHero.GainExperience(playerKills * expPerUnitKill);
        }
        else
        {
            playerHero.GainExperience(playerKills * expPerUnitKill);
            enemyHero.GainExperience(enemyKills * expPerUnitKill);
        }

        _ui.WriteLine($"Player base health: {playerTeam.BaseHealth}, Enemy base health: {enemyTeam.BaseHealth}");
    }

    private WaveTotals AggregateWaveStats(Hero hero, IReadOnlyList<UnitId> units)
    {
        var totals = new WaveTotals
        {
            Health = hero.MaxHealth,
            Damage = hero.Attack,
        };

        foreach (var id in units)
        {
            var blueprint = UnitCatalog.ById(id);
            totals.Health += blueprint.Health;
            totals.Damage += blueprint.Damage;
            totals.UnitCount++;
        }

        return totals;
    }

    private string ReadLine()
    {
        return _ui.ReadLine();
    }

    private int ReadInt()
    {
        while (true)
        {
            var text = ReadLine();
            if (int.TryParse(text, NumberStyles.Integer, CultureInfo.InvariantCulture, out var value))
            {
                return value;
            }

            _ui.Write("Enter a valid number: ");
        }
    }

    private int ReadIntWithDefault(int defaultValue)
    {
        var text = ReadLine();
        if (string.IsNullOrWhiteSpace(text))
        {
            return defaultValue;
        }

        return int.TryParse(text, NumberStyles.Integer, CultureInfo.InvariantCulture, out var value)
            ? value
            : defaultValue;
    }

    private sealed class WaveTotals
    {
        public int Health { get; set; }

        public int Damage { get; set; }

        public int UnitCount { get; set; }
    }
}
