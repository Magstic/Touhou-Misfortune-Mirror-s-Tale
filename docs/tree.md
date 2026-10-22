# Project Tree

## Directory Structure

```
Touhou_Misfortune_MirrorS_Tale/
├── build.xml                        # Ant build script (Ivy dependencies, ProGuard CLDC preverification, JAD)
├── ivy.xml                          # Pinned J2ME APIs and ProGuard dependencies
├── ivysettings.xml                  # Maven Central resolver and local dependency cache
├── .gitignore                       # Git ignore rules (build output, dependencies, emulator runtime, etc.)
├── README.md                        # Project overview
├── TODO.MD                          # Development task tracker
│
├── src/touhou/                      # ---- Java source (MIDP 2.0 / CLDC 1.1) ----
│   ├── MainMidlet.java              #   MIDlet entry point (packaged as root-level ThMirror.class)
│   ├── GameCore.java                #   Main game loop, core orchestrator (GameCanvas + Runnable)
│   ├── SceneController.java         #   Scene routing: UI / Battle / Ending / StaffRoll
│   ├── ImageBank.java               #   Sprite image loader, cache, StageEX color variant generator
│   ├── SpriteSheetCatalog.java       #   Stable logical IDs only; no filenames or source geometry
│   ├── ResourceLoader.java           #   Single required/optional classpath resource loader
│   ├── ResourcePaths.java            #   Canonical classpath resource paths
│   ├── ResourceException.java        #   CLDC-compatible required-resource failure
│   ├── BgmCatalog.java               #   Current 18-track BGMP schema
│   ├── SpriteCatalog.java           #   Final cc.dat: image table, geometry and reflection modes
│   ├── SpriteRenderer.java          #   Shared sprite rendering: trim, logical windows, anchor, alpha and clip
│   ├── BulletSystem.java            #   Shared player/enemy bullet, laser and effect store
│   ├── EnemyBulletSystem.java       #   Enemy bullet spawning and player-collision rules
│   ├── EnemyTargetRegistry.java     #   Targeting projection of StageRuntime enemies
│   ├── FixedSlotPool.java           #   Deterministic fixed-capacity slot allocator
│   ├── DropItemSystem.java          #   Drop item pool (power, point, bomb, life, etc.)
│   ├── Player.java                  #   Player character: movement, shooting, bomb, death/respawn
│   ├── ScoreSystem.java             #   Score events, graze, life/bomb awards, spell bonus
│   ├── AudioSystem.java             #   BGM playback (MIDI from bgm.dat)
│   ├── SoundEffectSystem.java       #   SE playback (WAV)
│   ├── GameOptions.java             #   User settings persistence (RMS)
│   ├── GameProgress.java            #   Save data: unlocks, high scores (RMS)
│   ├── UnlockFlags.java             #   Stage / Extra / Spell Practice unlock state
│   ├── MusicRoomSettings.java       #   Music room unlock tracking
│   ├── ResultStats.java             #   Singleton: playtime, boot time, spell stats -> GameProgress
│   ├── SpStore.java                 #   Scratch-pad storage (RMS)
│   ├── VibrationController.java     #   Vibration motor control
│   ├── Fixed.java                   #   Fixed-point 16.16 utilities
│   ├── Trig.java                    #   Fixed-point sin/cos lookup table
│   ├── Debug.java                   #   Debug overlay & profiling
│   │
│   ├── battle/                      #   -- Battle subsystem --
│   │   ├── BattleScene.java         #     Battle-state coordinator: handles stage clear panel, pause flow, spell practice end flow; delegates gameplay ticks to `GameCore` (battleMain* methods) and delegates rendering to `BattleRenderer`
│   │   ├── BattleEngine.java        #     Core gameplay tick: collision, bullets, bombs, death
│   │   ├── BattleRenderer.java      #     Read-only battle-frame drawing pipeline
│   │   ├── BattleRenderRules.java   #     Pure resource selection, layout, fade and clipping rules
│   │   ├── BattleEnemyVisuals.java  #     Enemy sprite resolution and update-time animation transitions
│   │   ├── BattleVisualState.java   #     Update-owned flash, fade, mask and resolved sprite state
│   │   ├── BattleSessionConfig.java #     Immutable battle mode, stage, difficulty and character configuration
│   │   ├── SpellPracticeCatalog.java#     Single spell-practice metadata catalog
│   │   ├── BattleStageFlow.java     #     Adapter: implements StageRuntime.StageFlow -> GameCore
│   │   ├── BattleStageHost.java     #     Adapter: implements StageRuntime.Host -> GameCore
│   │   ├── BattleBossHost.java      #     Adapter: implements BossController.Host -> GameCore
│   │   ├── BattleContinueSystem.java#     Continue count management & consumption
│   │   ├── BattleHudData.java       #     HUD data access interface
│   │   ├── BattleHudModel.java      #     HUD data model (implements BattleHudData)
│   │   └── BattleMath.java          #     Fixed-point speed/angle calculations (speedCos, speedSin, normalizeDeg)
│   │
│   ├── stage/                       #   -- Stage script & boss --
│   │   ├── StageRuntime.java        #     Stage tick driver: script, waves, transitions
│   │   ├── StagePreloader.java      #     Pre-stage resource loading (final step calls System.gc())
│   │   ├── StageDat.java            #     stage.dat parser (wave script)
│   │   ├── StageDochu.java          #     Mid-stage (dochu) enemy shooting patterns
│   │   ├── BossController.java      #     Boss FSM: HP, spell timer, phases; hosts BossStageLogic[7]
│   │   ├── BossStageLogic.java      #     Boss logic interface (tickBoss / tickMidboss)
│   │   ├── AbstractBossStageLogic.java#   Abstract base: state sync with BossController, shared fields
│   │   ├── Stage1BossLogic.java     #     Stage 1 boss patterns
│   │   ├── Stage2BossLogic.java     #     Stage 2 boss patterns
│   │   ├── Stage3BossLogic.java     #     Stage 3 boss patterns
│   │   ├── Stage4BossLogic.java     #     Stage 4 boss patterns
│   │   ├── Stage5BossLogic.java     #     Stage 5 boss patterns
│   │   ├── Stage6BossLogic.java     #     Stage 6 (final) boss patterns
│   │   └── StageExtraBossLogic.java #     EX stage boss patterns
│   │
│   ├── replay/                      #   -- Replay subsystem --
│   │   ├── ReplayHeader.java        #     Replay metadata (character, difficulty, score, etc.)
│   │   ├── ReplayInputBuffer.java   #     Fixed-size per-frame key buffer (pos 0..FRAME_COUNT)
│   │   ├── ReplayKeyCodec.java      #     Per-frame key bit-packing (keys/pressed -> byte)
│   │   ├── ReplayCodec.java         #     Fixed-size RLE codec for replay frames (30720 -> 7168)
│   │   ├── ReplayChecksum.java      #     Replay payload checksum
│   │   ├── ReplayFormat.java        #     Current replay/share format constants and validation
│   │   ├── ReplayRecordingController.java # Recording session controller
│   │   ├── ReplayPlaybackController.java  # Playback session controller
│   │   ├── ReplayTimeScaleController.java # Replay playback speed (slow/fast/auto-FF)
│   │   ├── ReplayStageRecorder.java #     Per-stage recording helper
│   │   ├── ReplayRng.java           #     Deterministic LCG RNG for replay sync
│   │   ├── ReplayRmsStore.java      #     Replay save/load via RMS
│   │   ├── ReplaySlotData.java      #     Complete replay slot value object
│   │   ├── ReplaySlotIntegrity.java #     Slot-wide integrity validation
│   │   ├── ReplaySlotStore.java     #     Atomic replay slot persistence contract
│   │   ├── ReplaySaveService.java   #     Save workflow orchestration
│   │   ├── ReplayShareCodec.java    #     Bluetooth share protocol codec
│   │   ├── ReplayShareService.java  #     Bluetooth replay packet import/export service
│   │   ├── ReplaySnapshotReader.java#     Snapshot deserialization
│   │   ├── ReplaySnapshotWriter.java#     Snapshot serialization
│   │   ├── ReplayStageSnapshotCodec.java  # Stage snapshot codec
│   │   ├── ReplayStageSnapshotService.java# Stage snapshot orchestration
│   │   └── ReplaySnapshotStore.java       # Stage-start snapshot persistence
│   │
│   ├── i18n/                        #   -- Internationalization --
│   │   ├── I18n.java                #     Static utility: language ID/code, resource path builder
│   │   ├── I18nBootstrap.java       #     Init I18N from saved options (loads pack, text, font)
│   │   ├── I18nPack.java            #     Binary .dat resource pack reader
│   │   ├── I18nSchema.java          #     UI/spell line-count schema
│   │   ├── SpellText.java           #     Current-language spell names and comments
│   │   ├── TextId.java              #     All text ID constants
│   │   └── UiText.java              #     Static UI text table loaded from ui.txt, indexed by TextId
│   │
│   ├── font/                        #   -- Bitmap font --
│   │   └── BitmapFont.java          #     font.dat renderer (variable-width bitmap glyphs)
│   │
│   ├── bt/                          #   -- Bluetooth --
│   │   └── BtSppLink.java           #     JSR-82 SPP serial link for replay sharing
│   │
│   └── ui/                          #   -- UI screens --
│       ├── UiController.java        #     UI screen lifecycle, state dispatch & navigation
│       ├── UiInput.java             #     Semantic confirm/cancel/direction input queries
│       ├── MenuCursor.java          #     Shared cursor wrapping, clamping & visible-window rules
│       ├── UiDraw.java              #     Shared UI drawing primitives
│       ├── UiTextLayout.java        #     Testable character/word wrapping & text effects
│       ├── FontTextMeasurer.java    #     MIDP/bitmap font adapter for UiTextLayout
│       ├── InputSystem.java         #     Per-frame key state capture, edge detection, softkey/tenkey
│       ├── StartupLanguageMenu.java #     First-launch language selection
│       ├── TitleScreen.java         #     Title menu
│       ├── StartSetupScreen.java    #     Game start: character & difficulty select
│       ├── LoadingScreen.java       #     Stage loading progress
│       ├── BattlePauseScreen.java   #     In-game pause overlay
│       ├── StageClearResultPanel.java#    Stage clear result & replay save prompt
│       ├── ResultScreen.java        #     Game over / all clear result
│       ├── EndingScreen.java        #     Per-character ending sequence
│       ├── StaffRollScreen.java     #     Credits roll
│       ├── StoryScreen.java         #     Story / dialog display
│       ├── OptionScreen.java        #     Settings menu
│       ├── HelpScreen.java          #     How-to-play guide
│       ├── MusicRoomScreen.java     #     Music room (BGM jukebox)
│       ├── SpellPracticeScreen.java #     Spell practice selection
│       ├── SpellPracticeEndScreen.java#   Spell practice result
│       ├── ReplayScreen.java        #     Replay list & playback
│       ├── ReplayUi.java            #     Shared replay background, slot, preview & message rendering
│       ├── ReplaySlotListModel.java #     Per-screen replay slot metadata and Boss-only availability
│       ├── ReplayPreview.java       #     Cached replay preview presentation model
│       ├── ReplayTransferProgress.java#   Update-owned transfer percentage and speed rules
│       ├── ReplayTransferUi.java    #     Shared Bluetooth status/progress presentation
│       ├── ReplaySendScreen.java    #     Bluetooth send UI
│       ├── ReplayReceiveScreen.java #     Bluetooth receive UI
│       └── QuitScreen.java          #     Quit confirmation
│
├── res/                             # ---- Game resources ----
│   ├── icon.png                     #   MIDlet icon
│   ├── cc.dat                       #   Sprite crop table (spriteId -> imgIndex + srcRect + anchor)
│   ├── stage.dat                    #   Stage wave script (enemy type, position, timing)
│   ├── sp/                          #   128 indexed 8-bit PNG sprite sheets: 000.png ~ 127.png; image indices 128–133 are runtime-generated StageEX variants
│   ├── bgm/                         #   Source MIDI, tracks 0–17 (packed, not copied into the JAR)
│   │   ├── <id>/intro.mid + loop.mid #     Split looping track
│   │   └── <id>.mid                  #     Single-file track
│   ├── se/                          #   Sound effects (WAV)
│   │   ├── shot.wav                 #     Player shot
│   │   ├── graze.wav                #     Graze
│   │   ├── crash.wav / crash2.wav   #     Player death / enemy death
│   │   ├── power.wav                #     Power item pickup
│   │   ├── life.wav                 #     1UP
│   │   ├── reimu.wav / marisa.wav / alice.wav  # Character voice
│   │   ├── nav.wav / select.wav / back.wav     # Menu navigation
│   │   ├── pause.wav                #     Pause
│   │   └── cd.wav                   #     Countdown
│   └── i18n/                        #   Localization source (per-language)
│       ├── en/ ja/ zht/             #     Each contains:
│       │   ├── ui.txt               #       UI text lines (indexed by TextId)
│       │   ├── story.txt            #       Story text
│       │   ├── ending_*.txt         #       Per-character ending text
│       │   ├── spcard.dat           #       Spell card names
│       │   ├── sprac.dat            #       Spell practice metadata
│       │   └── dialog/              #       In-game dialog scripts
│       └── (generated packs are never written here)
│
├── font/                            # ---- TTF source fonts (for font.dat generation) ----
│   ├── ja.ttf                       #   Japanese glyphs
│   ├── zh_hant.ttf                  #   Traditional Chinese (used by en & zht)
│   └── OFL.txt                      #   SIL Open Font License
│
├── .deps/                           # ---- Downloaded dependencies (gitignored) ----
│   ├── ivy/                         #   Apache Ivy 2.5.3 bootstrap JAR
│   ├── cache/                       #   Ivy resolution and artifact cache
│   └── lib/                         #   CLDC 1.1, MIDP 2.0, JSR-82 APIs and ProGuard 6.0.3
│
├── tools/                           # ---- Build tools (used by ant) ----
│   ├── CcDatCropViewer.java         #   cc.dat crop viewer (debug / resource inspection)
│   ├── I18nPackBuilder.java         #   Packs res/i18n/<lang>/ into <lang>.dat; generates font.dat
│   ├── BgmPackBuilder.java          #   Packs res/bgm into the current 18-track BGMP
│   ├── FontDatBuilder.java          #   Scans text files for used characters, rasterizes from TTF, outputs font.dat
│   └── dev/                         #   (gitignored) Dev-only helper tools
│
├── docs/                            # ---- Documentation ----
│   ├── build.md                     #   Build instructions
│   ├── dev.md                       #   Development guide, directory overview, I18N, tools
│   └── tree.md                      #   This file
│
├── build/                           # (gitignored) Ant build output
└── dist/                            # (gitignored) Final JAR + JAD
```

---

## Build Pipeline

```mermaid
flowchart TD
    CLEAN["ant clean (explicit)<br/>remove build/ dist/"]
    CLEAN -. "optional before package" .-> ANT
    ANT["ant (default: package)"] --> INIT["init<br/>mkdir build/classes/"]
    ANT --> DEPS["dependencies<br/>bootstrap Ivy; resolve APIs + ECJ + ProGuard<br/>Maven Central -> .deps/"]
    DEPS --> COMPILE
    INIT --> COMPILE["compile<br/>ECJ 4.20 src/ -> build/classes/<br/>(bootclasspath: Ivy cldc+midp+jsr82)"]
    INIT --> TOOLS["compile-tools-resources<br/>javac resource tools -> build/tools-classes/"]
    TOOLS --> I18N["i18n-pack<br/>validate UTF-8/schema and generate<br/>build/generated-res/i18n/<lang>.dat"]
    TOOLS --> BGM["bgm-pack<br/>validate res/bgm tracks 0–17 and generate<br/>build/generated-res/snd/bgm.dat"]
    COMPILE --> JAR
    I18N --> JAR
    BGM --> JAR
    JAR["jar<br/>classes + source res + generated res<br/>resources at JAR root<br/>-> build/raw.jar"] --> PG
    PG["proguard 6.0.3<br/>shrink, obfuscate and CLDC preverify<br/>root ThMirror.class + short class names<br/>-> dist/name.jar"] --> JAD
    JAD["write-jad<br/>generate dist/name.jad"]
```

---

## Runtime Architecture

```mermaid
graph TD
    MM["MainMidlet<br/>(MIDlet entry)"] -->|creates| GC

    GC["GameCore<br/>(GameCanvas + Runnable)<br/>16 FPS game loop<br/>owns subsystems + state"]
    GC --> AU["AudioSystem<br/>BGM / MIDI"]
    GC --> SE["SoundEffectSystem<br/>SE / WAV"]
    GC --> RES["ImageBank + SpriteCatalog<br/>sprite resources"]

    GC --> BS["BattleScene<br/>battle frame coordinator<br/>(update drives core methods)"]
    GC --> SC["SceneController<br/>(update when NOT inGame)<br/>(render always)"]

    GC --> BE["BattleEngine<br/>core gameplay tick<br/>(collision, bullets, bombs, death)"]
    GC --> SR["StageRuntime<br/>stage script + enemy waves"]
    GC --> BC["BossController<br/>HP, spells, phase FSM"]

    BS --> BR["BattleRenderer<br/>full-frame rendering"]
    BS -->|calls battleMain*| GC

    SC --> UI["UiController<br/>screen state machine"]
    SC --> END["EndingScreen"]
    SC --> STAFF["StaffRollScreen"]
    SC -->|renders when inGame| BS

    BE -->|Host callbacks| BEH["BattleEngine.Host<br/>(anonymous in GameCore)"]
    BEH --> GC

    SR -->|Host| BSH["BattleStageHost<br/>(StageRuntime.Host)"]
    BSH --> GC
    SR --> SDAT["StageDat<br/>loads /stage.dat"]

    BC -->|Host| BBH["BattleBossHost<br/>(BossController.Host)"]
    BBH --> GC
    BBH --> SR
    BC --> BL["Stage*BossLogic<br/>per-stage patterns"]
```

### UI Screen Flow

```mermaid
flowchart LR
    Loading --> StartupLanguage --> Title
    Title --> StartSetup --> Loading --> Battle
    Battle --> StageClear --> Ending --> StaffRoll --> Result
    StageClear --> Result
    Result --> Title
    Title --> Option
    Title --> Help
    Title --> MusicRoom
    Title --> SpellPractice
    Title --> Replay
    Title --> Quit
```

---

## Module Responsibilities

### Core Layer

| File | Role |
|------|------|
| `MainMidlet` | MIDlet lifecycle; creates `GameCore`, hands it to `Display` |
| `GameCore` | Central orchestrator: game loop, scene and battle state ownership, and typed module wiring |
| `SceneController` | Routes each frame's `update()` / `render()` to the active scene (UI, Battle, Ending, StaffRoll) |

### Battle Layer

| File | Role |
|------|------|
| `BattleScene` | Battle-state coordinator: handles stage clear panel, pause flow, spell practice end flow; delegates gameplay ticks to `GameCore` (battleMain* methods) and delegates rendering to `BattleRenderer` |
| `BattleEngine` | Pure gameplay tick: player input -> bullet advance -> collision -> death -> score. Stateless about scene flow |
| `BattleRenderer` | Read-only drawing pipeline: playfield, HUD, effects, dialog, boss cut-in and local image caches |
| `BattleRenderRules` | Pure battle resource, layout, fade, dialogue and clipping rules |
| `BattleEnemyVisuals` | Resolves enemy sprite/aura IDs and advances their visual animation once per update |
| `BattleVisualState` | Owns update-time flash, fade, blind mask and resolved enemy visual state |
| `BattleSessionConfig` | Immutable battle mode, stage, difficulty, character and spell-practice configuration |
| `SpellPracticeCatalog` | Canonical spell-practice metadata and lookup rules |
| `BattleStageFlow` | Adapter: implements `StageRuntime.StageFlow`, bridges stage flow events to `GameCore` |
| `BattleStageHost` | Adapter: implements `StageRuntime.Host`, bridges stage runtime events to `GameCore` |
| `BattleBossHost` | Adapter: implements `BossController.Host`, bridges boss events to `GameCore` |
| `BattleContinueSystem` | Continue count management & consumption (3 credits for normal mode) |
| `BattleHudData` | HUD data access interface (getScore, getLevel, etc.) |
| `BattleHudModel` | Concrete HUD data model, implements `BattleHudData` |
| `BattleMath` | Fixed-point speed/angle calculations (speedCos, speedSin, normalizeDeg) |

### Stage Layer

| File | Role |
|------|------|
| `StageRuntime` | Drives one stage: reads `stage.dat`, spawns enemy waves, manages transitions |
| `StagePreloader` | Pre-stage resource loading (final step calls System.gc()) |
| `StageDat` | Parses `stage.dat` binary format into wave table |
| `StageDochu` | Mid-stage (dochu) enemy shooting patterns per enemyType |
| `BossController` | Boss FSM: HP, spell timer, phase transitions; hosts `BossStageLogic[7]` for all stages |
| `AbstractBossStageLogic` | Abstract base: implements `BossStageLogic`, syncs shared state (bwave, spellId, etc.) with `BossController` via pull/push |
| `Stage[1-6]BossLogic` | Per-stage boss spell card patterns (faithful port from DoJa) |
| `StageExtraBossLogic` | EX stage boss patterns (single difficulty, no level branching) |

### Replay Layer

| File | Role |
|------|------|
| `ReplayHeader` | Metadata: character, difficulty, stage, score, date |
| `ReplayInputBuffer` | Fixed-size per-frame key buffer (FRAME_COUNT = 30720) |
| `ReplayKeyCodec` | Per-frame key bit-packing (keys/pressed -> byte) |
| `ReplayCodec` | Fixed-size RLE codec for replay frames (30720 -> 7168) |
| `ReplayChecksum` | Checksum calculation for replay payload integrity |
| `ReplayFormat` | Current replay and share format constants plus strict validation |
| `ReplayRecordingController` | Recording session: captures full-stage input, dialogue timings, Boss start frame and stage-start snapshot |
| `ReplayPlaybackController` | Feeds full-stage input back into the engine; Boss playback fast-forwards from stage start |
| `ReplayTimeScaleController` | Replay playback speed: slow/fast; Boss playback fast-forwards from stage start |
| `ReplayStageRecorder` | Per-stage recording lifecycle |
| `ReplayRng` | Seeded LCG RNG (state * 1103515245 + 12345) for deterministic replay |
| `ReplayRmsStore` | RMS persistence for replay slots |
| `ReplaySlotData` | Complete replay slot value object |
| `ReplaySlotIntegrity` | Validates all records belonging to a replay slot |
| `ReplaySlotStore` | Atomic replay slot persistence contract |
| `ReplaySaveService` | Save workflow orchestration |
| `ReplayShareCodec` | Bluetooth share protocol codec |
| `ReplayShareService` | Imports and exports current-format Bluetooth replay packets |
| `ReplaySnapshotReader` | Snapshot deserialization |
| `ReplaySnapshotWriter` | Snapshot serialization |
| `ReplayStageSnapshotCodec` | Stage snapshot codec |
| `ReplayStageSnapshotService` | Stage snapshot orchestration |
| `ReplaySnapshotStore` | Stage-start snapshot persistence |

### I18N & Font

| File | Role |
|------|------|
| `I18n` | Static utility: language ID/code management, resource path builder, text resource reader |
| `I18nBootstrap` | Initializes I18N subsystem from saved `GameOptions` (loads pack, text, font) |
| `I18nPack` | Reads binary `.dat` resource packs |
| `TextId` | All text ID constants (UI lines, dialog, endings) |
| `UiText` | Static UI text table loaded from `ui.txt`, line-indexed by `TextId` |
| `BitmapFont` | Renders variable-width bitmap glyphs from `font.dat` |

### Resource & System

| File | Role |
|------|------|
| `ImageBank` | Loads PNG sprites from `/sp/`, caches, generates StageEX color variants |
| `SpriteCatalog` | Reads final `/cc.dat`, including image filenames, sizes, EX bases/palettes and complete sprite geometry/reflection modes |
| `SpriteRenderer` | Draws sprites by ID with trim-aware logical windows, anchors, exact alpha and clip handling |
| `BulletSystem` | Shared player/enemy bullet, laser and effect state store (capacity 1024) |
| `EnemyBulletSystem` | Enemy bullet spawning policy and player collision/graze rules over `BulletSystem` |
| `EnemyTargetRegistry` | Targeting projection of the enemy table owned by `StageRuntime` |
| `FixedSlotPool` | Deterministic allocation and recycling contract for fixed-capacity entity stores |
| `DropItemSystem` | Drop item pool |
| `Player` | Player character: position, shot type, bomb, death, respawn, graze |
| `AudioSystem` | BGM: reads `bgm.dat` bundle, plays MIDI tracks via `Manager`/`Player` |
| `SoundEffectSystem` | SE: plays WAV files from `/se/` |
| `ScoreSystem` | Event-driven scoring: graze, spell bonus, life/bomb awards, miss penalty |
| `GameOptions` | User settings (key config, volume, difficulty) persisted to RMS |
| `GameProgress` | Save data: high scores, clears, unlocks |
| `UnlockFlags` | Tracks which stages / extras / spell practices are unlocked |
| `SpStore` | General-purpose scratch-pad RMS store |
| `VibrationController` | Device vibration motor |
| `Fixed` | Fixed-point 16.16 arithmetic |
| `Trig` | Pre-computed sin/cos table (fixed-point) |
| `Debug` | Debug overlay, frame profiler |

### UI Screens

| File | Role |
|------|------|
| `UiController` | Owns screen lifecycle, update/render dispatch and all UI transitions |
| `UiInput` | Gives UI screens semantic confirm, cancel, secondary and direction queries |
| `MenuCursor` | Shared wrapping, clamping and visible-window cursor rules |
| `UiDraw` | Shared drawing: text, boxes, list menus, bitmap font integration |
| `UiTextLayout` | Testable character/word wrapping and text animation rules |
| `FontTextMeasurer` | Adapts MIDP and bitmap fonts to `UiTextLayout` width measurement |
| `InputSystem` | Per-frame key state capture, edge detection, softkey/tenkey handling |
| `StartupLanguageMenu` | Startup language picker (shown after `LoadingScreen`, before entering `TitleScreen`) |
| `TitleScreen` | Main menu: Start / Continue / Practice / Replay / Option / Help / Quit |
| `StartSetupScreen` | Character select + difficulty select |
| `LoadingScreen` | Stage loading progress bar |
| `BattlePauseScreen` | Pause overlay during gameplay |
| `StageClearResultPanel` | Post-stage result + replay save prompt |
| `ResultScreen` | Game Over / All Clear result display |
| `EndingScreen` | Per-character ending narrative sequence |
| `StaffRollScreen` | Credits scroll |
| `StoryScreen` | In-game story / dialog renderer |
| `OptionScreen` | Settings: key config, volume, language switch |
| `HelpScreen` | How-to-play instruction pages |
| `MusicRoomScreen` | BGM jukebox (unlock-gated) |
| `SpellPracticeScreen` | Spell card practice selection |
| `SpellPracticeEndScreen` | Spell practice result |
| `ReplayScreen` | Saved replay list & playback launch |
| `ReplayUi` | Shared replay background, slot list, preview, save result and message rendering |
| `ReplaySlotListModel` | Per-screen replay slot metadata and Boss-only playback availability |
| `ReplayPreview` | Cached replay preview presentation data |
| `ReplayTransferProgress` | Update-owned Bluetooth transfer percentage and speed calculation |
| `ReplayTransferUi` | Shared Bluetooth device, status and progress rendering |
| `ReplaySendScreen` | Bluetooth send UI |
| `ReplayReceiveScreen` | Bluetooth receive UI |
| `QuitScreen` | Exit confirmation |

### Build Tools

| File | Role |
|------|------|
| `I18nPackBuilder` | Scans `res/i18n/<lang>/` and packs resources into `<lang>.dat`; generates font.dat |
| `FontDatBuilder` | Scans text files for used characters, rasterizes from TTF, outputs font.dat |
| `BgmPackBuilder` | Validates `res/bgm` tracks 0–17 and builds the deterministic BGMP |

### Resource Files

| Path | Format | Role |
|------|--------|------|
| `res/cc.dat` | CC01 binary, packaged unchanged | Image table and all 891 sprite records (13 shorts each); the sole filename/geometry/reflection definition |
| `res/stage.dat` | Binary | Stage wave script: enemy type, position, timing, formation |
| `res/sp/*.png` | Indexed 8-bit PNGs, packaged unchanged | 128 sprite sheets, 000.png ~ 127.png; each filename matches its image index. Indices 128–133 are runtime-generated StageEX variants. |
| `res/se/*.wav` | WAV | Sound effects |
| `res/bgm/` | MIDI | Source tracks 0–17; each is either `id.mid` or `id/intro.mid + loop.mid` |
| `build/generated-res/snd/bgm.dat` | Custom | Generated current 18-track MIDI bundle embedded as `/snd/bgm.dat` |
| `build/generated-res/i18n/<lang>.dat` | Binary | Generated deterministic I18N pack; contains validated text and `/i18n/<lang>/font/font.dat` |
| `res/i18n/<lang>/ui.txt` | Text | UI string lines, indexed by `TextId` constants |
| `res/i18n/<lang>/dialog/` | Text | In-game dialog scripts per stage |
| `res/i18n/<lang>/spcard.dat` | Text | Spell card name list |
| `/i18n/<lang>/font/font.dat` | Binary | Bitmap font glyph atlas (generated at build time and stored inside `i18n/<lang>.dat`) |

Ant packages `res/sp/` and `res/cc.dat` unchanged. There is no sprite builder, generated sprite directory or separate `sprites.dat`; BGM and i18n generation remain unchanged. See [sprite format and history](sprite-repack.md).
