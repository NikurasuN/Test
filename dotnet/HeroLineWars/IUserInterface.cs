namespace HeroLineWars;

internal interface IUserInterface
{
    void Write(string text);

    void WriteLine(string text = "");

    string ReadLine();
}
