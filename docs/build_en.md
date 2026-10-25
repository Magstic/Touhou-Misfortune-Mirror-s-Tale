[繁體中文](build.md) | [简体中文](build_zh_cn.md) | [日本語](build_ja.md)

# Build

## Prerequisites

1. **[OpenJDK 8](https://www.azul.com/downloads/?version=java-8-lts&package=jdk#zulu)**
   - Zulu, Temurin, or any other OpenJDK 8 distribution works. Point `JAVA_HOME` to JDK 8.
2. **[Apache Ant](https://ant.apache.org/bindownload.cgi)**
   - Windows: extract the archive and add Ant's `bin` to `PATH`.
   - Linux: `sudo apt install ant` (for Debian-based distributions).
   - Mac: `brew install ant` (Homebrew is recommended; otherwise, download Ant manually and configure the environment variables).


## Windows/Linux/Mac

Clone or download the project locally, then run the following in the project root directory:

```bash
ant
```


## Android

Install **Termux**: [GitHub](https://github.com/termux/termux-app) | [F-Droid](https://f-droid.org/en/packages/com.termux/)

Update the packages and install the required dependencies in Termux:

```bash
pkg update
pkg upgrade
pkg install git ant openjdk-21 openjdk-21-x fontconfig
```

Clone the project:

```bash
git clone https://github.com/Magstic/Touhou-Misfortune-Mirror-s-Tale.git
cd Touhou-Misfortune-Mirror-s-Tale
```

Temporarily set the Java paths in the project root, then build:

```bash
export JAVA_HOME="$PREFIX/lib/jvm/java-21-openjdk"
export JAVACMD="$JAVA_HOME/bin/java"
ant
```

Once the build is complete, set up shared storage access:

```bash
termux-setup-storage
```

Copy the build outputs to the Android device's Download folder:

```bash
cp dist/*.jar dist/*.jad ~/storage/downloads/
```


## Build Commands

| Command | Description |
|---------|-------------|
| `ant` | Normal build |
| `ant clean package` | Remove build outputs and rebuild |
| `ant clean` | Remove `build/` and `dist/`, retaining the dependency cache |


The first build requires access to Maven.

Ant automatically downloads Apache Ivy 2.5.3, then uses Ivy to retrieve these dependencies:

| Dependency | Maven coordinates |
|------------|-------------------|
| CLDC 1.1 API | `org.microemu:cldcapi11:2.0.4` |
| MIDP 2.0 API | `org.microemu:midpapi20:2.0.4` |
| JSR-82 API | `org.microemu:microemu-jsr-82:2.0.4` |
| JSR-75 API | `org.microemu:microemu-jsr-75:2.0.4` |
| ECJ (Eclipse 4.20) | `org.eclipse.jdt:ecj:3.26.0` |
| ProGuard | `net.sf.proguard:proguard-base:6.0.3` |

The output JAR and JAD can be found in `dist/` after a successful build.
