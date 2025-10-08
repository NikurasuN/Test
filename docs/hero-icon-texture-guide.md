# Updating Hero Icon Textures

This project renders hero and unit icons procedurally using the `IconLibrary` class rather than loading image files from disk. You can update the "texture" (visual look) of those icons by editing the drawing code inside the library.

## Where the icons are defined
- **File:** `src/main/java/com/example/herolinewars/IconLibrary.java`
- **Relevant types:** the `Category` enum exposes the high level icon groups. Dedicated factory methods like `createHeroGlyph`, `createEnemyGlyph`, and the attribute or item methods return the concrete icons that widgets use.

All icons are created through the private `createIcon(int width, int height, Consumer<Graphics2D>)` helper. That helper prepares an off-screen buffered image and then invokes the lambda you pass in to paint the vector artwork.

## Steps to adjust a hero icon
1. Locate the method that creates the glyph you want to restyle. For example, hero summary cards use `createHeroGlyph` and individual unit portraits use `createUnitGlyph` (if you add more differentiation, they should be implemented near the other helpers).
2. Inside the method, identify the drawing routine. You will usually see calls to helper methods like `paintBadgeBackground` followed by shapes drawn with the `Graphics2D` API (`fillOval`, `fillRoundRect`, `GeneralPath`, etc.).
3. Update colours or shapes to match the new texture:
   - Change colour constants at the top of the file if multiple icons share the palette.
   - Adjust gradient definitions or `BasicStroke` widths to alter outlines.
   - Modify the geometry (coordinates passed into the path and shape methods) to change the silhouette.
4. Rebuild or rerun the UI to verify the result.

Because everything is vector-based you do not need to manage sprite sheets or asset pipelines—editing the painting commands is enough.

## Adding bitmap textures (implementation needed)
If you prefer to replace the procedural drawing with external texture files (PNG, SVG, etc.), that capability is **not yet implemented**. You would need to extend `IconLibrary` so that `createIcon` can load an image resource instead of (or in addition to) running a paint lambda. Consider the following high level approach:

- Introduce a new helper such as `createIconFromResource(String path)` that reads an image from `src/main/resources` using `ImageIO.read`.
- Decide on a naming convention for hero/unit textures and expose configuration to map heroes to resource names.
- Update the UI components to use the new loader where appropriate, retaining the current procedural fallback.

Feel free to implement this pipeline and reach out if you need the UI to switch to those bitmap textures by default.
