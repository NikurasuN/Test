using System;
using System.Collections.Generic;
using System.Linq;

namespace HeroLineWars;

internal enum UnitId
{
    Scout,
    Soldier,
    Archer,
    Knight,
    SiegeGolem,
}

internal sealed class UnitBlueprint
{
    public UnitBlueprint(UnitId id, string name, int cost, int health, int damage, int incomeBonus, int range, string description)
    {
        Id = id;
        Name = name;
        Cost = cost;
        Health = health;
        Damage = damage;
        IncomeBonus = incomeBonus;
        Range = range;
        Description = description;
    }

    public UnitId Id { get; }

    public string Name { get; }

    public int Cost { get; }

    public int Health { get; }

    public int Damage { get; }

    public int IncomeBonus { get; }

    public int Range { get; }

    public string Description { get; }
}

internal static class UnitCatalog
{
    private static readonly IReadOnlyList<UnitBlueprint> Units = new List<UnitBlueprint>
    {
        new(UnitId.Scout, "Scout", 35, 20, 6, 2, 36, "A cheap and fast unit that increases income slightly."),
        new(UnitId.Soldier, "Soldier", 50, 35, 9, 3, 40, "Balanced melee fighter."),
        new(UnitId.Archer, "Archer", 65, 30, 12, 4, 160, "Ranged attacker that deals reliable damage."),
        new(UnitId.Knight, "Knight", 90, 55, 16, 6, 42, "Heavy unit with strong damage."),
        new(UnitId.SiegeGolem, "Siege Golem", 120, 80, 25, 8, 50, "Slow but devastating against bases."),
    };

    public static IReadOnlyList<UnitBlueprint> All()
    {
        return Units.Select(u => new UnitBlueprint(u.Id, u.Name, u.Cost, u.Health, u.Damage, u.IncomeBonus, u.Range, u.Description))
            .ToList();
    }

    public static UnitBlueprint ById(UnitId id)
    {
        foreach (var unit in Units)
        {
            if (unit.Id == id)
            {
                return unit;
            }
        }

        throw new InvalidOperationException("Unknown unit identifier.");
    }
}
