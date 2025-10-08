using System;
using System.Collections.Generic;
using System.Linq;

namespace HeroLineWars;

internal enum PrimaryAttribute
{
    Strength,
    Dexterity,
    Intelligence,
}

internal sealed class Hero
{
    private const int HealthPerStrength = 4;
    private const int ShieldPerIntelligence = 8;
    private const int RangeUnitPixels = 24;
    private const int PrimaryAttributeLevelGain = 3;
    private const int SecondaryAttributeLevelGain = 1;

    private const double DefensePerStrength = 1.0 / 3.0;
    private const double PrimaryDamageRatio = 0.5;
    private const double AttackSpeedPerDex = 0.03;
    private const double EvasionPerDex = 0.01;
    private const double MaxEvasion = 0.35;
    private const double CriticalPerInt = 0.015;
    private const double MaxCritical = 0.4;

    private readonly Dictionary<EquipmentSlot, int> _equippedUnique = new();

    public Hero(
        string name,
        int baseMaxHealth,
        int baseAttack,
        int baseDefense,
        int strength,
        int dexterity,
        int intelligence,
        PrimaryAttribute primary,
        int startingGold,
        int startingIncome,
        int attackRangeUnits)
    {
        if (attackRangeUnits <= 0)
        {
            attackRangeUnits = 1;
        }

        Name = name;
        Primary = primary;
        BaseMaxHealth = baseMaxHealth;
        BaseAttack = baseAttack;
        BaseDefense = baseDefense;
        AttackRangeUnits = attackRangeUnits;
        AttackRangePixels = attackRangeUnits * RangeUnitPixels;
        Strength = strength;
        Dexterity = dexterity;
        Intelligence = intelligence;
        Gold = startingGold;
        Income = startingIncome;
        Level = 1;

        ExperienceToNextLevel = ComputeExperienceForLevel(Level);
        RecalculateStats();
        CurrentHealth = MaxHealth;
        CurrentShield = GetMaxEnergyShield();
    }

    public string Name { get; }

    public PrimaryAttribute Primary { get; }

    public List<Item> Inventory { get; } = new();

    public int BaseMaxHealth { get; }

    public int BaseAttack { get; }

    public int BaseDefense { get; }

    public int AttackRangeUnits { get; }

    public int AttackRangePixels { get; }

    public int ItemAttackBonus { get; private set; }

    public int ItemDefenseBonus { get; private set; }

    public int ItemStrengthBonus { get; private set; }

    public int ItemDexterityBonus { get; private set; }

    public int ItemIntelligenceBonus { get; private set; }

    public int Strength { get; private set; }

    public int Dexterity { get; private set; }

    public int Intelligence { get; private set; }

    public int MaxHealth { get; private set; }

    public int CurrentHealth { get; private set; }

    public int CurrentShield { get; private set; }

    public int Attack { get; private set; }

    public int Defense { get; private set; }

    public int Gold { get; set; }

    public int Income { get; set; }

    public int Level { get; private set; }

    public int Experience { get; private set; }

    public int ExperienceToNextLevel { get; private set; }

    public int GetStrength() => Strength + ItemStrengthBonus;

    public int GetDexterity() => Dexterity + ItemDexterityBonus;

    public int GetIntelligence() => Intelligence + ItemIntelligenceBonus;

    public double GetAttackSpeedMultiplier() => 1 + GetDexterity() * AttackSpeedPerDex;

    public double GetEvasionChance()
    {
        var evasion = GetDexterity() * EvasionPerDex;
        return evasion > MaxEvasion ? MaxEvasion : evasion;
    }

    public double GetCriticalChance()
    {
        var critical = GetIntelligence() * CriticalPerInt;
        return critical > MaxCritical ? MaxCritical : critical;
    }

    public int GetMaxEnergyShield() => GetIntelligence() * ShieldPerIntelligence;

    public int RollAttackDamage(Random random)
    {
        var damage = Math.Max(1, Attack);
        if (random.NextDouble() < GetCriticalChance())
        {
            damage = (int)Math.Round(damage * 1.75);
        }

        return Math.Max(1, damage);
    }

    public void ResetHealth()
    {
        CurrentHealth = MaxHealth;
        CurrentShield = GetMaxEnergyShield();
    }

    public void EarnIncome() => Gold += Income;

    public bool SpendGold(int amount)
    {
        if (amount < 0 || Gold < amount)
        {
            return false;
        }

        Gold -= amount;
        return true;
    }

    public bool ApplyItem(Item item)
    {
        if (item.Slot == EquipmentSlot.Ring)
        {
            var ringCount = Inventory.Count(i => i.Slot == EquipmentSlot.Ring);
            if (ringCount >= 2)
            {
                return false;
            }
        }

        if (item.Slot != EquipmentSlot.None && Item.IsUniqueSlot(item.Slot) && _equippedUnique.TryGetValue(item.Slot, out var index))
        {
            var replaced = Inventory[index];
            RemoveItemBonuses(replaced);
            Inventory[index] = item;
            ApplyItemBonuses(item);
            RecalculateStats();
            return true;
        }

        Inventory.Add(item);
        ApplyItemBonuses(item);
        if (item.Slot != EquipmentSlot.None && Item.IsUniqueSlot(item.Slot))
        {
            _equippedUnique[item.Slot] = Inventory.Count - 1;
        }

        RecalculateStats();
        return true;
    }

    private void RemoveItemBonuses(Item item)
    {
        ItemAttackBonus -= item.AttackBonus;
        ItemDefenseBonus -= item.DefenseBonus;
        ItemStrengthBonus -= item.StrengthBonus;
        ItemDexterityBonus -= item.DexterityBonus;
        ItemIntelligenceBonus -= item.IntelligenceBonus;
    }

    private void ApplyItemBonuses(Item item)
    {
        ItemAttackBonus += item.AttackBonus;
        ItemDefenseBonus += item.DefenseBonus;
        ItemStrengthBonus += item.StrengthBonus;
        ItemDexterityBonus += item.DexterityBonus;
        ItemIntelligenceBonus += item.IntelligenceBonus;
    }

    public bool UpgradeAttribute(PrimaryAttribute attribute, int cost)
    {
        if (cost < 0 || Gold < cost)
        {
            return false;
        }

        Gold -= cost;
        switch (attribute)
        {
            case PrimaryAttribute.Strength:
                Strength++;
                break;
            case PrimaryAttribute.Dexterity:
                Dexterity++;
                break;
            case PrimaryAttribute.Intelligence:
                Intelligence++;
                break;
        }

        RecalculateStats();
        CurrentHealth = MaxHealth;
        CurrentShield = GetMaxEnergyShield();
        return true;
    }

    public bool TakeDamage(Random random, int amount)
    {
        if (amount <= 0)
        {
            return false;
        }

        if (random.NextDouble() < GetEvasionChance())
        {
            return false;
        }

        var remaining = amount;
        if (CurrentShield > 0)
        {
            var absorbed = Math.Min(CurrentShield, remaining);
            CurrentShield -= absorbed;
            remaining -= absorbed;
        }

        if (remaining <= 0)
        {
            return false;
        }

        CurrentHealth -= remaining;
        return CurrentHealth <= 0;
    }

    public int GainExperience(int amount)
    {
        if (amount <= 0)
        {
            return 0;
        }

        Experience += amount;
        var levels = 0;
        while (Experience >= ExperienceToNextLevel)
        {
            Experience -= ExperienceToNextLevel;
            Level++;
            levels++;
            ApplyLevelUp();
            ExperienceToNextLevel = ComputeExperienceForLevel(Level);
        }

        return levels;
    }

    public string Summary()
    {
        return string.Format(
            "{0} - HP: {1}/{2} ({3} shield), ATK: {4}, DEF: {5}, Gold: {6}, Income: {7}",
            Name,
            CurrentHealth,
            MaxHealth,
            CurrentShield,
            Attack,
            Defense,
            Gold,
            Income);
    }

    public void RecalculateStats()
    {
        var totalStrength = GetStrength();
        MaxHealth = (int)Math.Round(BaseMaxHealth + totalStrength * (double)HealthPerStrength);
        if (CurrentHealth > MaxHealth)
        {
            CurrentHealth = MaxHealth;
        }

        Attack = BaseAttack + ItemAttackBonus + CalculatePrimaryDamageBonus();
        Defense = (int)Math.Round(BaseDefense + ItemDefenseBonus + totalStrength * DefensePerStrength);

        var maxShield = GetMaxEnergyShield();
        if (CurrentShield > maxShield)
        {
            CurrentShield = maxShield;
        }
    }

    private int CalculatePrimaryDamageBonus()
    {
        return (int)Math.Round(GetPrimaryAttributeValue() * PrimaryDamageRatio);
    }

    private int GetPrimaryAttributeValue()
    {
        return Primary switch
        {
            PrimaryAttribute.Strength => GetStrength(),
            PrimaryAttribute.Dexterity => GetDexterity(),
            _ => GetIntelligence(),
        };
    }

    private void ApplyLevelUp()
    {
        switch (Primary)
        {
            case PrimaryAttribute.Strength:
                Strength += PrimaryAttributeLevelGain;
                Dexterity += SecondaryAttributeLevelGain;
                Intelligence += SecondaryAttributeLevelGain;
                break;
            case PrimaryAttribute.Dexterity:
                Dexterity += PrimaryAttributeLevelGain;
                Strength += SecondaryAttributeLevelGain;
                Intelligence += SecondaryAttributeLevelGain;
                break;
            default:
                Intelligence += PrimaryAttributeLevelGain;
                Strength += SecondaryAttributeLevelGain;
                Dexterity += SecondaryAttributeLevelGain;
                break;
        }

        RecalculateStats();
        CurrentHealth = MaxHealth;
        CurrentShield = GetMaxEnergyShield();
    }

    private static int ComputeExperienceForLevel(int level)
    {
        return 120 + Math.Max(0, level - 1) * 60;
    }
}
