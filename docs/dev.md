# Development

## Environment

- **Visual Studio Code**
  - JAVA Runtime: Zulu JDK 17
  - Build JDK: JDK 8 or later; JDK 21 recommended (set `JAVA_HOME` for Ant).
  - Game compiler: ECJ from Eclipse 4.20 (`org.eclipse.jdt:ecj:3.26.0`), downloaded by Ivy.
  - Run `ant` to fetch the API JARs into `.deps/lib/`.
  - Libraries: `cldcapi11-2.0.4.jar`, `midpapi20-2.0.4.jar`, `microemu-jsr-82-2.0.4.jar`


## Tree

See [Tree](tree.md).

## Tools

### Build Tools

| Tools | Path | Description |
|------|------|------|
| FontDat | `tools/FontDatBuilder.java` | Build bitmap font from TTF |
| I18nPack | `tools/I18nPackBuilder.java` | Validate UTF-8, file/line/TextId schemas and build deterministic i18n packs |
| BgmPack | `tools/BgmPackBuilder.java` | Pack original BGM and battle-only lite BGM into separate MIDI bundles |

### Helper Tools

| Tools | Path | Description |
|------|------|------|
| CcDatCropViewer | `tools/CcDatCropViewer.java` | View cc.dat crop rectangles on sprite sheets |
| BgmLiteBuilder | `tools/BgmLiteBuilder.java` | Generate lite battle MIDIs from the original channel layout; run manually, overwrites its output files |
