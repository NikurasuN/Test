using System.Text;

namespace HeroLineWars;

internal static class Glyphs
{
    public static string HeroGlyph(string name)
    {
        var builder = new StringBuilder();
        builder.Append('[').Append(name).AppendLine("]");
        builder.AppendLine("  \\o/");
        builder.AppendLine("   |");
        builder.Append("  / \\");
        return builder.ToString();
    }

    public static string UnitGlyph(string name)
    {
        var builder = new StringBuilder();
        builder.Append('{').Append(name).AppendLine("}");
        builder.AppendLine("  /\\");
        builder.AppendLine(" /==\\");
        builder.Append("  \\//");
        return builder.ToString();
    }

    public static string AttributeGlyph(string attributeName)
    {
        var builder = new StringBuilder();
        builder.Append('<').Append(attributeName).AppendLine(">");
        builder.AppendLine("  *");
        builder.Append(" ***");
        return builder.ToString();
    }
}
