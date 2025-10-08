#ifdef _WIN32
#include <windows.h>

#include "HeroLineWarsGame.h"

#include <cstdlib>
#include <filesystem>
#include <iostream>
#include <stdexcept>
#include <string>
#include <system_error>
#include <vector>

namespace
{
std::filesystem::path GetExecutableDirectory()
{
    wchar_t buffer[MAX_PATH];
    DWORD length = GetModuleFileNameW(nullptr, buffer, MAX_PATH);
    if (length == 0 || length == MAX_PATH)
    {
        throw std::runtime_error("Unable to determine module file name");
    }

    std::filesystem::path modulePath(buffer, buffer + length);
    modulePath.remove_filename();
    return modulePath;
}

std::filesystem::path TryCanonical(const std::filesystem::path &candidate)
{
    std::error_code ec;
    std::filesystem::path resolved = std::filesystem::canonical(candidate, ec);
    if (!ec)
    {
        return resolved;
    }
    return {};
}

std::filesystem::path FindGameExecutable(const std::filesystem::path &start)
{
    const std::vector<std::filesystem::path> candidates = {
        start / L"hero_line_wars.exe",
        start.parent_path() / L"hero_line_wars.exe",
        start / L"Release" / L"hero_line_wars.exe",
        start / L"Debug" / L"hero_line_wars.exe",
        start.parent_path() / L"Release" / L"hero_line_wars.exe",
        start.parent_path() / L"Debug" / L"hero_line_wars.exe"};

    for (const auto &candidate : candidates)
    {
        if (candidate.empty())
        {
            continue;
        }

        std::error_code existsError;
        if (std::filesystem::exists(candidate, existsError) && !existsError)
        {
            std::filesystem::path resolved = TryCanonical(candidate);
            if (!resolved.empty())
            {
                return resolved;
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

        if (it->path().filename() == L"hero_line_wars.exe")
        {
            std::filesystem::path resolved = TryCanonical(it->path());
            if (!resolved.empty())
            {
                return resolved;
            }
        }
    }

    return {};
}

std::wstring BuildCommandLine(const std::filesystem::path &exe, int argc, wchar_t *argv[])
{
    std::wstring command = L"\"" + exe.wstring() + L"\"";
    for (int i = 1; i < argc; ++i)
    {
        command += L" \"";
        command += argv[i];
        command += L"\"";
    }

    return command;
}

std::wstring Utf8ToWide(const std::string &text)
{
    if (text.empty())
    {
        return std::wstring();
    }

    int sizeNeeded = MultiByteToWideChar(CP_UTF8, 0, text.c_str(), -1, nullptr, 0);
    if (sizeNeeded <= 0)
    {
        return std::wstring(L"(unable to decode error message)");
    }

    std::wstring wide(static_cast<std::size_t>(sizeNeeded) - 1, L'\0');
    MultiByteToWideChar(CP_UTF8, 0, text.c_str(), -1, wide.data(), sizeNeeded);
    return wide;
}

[[noreturn]] void AbortWithMessage(const std::wstring &message);

int RunEmbeddedGame()
{
    try
    {
        std::wcout << L"Launching embedded Hero Line Wars build..." << std::endl;
        herolinewars::runGame();
        return EXIT_SUCCESS;
    }
    catch (const std::exception &ex)
    {
        AbortWithMessage(L"Standalone launcher error: " + Utf8ToWide(ex.what()));
    }
    catch (...)
    {
        AbortWithMessage(L"An unknown error occurred while running the embedded game.");
    }

    return EXIT_FAILURE;
}

[[noreturn]] void AbortWithMessage(const std::wstring &message)
{
    std::wcerr << message << std::endl;
    MessageBoxW(nullptr, message.c_str(), L"Hero Line Wars", MB_ICONERROR | MB_OK);
    std::exit(EXIT_FAILURE);
}
}

int wmain(int argc, wchar_t *argv[])
{
    try
    {
        const std::filesystem::path exeDir = GetExecutableDirectory();
        const std::filesystem::path gameExecutable = FindGameExecutable(exeDir);

        if (gameExecutable.empty())
        {
            std::wcerr << L"Unable to locate hero_line_wars.exe next to the launcher." << std::endl;
            std::wcerr << L"Falling back to the embedded standalone game." << std::endl;
            return RunEmbeddedGame();
        }

        std::wstring commandLine = BuildCommandLine(gameExecutable, argc, argv);
        std::vector<wchar_t> commandBuffer(commandLine.begin(), commandLine.end());
        commandBuffer.push_back(L'\0');

        std::wstring workingDirectory = gameExecutable.parent_path().wstring();

        STARTUPINFOW startupInfo{};
        PROCESS_INFORMATION processInfo{};
        startupInfo.cb = sizeof(startupInfo);

        BOOL created = CreateProcessW(
            nullptr,
            commandBuffer.data(),
            nullptr,
            nullptr,
            FALSE,
            0,
            nullptr,
            workingDirectory.c_str(),
            &startupInfo,
            &processInfo);

        if (!created)
        {
            DWORD error = GetLastError();
            std::wstring message = L"Failed to launch hero_line_wars.exe (error code " + std::to_wstring(error) + L").";
            AbortWithMessage(message);
        }

        CloseHandle(processInfo.hThread);
        WaitForSingleObject(processInfo.hProcess, INFINITE);
        DWORD exitCode = EXIT_FAILURE;
        if (!GetExitCodeProcess(processInfo.hProcess, &exitCode))
        {
            exitCode = EXIT_FAILURE;
        }
        CloseHandle(processInfo.hProcess);

        return static_cast<int>(exitCode);
    }
    catch (const std::exception &ex)
    {
        AbortWithMessage(L"Launcher error: " + Utf8ToWide(ex.what()));
    }
    catch (...)
    {
        AbortWithMessage(L"An unknown error occurred while launching the game.");
    }

    return EXIT_FAILURE;
}

extern int __argc;
extern wchar_t **__wargv;

int APIENTRY wWinMain(HINSTANCE, HINSTANCE, LPWSTR, int)
{
    return wmain(__argc, __wargv);
}
#endif
