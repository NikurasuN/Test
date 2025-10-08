namespace HeroLineWars;

internal sealed class ConsoleUserInterface : IUserInterface
{
    public void Write(string text) => Console.Write(text);

    public void WriteLine(string text = "") => Console.WriteLine(text);

    public string ReadLine()
    {
        return Console.ReadLine() ?? string.Empty;
    }
}
