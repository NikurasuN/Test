#pragma once

#include <string>

namespace herolinewars {

class IconLibrary {
public:
    static std::string heroGlyph(const std::string& name);
    static std::string unitGlyph(const std::string& name);
    static std::string attributeGlyph(const std::string& attributeName);
};

}  // namespace herolinewars
