#ifndef _WIN32

#include "HeroLineWarsGame.h"

#include <cerrno>
#include <csignal>
#include <cstdlib>
#include <filesystem>
#include <iostream>
#include <stdexcept>
#include <string>
#include <system_error>
#include <vector>

#if defined(__APPLE__)
#include <mach-o/dyld.h>
#endif

#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>

namespace
{
std::filesystem::path TryCanonical(const std::filesystem::path &candidate)
{
    if (candidate.empty())
    {
        return {};
    }

    std::error_code ec;
    std::filesystem::path resolved = std::filesystem::weakly_canonical(candidate, ec);
    if (!ec && !resolved.empty())
    {
        return resolved;
    }

    resolved = std::filesystem::canonical(candidate, ec);
    if (!ec && !resolved.empty())
    {
        return resolved;
    }

    return {};
}

std::filesystem::path GetExecutablePath(int argc, char *argv[])
{
#if defined(__linux__)
    if (auto resolved = TryCanonical("/proc/self/exe"); !resolved.empty())
    {
        return resolved;
    }
#endif

#if defined(__APPLE__)
    uint32_t size = 0;
    _NSGetExecutablePath(nullptr, &size);
    std::string buffer(static_cast<std::size_t>(size), '\0');
    if (_NSGetExecutablePath(buffer.data(), &size) == 0)
    {
        if (auto resolved = TryCanonical(std::filesystem::path(buffer.c_str())); !resolved.empty())
        {
            return resolved;
        }
    }
#endif

    if (argc > 0 && argv[0] != nullptr)
    {
        std::filesystem::path candidate(argv[0]);
        if (candidate.is_relative())
        {
            std::error_code cwdError;
            const std::filesystem::path cwd = std::filesystem::current_path(cwdError);
            if (!cwdError)
            {
                candidate = cwd / candidate;
            }
        }

        if (auto resolved = TryCanonical(candidate); !resolved.empty())
        {
            return resolved;
        }
    }

    return {};
}

std::filesystem::path GetExecutableDirectory(int argc, char *argv[])
{
    std::filesystem::path executable = GetExecutablePath(argc, argv);
    if (!executable.empty())
    {
        executable.remove_filename();
    }
    return executable;
}

std::filesystem::path FindGameExecutable(const std::filesystem::path &start)
{
    if (start.empty())
    {
        return {};
    }

    const std::vector<std::filesystem::path> candidateDirectories = {
        start,
        start.parent_path(),
        start / "Release",
        start / "Debug",
        start.parent_path() / "Release",
        start.parent_path() / "Debug"};

    const std::vector<std::string> candidateNames = {
        "hero_line_wars",
        "hero_line_wars.exe"};

    for (const auto &directory : candidateDirectories)
    {
        if (directory.empty())
        {
            continue;
        }

        for (const auto &name : candidateNames)
        {
            const std::filesystem::path candidate = directory / name;
            std::error_code existsError;
            if (std::filesystem::exists(candidate, existsError) && !existsError)
            {
                if (auto resolved = TryCanonical(candidate); !resolved.empty())
                {
                    return resolved;
                }
            }
        }
    }

    std::error_code iteratorError;
    std::filesystem::recursive_directory_iterator it(
        start,
        std::filesystem::directory_options::skip_permission_denied,
        iteratorError);

    for (std::filesystem::recursive_directory_iterator end; it != end; it.increment(iteratorError))
    {
        if (iteratorError)
        {
            iteratorError.clear();
            continue;
        }

        if (!it->is_regular_file())
        {
            continue;
        }

        const std::filesystem::path filename = it->path().filename();
        if (filename == "hero_line_wars" || filename == "hero_line_wars.exe")
        {
            if (auto resolved = TryCanonical(it->path()); !resolved.empty())
            {
                return resolved;
            }
        }
    }

    return {};
}

int LaunchGame(const std::filesystem::path &gameExecutable, int argc, char *argv[])
{
    const std::filesystem::path workingDirectory = gameExecutable.parent_path();
    const std::string workingDirectoryString = workingDirectory.string();

    std::vector<std::string> argumentStorage;
    argumentStorage.reserve(static_cast<std::size_t>(argc));
    argumentStorage.emplace_back(gameExecutable.string());
    for (int i = 1; i < argc; ++i)
    {
        argumentStorage.emplace_back(argv[i] != nullptr ? argv[i] : "");
    }

    std::vector<char *> execArguments;
    execArguments.reserve(argumentStorage.size() + 1);
    for (auto &argument : argumentStorage)
    {
        execArguments.push_back(argument.data());
    }
    execArguments.push_back(nullptr);

    pid_t pid = fork();
    if (pid == -1)
    {
        throw std::system_error(errno, std::generic_category(), "fork");
    }

    if (pid == 0)
    {
        if (!workingDirectoryString.empty())
        {
            if (chdir(workingDirectoryString.c_str()) != 0)
            {
                std::perror("chdir");
                _exit(EXIT_FAILURE);
            }
        }

        execv(argumentStorage.front().c_str(), execArguments.data());
        std::perror("execv");
        _exit(EXIT_FAILURE);
    }

    int status = 0;
    if (waitpid(pid, &status, 0) == -1)
    {
        throw std::system_error(errno, std::generic_category(), "waitpid");
    }

    if (WIFEXITED(status))
    {
        return WEXITSTATUS(status);
    }

    if (WIFSIGNALED(status))
    {
        std::cerr << "Game terminated by signal " << WTERMSIG(status) << std::endl;
    }

    return EXIT_FAILURE;
}

int RunEmbeddedGame()
{
    try
    {
        std::cout << "Launching embedded Hero Line Wars build..." << std::endl;
        herolinewars::runGame();
        return EXIT_SUCCESS;
    }
    catch (const std::exception &ex)
    {
        std::cerr << "Standalone launcher error: " << ex.what() << std::endl;
    }
    catch (...)
    {
        std::cerr << "An unknown error occurred while running the embedded game." << std::endl;
    }

    return EXIT_FAILURE;
}
}

int main(int argc, char *argv[])
{
    try
    {
        const std::filesystem::path executableDirectory = GetExecutableDirectory(argc, argv);
        const std::filesystem::path gameExecutable = FindGameExecutable(executableDirectory);

        if (gameExecutable.empty())
        {
            std::cerr << "Unable to locate a hero_line_wars binary next to the launcher." << std::endl;
            std::cerr << "Falling back to the embedded standalone game." << std::endl;
            return RunEmbeddedGame();
        }

        return LaunchGame(gameExecutable, argc, argv);
    }
    catch (const std::exception &ex)
    {
        std::cerr << "Launcher error: " << ex.what() << std::endl;
    }
    catch (...)
    {
        std::cerr << "An unknown error occurred while launching the game." << std::endl;
    }

    return EXIT_FAILURE;
}

#endif
