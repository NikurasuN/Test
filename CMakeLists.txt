cmake_minimum_required(VERSION 3.16)
project(HeroLineWarsGame LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

add_executable(hero_line_wars
    src/main.cpp
    src/HeroLineWarsGame.cpp
    src/Hero.cpp
    src/IconLibrary.cpp
    src/Item.cpp
    src/Team.cpp
    src/UnitType.cpp)

target_include_directories(hero_line_wars PRIVATE src)

if (WIN32)
    add_executable(run_game_launcher
        src/RunGameLauncher.cpp)
    set_target_properties(run_game_launcher PROPERTIES WIN32_EXECUTABLE ON)
else()
    add_executable(run_game_launcher
        src/RunGameLauncherPosix.cpp)
endif()

set_target_properties(run_game_launcher PROPERTIES OUTPUT_NAME "run_game")
add_dependencies(run_game_launcher hero_line_wars)
