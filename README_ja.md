[English](README_en.md) | [繁體中文](README.md) | [简体中文](README_zh_cn.md)

# 東方氷幻鏡 ～ Misfortune Mirror's Tale

## Info

『東方氷幻鏡 Plus V1.0.3』をベースに移植。

オリジナル版のバグ修正に加え、多数の利便性向上を行っています。

---

## Item

[TODO](https://github.com/Magstic/Touhou_Misfortune_MirrorS_Tale/issues/1) を参照。

---

## Play

### Minimum specs

| Item | Recommended |  | Item | Recommended |
|------|------|------|------|------|
| Platform | *MIDP 2.0, CLDC 1.1* |  | CPU & RAM | *≥ 234 MHz*, *JVM ≥ 2.0 MB* |
| Screen | *240 x 240* |  |Optional | *JSR-82、JSR-75*|

### Recommended specs

| Item | Recommended |  | Item | Recommended |
|------|------|------|------|------|
| Platform | *MIDP 2.0, CLDC 1.1* |  | CPU & RAM | *≥ 600 MHz*, *JVM ≥ 4.0 MB* |
| Screen | *240 x 240* |  |Optional | *JSR-82、JSR-75*|

---

### PC

カスタマイズ版 [**FreeJ2ME-Plus**](https://github.com/Magstic/freej2me-plus/releases/tag/VirtualMIDISynth) + [**Nokia 3110c.dls**](https://musical-artifacts.com/artifacts/828) の組み合わせを推奨します。Docomo のミキシングに最も近い環境かもしれません。

---

### Android

カスタム SF2 サウンドフォントに対応した [JL-MOD](https://github.com/woesss/JL-Mod/releases) が最良の選択です。

もちろん、ガラスをタップして STG をプレイしたいとは思わないでしょうし、SF2 を使っても Android のミキシングは相変わらずひどいですが lol。

また、JL-MOD の JSR-82 実装にもいくつか問題があります。

---

### Real Device

実機向けに多数の最適化を行っています。

パフォーマンスのボトルネックはヒープサイズではなく、雨のような弾幕の描画が CPU 処理能力の大半を消費する点にあります。

現在は、S60 V3 端末で Easy、Normal、Hard を比較的スムーズにプレイできることを最適化の目標としています。一部の Sony Ericsson 端末でも同様にプレイできます。

S40 V5 には JVM のインタープリターを高速化するハードウェア支援があるため、ヒープが小さく CPU 性能が低めでも、アルファ描画を無効にすれば Easy と Normal を比較的スムーズにプレイできます。

---

## Build

本プロジェクトのビルドに IDE や SDK のインストールは不要です。ルートディレクトリに同梱されている `build.xml` で、すべてのビルド作業を実行できます。

[中文（繁體）](docs/build.md) | [中文（简体）](docs/build_zh_cn.md) | [English](docs/build_en.md) | [日本語](docs/build_ja.md)

---

## Staff

**Framework**：

- Magstic：フレームワークの設計、ドキュメント執筆、限定的なコードレビュー

**Coder**：

- ChatGPT 5.2：コード整理・リファクタリング作業の 99%、最終チェック（2026.01）
- Claude 4.6 Opus：Markdown ドキュメントの英語・日本語翻訳、ゲーム内の字形チェック（2026.02）
- 5.6 Sol：アーキテクチャの初期リファクタリング（2026.08）
- 6.1 Sol：アーキテクチャの最終リファクタリングと整理、ロジック修正、最終チェック（2026.10）
- 6 Astra：Markdown ドキュメントの英語・日本語翻訳、ゲーム内の字形チェック（2026.10）

**Support**

- SilentTalk：ビルドとパフォーマンス最適化に関する技術支援


## Thanks

**L-Garden** & **トッパツプラン** — この驚異的な STG を完成させた

**Keitai Archive** — この作品をアーカイブしてくれた

**Fusion Pixel 12Px** — ビットマップフォント

**Salvation_Jarsis** — Bluetooth による Replay 共有のアイデアを提供

---

## About

本プロジェクトは『東方氷幻鏡 ～ Misfortune Mirror's Tale』の派生作品です。

『東方氷幻鏡 ～ Misfortune Mirror's Tale』は東方 Project の二次創作であり、同人サークル L-Garden & トッパツプラン が開発、著作権は L-Garden に帰属します。

L-Garden の関係者の方で、本プロジェクトの存在を望まない場合は、深くお詫び申し上げます。[ホームページ](https://magstic.art/)の Email からいつでもご連絡ください。

### Clone してローカルでビルド・プレイすることは歓迎しますが、**いかなる形式の JAR 再配布も禁止** です。
