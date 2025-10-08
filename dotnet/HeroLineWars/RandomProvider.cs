namespace HeroLineWars;

internal static class RandomProvider
{
    public static Random Create()
    {
        var seed = Environment.TickCount ^ (int)DateTime.UtcNow.Ticks;
        return new Random(seed);
    }
}
