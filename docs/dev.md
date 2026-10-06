# Development

## Environment

- **Visual Studio Code**
  - JAVA Runtime: Zulu JDK 17
  - Build JDK: OpenJDK 8 (set `JAVA_HOME` for Ant).
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
| BgmPack | `tools/BgmPackBuilder.java` | Validate tracks 0–17 and build the deterministic MIDI pack |

### Helper Tools

| Tools | Path | Description |
|------|------|------|
| CcDatCropViewer | `tools/CcDatCropViewer.java` | View and crop cc.dat |
