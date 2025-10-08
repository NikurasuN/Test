#include "IconLibrary.h"

#include <sstream>

namespace herolinewars {

std::string IconLibrary::heroGlyph(const std::string& name) {
    std::ostringstream out;
    out << "[" << name << "]\n";
    out << "  \\o/" << '\n';
    out << "   |" << '\n';
    out << "  / \\";
    return out.str();
}

std::string IconLibrary::unitGlyph(const std::string& name) {
    std::ostringstream out;
    out << "{" << name << "}" << '\n';
    out << "  /\\" << '\n';
    out << " /==\\" << '\n';
    out << "  \\//";
    return out.str();
}

std::string IconLibrary::attributeGlyph(const std::string& attributeName) {
    std::ostringstream out;
    out << "<" << attributeName << ">" << '\n';
    out << "  *" << '\n';
    out << " ***";
    return out.str();
}

}  // namespace herolinewars
