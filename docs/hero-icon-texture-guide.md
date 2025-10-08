# Updating Hero Icon Glyphs

The terminal version of Hero Line Wars renders lightweight ASCII glyphs via the `IconLibrary` helpers instead of generating vect
or art with Swing. You can tweak the characters that appear for heroes, units, and attributes by editing the drawing code.

## Where the glyphs are defined
- **File:** `src/IconLibrary.cpp`
- **Relevant functions:** `heroGlyph`, `unitGlyph`, and `attributeGlyph` return the strings that are displayed in the console. Ea
ch function assembles a small multi-line ASCII emblem.

## Steps to adjust a glyph
1. Open the corresponding function inside `IconLibrary.cpp`.
2. Modify the characters written to the `std::ostringstream`. The helper already handles adding new lines, so simply change the
strings passed to the stream.
3. Rebuild and rerun the application to preview your updated artwork.

Because these glyphs are only text, you do not need an asset pipeline. Editing the ASCII templates is enough to update the UI.
