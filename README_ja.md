[English](README_en.md) | [繁體中文](README.md) | [简体中文](README_zh_cn.md)

# 東方氷幻鏡 ～ Misfortune Mirror's Tale

> [!NOTE]
> 『東方氷幻鏡』のリメイクが進行中！
> Ciena さんの X の投稿をご覧ください：[Tweet](https://x.com/ciena_game/status/2073778918889558485)

## Info

『東方氷幻鏡 Plus V1.0.3』をベースに移植。

オリジナル版のバグ修正に加え、多数の利便性向上を行っています。

![](./img.webp)

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

Android でのプレイはおすすめしません。ガラスをタップする操作は快適ではなく、内蔵の SONiVOX による MIDI 合成も非常に質が悪いためです。

---

### Real Device

実機向けに多数の最適化を行っています。

特に S40 V5 は、JVM のハードウェア支援によるインタープリターの高速化が S60 V3 を大きく上回ります。ヒープが小さく CPU 性能が低めでも、豪華な背景（OOM を避けるため）と SFX を無効にすれば、Easy、Normal、Hard、Extra を非常にスムーズにプレイできます！

一部の Sony Ericsson 端末にも同じことが当てはまります。

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

---

## Thanks

**L-Garden** & **トッパツプラン** — この驚異的な STG を完成させた

**Keitai Archive** — この作品をアーカイブしてくれた

**Fusion Pixel 12Px** — ビットマップフォント

**Salvation_Jarsis** — Bluetooth による Replay 共有のアイデアを提供

---

## About

本プロジェクトは、原作ゲームを逆コンパイルしたロジックに基づく MIDP 移植作品です。

『東方氷幻鏡 ～ Misfortune Mirror's Tale』は、同人サークル L-Garden & トッパツプラン が開発した東方 Project の二次創作ゲームです。原作ゲームおよび東方 Project の権利は、それぞれの権利者に帰属します。

L-Garden の関係者の方で、本プロジェクトの存在を望まない場合は、深くお詫び申し上げます。[ホームページ](https://magstic.art/)の Email からいつでもご連絡ください。

---

## License

移植で追加した部分には、独自の[許諾契約](LICENSE)を適用します。

ソースコードの閲覧・改変・個人ビルドを認めますが、**改変版や再パッケージ版を含め、本プロジェクトのゲーム JAR をいかなる形でも配布することを禁止します**。

繁体字中国語の契約本文を正文とします。

原作ゲームおよびその他の第三者コンテンツには、それぞれの許諾条件が適用されます。各部分の出典・権利帰属・許諾状況は、[出典・権利一覧](docs/provenance.md)（繁体字中国語）を参照してください。
