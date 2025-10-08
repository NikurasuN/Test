using System.Collections.Generic;
using System.Linq;

namespace HeroLineWars;

internal sealed class Team
{
    private readonly List<UnitId> _queued = new();

    public Team(string name, Hero hero)
    {
        Name = name;
        Hero = hero;
        BaseHealth = 100;
    }

    public string Name { get; }

    public Hero Hero { get; }

    public int BaseHealth { get; private set; }

    public void DamageBase(int amount)
    {
        if (amount < 0)
        {
            amount = 0;
        }

        BaseHealth -= amount;
        if (BaseHealth < 0)
        {
            BaseHealth = 0;
        }
    }

    public bool IsDefeated() => BaseHealth <= 0;

    public void QueueUnit(UnitId id)
    {
        _queued.Add(id);
    }

    public IReadOnlyList<UnitId> DrainQueuedUnits()
    {
        var copy = _queued.ToList();
        _queued.Clear();
        return copy;
    }

    public IReadOnlyList<UnitId> QueuedUnitsSnapshot()
    {
        return _queued.ToList();
    }

    public bool HasQueuedUnits() => _queued.Count > 0;
}
