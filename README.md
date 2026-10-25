[简体中文](README_zh_cn.md) | [日本語](README_ja.md) | [English](README_en.md)

# 東方氷幻鏡 ～ Misfortune Mirror's Tale

> [!NOTE]
> 東方冰幻鏡 正在重製中！
> 請見 Ciena 在 X 上的推文：[Tweet](https://x.com/ciena_game/status/2073778918889558485)

## Info

基於『東方冰幻鏡 Plus V1.0.3』進行移植。

在修復原版 BUG 的同時，進行了諸多的便利性改進。

---

## Item

請見 [TODO](https://github.com/Magstic/Touhou_Misfortune_MirrorS_Tale/issues/1)。

---

## Play

### Minimum specs

| Item | Recommended |  | Item | Recommended |
|------|------|------|------|------|
| Platform | *MIDP 2.0, CLDC 1.1* |  | CPU & RAM | *≥ 234 MHz*, *JVM ≥ 2.0 MB* |
| Screen | *240 x 240* |  |Optional | *JSR-82，JSR-75*|

### Recommended specs

| Item | Recommended |  | Item | Recommended |
|------|------|------|------|------|
| Platform | *MIDP 2.0, CLDC 1.1* |  | CPU & RAM | *≥ 600 MHz*, *JVM ≥ 4.0 MB* |
| Screen | *240 x 240* |  |Optional | *JSR-82，JSR-75*|

---

### PC

推薦使用客製化的 [**FreeJ2ME-Plus**](https://github.com/Magstic/freej2me-plus/releases/tag/VirtualMIDISynth) + [**Nokia 3110c.dls**](https://musical-artifacts.com/artifacts/828)，這可能是最接近 Docomo 混音的遊玩方式。

---

### Android

支援自訂 SF2 音源的 [JL-MOD](https://github.com/woesss/JL-Mod/releases) 是至高選擇。

當然，我保證你不會想戳玻璃 PLAY STG，更不提儘管有 SF2 加持，但混音依然垃圾的 ANDROID lol。

此外，JL-MOD 的 JSR-82 也存在一些問題。

---

### Real Device

程式已針對真機進行了諸多最佳化。

效能瓶頸和 Heap 大小無關，彈幕如雨般的渲染註定會吃掉大部分的 CPU。

現在最佳化的基準是可以使用 S60V3 的裝置較為流暢地遊玩 Easy，Normal，Hard 難度，一些索愛機型也可以做到。

至於 S40V5，因為其有 JVM 硬體解釋加速，即便 Heap 和 CPU 糟糕了點，也可以在關閉 Alpha 渲染的情況下較為流暢的遊玩 Easy，Normal 難度。

---

## Build

本專案的建構無須任何 IDE 和 SDK，根路徑下自帶的 `build.xml` 可以完成 100% 的工作。

[中文（繁體）](docs/build.md) | [中文（简体）](docs/build_zh_cn.md) | [English](docs/build_en.md) | [日本語](docs/build_ja.md)

---

## Staff

**Framework**：

- Magstic：框架設計，文檔撰寫，有限的程式碼審查

**Coder**：

- ChatGPT 5.2：99% 的程式碼整理，重構工作，最終檢查（2026.01）
- Claude 4.6 Opus：MD 文檔的英/日翻譯，遊戲内字形的檢查（2026.02）
- 5.6 Sol：架構的初步重構（2026.08）
- 6.1 Sol：架構的最終重構、整理，邏輯修復，最終檢查（2026.10）
- 6 Astra：MD 文檔的英/日翻譯，遊戲内字形的檢查（2026.10）

**Support**

- SilentTalk：構建，效能優化的技術支援

---

## Thanks

**L-Garden** & **トッパツプラン**：完成了如此驚人的 STG

**Keitai Archive**：存檔了這款作品

**Fusion Pixel 12Px**：提供位圖字型

**Salvation_Jarsis**：提供了藍牙共享 Replay 的思路

---

## About

本專案是基於原遊戲反編譯邏輯的 MIDP 移植作品。

《東方氷幻鏡 ～ Misfortune Mirror's Tale》是《東方 Project》的衍生作品，由同人社團 L-Garden & トッパツプラン 開發。原遊戲及《東方 Project》的相關權利分別歸各自權利人所有。

若您是 L-Garden 所屬，且不希望這種行為，我深感歉意，且隨時歡迎使用 [主頁](https://magstic.art/) 的 Email 聯絡我。

---

## License

移植版中新增的部分採用 [自訂許可協議](LICENSE)。

允許原始碼閱覽、修改及個人建置，但 **禁止以任何形式散布本專案的遊戲 JAR，包含修改及重新封裝的版本**。

協議以繁體中文正文為準。

原作及其他第三方內容依各自授權處理。各部分的來源、歸屬與授權狀態見 [來源與權利清單](docs/provenance.md)。