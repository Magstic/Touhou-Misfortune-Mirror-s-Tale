[繁體中文](build.md) | [English](build_en.md) | [日本語](build_ja.md)

# Build

## 环境要求

1. **[OpenJDK 8](https://www.azul.com/downloads/?version=java-8-lts&package=jdk#zulu)**
   - Zulu、Temurin 或其他 OpenJDK 8 发行版均可。请将 `JAVA_HOME` 指向 JDK 8。
2. **[Apache Ant](https://ant.apache.org/bindownload.cgi)**
   - Windows：解压后将 Ant 的 `bin` 加入 `PATH`。
   - Linux：`sudo apt install ant`（以 Debian 系发行版为例）。
   - Mac：`brew install ant`（推荐使用 Homebrew；否则需手动下载并配置环境变量）。


## Windows/Linux/Mac

将项目克隆或下载到本地，在项目根目录下执行：

```bash
ant
```


## Android

安装 **Termux**：[GitHub](https://github.com/termux/termux-app) | [F-Droid](https://f-droid.org/zh_Hans/packages/com.termux/)

授予必要权限后，在 Termux 内安装所需依赖：

```bash
pkg update
pkg install git ant openjdk-21 openjdk-21-x fontconfig
```

克隆项目到本地并构建：

```bash
git clone https://github.com/Magstic/Touhou-Misfortune-Mirror-s-Tale.git
cd Touhou-Misfortune-Mirror-s-Tale
ant
```

构建完成后，将构建产物复制到 Android 设备的 Download 文件夹：

```bash
termux-setup-storage
cp dist/*.jar dist/*.jad ~/storage/downloads/
```


## 构建命令

| 命令 | 说明 |
|------|------|
| `ant` | 正常构建 |
| `ant clean package` | 清除构建产物，重新构建 |
| `ant clean` | 清理 `build/` 与 `dist/`，保留依赖缓存 |


首次构建需要连接 Maven。

Ant 会自动下载 Apache Ivy 2.5.3，再通过 Ivy 获取以下依赖：

| 依赖 | Maven 坐标 |
|------|------------|
| CLDC 1.1 API | `org.microemu:cldcapi11:2.0.4` |
| MIDP 2.0 API | `org.microemu:midpapi20:2.0.4` |
| JSR-82 API | `org.microemu:microemu-jsr-82:2.0.4` |
| JSR-75 API | `org.microemu:microemu-jsr-75:2.0.4` |
| ECJ（Eclipse 4.20） | `org.eclipse.jdt:ecj:3.26.0` |
| ProGuard | `net.sf.proguard:proguard-base:6.0.3` |

构建完成后，可在 `dist/` 目录下获取 JAR 与 JAD 文件。
