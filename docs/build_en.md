[繁體中文](build.md) | [日本語](build_ja.md)

# Build

## Prerequisites

1. **[OpenJDK 8](https://www.azul.com/downloads/?version=java-8-lts&package=jdk#zulu)**
   - Zulu, Temurin, or any other OpenJDK 8 distribution works. Point `JAVA_HOME` to JDK 8.
2. **[Apache Ant](https://ant.apache.org/bindownload.cgi)**
   - Windows: extract the archive and add Ant's `bin` to `PATH`.
   - Linux: `sudo apt install ant`.

The first build requires access to Maven.

Ant automatically downloads Apache Ivy 2.5.3, then uses Ivy to retrieve these dependencies:

| Dependency | Maven coordinates |
|------------|-------------------|
| CLDC 1.1 API | `org.microemu:cldcapi11:2.0.4` |
| MIDP 2.0 API | `org.microemu:midpapi20:2.0.4` |
| JSR-82 API | `org.microemu:microemu-jsr-82:2.0.4` |
| ProGuard | `net.sf.proguard:proguard-base:6.0.3` |

## Build Commands

| Command | Description |
|---------|-------------|
| `ant` | Normal build |
| `ant clean package` | Remove build outputs and rebuild |
| `ant run` | Normal build and launch the emulator |
| `ant clean` | Remove `build/` and `dist/`, retaining the dependency cache |

The output JAR and JAD can be found in `dist/` after a successful build.

To use `run`, first place `freej2me.jar` in the project's `emulator` directory.
