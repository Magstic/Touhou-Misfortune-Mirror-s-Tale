[English](build_en.md) | [繁體中文](build.md)

# ビルド

## 必要な環境

1. **[OpenJDK 8](https://www.azul.com/downloads/?version=java-8-lts&package=jdk#zulu)**
   - Zulu、Temurin などの OpenJDK 8 を使用できます。`JAVA_HOME` を JDK 8 に設定してください。
2. **[Apache Ant](https://ant.apache.org/bindownload.cgi)**
   - Windows：アーカイブを展開し、Ant の `bin` を `PATH` に追加します。
   - Linux：`sudo apt install ant`。

初回ビルドには Maven への接続が必要です。

Ant が Apache Ivy 2.5.3 を自動ダウンロードし、Ivy 経由で以下の依存ライブラリを取得します。

| 依存ライブラリ | Maven 座標 |
|----------------|------------|
| CLDC 1.1 API | `org.microemu:cldcapi11:2.0.4` |
| MIDP 2.0 API | `org.microemu:midpapi20:2.0.4` |
| JSR-82 API | `org.microemu:microemu-jsr-82:2.0.4` |
| ProGuard | `net.sf.proguard:proguard-base:6.0.3` |

## ビルドコマンド

| コマンド | 説明 |
|----------|------|
| `ant` | 通常ビルド |
| `ant clean package` | ビルド成果物を削除して再ビルド |
| `ant run` | 通常ビルド＋エミュレータ起動 |
| `ant clean` | `build/` と `dist/` を削除し、依存ライブラリのキャッシュは保持 |

ビルド完了後、`dist/` に JAR と JAD が生成されます。

`run` を使用する場合は、まず `freej2me.jar` をプロジェクトの `emulator` ディレクトリに配置してください。
