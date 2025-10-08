using System.Collections.Generic;
using System.Text;

namespace HeroLineWars;

internal enum EquipmentSlot
{
    None,
    Helmet,
    Chestplate,
    Weapon,
    Shield,
    Accessory,
    Ring,
}

internal sealed class Item
{
    public Item(
        string name,
        int attackBonus,
        int defenseBonus,
        int cost,
        string description,
        EquipmentSlot slot,
        int strengthBonus = 0,
        int dexterityBonus = 0,
        int intelligenceBonus = 0)
    {
        Name = name;
        AttackBonus = attackBonus;
        DefenseBonus = defenseBonus;
        Cost = cost;
        Description = description;
        Slot = slot;
        StrengthBonus = strengthBonus;
        DexterityBonus = dexterityBonus;
        IntelligenceBonus = intelligenceBonus;
    }

    public string Name { get; }

    public int AttackBonus { get; }

    public int DefenseBonus { get; }

    public int Cost { get; }

    public string Description { get; }

    public EquipmentSlot Slot { get; }

    public int StrengthBonus { get; }

    public int DexterityBonus { get; }

    public int IntelligenceBonus { get; }

    public string Summary()
    {
        var stats = new List<string>();
        if (AttackBonus != 0)
        {
            stats.Add($"+{AttackBonus} ATK");
        }
        if (DefenseBonus != 0)
        {
            stats.Add($"+{DefenseBonus} DEF");
        }
        if (StrengthBonus != 0)
        {
            stats.Add($"+{StrengthBonus} STR");
        }
        if (DexterityBonus != 0)
        {
            stats.Add($"+{DexterityBonus} DEX");
        }
        if (IntelligenceBonus != 0)
        {
            stats.Add($"+{IntelligenceBonus} INT");
        }

        if (stats.Count == 0)
        {
            stats.Add("No bonuses");
        }

        var builder = new StringBuilder();
        builder.Append(Name).Append(" (Cost: ").Append(Cost).Append(") ");
        builder.Append(string.Join(", ", stats));
        if (Slot != EquipmentSlot.None)
        {
            builder.Append(' ').Append('[').Append(SlotDisplayName(Slot)).Append(']');
        }
        builder.Append(" - ").Append(Description);
        return builder.ToString();
    }

    public static IReadOnlyList<Item> CreateDefaultShopItems()
    {
        return new List<Item>
        {
            new("Sharpened Arrows", 6, 0, 85,
                "Lightweight arrowheads that increase ranged damage.", EquipmentSlot.None),
            new("Bulwark Shield", 0, 6, 90,
                "Sturdy shield that absorbs blows.", EquipmentSlot.Shield, strengthBonus: 1),
            new("War Banner", 4, 3, 110,
                "Rallying banner granting balanced power.", EquipmentSlot.Accessory, strengthBonus: 1, dexterityBonus: 1),
            new("Arcane Tome", 9, 0, 140,
                "Magical tome that empowers offensive spells.", EquipmentSlot.Weapon, intelligenceBonus: 2),
            new("Guardian Armor", 0, 9, 150,
                "Heavy armor that keeps you standing longer.", EquipmentSlot.Chestplate, strengthBonus: 1),
            new("Heroic Relic", 6, 6, 185,
                "Relic of old heroes granting all-around strength.", EquipmentSlot.Accessory,
                strengthBonus: 2, dexterityBonus: 2, intelligenceBonus: 2),
            new("Steel Helm", 0, 4, 160,
                "Fortified helmet that hardens resolve.", EquipmentSlot.Helmet, strengthBonus: 2),
            new("Knight's Chestplate", 0, 7, 210,
                "Immovable armor that protects the torso.", EquipmentSlot.Chestplate, strengthBonus: 3),
            new("Ring of Fortitude", 0, 0, 125,
                "Enchanted band that bolsters physical might.", EquipmentSlot.Ring, strengthBonus: 3),
            new("Ring of Swiftness", 0, 0, 125,
                "A nimble ring that heightens agility.", EquipmentSlot.Ring, dexterityBonus: 3),
            new("Ring of Insight", 0, 0, 125,
                "A crystalline ring that sharpens arcane focus.", EquipmentSlot.Ring, intelligenceBonus: 3),
        };
    }

    public static bool IsUniqueSlot(EquipmentSlot slot)
    {
        return slot is EquipmentSlot.Helmet or EquipmentSlot.Chestplate or EquipmentSlot.Weapon or EquipmentSlot.Shield;
    }

    public static string SlotDisplayName(EquipmentSlot slot)
    {
        return slot switch
        {
            EquipmentSlot.Helmet => "Helmet",
            EquipmentSlot.Chestplate => "Chestplate",
            EquipmentSlot.Weapon => "Weapon",
            EquipmentSlot.Shield => "Shield",
            EquipmentSlot.Accessory => "Accessory",
            EquipmentSlot.Ring => "Ring",
            _ => "None",
        };
    }
}
