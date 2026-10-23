[English](build_en.md) | [繁體中文](build.md) | [简体中文](build_zh_cn.md)

# ビルド

## 必要な環境

1. **[OpenJDK 8](https://www.azul.com/downloads/?version=java-8-lts&package=jdk#zulu)**
   - Zulu、Temurin などの OpenJDK 8 を使用できます。`JAVA_HOME` を JDK 8 に設定してください。
2. **[Apache Ant](https://ant.apache.org/bindownload.cgi)**
   - Windows：アーカイブを展開し、Ant の `bin` を `PATH` に追加します。
   - Linux：`sudo apt install ant`（Debian 系の場合）。
   - Mac：`brew install ant`（Homebrew を推奨します。使用しない場合は、Ant を手動でダウンロードし、環境変数を設定してください）。


## Windows/Linux/Mac

プロジェクトをローカルにクローンまたはダウンロードし、プロジェクトのルートディレクトリで以下を実行します。

```bash
ant
```


## Android

**Termux** をインストールします：[GitHub](https://github.com/termux/termux-app) | [F-Droid](https://f-droid.org/zh_Hant/packages/com.termux/)

必要な権限を付与した後、Termux 内で必要な依存パッケージをインストールします。

```bash
pkg update
pkg install git ant openjdk-21 openjdk-21-x fontconfig
```

プロジェクトをローカルにクローンし、ビルドします。

```bash
git clone https://github.com/Magstic/Touhou-Misfortune-Mirror-s-Tale.git
cd Touhou-Misfortune-Mirror-s-Tale
ant
```

ビルド完了後、成果物を Android 端末の Download フォルダーにコピーします。

```bash
termux-setup-storage
cp dist/*.jar dist/*.jad ~/storage/downloads/
```


## ビルドコマンド

| コマンド | 説明 |
|----------|------|
| `ant` | 通常ビルド |
| `ant clean package` | ビルド成果物を削除して再ビルド |
| `ant clean` | `build/` と `dist/` を削除し、依存ライブラリのキャッシュは保持 |


初回ビルドには Maven への接続が必要です。

Ant が Apache Ivy 2.5.3 を自動ダウンロードし、Ivy 経由で以下の依存ライブラリを取得します。

| 依存ライブラリ | Maven 座標 |
|----------------|------------|
| CLDC 1.1 API | `org.microemu:cldcapi11:2.0.4` |
| MIDP 2.0 API | `org.microemu:midpapi20:2.0.4` |
| JSR-82 API | `org.microemu:microemu-jsr-82:2.0.4` |
| JSR-75 API | `org.microemu:microemu-jsr-75:2.0.4` |
| ECJ（Eclipse 4.20） | `org.eclipse.jdt:ecj:3.26.0` |
| ProGuard | `net.sf.proguard:proguard-base:6.0.3` |

ビルド完了後、`dist/` に JAR と JAD が生成されます。
