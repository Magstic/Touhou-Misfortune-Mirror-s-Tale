[繁體中文](README.md) | [日本語](README_ja.md) | [English](README_en.md)

# 東方氷幻鏡 ～ Misfortune Mirror's Tale

> [!NOTE]
> 东方冰幻镜正在重制中！
> 请见 Ciena 在 X 上的推文：[Tweet](https://x.com/ciena_game/status/2073778918889558485)

## Info

基于《东方冰幻镜 Plus V1.0.3》进行移植。

在修复原版 BUG 的同时，进行了诸多便利性改进。

![](./img.webp)

---

## Item

请见 [TODO](https://github.com/Magstic/Touhou_Misfortune_MirrorS_Tale/issues/1)。

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

推荐使用定制版 [**FreeJ2ME-Plus**](https://github.com/Magstic/freej2me-plus/releases/tag/VirtualMIDISynth) + [**Nokia 3110c.dls**](https://musical-artifacts.com/artifacts/828)，这可能是最接近 Docomo 混音的游玩方式。

---

### Android

我不推荐使用 Android，因为戳玻璃不但难受，其内置的 SONiVOX 在 MIDI 合成方面也非常糟糕。

---

### Real Device

程序已针对真机进行了诸多优化。

特别是 S40V5。因为其在 JVM 上有着远胜于 S60V3 的硬件解释加速，即便 Heap 和 CPU 差了点，也可以在关闭华丽背景（避免 OOM）和 SFX 的情况下极为流畅地游玩 Easy、Normal、Hard、Extra！

这当然也适用于一些索爱机型。

---

## Build

本项目的构建无须任何 IDE 和 SDK，根目录下自带的 `build.xml` 可以完成 100% 的工作。

[中文（繁體）](docs/build.md) | [中文（简体）](docs/build_zh_cn.md) | [English](docs/build_en.md) | [日本語](docs/build_ja.md)

---

## Staff

**Framework**：

- Magstic：框架设计、文档编写、有限的代码审查

**Coder**：

- ChatGPT 5.2：99% 的代码整理、重构工作、最终检查（2026.01）
- Claude 4.6 Opus：Markdown 文档的英／日翻译、游戏内字形检查（2026.02）
- 5.6 Sol：架构的初步重构（2026.08）
- 6.1 Sol：架构的最终重构与整理、逻辑修复、最终检查（2026.10）
- 6 Astra：Markdown 文档的英／日翻译、游戏内字形检查（2026.10）

**Support**

- SilentTalk：构建与性能优化的技术支持

---

## Thanks

**L-Garden** & **トッパツプラン**：完成了如此惊人的 STG

**Keitai Archive**：存档了这款作品

**Fusion Pixel 12Px**：提供位图字体

**Salvation_Jarsis**：提供了通过蓝牙共享 Replay 的思路

---

## About

本项目是基于原游戏反编译逻辑的 MIDP 移植作品。

《東方氷幻鏡 ～ Misfortune Mirror's Tale》是《东方 Project》的衍生作品，由同人社团 L-Garden & トッパツプラン 开发。原游戏及《东方 Project》的相关权利分别归各自权利人所有。

若您是 L-Garden 所属，且不希望这种行为，我深感歉意，并随时欢迎使用[主页](https://magstic.art/)的 Email 联系我。

---

## License

移植版中新增的部分采用[自定义许可协议](LICENSE)。

允许源码阅览、修改及个人构建，但 **禁止以任何形式分发本项目的游戏 JAR，包括修改及重新打包的版本**。

协议以繁体中文正文为准。

原作及其他第三方内容依各自授权处理。各部分的来源、归属与授权状态见[来源与权利清单](docs/provenance.md)。
