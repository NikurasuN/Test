cmake_minimum_required(VERSION 3.16)
project(HeroLineWarsGame LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

add_executable(hero_line_wars
    src/HeroLineWarsGame.cpp
    src/Hero.cpp
    src/IconLibrary.cpp
    src/Item.cpp
    src/Team.cpp
    src/UnitType.cpp)

target_include_directories(hero_line_wars PRIVATE src)
