# 資產來源與權利劃分

本檔記錄專案各部分的來源、移植工作及適用授權。


## 分類原則

| 分類 | 內容 | 適用授權 |
|---|---|---|
| 原作內容/衍生 | 原遊戲資產、反編譯衍生資產 | 原遊戲相關權利人的授權 |
| 移植原創 | 本專案新增的程式碼、檔案及素材 | 移植方有權授權的部分適用 LICENSE |
| 混合來源 | 同時包含原作內容與移植新增部分 | 依其來源分別適用授權 |
| 其他第三方內容 | 外部程式碼、字型及建置依賴 | 各來源的授權 |


## 原始遊戲

DoJa 版《東方氷幻鏡 Plus》1.0.3：

- 東方氷幻鏡 Plus.jar
- 東方氷幻鏡 Plus.sp
- 東方氷幻鏡 Plus.jam

遊戲內製作名單：

| 部分 | 署名 |
|---|---|
| 企劃、程式 | シーナ / L-Garden |
| 圖像 | ふー、Radram |
| 音樂 | Scirocco |
| 開發 | L-Garden（シーナ）、トッパツプラン（ふー） |
| 東方 Project 原作 | 上海アリス幻樂団 / ZUN |

因為目前無法聯絡到原作者，所以專案維護者現無原作者對遊戲資產的授權。

東方 Project 的相關規範見[二次創作指引](https://touhou-project.news/guideline/)，原遊戲與東方 Project 的權利分別由各自權利人行使。

## 程式碼

| 程式 | 來源 | 原創 |
|---|---|---|
| `src/touhou/GameCore.java`、`Player.java`、`BulletSystem.java`、`EnemyBulletSystem.java`、`DropItemSystem.java`、`ScoreSystem.java` | 流程/射擊/彈幕/碰撞/掉落/計分/數值 | 模組協調，邏輯修正與效能改寫 |
| `src/touhou/stage/*.java` | 道中與 BOSS 行動/符卡/時間/參數 | 介面拆分，資產生命週期及載入管理 |
| `src/touhou/battle/*.java` | 戰鬥規則/符卡目錄/資源選擇/畫面配置 | 模組結構，資料介面及呈現分工 |
| `src/touhou/Fixed.java`、`Trig.java` | 遊戲數值計算及查表 | 工具抽離與整理 |
| `src/touhou/GameOptions.java`、`GameProgress.java`、`UnlockFlags.java`、`MusicRoomSettings.java`、`ResultStats.java` | 存檔語義/成績/解鎖條件 | 新增設定，測試開關及儲存管理 |
| `src/touhou/SpStore.java` | Scratchpad 位址/資料語義 | MIDP RMS 儲存實作 |
| `src/touhou/AudioSystem.java`、`SoundEffectSystem.java`、`BgmCatalog.java`、`VibrationController.java` | 曲目/事件對應/遊戲時序 | MIDP 播放，快取，二段循環及震動介接 |
| `src/touhou/ImageBank.java`、`SpriteCatalog.java`、`SpriteRenderer.java`、`SpriteSheetCatalog.java`、`AdditiveBackground.java` | 精靈索引/裁切資料/畫面效果 | 針對 MIDP 的解碼，回收，快取，混色及繪製最佳化 |
| `src/touhou/MainMidlet.java`、`SceneController.java` | 接入的原有遊戲流程 | MIDP 入口及場景生命週期 |
| `src/touhou/ResourceLoader.java`、`ResourcePaths.java`、`ResourceException.java`、`StorageException.java` | 引用的資源名稱 | 資源存取及錯誤處理 |
| `src/touhou/FixedSlotPool.java`、`EnemyTargetRegistry.java`、`Debug.java` | 引用的戰鬥資料 | 池管理，資產投影及偵錯實作 |
| `src/touhou/replay/*.java` | 錄製與重播流程/RLE 編碼及容量 | 自訂亂數，64 位元組標頭，關卡快照格式，控制器拆分，儲存與完整性管理 |
| `src/touhou/replay/ReplayFileService.java`、`ReplayShareCodec.java`、`ReplayShareService.java` | 引用的 Replay 欄位及遊戲狀態 | REP 檔案存取與分享封裝 |
| `src/touhou/bt/BtSppLink.java` | —— | JSR-82 SPP 通訊，技術思路的提供者為「Salvation_Jarsis」 |
| `src/touhou/i18n/*.java` | `TextId`/`SpellText` /結構描述所引用的文字/符卡與資料順序 | 語言選擇，文字載入與資源包機制 |
| `src/touhou/ui/*.java` | 原畫面/選項/內容/場景切換規則 | MIDP 適配，Replay，新增 Story（文字來自 L-Garden 官網）,字型及動畫 |
| `src/touhou/ui/SoftkeyBar.java`、`StartupLanguageMenu.java`、`ReplayFileScreen.java` | 使用的原版文字及素材 | 螢幕軟鍵,初次語言選擇及 REP 檔案選擇介面 |
| `src/touhou/font/BitmapFont.java`、`src/touhou/ui/UiTextLayout.java`、`src/touhou/ui/FontTextMeasurer.java`、`src/touhou/ui/UiDraw.java` 的字型支援 | SilentTalk 提供的字型核心程式碼 | 字型與排版的適配，整合及改寫 |
| `tools/FontDatBuilder.java`、`I18nPackBuilder.java`、`CcDatCropViewer.java` | 讀取的原版精靈與文字資料 | 字型建置，語言包建置與裁切檢視工具 |
| `tools/BgmLiteBuilder.java` | 原曲聲部配置 | MIDI 簡化工具 |
| `build.xml`、`ivy.xml`、`ivysettings.xml`、`.gitignore`、`.gitattributes` | —— | 建置及程式碼庫維護 |
| `README*.md`、`docs/*.md` | 引用的遊戲名稱/製作名單/原文/第三方資料 | 移植文件的撰寫與整理 |


## 素材與資料

| 素材 | 分類 | 來源與移植作業 |
|---|---|---|
| `res/stage.dat` | 原作內容 | —— |
| `res/cc.dat` | 混合來源 | 原版裁切/索引資料，加上拆圖、重編號、中文開幕文字及新增 UI 資料 |
| `res/icon.png` | 原作內容 | 經 `icon48.gif` 轉檔 |
| `res/sp/000.png` 至 `126.png` | 原作衍生 | 原版圖像的拆分、裁切、調色、索引色轉換及重編號 |
| `res/sp/043_zh.png`、`057_zh.png`、`069_zh.png`、`089_zh.png`、`094_zh.png`、`109_zh.png`、`116_zh.png` | 混合來源 | 原版開幕文字的中文翻譯修圖，與新增美術編排 |
| `res/sp/127.png` | 移植新增 | 語言選擇圖的編排與文字製作，使用字型為 Fusion Pixel 12Px（OFL1.1 許可） |
| `res/sp/128.png`、`129.png` | 移植新增 | 維護者自行繪製的軟鍵背景及初始化提示框 |
| `res/bgm/**` | 原作衍生 | 原遊戲 MLD 音樂的 MIDI 轉換，含前奏/循環分段 |
| `res/bgm-lite/**` | 原作衍生 | 上述 MIDI 刪減聲部的簡化版本 |
| `res/se/alice.wav`、`res/se/shot.wav` | 移植新增 | 維護者自行錄製 |
| `res/se/back.wav`、`cd.wav`、`crash.wav`、`crash2.wav`、`graze.wav`、`life.wav`、`marisa.wav`、`nav.wav`、`pause.wav`、`power.wav`、`reimu.wav`、`select.wav` | 原作衍生 | 錄製自原遊戲+修音處理的音效 |
| `res/i18n/ja/dialog/**`、`spcard.dat`、`sprac.dat` | 原作內容 | 日文對話及符卡資料的編碼轉換/整理 |
| `res/i18n/ja/story.txt` | 原作內容 | L-Garden 官網 Story 文字的整理 |
| `res/i18n/ja/ending_*.txt` | 原作內容 | 原遊戲結局文字的移植整理 |
| `res/i18n/en/**`、`res/i18n/zht/**` | 混合來源 | 原版內容的翻譯、校訂及移植新增文字 |
| `res/i18n/*/ui.txt` | 混合來源 | 原版 UI/製作名單，翻譯與新增設定、檔案操作及相容性提示文字 |


### 字型

| 路徑 | 來源 | 授權 |
|---|---|---|
| `font/ja.ttf`、`font/zh_hans.ttf`、`font/zh_hant.ttf` | Fusion Pixel 12px Prop；TakWolf；保留字型名稱 Fusion Pixel | SIL Open Font License 1.1，見 [font/OFL.txt](../font/OFL.txt) |
| `font/LICENSE/ark-pixel.txt`、`cubic-11.txt`、`galmuri.txt` | Fusion Pixel 來源字型的隨附聲明 | 各檔案所載條款 |
| 建置產生的 `font.dat` | 上述字型取字、點陣化及重新封裝的資料 | 原字型（OFL 1.1）授權許可 |

字型的使用、擷取、修改及散布依原有條款：[OFL FAQ](https://openfontlicense.org/ofl-faq/)。


## 建置依賴與輸出

| 路徑或產物 | 授權 |
|---|---|
| `.deps/` | 依各自上游授權，版本與來源見 `build.xml`、`ivy.xml`、`ivysettings.xml` |
| `build/classes/`、`build/raw.jar`、`dist/*.jar` | 混合來源的遊戲建置產物，適用 LICENSE 第四條及相關第三方授權 |
| `build/generated-res/i18n/` | 語言包及字型的建置輸出，沿用各內含內容的授權 |
| 遊戲匯出的 `.rep` | Replay 操作紀錄，依遊戲的 Replay 分享規則處理 |
