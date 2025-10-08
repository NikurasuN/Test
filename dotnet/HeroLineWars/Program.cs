using System.Text;

namespace HeroLineWars;

internal static class Program
{
    private static void Main()
    {
        Console.OutputEncoding = Encoding.UTF8;
        var ui = new ConsoleUserInterface();
        var rng = RandomProvider.Create();
        var game = new Game(ui, rng);
        game.Run();
    }
}
