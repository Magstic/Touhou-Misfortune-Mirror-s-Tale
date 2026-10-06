[English](build_en.md) | [日本語](build_ja.md)

# Build

## 環境需求

1. **[OpenJDK 8](https://www.azul.com/downloads/?version=java-8-lts&package=jdk#zulu)**
   - Zulu、Temurin 或其他 OpenJDK 8 發行版皆可，請將 `JAVA_HOME` 指向 JDK 8。
2. **[Apache Ant](https://ant.apache.org/bindownload.cgi)**
   - Windows：解壓後將 Ant 的 `bin` 加入 `PATH`。
   - Linux：`sudo apt install ant`。

首次建置需連線至 Maven。

Ant 會自動下載 Apache Ivy 2.5.3，再透過 Ivy 取得以下依賴：

| 依賴 | Maven 座標 |
|------|------------|
| CLDC 1.1 API | `org.microemu:cldcapi11:2.0.4` |
| MIDP 2.0 API | `org.microemu:midpapi20:2.0.4` |
| JSR-82 API | `org.microemu:microemu-jsr-82:2.0.4` |
| ProGuard | `net.sf.proguard:proguard-base:6.0.3` |

## 建置指令

| 指令 | 說明 |
|------|------|
| `ant` | 正常建置 |
| `ant clean package` | 清除建置產物，重新建置 |
| `ant run` | 正常建置並啟動模擬器 |
| `ant clean` | 清理 `build/` 與 `dist/`，保留依賴快取 |

建置完成後，可在 `dist/` 目錄下取得 JAR 與 JAD 檔。

若想使用 `run` 指令，您需要先將 `freej2me.jar` 放到專案的 `emulator` 下。