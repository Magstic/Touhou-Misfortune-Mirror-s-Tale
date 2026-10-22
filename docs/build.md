[English](build_en.md) | [日本語](build_ja.md)

# Build

## 環境需求

1. **[OpenJDK 8](https://www.azul.com/downloads/?version=java-8-lts&package=jdk#zulu)**
   - Zulu、Temurin 或其他 OpenJDK 8 發行版皆可。請將 `JAVA_HOME` 指向 JDK 8。
2. **[Apache Ant](https://ant.apache.org/bindownload.cgi)**
   - Windows：解壓後將 Ant 的 `bin` 加入 `PATH`。
   - Linux：`sudo apt install ant`（以 Debian 系為例）。
   - Mac：`brew install ant`（推薦 Homebrew。若無，需手動下載配置變數）


## Windows/Linux/Mac

把專案 Clone 或者下載到本地，在專案根路徑下執行：

```bash
ant
```


## Android

安裝 **Termux**：[Github](https://github.com/termux/termux-app) | [F-droid](https://f-droid.org/zh_Hant/packages/com.termux/)

授予必要權限後，在 Termux 內安裝必要依賴：

```bash
pkg update
pkg install git ant openjdk-21 openjdk-21-x fontconfig
```

Clone 專案到本地，並構建：

```bash
git clone https://github.com/Magstic/Touhou-Misfortune-Mirror-s-Tale.git
cd Touhou-Misfortune-Mirror-s-Tale
ant
```

構建完成後，匯出建置產物至 Android 裝置的 Download 資料夾：

```bash
termux-setup-storage
cp dist/*.jar dist/*.jad ~/storage/downloads/
```


## 建置指令

| 指令 | 說明 |
|------|------|
| `ant` | 正常建置 |
| `ant clean package` | 清除建置產物，重新建置 |
| `ant clean` | 清理 `build/` 與 `dist/`，保留依賴快取 |


首次建置需連線至 Maven。

Ant 會自動下載 Apache Ivy 2.5.3，再透過 Ivy 取得以下依賴：

| 依賴 | Maven 座標 |
|------|------------|
| CLDC 1.1 API | `org.microemu:cldcapi11:2.0.4` |
| MIDP 2.0 API | `org.microemu:midpapi20:2.0.4` |
| JSR-82 API | `org.microemu:microemu-jsr-82:2.0.4` |
| ECJ（Eclipse 4.20） | `org.eclipse.jdt:ecj:3.26.0` |
| ProGuard | `net.sf.proguard:proguard-base:6.0.3` |

建置完成後，可在 `dist/` 目錄下取得 JAR 與 JAD 檔。