package touhou;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import touhou.battle.BattleContinueSystem;
import touhou.battle.BattleEngine;
import touhou.battle.BattleHudData;
import touhou.battle.BattleHudModel;
import touhou.battle.BattleMath;
import touhou.battle.BattleRenderer;
import touhou.battle.BattleScene;
import touhou.battle.BattleSessionConfig;
import touhou.battle.BattleStageFlow;
import touhou.battle.BattleStageHost;
import touhou.battle.BattleVisualState;
import touhou.battle.SpellPracticeCatalog;
import touhou.i18n.I18nBootstrap;
import touhou.i18n.SpellText;
import touhou.i18n.TextId;
import touhou.replay.ReplayHeader;
import touhou.replay.ReplayPlaybackController;
import touhou.replay.ReplayRecordingController;
import touhou.replay.ReplayRng;
import touhou.replay.ReplaySlotData;
import touhou.replay.ReplayStageSnapshotCodec;
import touhou.replay.ReplayTimeScaleController;
import touhou.stage.BossController;
import touhou.stage.StageAssets;
import touhou.stage.StageDat;
import touhou.stage.StageDochu;
import touhou.stage.StageRuntime;
import touhou.ui.BattlePauseScreen;
import touhou.ui.EndingScreen;
import touhou.ui.InputSystem;
import touhou.ui.SoftkeyBar;
import touhou.ui.SpellPracticeEndScreen;
import touhou.ui.StaffRollScreen;
import touhou.ui.StageClearResultPanel;
import touhou.ui.UiController;

public final class GameCore extends javax.microedition.lcdui.game.GameCanvas implements Runnable {
    // Game loop timing and logical screen size.
    private static final int TARGET_FPS = 16;
    private static final int TARGET_FRAME_MS = 1000 / TARGET_FPS;
    private static final int LOGICAL_W = 240;
    private static final int LOGICAL_H = 240;

    private int targetFps = TARGET_FPS;

    // Animation/effect tables.
    private static final int[][] EFFECT_TABLE = new int[][] { { 1, 392, 0, 393, 0, 394, 255 },
        { 1, 395, 0, 396, 0, 397, 0, 398, 2, 399, 255 }, { 1, 442, 0, 443, 0, 444, 255 },
        { 2, 445, 2, 446, 2, 447, 255 }, { 2, 448, 2, 449, 2, 450, 255 }, { 2, 451, 2, 452, 2, 453, 255 },
        { 5, 166, 5, -1, 255 }, { 5, 173, 5, -1, 255 }, { 5, 179, 5, -1, 255 }, { 5, 196, 5, -1, 255 },
        { 5, 216, 5, -1, 255 }, { 5, 356, 5, -1, 255 }, { 5, 370, 5, -1, 255 }, { 5, 504, 5, -1, 255 },
        { 5, 711, 5, -1, 255 }, { 5, 719, 5, -1, 255 }, { 2, 454, 15, 454, 2, 454, 255 },
        { 2, -1, 2, 455, 15, 455, 2, 455, 255 }, { 4, -1, 2, 456, 15, 456, 2, 456, 255 },
        { 1, 457, 1, 458, 1, 459, 1, 460, 1, 461, 255 }, { 1, 462, 1, 463, 1, 464, 1, 465, 1, 466, 255 }, { 255, -1 },
        { 1, 500, 1, 501, 1, 502, 1, 503, 255 }, { 3, 401, 20, 400, 2, 401, 2, 402, 255 },
        { 2, 404, 20, 403, 2, 404, 2, 405, 255 }, { 3, 407, 20, 406, 2, 407, 2, 408, 255 },
        { 2, 410, 20, 409, 2, 410, 2, 411, 255 }, { 3, 413, 20, 412, 2, 413, 2, 414, 255 },
        { 2, 416, 20, 415, 2, 416, 2, 417, 255 }, { 3, 419, 20, 418, 2, 419, 2, 420, 255 },
        { 2, 422, 20, 421, 2, 422, 2, 423, 255 }, { 3, 425, 20, 424, 2, 425, 2, 426, 255 },
        { 2, 428, 20, 427, 2, 428, 2, 429, 255 }, { 3, 431, 20, 430, 2, 431, 2, 432, 255 },
        { 2, 434, 20, 433, 2, 434, 2, 435, 255 }, { 3, 437, 20, 436, 2, 437, 2, 438, 255 },
        { 2, 440, 20, 439, 2, 440, 2, 441, 255 } };

    // Softkey input mask.
    public static final int SOFTKEY_R_PRESSED = 1 << 28;

    // Key bitmask for tenkey '#'.
    public static final int KEY_POUND_PRESSED = 1 << 29;

    // Playfield bounds in logical coordinates.
    private static final int PLAY_X = 0;
    private static final int PLAY_Y = 8;
    private static final int PLAY_W = 194;
    private static final int PLAY_H = 226;

    // Main loop control.
    private Thread loop;
    private volatile boolean running;
    private volatile boolean paused;
    private final ReplayRng random;

    // Core gameplay systems.
    private final InputSystem input;
    private final SceneController sceneController;
    private final BattleScene battleScene;
    private final BattleRenderer battleRenderer;
    private final StageAssets stageAssets = new StageAssets();
    private final BattleRenderAccess battleRenderAccess;
    private final BattleVisualState battleVisualState;
    private final BulletSystem bullets;
    private final EnemyBulletSystem enemyBullets;
    private final EnemyTargetRegistry enemies;

    private final BattleEngine battleEngine;

    private final StageDochu stageDochu;

    private final StageRuntime stageRuntime;

    private final BattleContinueSystem continueSystem;

    private final DropItemSystem dropItems;

    private final VibrationController vibration;

    private final DropItemSystem.IconDrawer dropItemDrawer = new DropItemSystem.IconDrawer() {
        public void draw(Graphics g, int ccIndex, int x, int y) {
            sprites.draw(g, ccIndex, x, y);
        }
    };

    private boolean shouldEnterEndingAfterStageClear() {
        if (session.isSpellPractice()) {
            return false;
        }

        // Stage6 clear (story) skips stage clear panel and proceeds to ending.
        // Extra clear proceeds directly to the final result panel (no ending/staff roll).
        if (mGamemode == BattleSessionConfig.MODE_STORY) {
            return mStage >= StageRuntime.STAGE_6; // Stage index: 5 == Stage6.
        }
        return false;
    }

    private void beginEndingTransition() {
        if (endingActive) {
            return;
        }
        if (endingFadeToWhite > 0) {
            return;
        }

        replayRecording.stop();

        // DoJa : replay save is disabled after using continue (ci=false).
        // For story clear (Stage6), there is no stage clear panel, so we arm pending here to allow
        // the final result screen to prompt for replay saving.
        replayRecording.clearPending();
        if (!replayPlayback.isActive() && !session.isSpellPractice() && (mGamemode == BattleSessionConfig.MODE_STORY || mGamemode == BattleSessionConfig.MODE_EXTRA)) {
            int continuesUsed = runInitialContinueCount - continueSystem.getRemainingContinues();
            if (continuesUsed < 0) {
                continuesUsed = 0;
            }
            if (continuesUsed == 0) {
                int lvl = (hud != null) ? hud.getLevel() : session.getDifficulty();
                int score = scoreSystem.getScore();
                int spellId = stageLastSpellId;
                if (spellId < 0) {
                    spellId = 255;
                }

                replayRecording.armPendingForStageClear(lvl, session.getCharacter(), mStage, score, session.getShotType(), mGamemode, spellId);
            }
        }

        battlePaused = false;
        stageClearPanelActive = false;
        stageClearPhoto = null;
        endingFadeToWhite = 1;
    }

    public void enterEnding() {
        if (replayPlayback.isActive()) {
            returnReplayToMenu(null); // If playing a replay, do not enter ending for now.
            return;
        }

        resultStats.onEnterEnding(replayPlayback.isActive());

        // Persist unlock/score state first.
        UnlockFlags.tryUnlockExtraOnStoryClear(mStage, mGamemode, session.getDifficulty(), continueSystem.getRemainingContinues(), session.getRunStartStage(), session.isPracticeBoss());
        scoreSystem.commitScoreToProgress();

        // Stop gameplay and show ending, preserving the replay prepared above.
        stopBattleRuntime();
        // Keep pending replay data (if any) for the final result replay-save prompt.

        boolean qualifiesNextExtra = UnlockFlags.qualifiesNextExtraOnStoryClear(
                mStage, mGamemode, session.getDifficulty(), continueSystem.getRemainingContinues(), session.getRunStartStage(), session.isPracticeBoss());
        endingScreen.enter(session.getCharacter(), qualifiesNextExtra, imgs);
        endingActive = true;
    }

    private void convertEnemyBulletsToCrisisItems() {
        int power = (hud != null) ? hud.getPower() : 0;
        boolean bossActive = sBossf != 0;
        int max = bullets.capacity();
        for (int i = 0; i < max; i++) {
            if (!bullets.isActive(i) || bullets.isFromPlayer(i)) {
                continue;
            }
            int bx = bullets.getXFixed(i);
            int by = bullets.getYFixed(i);
            dropItems.spawn(DropItemSystem.TYPE_CRISIS, 0, 0, bx, by, mStage, power, bossActive);
            bullets.deactivate(i);
        }
    }

    private final BossController bossController;

    // Sprite resources.
    private SpriteRenderer sprites;

    // Debug output strings.
    private String debugLine;
    private String debugLine2;
    private String debugLine3;
    private String debugLine4;
    private String debugLine5;

    // Global resource tables.
    private SpriteCatalog cc;
    private ImageBank imgs;

    private final ScoreSystem scoreSystem;

    private final ResultStats resultStats = ResultStats.INSTANCE;

    private final AudioSystem audioSystem = new AudioSystem();

    // UI/game state flags.
    private boolean inGame;
    private boolean battlePaused;

    // Ending screen state.
    private final EndingScreen endingScreen = new EndingScreen();
    private boolean endingActive;
    private int endingFadeToWhite;

    // Staff roll state.
    private final StaffRollScreen staffRollScreen = new StaffRollScreen();
    private boolean staffRollActive;

    private UiController ui;
    private SoftkeyBar softkeys;

    // Current player and pause UI.
    private Player player;
    private BattlePauseScreen pauseScreen;

    // Stage clear result panel state.
    private final StageClearResultPanel stageClearResultPanel = new StageClearResultPanel();
    private boolean stageClearPanelActive;
    private Image stageClearPhoto;

    // Spell practice end flow state.
    private final SpellPracticeEndScreen spellPracticeEndScreen = new SpellPracticeEndScreen();
    private boolean spellPracticeEndFlg;
    private int spellPracticeEndCnt;
    private boolean spellPracticeEndActive;
    private boolean spellPracticeEndWon;

    private int stageLastSpellId;

    // Run-wide result counters (used by final result panel after staff roll).
    private int runMissCount;
    private int runBombUsedCount;
    private int runSpellSeenCount;
    private int runSpellBonusCount;
    private int runInitialContinueCount;

    // Frame counters and input tracking.
    private int fi;
    private boolean shooting;

    private final ReplayRecordingController replayRecording = new ReplayRecordingController();
    private final ReplayPlaybackController replayPlayback = new ReplayPlaybackController();
    private final ReplayTimeScaleController replayTimeScale = new ReplayTimeScaleController();

    private int timeStop;

    // Intro shatter effect state.
    private javax.microedition.lcdui.Image introShatterImg;
    private int introShatterCnt;
    private int introShatterParity;

    // Bomb and spell cut-in state.
    private int bombCnt;

    private boolean bossSpellCutInActive;
    private int bossSpellCutInPrevBspellstep;
    private int bossSpellCutInStage;
    private int bossSpellCutInSpellId;
    private int bossSpellCutInChara;
    private int bossSpellCutInStartFi;

    // Rendering options: alpha rendering toggles.
    private boolean optCloseUpAlphaEnabled;

    // Rendering option: if disabled, Stage2 blind mask switches instantly (no fade).
    private boolean optBlindMaskFadeEnabled;

    // Rendering option: if disabled, background decorations and alpha overlays are skipped.
    private boolean optHeavyBgEnabled;

    // Rendering option: render red hit spark rectangles (DoJa effectId 21).
    private boolean optHitSparkEnabled;

    // Debug options.
    private boolean optDebugFpsEnabled;
    private boolean optDebugPerfEnabled;
    private boolean optDebugResourceEnabled;

    // Stage/boss counters and status.
    private int sBossf;
    private int sBariacnt;
    private int mGcnt;
    private int mTacnt;
    private int ca;
    private int sDeadcnt;
    private int mResetf;

    // Stage script state.
    private int mStage;
    private int mStep;
    private boolean mLatterf;
    private boolean mBossmodef;

    // Game mode and barrier counters.
    private int mGamemode;
    private int sBbaria;
    private int sBstock;
    // Boss helper buffers.
    private final int[] sBossx = new int[3];
    private final int[] sBossy = new int[3];

    // Enemy/effect storage.
    private int[][] enemylist;
    private int[] mBossWrk;

    private boolean disableBossBombShieldCheat;
    private boolean disableItemCollectLimitCheat;

    private int[][] effectlist;

    // Per-frame camera origin offset (reset in BattleScene).
    int mOriginx;
    int mOriginy;

    private BattleSessionConfig session = BattleSessionConfig.standard(0, 0, 0, 0, 0, false);

    private int reimuBombExplosionId;

    private int bgMoveSpd;

    private BattleHudModel hud;

    private volatile String failureMessage;

    private final MainMidlet midlet;

    // Convenience constructor.
    public GameCore() {
        this(null);
    }

    // Update boss spell cut-in state.
    private void bossSpellCutInTick() {
        int bspellstep = bossController.getBspellstep();
        if (bossSpellCutInActive) {
            if (bspellstep != 1) {
                bossSpellCutInActive = false;
            }
        } else if (bossSpellCutInPrevBspellstep == 0 && bspellstep == 1) {
            int mt = resolveBossMtForCutIn();
            bossSpellCutInActive = true;
            bossSpellCutInStage = mStage;
            bossSpellCutInSpellId = bossController.getSpellId();
            bossSpellCutInChara = resolveBossCharaForCutIn(mStage, mt, bossSpellCutInSpellId);
            bossSpellCutInStartFi = fi;
        }
        bossSpellCutInPrevBspellstep = bspellstep;
    }

    // Resolve cut-in portrait based on boss/spell.
    private static int resolveBossCharaForCutIn(int stage, int mt, int spellId) {
        if (stage != StageRuntime.STAGE_1) {
            return 0;
        }

        if (spellId == 107) {
            return 108;
        }
        if (spellId == 108) {
            return 110;
        }
        if (spellId == 109) {
            return 109;
        }

        if (spellId >= 14 && spellId <= 17) {
            return 111;
        }

        if (spellId >= 6 && spellId <= 9) {
            return 110;
        }
        if (spellId >= 10 && spellId <= 13) {
            return 109;
        }
        if (spellId >= -2 && spellId <= 5) {
            return 108;
        }

        if (mt == BossController.MIDBOSS_PATTERN || mt == BossController.BOSS_PATTERN) {
            return 108;
        }
        if (mt == BossController.SECOND_BOSS_PATTERN) {
            return 109;
        }
        if (mt == BossController.THIRD_BOSS_PATTERN) {
            return 110;
        }
        return 0;
    }

    // Find boss enemy type for cut-in overlay.
    private int resolveBossMtForCutIn() {
        int idx = bossController.getHpBarTargetIndex();
        if (idx >= 0 && idx < 32 && enemylist[idx][StageRuntime.ENEMY_ACTIVE] != 0) {
            int mt = enemylist[idx][StageRuntime.ENEMY_MOVE_PATTERN];
            if (mt >= BossController.MIDBOSS_PATTERN && mt <= BossController.THIRD_BOSS_PATTERN) {
                return mt;
            }
        }

        for (int i = 0; i < 32; i++) {
            if (enemylist[i][StageRuntime.ENEMY_ACTIVE] == 0) {
                continue;
            }
            int mt = enemylist[i][StageRuntime.ENEMY_MOVE_PATTERN];
            if (mt >= BossController.MIDBOSS_PATTERN && mt <= BossController.THIRD_BOSS_PATTERN) {
                return mt;
            }
        }
        return 0;
    }

    // Initialize canvas resources and core systems.
    public GameCore(MainMidlet midlet) {
        super(false);
        setFullScreenMode(true);
        input = new InputSystem(this);
        battleVisualState = new BattleVisualState();
        battleRenderer = new BattleRenderer();
        battleRenderAccess = new BattleRenderAccess(this);
        battleScene = new BattleScene(battleRenderer, battleRenderAccess, audioSystem);
        sceneController = new SceneController(battleScene, endingScreen, staffRollScreen, stageClearResultPanel, audioSystem);
        this.midlet = midlet;
        vibration = new VibrationController(midlet);
        scoreSystem = new ScoreSystem();
        scoreSystem.setProgress(GameProgress.INSTANCE);
        scoreSystem.setListener(new ScoreSystem.Listener() {
            public void onLifeOrBombIncreased() {
                audioSystem.playSeType(SoundEffectSystem.SE_LIFE);
            }
        });
        continueSystem = new BattleContinueSystem(GameProgress.INSTANCE);
        random = new ReplayRng();
        random.setState((int) System.currentTimeMillis());
        bullets = new BulletSystem(1024);
        enemyBullets = new EnemyBulletSystem(bullets);
        enemies = new EnemyTargetRegistry();

        stageDochu = new StageDochu(enemyBullets);

        StageRuntime.StageFlow stageFlow = new BattleStageFlow(this);

        stageRuntime = new StageRuntime(new BattleStageHost(this, stageFlow, audioSystem));

        enemylist = stageRuntime.getEnemyList();
        mBossWrk = stageRuntime.getBossWrk();
        effectlist = stageRuntime.getEffectList();

        battleEngine = new BattleEngine(bullets, enemyBullets, scoreSystem, new BattleEngine.Host() {
            public void spawnStageEffect(int effectId, int xFixed, int yFixed) {
                stageKcEffect(effectId, xFixed, yFixed);
            }

            public void spawnEffectEntry(int effectId, int xFixed, int yFixed) {
                stageKc(effectId, xFixed, yFixed);
            }

            public void setResetFlag(int value) {
                mResetf = value;
            }

            public void setFlash(int value) {
                battleVisualState.setFlash(value);
            }

            public void setTargetFps(int fps) {
                stageHostSetTargetFps(fps);
            }

            public void onEnemyKilled(int enemyIndex) {
                GameCore.this.stageJb(enemyIndex);
            }

            public void playSe(int soundId) {
                if (soundId == SoundEffectSystem.SE_CRASH) {
                    // Player hit: vibrate on death.
                    GameCore.this.vibration.requestFrames(5);
                }
                audioSystem.playSeType(soundId);
            }

            public void onBombTick(int d0, int bombCntRemaining) {
                GameCore.this.bombCnt = bombCntRemaining;
                GameCore.this.bombTick(d0);
            }
        });

        dropItems = new DropItemSystem();
        dropItems.setListener(new DropItemSystem.Listener() {
            public void addPower(int amount) {
                if (hud == null) {
                    return;
                }
                BattleHudModel m = hud;
                int old = m.getPower();
                int p = old + amount;
                if (p > 128) {
                    p = 128;
                }
                if (p < 0) {
                    p = 0;
                }

                int oldStage = old >> 5;
                int newStage = p >> 5;
                if (newStage > oldStage && p < 128) {
                    audioSystem.playSeType(SoundEffectSystem.SE_POWER);
                }
                if (p >= 128) {
                    if (old < 128) {
                        audioSystem.playSeType(SoundEffectSystem.SE_POWER);
                        if (amount != 128) {
                            mResetf = 2;
                        } else {
                            mResetf = 3;
                        }
                        stageKcEffect(16, 3342336, 7274496);
                        stageKcEffect(17, 5963776, 7274496);
                        stageKcEffect(18, 9109504, 7274496);
                    }
                    p = 128;
                    scoreSystem.post(ScoreSystem.EVT_ADD_SCORE, 300, 0, 0);
                }
                m.setPower(p);
            }

            public void addBomb(int amount) {
                if (hud == null) {
                    return;
                }
                scoreSystem.post(ScoreSystem.EVT_ADD_BOMB, amount, 0, 0);
            }

            public void addPlayer(int amount) {
                if (hud == null) {
                    return;
                }
                scoreSystem.post(ScoreSystem.EVT_ADD_PLAYER, amount, 0, 0);
            }

            public void addScore(int amount) {
                scoreSystem.post(ScoreSystem.EVT_ADD_SCORE, amount, 0, 0);
            }

            public void onScoreItemCollected() {
                scoreSystem.post(ScoreSystem.EVT_SCORE_ITEM_COLLECTED, 0, 0, 0);
            }
        });

        bossController = new BossController(battleScene.createBossHost(this, stageRuntime));
        bossController.reset();

        try {
            SpriteCatalog cc = SpriteCatalog.load();
            ImageBank imgs = new ImageBank(cc);
            sprites = new SpriteRenderer(cc, imgs);
            bullets.setSprites(sprites);
            bullets.setEnemies(enemies);
            bullets.setEffectSpawner(new BulletSystem.EffectSpawner() {
                public void spawnEffect(int effectId, int xFixed, int yFixed) {
                    stageKc(effectId, xFixed, yFixed);
                }
            });

            this.cc = cc;
            this.imgs = imgs;

            reimuBombExplosionId = 457;

            inGame = false;

            int[] opt = GameOptions.load();

            I18nBootstrap.initFromOptions(opt);
            softkeys = new SoftkeyBar();

            audioSystem.setVolumes(opt[GameOptions.IDX_BGM_VOLUME], opt[GameOptions.IDX_SE_VOLUME]);
            audioSystem.setHeavyBgmEnabled(GameOptions.isHeavyBgmEnabled(opt));
            int seMask = (opt[GameOptions.IDX_SE_MASK] & 0xFF) | ((opt[GameOptions.IDX_SE_MASK_HI] & 0xFF) << 8);
            audioSystem.setSeEnabledMask14(seMask);
            // Title BGM (00): request once; actual start is driven by polling tick().
            audioSystem.requestBgm(0, true);

            ui = new UiController(imgs, sprites, new UiController.Listener() {
                public void onStartGameRequested(int gamemode, int level, int chara, int type, int startStage, boolean pracBoss) {
                    startGame(gamemode, level, chara, type, startStage, pracBoss);
                }

                public void onStartReplayRequested(int slot, boolean bossOnly) {
                    startReplay(slot, bossOnly);
                }

                public void onStartSpellPracticeRequested(int chara, int type, int spellId) {
                    startSpellPractice(chara, type, spellId);
                }

                public void onQuitRequested() {
                    requestQuit();
                }

                public void onPlayBgmRequested(int trackId) {
                    audioSystem.requestBgm(trackId, true);
                }

                public void onPlaySeTypeRequested(int seType) {
                    audioSystem.playSeType(seType);
                }

                public void onVolumesChanged(int bgmVol, int seVol) {
                    audioSystem.setVolumes(bgmVol, seVol);
                }

                public void onSeMaskChanged(int mask) {
                    audioSystem.setSeEnabledMask14(mask);
                }

                public void onHeavyBgmChanged(boolean enabled) {
                    audioSystem.setHeavyBgmEnabled(enabled);
                }

                public void onMusicRoomVolumeChanged(int bgmVol) {
                    audioSystem.setBgmVolumeOverride(bgmVol);
                }
            });

            pauseScreen = new BattlePauseScreen();
            battlePaused = false;

            mOriginx = 0;
            mOriginy = 0;

            input.refreshKeyOptions();

            StringBuffer sb = new StringBuffer();
            sb.append("test bulletId:");
            debugLine = sb.toString();

            debugLine2 = "exp=" + reimuBombExplosionId + " " + formatCcRow(cc, reimuBombExplosionId);
            debugLine3 = formatCcRow(cc, 83);
            debugLine4 = formatCcRow(cc, 93);
            debugLine5 = formatCcRow(cc, 0);
        } catch (Exception e) {
            reportFailure(e);
        }
    }

    // Entry point for new game start.
    private void startGame(int gamemode, int level, int chara, int type, int startStage, boolean pracBoss) {
        audioSystem.requestBgm(-1, false);
        beginIntroShatterFromUi();
        startBattleSession(BattleSessionConfig.standard(gamemode, level, chara, type, startStage, pracBoss));
    }

    // Entry point for replay playback.
    private void startReplay(int slot, boolean bossOnly) {
        audioSystem.requestBgm(-1, false);
        beginIntroShatterFromUi();

        replayTimeScale.reset();

        // DoJa: disable progress writes during replay playback (!di guards).
        scoreSystem.setProgressWriteEnabled(false);

        ReplaySlotData replay = replayPlayback.startFromSlot(slot, bossOnly);
        if (replay == null) {
            scoreSystem.setProgressWriteEnabled(true);
            if (ui != null) {
                ui.returnToReplayKeepCursor();
            }
            return;
        }

        ReplayHeader header = replay.header;
        BattleSessionConfig replaySession;
        try {
            if (header.getMode() == BattleSessionConfig.MODE_SPELL_PRACTICE) {
                replaySession = BattleSessionConfig.spellPractice(header.getChara(), header.getType(), header.getSpellId());
                if (replaySession.getStage() != header.getStage()) {
                    returnReplayToMenu("Invalid replay session.");
                    return;
                }
            } else {
                // A stage-practice recording marked at frame zero starts directly at the boss.
                boolean practiceBoss = header.getMode() == BattleSessionConfig.MODE_STAGE_PRACTICE && header.getBossStartFrame() == 0;
                replaySession = BattleSessionConfig.standard(header.getMode(), header.getDifficulty(), header.getChara(),
                        header.getType(), header.getStage(), practiceBoss);
            }
        } catch (IllegalArgumentException e) {
            returnReplayToMenu("Invalid replay session.");
            return;
        }
        startBattleSession(replaySession);

        boolean ok = ReplayStageSnapshotCodec.decodeAndApply(replay.stageSnapshot, random, continueSystem, hud, scoreSystem, player);
        if (!ok) {
            returnReplayToMenu("Invalid stage snapshot.");
            return;
        }
        if (bossOnly) {
            replayTimeScale.startAutoFastForwardToFrame(header.getBossStartFrame());
        }
    }

    private void returnReplayToMenu(String message) {
        clearBattleSession();
        if (ui != null) {
            if (message == null) {
                ui.returnToReplayKeepCursor();
            } else {
                ui.returnToReplayKeepCursorWithMessage(message);
            }
        }
    }

    private void clearBattleSession() {
        stopBattleRuntime();
        replayRecording.clearPending();
        resetSpellPracticeEndState();
    }

    private void stopBattleRuntime() {
        replayPlayback.stop();
        replayTimeScale.reset();
        scoreSystem.setProgressWriteEnabled(true);
        replayRecording.stop();

        battlePaused = false;
        inGame = false;
        stageClearPanelActive = false;
        stageClearPhoto = null;

        bullets.clear();
        enemyBullets.clear();
        enemies.clear();
        player = null;
        battleRenderer.release();
        stageClearResultPanel.leave(imgs);
        imgs.clear();
    }

    private void startBattleSession(BattleSessionConfig nextSession) {
        session = nextSession;
        targetFps = TARGET_FPS;
        resetSpellPracticeEndState();

        if (session.isSpellPractice()) {
            resultStats.onSpellPracticeStart(replayPlayback.isActive());
        } else {
            resultStats.onRunStart(replayPlayback.isActive(), session.getGameMode(), session.getDifficulty());
        }

        inGame = true;
        continueSystem.resetForNewRun(session.getContinueGameMode(), session.isSpellPractice());
        resetRunResultCounters();
        stageLastSpellId = -1;
        stageClearPanelActive = false;
        stageClearPhoto = null;
        replayRecording.clearPending();

        hud = createSessionHud();
        syncScoreSystemForHud();
        battleEngine.reset();
        resetStageRuntimeState();
        initializeSessionStage();
        input.refreshKeyOptions();
        createSessionPlayer();
        startSessionRecording();
    }

    public void restartBattle() {
        resultStats.onRetry(replayPlayback.isActive());
        introShatterImg = null;
        introShatterCnt = 0;
        resetSpellPracticeEndState();
        stageLastSpellId = -1;
        stageClearPanelActive = false;
        stageClearPhoto = null;
        replayRecording.clearPending();

        hud = createSessionHud();
        syncScoreSystemForHud();
        battleEngine.reset();
        resetStageRuntimeState();
        initializeSessionStage();
        createSessionPlayer();
        startSessionRecording();
    }

    private void startSessionRecording() {
        if (!replayPlayback.isActive()) {
            replayRecording.startSession(session.isPracticeBoss());
            replayRecording.captureStageStartSnapshot(random, continueSystem, hud, scoreSystem, player);
        } else {
            replayRecording.stop();
        }
    }

    private BattleHudModel createSessionHud() {
        if (session.isSpellPractice()) {
            return createHudForSpellPractice(session.getSpellId());
        }
        return createHudForStart(session.getGameMode(), session.getDifficulty(), session.getCharacter(),
                session.getShotType(), session.getStage(), session.isPracticeBoss());
    }

    private void resetStageRuntimeState() {
        targetFps = TARGET_FPS;
        timeStop = 0;
        battleVisualState.reset();
        bullets.clear();
        enemyBullets.clear();
        enemies.clear();
        battlePaused = false;
        bgMoveSpd = 0;
        fi = 0;
        shooting = true;
        bombCnt = 0;
        bossSpellCutInActive = false;
        bossSpellCutInPrevBspellstep = 0;
        bossSpellCutInStage = 0;
        bossSpellCutInSpellId = -1;
        bossSpellCutInChara = 0;
        bossSpellCutInStartFi = 0;
        mOriginx = 0;
        mOriginy = 0;
        sBossf = 0;
        sBariacnt = 0;
        mGcnt = 0;
        mTacnt = 0;
        ca = 0;
        sDeadcnt = 0;
        mResetf = 0;

        mGamemode = session.getGameMode();
        mStage = session.getStage();
        mStep = 0;
        mLatterf = false;
        mBossmodef = false;
        stageRuntime.setTalkCnt(3);
        sBbaria = 0;
        sBstock = 0;
        for (int i = 0; i < 3; i++) {
            sBossx[i] = 0;
            sBossy[i] = 0;
        }
        stageIb();
    }

    private void initializeSessionStage() {
        loadStageAssets();
        ensureStageDatLoaded();
        ensureTalkTableLoaded();
        if (session.isSpellPractice()) {
            initSpellPracticeBoss();
            int bossBgm = SpellPracticeCatalog.getBossBgmTrack(session.getSpellId());
            GameProgress.INSTANCE.markMusicPlayed(bossBgm);
            audioSystem.requestBgm(bossBgm, true);
        } else if (session.getGameMode() == BattleSessionConfig.MODE_STAGE_PRACTICE && session.isPracticeBoss()) {
            stageCe();
            ensureTalkTableLoaded();
        }
    }

    private void loadStageAssets() {
        battleRenderer.release();
        stageAssets.enter(imgs, sprites, session.getStage(), session.getCharacter(),
                session.isSpellPractice() ? SpellPracticeCatalog.getBossStep(session.getSpellId()) : -1,
                session.isSpellPractice() || session.isPracticeBoss(), GameOptions.load());
    }

    private void createSessionPlayer() {
        if (cc != null) {
            player = new Player(session.getCharacter(), session.getShotType(), PLAY_X + (PLAY_W / 2), PLAY_Y + PLAY_H - 28);
        } else {
            player = null;
        }
    }

    private void resetSpellPracticeEndState() {
        spellPracticeEndFlg = false;
        spellPracticeEndCnt = 0;
        spellPracticeEndActive = false;
        spellPracticeEndWon = false;
    }

    // Entry point for spell practice mode.
    private void startSpellPractice(int chara, int type, int spellId) {
        audioSystem.requestBgm(-1, false);
        beginIntroShatterFromUi();
        startBattleSession(BattleSessionConfig.spellPractice(chara, type, spellId));
    }

    // Spawn boss setup for spell practice.
    private void initSpellPracticeBoss() {
        int enemy = SpellPracticeCatalog.getEnemyId(session.getSpellId());

        boolean b = false;
        int n4 = -1;

        sBossx[0] = 97;
        sBossy[0] = 56;

        switch (enemy) {
            case 130:
                n4 = stageFb(9, 130, 3000, 0, 100, 180, 5, sBossx[0] << 16, sBossy[0] << 16);
                b = true;
                break;
            case 108:
            case 109:
            case 110:
            case 111:
                sBossx[0] = 97;
                sBossy[0] = 68;
                sBossx[1] = 30;
                sBossy[1] = 48;
                sBossx[2] = 164;
                sBossy[2] = 48;
                if (enemy == 108) {
                    sBossx[1] = -30;
                    sBossx[2] = 30;
                    sBossy[2] = -42;
                }
                if (enemy == 109) {
                    sBossx[1] = 97;
                    sBossy[1] = 68;
                    sBossx[0] = 164;
                    sBossy[0] = -42;
                    sBossx[2] = 30;
                    sBossy[2] = -42;
                }
                if (enemy == 110) {
                    sBossx[2] = 97;
                    sBossy[2] = 68;
                    sBossx[0] = 164;
                    sBossy[0] = -32;
                    sBossx[1] = 30;
                    sBossy[1] = -32;
                }
                int i0 = stageFb(9, 108, 3000, 0, 101, 180, 5, sBossx[0] << 16, sBossy[0] << 16);
                if (i0 >= 0) {
                    enemylist[i0][StageRuntime.ENEMY_TARGETABLE] = 1;
                }
                int i1 = stageFb(11, 109, 3000, 0, 102, 180, 5, sBossx[1] << 16, sBossy[1] << 16);
                if (i1 >= 0) {
                    enemylist[i1][StageRuntime.ENEMY_TARGETABLE] = 1;
                }
                n4 = stageFb(10, 110, 3000, 0, 103, 180, 5, sBossx[2] << 16, sBossy[2] << 16);
                if (n4 >= 0) {
                    enemylist[n4][StageRuntime.ENEMY_TARGETABLE] = 1;
                }
                break;
            case 112:
                n4 = stageFb(13, 112, 3000, 0, 101, 180, 5, sBossx[0] << 16, sBossy[0] << 16);
                break;
            case 132:
                n4 = stageFb(14, 132, 3000, 0, 100, 180, 5, sBossx[0] << 16, sBossy[0] << 16);
                b = true;
                break;
            case 113:
                n4 = stageFb(14, 113, 3000, 0, 101, 180, 5, sBossx[0] << 16, sBossy[0] << 16);
                break;
            case 114:
                n4 = stageFb(15, 114, 3000, 0, 101, 180, 5, sBossx[0] << 16, sBossy[0] << 16);
                break;
            case 115:
                n4 = stageFb(16, 115, 3000, 0, 101, 180, 5, sBossx[0] << 16, sBossy[0] << 16);
                break;
            case 116:
                n4 = stageFb(17, 116, 3000, 0, 101, 180, 5, sBossx[0] << 16, sBossy[0] << 16);
                break;
            case 117:
                n4 = stageFb(19, 117, 3000, 0, 101, 180, 5, sBossx[0] << 16, sBossy[0] << 16);
                break;
        }

        if (n4 >= 0) {
            enemylist[n4][StageRuntime.ENEMY_TARGETABLE] = 1;
        }
        if (!b) {
            sBossf = 2;
            sBbaria = 1;
            mBossmodef = true;
        } else if (n4 >= 0) {
            if (mStage == StageRuntime.STAGE_1 && hud != null) {
                hud.setPower(28);
            }
            enemylist[n4][StageRuntime.ENEMY_MOVE_STATE] = 1;
        }
    }

    // Build HUD data for spell practice.
    private static BattleHudModel createHudForSpellPractice(int spellId) {
        int stage = SpellPracticeCatalog.getStage(spellId);
        int level = SpellPracticeCatalog.getDifficulty(spellId);
        int enemy = SpellPracticeCatalog.getEnemyId(spellId);

        BattleHudModel m = new BattleHudModel();
        m.setLevel(level);
        m.setPlayer(1);
        m.setBomb(0);
        m.setHiScore(0);
        m.setScore(0);
        m.setGraze(0);

        int power = 128;
        if (stage == StageRuntime.STAGE_1) {
            power = 64;
        }
        if (level == 5) {
            power = 128;
        }
        if (stage == StageRuntime.STAGE_1 && enemy == 130) {
            power = 28;
        }
        m.setPower(power);
        return m;
    }

    // Build HUD data for standard game start.
    private static BattleHudModel createHudForStart(int gamemode, int level, int chara, int type, int startStage, boolean pracBoss) {
        int[] opt = GameOptions.load();

        BattleHudModel m = new BattleHudModel();
        m.setLevel(level);

        int player;
        int bomb;
        if (gamemode == BattleSessionConfig.MODE_STORY) {
            player = opt[GameOptions.IDX_LIVES];
            bomb = 3;
        } else if (gamemode == BattleSessionConfig.MODE_EXTRA) {
            player = 3;
            bomb = 3;
        } else if (gamemode == BattleSessionConfig.MODE_STAGE_PRACTICE) {
            player = 7;
            bomb = 3;
        } else {
            player = 1;
            bomb = 0;
        }
        m.setPlayer(player);
        m.setBomb(bomb);

        int unit = (chara << 1) + type;
        int hi = 0;
        if (gamemode != BattleSessionConfig.MODE_SPELL_PRACTICE) {
            hi = GameProgress.INSTANCE.getHighScore(unit, level);
        }
        m.setHiScore(hi);
        m.setScore(0);

        int power = 0;
        if (startStage == StageRuntime.STAGE_1) {
            power = 0;
            if (pracBoss) {
                power = 70;
            }
        } else if (startStage == StageRuntime.STAGE_2) {
            power = 70;
            if (pracBoss) {
                power = 128;
            }
        } else if (startStage == StageRuntime.STAGE_EXTRA) {
            power = 0;
        } else {
            power = 128;
        }
        m.setPower(power);
        m.setGraze(0);
        return m;
    }

    // Quit to midlet and stop the loop.
    private void requestQuit() {
        stop();
        if (midlet != null) {
            try {
                midlet.notifyDestroyed();
            } catch (Throwable t) {
            }
        }
    }

    // Build debug text for boss summon state.
    private String buildSummonDebugLine(int bossWrkIndex) {
        if (bossWrkIndex < 0 || bossWrkIndex >= mBossWrk.length) {
            return "wrk" + bossWrkIndex + "=<oob>";
        }
        int si = mBossWrk[bossWrkIndex];
        if (si < 0 || si >= enemylist.length) {
            return "wrk" + bossWrkIndex + "=" + si;
        }
        if (enemylist[si][StageRuntime.ENEMY_ACTIVE] == 0) {
            return "wrk" + bossWrkIndex + "=" + si + " dead";
        }
        return "wrk" + bossWrkIndex + "=" + si
                + " id=" + enemylist[si][StageRuntime.ENEMY_APPEARANCE]
                + " mt=" + enemylist[si][StageRuntime.ENEMY_MOVE_PATTERN]
                + " x=" + (enemylist[si][StageRuntime.ENEMY_X] >> 16)
                + " y=" + (enemylist[si][StageRuntime.ENEMY_Y] >> 16);
    }

    // Format logical sprite geometry for debug output.
    private static String formatCcRow(SpriteCatalog cc, int bulletId) {
        if (!cc.hasSpriteMeta(bulletId)) {
            return "cc[" + bulletId + "]=<none>";
        }
        int imgIndex = cc.getImgIndex(bulletId);
        int w = cc.getW(bulletId);
        int h = cc.getH(bulletId);
        int ax = cc.getAx(bulletId);
        int ay = cc.getAy(bulletId);

        StringBuffer sb = new StringBuffer();
        sb.append("cc[");
        sb.append(bulletId);
        sb.append("] img=");
        sb.append(imgIndex);
        sb.append(" wh=");
        sb.append(w);
        sb.append('x');
        sb.append(h);
        sb.append(" a=");
        sb.append(ax);
        sb.append(',');
        sb.append(ay);
        return sb.toString();
    }

    public boolean hasFailure() {
        return failureMessage != null;
    }

    // Fatal failures remain visible across lifecycle resume and never resume simulation.
    public synchronized void reportFailure(Throwable error) {
        if (failureMessage == null) {
            failureMessage = String.valueOf(error);
            System.out.println("Fatal game failure: " + failureMessage);
        }
        paused = true;
        audioSystem.stopAll();
    }

    // Start the main loop thread, or resume only when the session has not failed.
    public synchronized void start() {
        paused = hasFailure();
        if (running) {
            if (!paused) {
                audioSystem.resumeAll();
            }
            return;
        }
        running = true;
        loop = new Thread(this);
        loop.start();
    }

    // Pause the main loop updates.
    public void pause() {
        paused = true;
        audioSystem.pauseAll();
    }

    // Stop the main loop thread.
    public void stop() {
        running = false;
        paused = false;
        audioSystem.stopAll();
    }

    // Main loop timing and render dispatch.
    public void run() {
        long last = System.currentTimeMillis();
        while (running) {
            int frameMs = TARGET_FRAME_MS;
            if (inGame) {
                int fps = targetFps;
                if (fps <= 0) {
                    fps = TARGET_FPS;
                }
                frameMs = 1000 / fps;
                if (frameMs <= 0) {
                    frameMs = 1;
                }
            }

            long now = System.currentTimeMillis();
            long dt = now - last;
            if (dt < frameMs) {
                try {
                    Thread.sleep(frameMs - dt);
                } catch (InterruptedException e) {
                }
                continue;
            }
            // Limit catch-up to 2 steps to avoid CPU bursts that starve
            // the MIDI synthesis thread and cause cascading stutter.
            int steps = 0;
            long frameStart = now;
            long updateTotal = 0;
            while (dt >= frameMs) {
                if (!paused && !hasFailure()) {
                    try {
                        long u0 = System.currentTimeMillis();
                        update();
                        updateTotal += System.currentTimeMillis() - u0;
                    } catch (Throwable t) {
                        reportFailure(t);
                    }
                }
                last += frameMs;
                dt = now - last;
                steps++;
                if (steps >= 2) {
                    last = now;
                    break;
                }
            }

            long r0 = System.currentTimeMillis();
            boolean renderingFailure = hasFailure();
            try {
                render();
            } catch (Throwable t) {
                reportFailure(t);
                // A scene render failure gets the diagnostic screen on the next frame.
                // Stop only if even the independent diagnostic renderer cannot draw.
                if (renderingFailure) {
                    running = false;
                }
            }
            long r1 = System.currentTimeMillis();

            // Frame spike detector (always active, lightweight).
            int totalFrameMs = (int) (r1 - frameStart);
            if (totalFrameMs >= 100) {
                Debug.recordFrameSpike(totalFrameMs, (int) updateTotal, (int) (r1 - r0), 0, lastAudioMs);
            }

            // Yield after render to give audio/system threads CPU time,
            // reducing frame-time spikes when MIDI synthesis is active.
            Thread.yield();
        }
    }

    private int audioOptPollCnt;

    private int lastUiState = UiController.STATE_TITLE;

    private void tickAudioSystem() {
        if (stageClearPanelActive) {
            audioSystem.requestBgm(-1, false);
            return;
        }
        // Poll options at low frequency in UI to avoid per-frame RMS overhead.
        if (!inGame) {
            audioOptPollCnt--;
            if (audioOptPollCnt <= 0) {
                audioOptPollCnt = 30;
                int[] opt = GameOptions.load();
                audioSystem.setVolumes(opt[GameOptions.IDX_BGM_VOLUME], opt[GameOptions.IDX_SE_VOLUME]);
                audioSystem.setHeavyBgmEnabled(GameOptions.isHeavyBgmEnabled(opt));
                int seMask = (opt[GameOptions.IDX_SE_MASK] & 0xFF) | ((opt[GameOptions.IDX_SE_MASK_HI] & 0xFF) << 8);
                audioSystem.setSeEnabledMask14(seMask);
            }
        }

        // Scene-based BGM routing (kept outside core logic).
        if (endingActive) {
            GameProgress.INSTANCE.markMusicPlayed(15);
            audioSystem.requestBgm(15, false);
        } else if (staffRollActive) {
            GameProgress.INSTANCE.markMusicPlayed(16);
            audioSystem.requestBgm(16, false);
        } else if (!inGame) {
            int st = (ui != null) ? ui.getState() : UiController.STATE_TITLE;

            if (lastUiState == UiController.STATE_MUSIC_ROOM && st != UiController.STATE_MUSIC_ROOM) {
                audioSystem.clearBgmVolumeOverride();
            }

            if (st == UiController.STATE_MUSIC_ROOM) {
                if (lastUiState != UiController.STATE_MUSIC_ROOM) {
                    audioSystem.requestBgm(-1, false);
                }
                // Music room controls BGM explicitly. (no default playback)
            } else if (st == UiController.STATE_TITLE || st == UiController.STATE_START_SETUP) {
                if (lastUiState == UiController.STATE_MUSIC_ROOM) {
                    // Force restart Title BGM even when Music Room was playing the same track.
                    audioSystem.requestBgm(-1, false);
                }
                GameProgress.INSTANCE.markMusicPlayed(0);
                audioSystem.requestBgm(0, true);
            } else {
                audioSystem.requestBgm(-1, false);
                lastUiState = st;
                return;
            }

            lastUiState = st;
        }
        // In-game: stage scripts (op 251) will request BGM via StageRuntime.Host.

        audioSystem.tick();
    }

    private int lastAudioMs;

    // Per-frame game logic update.
    private void update() {
        long a0 = System.currentTimeMillis();
        tickAudioSystem();
        lastAudioMs = (int) (System.currentTimeMillis() - a0);
        vibration.tick();
        InputSystem.FrameInput frameInput = input.poll(
                inGame,
                battlePaused,
                stageClearPanelActive,
                session.isSpellPractice(),
                spellPracticeEndActive);
        int liveKeys = frameInput.keys;
        int keys = liveKeys;
        int pressed = frameInput.pressed;
        int fireActionPressed = frameInput.fireActionPressed;

        if (introShatterCnt > 0) {
            introShatterCnt -= 2;
            if (introShatterCnt <= 0) {
                introShatterCnt = 0;
                introShatterImg = null;
            }
        }

        if (replayPlayback.wantsQuitToMenu(pressed)) {
            returnReplayToMenu(null);
            return;
        }

        if (replayPlayback.isActive() && inGame && !battlePaused && !stageClearPanelActive) {
            int steps = replayTimeScale.computeSteps(liveKeys, replayPlayback);
            if (steps <= 0) {
                return;
            }

            for (int i = 0; i < steps; i++) {
                ReplayPlaybackController.FrameInput r = replayPlayback.nextFrame();
                if (r == null) {
                    returnReplayToMenu(null);
                    return;
                }

                keys = r.keys;
                pressed = r.pressed;
                fireActionPressed = r.fireActionPressed;

                updateSceneOnce(keys, pressed, fireActionPressed);
            }
            return;
        }

        if (inGame && !battlePaused) {
            if (endingFadeToWhite == 0 && !stageClearPanelActive && !replayPlayback.isActive() && !endingActive && !staffRollActive) {
                // Do not record the pause-trigger frame (left softkey / GAME_A).
                // The game returns early and does not advance gameplay state on that frame.
                if ((pressed & GAME_A_PRESSED) == 0) {
                    if (!session.isSpellPractice() || (!spellPracticeEndFlg && !spellPracticeEndActive)) {
                        replayRecording.onFrame(keys, pressed, fireActionPressed);
                    }
                }
            }
        }

        updateSceneOnce(keys, pressed, fireActionPressed);
    }

    private void updateSceneOnce(int keys, int pressed, int fireActionPressed) {
        long startedAt = System.currentTimeMillis();
        boolean battleHandled = battleScene.update(this, keys, pressed, fireActionPressed);
        Debug.recordBattleSceneMs((int) (System.currentTimeMillis() - startedAt));
        if (!battleHandled) {
            sceneController.update(this, keys, pressed);
        }
    }

    public boolean battleFlowIsSpellPractice() {
        return session.isSpellPractice();
    }

    public int battleFlowGetEndingFadeToWhite() {
        return endingFadeToWhite;
    }

    public void battleFlowSetEndingFadeToWhite(int v) {
        endingFadeToWhite = v;
    }

    public boolean battleFlowIsStageClearPanelActive() {
        return stageClearPanelActive;
    }

    public void battleFlowSetStageClearPanelActive(boolean v) {
        stageClearPanelActive = v;
    }

    public void battleFlowClearStageClearPhoto() {
        stageClearPhoto = null;
    }

    public boolean battleFlowIsSpellPracticeEndActive() {
        return spellPracticeEndActive;
    }

    public boolean battleFlowTickSpellPracticeEndCountdownIfNeeded() {
        if (!session.isSpellPractice()) {
            return false;
        }
        if (!spellPracticeEndFlg) {
            return false;
        }
        if (spellPracticeEndActive) {
            return false;
        }
        if (battlePaused || stageClearPanelActive) {
            return false;
        }

        spellPracticeEndCnt--;
        if (spellPracticeEndCnt > 0) {
            return false;
        }

        stageHostSetTargetFps(16);
        enterSpellPracticeEndScreen();
        return true;
    }

    public boolean battleFlowIsReplayPlaybackActive() {
        return replayPlayback.isActive();
    }

    public boolean stageHostIsReplayRecordingActive() {
        return replayRecording.isActive();
    }

    public void stageHostRecordDialoguePageCount(int encounterIndex, int pageCount) {
        replayRecording.recordDialoguePageCount(encounterIndex, pageCount);
    }

    public int stageHostGetRecordedDialoguePageCount(int encounterIndex) {
        return replayPlayback.getRecordedDialoguePageCount(encounterIndex);
    }

    public void battleFlowClearReplayPending() {
        replayRecording.clearPending();
    }

    public StageClearResultPanel battleFlowStageClearResultPanel() {
        return stageClearResultPanel;
    }

    public ImageBank battleFlowImgs() {
        return imgs;
    }

    public SpellPracticeEndScreen battleFlowSpellPracticeEndScreen() {
        return spellPracticeEndScreen;
    }

    // Battle pause bridge helpers (used by BattleScene).
    public boolean battlePauseIsPaused() {
        return battlePaused;
    }

    public void battlePauseSyncPlayerBombActive() {
        if (player != null) {
            player.setBombActive(bombCnt > 0);
        }
    }

    public BattlePauseScreen battlePauseScreen() {
        return pauseScreen;
    }

    public void battlePauseResume() {
        battlePaused = false;
        audioSystem.resumeBgm();
    }

    public void battlePauseQuitToTitle() {
        audioSystem.stopAll();
        resultStats.onRunEnd(replayPlayback.isActive());
        if (replayPlayback.isActive()) {
            returnReplayToMenu(null);
            return;
        }
        boolean spellPractice = session.isSpellPractice();
        clearBattleSession();
        if (ui != null) {
            if (spellPractice) {
                ui.returnToSpellPracticeKeepCursor();
            } else {
                ui.returnToTitleKeepCursor();
            }
        }
    }

    public void battlePauseRestart() {
        audioSystem.stopAll();
        restartBattle();
    }

    // Pause menu action: restart the whole run from the stage selected at run start.
    // (Story/Practice stage-select: should restart back to selected stage, not always Stage 1.)
    public void battlePauseRestartFromBeginning() {
        audioSystem.stopAll();
        resultStats.onRetry(replayPlayback.isActive());
        // Keep current run settings (difficulty/chara/type), and restart from selected start stage.
        startBattleSession(session.atRunStartWithoutBossSkip());
    }

    public void battlePauseTryContinueYes() {
        if (continueSystem.consumeContinueAndRecord()) {
            scoreSystem.post(ScoreSystem.EVT_CONTINUE_USED, 0, mStage, 0);
            scoreSystem.post(ScoreSystem.EVT_CONTINUE_RESET, 0, 0, 0);
            int[] opt = GameOptions.load();
            int playerCount = opt[GameOptions.IDX_LIVES];
            BattleEngine.ContinueReviveResult rr = battleEngine.continueReviveInPlace(dropItems, hud,
                    player, PLAY_X, PLAY_Y, PLAY_W, PLAY_H, playerCount);
            bombCnt = rr.newBombCnt;
            sDeadcnt = rr.newDeadcnt;
            sBariacnt = rr.newBariacnt;
            mTacnt = rr.newTacnt;
            shooting = rr.newShooting;
            battlePaused = false;
            audioSystem.resumeBgm();
        } else {
            enterGameOverFinalResultPanel();
        }
    }

    public void battlePauseContinueNo() {
        enterGameOverFinalResultPanel();
    }

    public void battlePauseEnterFromGame() {
        battlePaused = true;
        audioSystem.playSeType(SoundEffectSystem.SE_PAUSE);
        audioSystem.pauseBgm();
        if (pauseScreen != null) {
            pauseScreen.enterPause(session.isSpellPractice());
        }
    }

    // StageRuntime.StageFlow bridge helpers (used by BattleStageFlow).
    public boolean stageFlowShouldEnterEndingAfterStageClear() {
        return shouldEnterEndingAfterStageClear();
    }

    public void stageFlowBeginEndingTransition() {
        beginEndingTransition();
    }

    public void stageFlowEnterStageClearResultPanel() {
        enterStageClearResultPanel();
    }

    // StageRuntime.Host bridge helpers (used by BattleStageHost).
    public void stageHostClearDropItems() {
        dropItems.clear();
    }

    public void stageHostClearEnemies() {
        enemies.clear();
    }

    public void stageHostResetBossController() {
        bossController.reset();
    }

    public int stageHostGetBstock() {
        return sBstock;
    }

    public void stageHostSetBstock(int v) {
        sBstock = v;
    }

    public void stageHostSetEnemyAabb(int idx, int xFixed, int yFixed,
            int leftExtentFixed, int rightExtentFixed, int topExtentFixed, int bottomExtentFixed,
            boolean canTarget) {
        enemies.put(idx, xFixed, yFixed, leftExtentFixed, rightExtentFixed, topExtentFixed, bottomExtentFixed, canTarget);
    }

    public int[][] stageHostGetEffectTable() {
        return EFFECT_TABLE;
    }

    public boolean stageHostIsHitSparkEnabled() {
        return optHitSparkEnabled;
    }

    public int stageHostStageNa() {
        return stageNa();
    }

    public int stageHostGetStep() {
        return mStep;
    }

    public void stageHostSetStep(int v) {
        mStep = v;
    }

    public int stageHostGetTaCnt() {
        return mTacnt;
    }

    public void stageHostSetTaCnt(int v) {
        mTacnt = v;
    }

    public void stageHostSetIFlash(int v) {
        battleVisualState.setScreenFade(v);
    }

    public void stageHostStageHd(int enemyIdx) {
        stageHd(enemyIdx);
    }

    public void stageHostStageUb(int enemyIdx) {
        stageUb(enemyIdx);
    }

    public void stageHostStageDb(int enemyIdx) {
        stageDb(enemyIdx);
    }

    public void stageHostStageEb(int enemyIdx) {
        stageEb(enemyIdx);
    }

    public void stageHostFreeEnemy(int enemyIdx) {
        enemies.remove(enemyIdx);
    }

    public void stageHostSpawnBulletEffect(int effectId, int xFixed, int yFixed) {
        bullets.spawnEffect(effectId, 1, 10, xFixed, yFixed);
    }

    public int stageHostRandomNextInt() {
        return random.nextInt();
    }

    public int stageHostGetOriginX() {
        return mOriginx;
    }

    public int stageHostGetOriginY() {
        return mOriginy;
    }

    public int stageHostGetPlayX() {
        return PLAY_X;
    }

    public int stageHostGetPlayY() {
        return PLAY_Y;
    }

    public int stageHostGetPlayW() {
        return PLAY_W;
    }

    public int stageHostGetPlayH() {
        return PLAY_H;
    }

    public void stageHostSetLatterFlag(boolean v) {
        mLatterf = v;
    }

    public void stageHostSetBossMode(boolean v) {
        mBossmodef = v;
    }

    public int[] stageHostGetBossX() {
        return sBossx;
    }

    public int[] stageHostGetBossY() {
        return sBossy;
    }

    public int stageHostGetStartLevel() {
        return session.getDifficulty();
    }

    public void stageHostMarkBossStart() {
        replayRecording.markBossStart();
    }

    public void stageHostSetHFlash(int v) {
        battleVisualState.setFlash(v);
    }

    public void stageHostDrawCcIconSafe(Graphics g, int spriteId, int x, int y) {
        sprites.draw(g, spriteId, x, y);
    }

    // BossController.Host bridge helpers (used by BattleBossHost).
    public boolean bossHostIsBossMode() {
        return mBossmodef;
    }

    public int bossHostGetStage() {
        return mStage;
    }

    public int bossHostGetGameMode() {
        return mGamemode;
    }

    public int bossHostGetChara() {
        return session.getCharacter();
    }

    public int bossHostGetLevel() {
        return (hud != null) ? hud.getLevel() : 0;
    }

    public int bossHostGetPower() {
        return (hud != null) ? hud.getPower() : 0;
    }

    public int bossHostGetFi() {
        return fi;
    }

    public int bossHostGetMGCnt() {
        return mGcnt;
    }

    public void bossHostSetMGCnt(int v) {
        mGcnt = v;
    }

    public int bossHostGetBossF() {
        return sBossf;
    }

    public void bossHostSetBossF(int v) {
        sBossf = v;
    }

    public void bossHostSetBbaria(int v) {
        sBbaria = v;
    }

    public void bossHostSetResetFlag(int v) {
        mResetf = v;
    }

    public int bossHostGetPlayerXFixed() {
        if (player != null) {
            return player.getXFixed();
        }
        return (PLAY_X + (PLAY_W / 2)) << 16;
    }

    public int bossHostGetPlayerYFixed() {
        if (player != null) {
            return player.getYFixed();
        }
        return (PLAY_Y + PLAY_H - 28) << 16;
    }

    public int bossHostGetSpellPracticeBossStep() {
        return session.isSpellPractice() ? SpellPracticeCatalog.getBossStep(session.getSpellId()) : 0;
    }

    public void bossHostSetSpellBonusArmed(boolean v) {
        scoreSystem.post(ScoreSystem.EVT_SET_SPELL_BONUS_ARMED, v ? 1 : 0, 0, 0);
    }

    public void bossHostStageJb(int enemyIdx) {
        stageJb(enemyIdx);
    }

    public int bossHostGetTimeStop() {
        return timeStop;
    }

    public void bossHostSetTimeStop(int v) {
        timeStop = v;
    }

    public void bossHostAimEnemyBulletsToPlayer() {
        bullets.aimEnemyBulletsTo(bossHostGetPlayerXFixed(), bossHostGetPlayerYFixed());
    }

    public void bossHostSetEnemyBulletGlobalDir(int deg) {
        bullets.setGlobalDir(deg);
    }

    public void bossHostSetEnemyBulletLock(int idx, int value) {
        bullets.setEnemyBulletLock(idx, value);
    }

    public int bossHostSpawnEnemyBullet(int bulletId, int moveType, int angleDeg, int speedParam, int xFixed, int yFixed) {
        int px = bossHostGetPlayerXFixed();
        int py = bossHostGetPlayerYFixed();
        return enemyBullets.spawn(bulletId, moveType, angleDeg, speedParam, xFixed, yFixed, px, py);
    }

    public void bossHostConvertEnemyBulletsTo81Rain(int bossYFixed, int gc) {
        bullets.convertEnemyBulletsTo81Rain(bossYFixed, gc);
    }

    public void bossHostSetEnemyBulletsDeltaForIdAndMoveType(int bulletId, int moveType, int newDelta) {
        bullets.setEnemyBulletsDeltaForIdAndMoveType(bulletId, moveType, newDelta);
    }

    public void bossHostSetEnemyBulletsDeltaForIdIfDeltaZero(int bulletId, int baseDelta, int deltaStep) {
        bullets.setEnemyBulletsDeltaForIdIfDeltaZero(bulletId, baseDelta, deltaStep);
    }

    // Battle main tick bridge helpers (used by BattleScene).
    public boolean battleMainIsDialogueActiveForInput() {
        return isDialogueActiveForInput(mGcnt + 1);
    }

    // Handle mResetf-driven side effects during update (renderer must remain read-only).
    public void battleMainHandleResetFlagSideEffects() {
        if (mResetf == 0) {
            return;
        }
        convertEnemyBulletsToCrisisItems();
        if (mResetf == 2) {
            dropItems.forceCollect(0);
        } else if (mResetf == 3) {
            dropItems.forceCollect(2);
        } else if (mResetf == 4) {
            dropItems.forceCollect(1);
        }
        mResetf = 0;
    }

    public void battleMainTickInput(int pressed, int fireActionPressed, boolean dialogueActive) {
        if ((pressed & SOFTKEY_R_PRESSED) != 0) {
            battleEngine.tickSlowToggle(player);
        }

        if ((pressed & GAME_B_PRESSED) != 0) {
            battleEngine.tickSlowToggle(player);
        }

        if ((pressed & GAME_C_PRESSED) != 0) {
            BattleEngine.BombActivateResult r = battleEngine.tickBombActivation(dialogueActive, hud != null, bombCnt, sBossf, sBariacnt, fi,
                    mGcnt, mTacnt, ca, sDeadcnt);
            if (r.bombActivated) {
                runBombUsedCount++;
                if (session.getCharacter() == Player.REIMU) {
                    audioSystem.playSeType(SoundEffectSystem.SE_REIMU_BOMB);
                } else if (session.getCharacter() == Player.MARISA) {
                    audioSystem.playSeType(SoundEffectSystem.SE_MARISA_BOMB);
                } else {
                    audioSystem.playSeType(SoundEffectSystem.SE_ALICE_BOMB);
                }
                ca = r.newCa;
                sDeadcnt = r.newDeadcnt;
                bombCnt = r.newBombCnt;
            }
        }

        BattleEngine.FireInputResult fr = battleEngine.tickFireInput((pressed & FIRE_PRESSED) != 0, (fireActionPressed & FIRE_PRESSED) != 0, dialogueActive, mTacnt,
                shooting);
        mTacnt = fr.newTacnt;
        shooting = fr.newShooting;
    }

    public int battleMainGetDeadcnt() {
        return sDeadcnt;
    }

    public boolean battleMainAdvanceAndHandleDeath() {
        if (sDeadcnt == 0) {
            if (mStage == StageRuntime.STAGE_2 || mStage == StageRuntime.STAGE_3) {
                bgMoveSpd++;
            } else {
                bgMoveSpd += 2;
            }
        }
        fi++;
        mGcnt++;
        scoreSystem.post(ScoreSystem.EVT_TICK, 0, 0, 0);
        if (sBariacnt > 0) {
            sBariacnt--;
        }

        if (player != null) {
            battleEngine.tickPlayerDeathAndInvincibility(player, sBariacnt, PLAY_X, PLAY_Y, PLAY_W, PLAY_H);
        }
        if (sDeadcnt > 0) {
            boolean bossActive = sBossf != 0;
            BattleEngine.DeathTickResult r = battleEngine.tickDeathCountdown(sDeadcnt, mStage, dropItems, hud,
                    player, session.isSpellPractice(), bossActive);
            sDeadcnt = r.newDeadcnt;
            if (r.barrierTicks > 0) {
                sBariacnt = r.barrierTicks;
            }
            if (r.deathProcessed) {
                runMissCount++;
            }
            if (r.deathProcessed && r.noPlayersLeft) {
                if (session.isSpellPractice()) {
                    startSpellPracticeEnd(false, 10);
                    return true;
                }
                // During replay playback, auto-finish on game over to avoid showing the continue menu.
                if (replayPlayback.isActive()) {
                    enterGameOverFinalResultPanel();
                    return true;
                }
                if (continueSystem.canOfferContinue(mGamemode, session.isSpellPractice()) && pauseScreen != null) {
                    battleVisualState.setFlash(0);
                    battlePaused = true;
                    audioSystem.pauseBgm();
                    pauseScreen.enterContinue(continueSystem.getRemainingContinues());
                    return true;
                }
                enterGameOverFinalResultPanel();
                return true;
            }
        }

        return false;
    }

    public void battleMainTickBomb() {
        bombCnt = battleEngine.tickBomb(bombCnt);
    }

    public boolean battleMainTickTimeStop() {
        if (timeStop > 0) {
            stageGb();
            stageRuntime.advanceEnemyAnimations();
            bossSpellCutInTick();
            timeStop--;
            bossController.tickHpBarOncePerFrame();
            return true;
        }
        return false;
    }

    public boolean battleMainTickWorldAndStage(int keys) {
        long w0 = System.currentTimeMillis();
        if (player != null) {
            int p = (hud != null) ? hud.getPower() : 0;
            battleEngine.tickPlayer(player, keys, PLAY_X, PLAY_Y, PLAY_W, PLAY_H, p, sBbaria != 0, shooting, sBossf, bombCnt, session.getCharacter(), enemies, fi, sDeadcnt);
        }

        long w1 = System.currentTimeMillis();
        stageGb();

        long w2 = System.currentTimeMillis();

        bossSpellCutInTick();

        battleEngine.tickStageEffects(effectlist, EFFECT_TABLE);

        battleEngine.tickDropItems(dropItems, hud, player, PLAY_X, PLAY_Y, PLAY_W, PLAY_H, mStage, disableItemCollectLimitCheat, session.getCharacter(), sDeadcnt);

        long w3 = System.currentTimeMillis();
        if (mGamemode != BattleSessionConfig.MODE_SPELL_PRACTICE) {
            stageMa();
        }
        stageRuntime.advanceEnemyAnimations();
        long w4 = System.currentTimeMillis();

        // gb = stageGb (boss/enemy update+collision), ma = stageMa (script advance),
        // rst = bossController.tick time, o = player+effects+items+cutIn.
        Debug.recordWorldBreakdown((int) (w2 - w1), (int) (w4 - w3), lastBossTickMs, (int) (w1 - w0) + (int) (w3 - w2));

        return stageClearPanelActive;
    }

    public void battleMainTickBulletsAndCollisionAndHpBar() {
        int enemyLevel = hud != null ? hud.getLevel() : BattleSessionConfig.DIFFICULTY_EASY;

        if (optDebugPerfEnabled) {
            Debug.perfBeginFrame();
            if (bullets != null) {
                bullets.perfBeginFrame();
            }
        }

        boolean hasAimTarget = player != null;
        int aimXFixed = 0;
        int aimYFixed = 0;
        if (hasAimTarget) {
            aimXFixed = player.getXFixed();
            aimYFixed = player.getYFixed();
        }

        if (optDebugPerfEnabled) {
            long t0 = System.currentTimeMillis();
            battleEngine.tickBullets(PLAY_X, PLAY_Y, PLAY_W, PLAY_H, enemyLevel, hasAimTarget, aimXFixed, aimYFixed);
            long t1 = System.currentTimeMillis();
            Debug.perfSetBulletUpdateMs((int) (t1 - t0));
        } else {
            battleEngine.tickBullets(PLAY_X, PLAY_Y, PLAY_W, PLAY_H, enemyLevel, hasAimTarget, aimXFixed, aimYFixed);
        }

        if (player != null && sBariacnt == 0 && sDeadcnt == 0 && bombCnt == 0) {
            if (optDebugPerfEnabled) {
                long c0 = System.currentTimeMillis();
                sDeadcnt = battleEngine.tickPlayerCollision(player, enemylist, sBariacnt, sDeadcnt, bombCnt);
                long c1 = System.currentTimeMillis();
                Debug.perfSetPlayerCollisionMs((int) (c1 - c0));
            } else {
                sDeadcnt = battleEngine.tickPlayerCollision(player, enemylist, sBariacnt, sDeadcnt, bombCnt);
            }
        }

        bossController.tickHpBarOncePerFrame();
    }

    // Check if dialogue input should block gameplay input.
    private boolean isDialogueActiveForInput(int gcntPlusOne) {
        ensureStageDatLoaded();
        return stageRuntime.isDialogueActiveForInput(stageNa(), mStep, gcntPlusOne);
    }

    // Key down handler for input routing.
    protected void keyPressed(int keyCode) {
        boolean callSuper = input.onKeyPressed(
                keyCode,
                inGame,
                battlePaused,
                stageClearPanelActive,
                session.isSpellPractice(),
                spellPracticeEndActive);
        if (callSuper) {
            super.keyPressed(keyCode);
        }
    }

    // Key up handler for input routing.
    protected void keyReleased(int keyCode) {
        boolean callSuper = input.onKeyReleased(keyCode, inGame, battlePaused);
        if (callSuper) {
            super.keyReleased(keyCode);
        }
    }

    // Top-level render dispatch.
    private void render() {
        Graphics g = getGraphics();
        g.setClip(0, 0, getWidth(), getHeight());
        g.setColor(0x000000);
        g.fillRect(0, 0, getWidth(), getHeight());

        int ox = (getWidth() - LOGICAL_W) / 2;
        int oy = (getHeight() - LOGICAL_H) / 2;
        g.translate(ox, oy);

        g.setClip(0, 0, LOGICAL_W, LOGICAL_H);

        if (hasFailure()) {
            renderFailure(g);
        } else {
            sceneController.render(this, g);
            renderIntroShatter(g);
        }

        g.translate(-ox, -oy);
        g.setClip(0, 0, getWidth(), getHeight());
        if (!hasFailure()) {
            renderSoftkeys(g);
        }

        flushGraphics();
    }

    private void renderSoftkeys(Graphics g) {
        int left = -1;
        int right = -1;
        if (replayPlayback.isActive()) {
            left = TextId.SOFTKEY_BACK;
        } else if (endingFadeToWhite > 0) {
            return;
        } else if (endingActive) {
            left = TextId.SOFTKEY_SKIP;
        } else if (staffRollActive) {
            left = staffRollScreen.getLeftSoftkeyTextId();
        } else if (stageClearPanelActive) {
            left = stageClearResultPanel.getLeftSoftkeyTextId();
        } else if (!inGame) {
            left = ui.getLeftSoftkeyTextId();
            right = ui.getRightSoftkeyTextId();
        } else if (spellPracticeEndActive) {
            left = spellPracticeEndScreen.getLeftSoftkeyTextId();
        } else if (battlePaused) {
            left = pauseScreen.getLeftSoftkeyTextId();
            right = pauseScreen.getRightSoftkeyTextId();
        } else {
            left = TextId.SOFTKEY_PAUSE;
            right = player.isSlow() ? TextId.SOFTKEY_FAST : TextId.SOFTKEY_SLOW;
        }
        softkeys.render(g, getWidth(), getHeight(), left, right);
    }

    private void renderFailure(Graphics g) {
        // Use the device font: diagnostics must work even before i18n/font initialization.
        javax.microedition.lcdui.Font font = javax.microedition.lcdui.Font.getFont(
                javax.microedition.lcdui.Font.FACE_SYSTEM,
                javax.microedition.lcdui.Font.STYLE_PLAIN,
                javax.microedition.lcdui.Font.SIZE_SMALL);
        g.setFont(font);
        g.setColor(0xFFFFFF);
        int y = 4;
        g.drawString("Game stopped", 2, y, Graphics.TOP | Graphics.LEFT);
        y += font.getHeight() + 4;
        String message = failureMessage;
        for (int start = 0; start < message.length() && y + font.getHeight() <= LOGICAL_H;) {
            int end = start;
            int width = 0;
            while (end < message.length()) {
                int next = font.charWidth(message.charAt(end));
                if (end > start && width + next > LOGICAL_W - 4) {
                    break;
                }
                width += next;
                end++;
            }
            g.drawString(message.substring(start, end), 2, y, Graphics.TOP | Graphics.LEFT);
            start = end;
            y += font.getHeight();
        }
    }

    boolean sceneIsEndingActive() {
        return endingActive;
    }

    void sceneSetEndingActive(boolean v) {
        endingActive = v;
    }

    boolean sceneIsStaffRollActive() {
        return staffRollActive;
    }

    void sceneSetStaffRollActive(boolean v) {
        staffRollActive = v;
    }

    ImageBank sceneImgs() {
        return imgs;
    }

    SpriteRenderer sceneSprites() {
        return sprites;
    }

    UiController sceneUi() {
        return ui;
    }

    void sceneEnterFinalResultPanelAfterStaffRoll() {
        enterFinalResultPanelAfterStaffRoll();
    }

    // Scene hook: query current in-game state.
    public boolean sceneIsInGame() {
        return inGame;
    }

    // Battle hook: per-frame origin reset.
    public void battleFrameResetOrigin() {
        mOriginx = 0;
        mOriginy = 0;
    }

    public void battleVisualBeginFrame() {
        battleVisualState.beginFrame();
    }

    public void battleVisualEndFrame() {
        int spellId = (bossController == null) ? -1 : bossController.getSpellId();
        battleVisualState.endFrame(mStage, mBossmodef, spellId, sBossf, optBlindMaskFadeEnabled);
        if (inGame) {
            stageAssets.updateSpellAssets(imgs, sprites, bossController, battleVisualState,
                    bossSpellCutInChara, timeStop, enemylist, effectlist);
        }
    }

    // Render full battle UI and playfield.
    private void renderBattleUi(Graphics g) {
        battleRenderer.renderBattleUi(battleRenderAccess, g);
    }

    // Capture UI snapshot for intro shatter effect.
    private void beginIntroShatterFromUi() {
        if (ui == null) {
            return;
        }
        try {
            javax.microedition.lcdui.Image img = javax.microedition.lcdui.Image.createImage(256, 256);
            Graphics g = img.getGraphics();
            g.setColor(0x000000);
            g.fillRect(0, 0, 256, 256);
            g.setClip(0, 0, LOGICAL_W, LOGICAL_H);
            ui.render(g);
            introShatterImg = img;
            introShatterCnt = 100;
            introShatterParity = Math.abs(random.nextInt()) & 0x1;
        } catch (Throwable t) {
            introShatterImg = null;
            introShatterCnt = 0;
            introShatterParity = 0;
        }
    }

    public void beginIntroShatterWhite() {
        try {
            javax.microedition.lcdui.Image img = javax.microedition.lcdui.Image.createImage(256, 256);
            Graphics g = img.getGraphics();
            g.setColor(0xFFFFFF);
            g.fillRect(0, 0, 256, 256);
            introShatterImg = img;
            introShatterCnt = 100;
            introShatterParity = Math.abs(random.nextInt()) & 0x1;
        } catch (Throwable t) {
            introShatterImg = null;
            introShatterCnt = 0;
            introShatterParity = 0;
        }
    }

    // Render intro shatter overlay animation.
    private void renderIntroShatter(Graphics g) {
        if (introShatterCnt <= 0) {
            return;
        }
        if (introShatterImg == null) {
            return;
        }

        int oldClipX = g.getClipX();
        int oldClipY = g.getClipY();
        int oldClipW = g.getClipWidth();
        int oldClipH = g.getClipHeight();

        try {
            int n = introShatterParity;
            int n2 = 100 - introShatterCnt;
            for (int i = 0; i <= 120; i += 15) {
                for (int j = 0; j <= 120; j += 15) {
                    int n3 = (n2 << 3) - (240 - (i + j + 30));

                    int n4 = i;
                    int n5 = j;
                    if (n3 > 0) {
                        if (n == 0) {
                            n4 += n3 << 1;
                            n5 += n3;
                        } else {
                            n4 += n3;
                            n5 += n3 << 1;
                        }
                    }
                    g.setClip(n4, n5, 15, 15);
                    g.drawImage(introShatterImg, n4 - i, n5 - j, Graphics.TOP | Graphics.LEFT);

                    int n6 = 240 - i;
                    int n7 = j;
                    if (n3 > 0) {
                        if (n == 0) {
                            n6 -= n3;
                            n7 += n3 << 1;
                        } else {
                            n6 -= n3 << 1;
                            n7 += n3;
                        }
                    }
                    g.setClip(n6, n7, 15, 15);
                    g.drawImage(introShatterImg, n6 - (240 - i), n7 - j, Graphics.TOP | Graphics.LEFT);

                    int n8 = i;
                    int n9 = 240 - j;
                    if (n3 > 0) {
                        if (n == 0) {
                            n8 += n3;
                            n9 -= n3 << 1;
                        } else {
                            n8 += n3 << 1;
                            n9 -= n3;
                        }
                    }
                    g.setClip(n8, n9, 15, 15);
                    g.drawImage(introShatterImg, n8 - i, n9 - (240 - j), Graphics.TOP | Graphics.LEFT);

                    int n10 = 240 - i;
                    int n11 = 240 - j;
                    if (n3 > 0) {
                        if (n == 0) {
                            n10 -= n3 << 1;
                            n11 -= n3;
                        } else {
                            n10 -= n3;
                            n11 -= n3 << 1;
                        }
                    }
                    g.setClip(n10, n11, 15, 15);
                    g.drawImage(introShatterImg, n10 - (240 - i), n11 - (240 - j), Graphics.TOP | Graphics.LEFT);
                }
            }
        } catch (Throwable t) {
        } finally {
            try {
                g.setClip(oldClipX, oldClipY, oldClipW, oldClipH);
            } catch (Throwable ignore) {
            }
        }
    }

    // Per-frame bomb effect logic.
    private void bombTick(int d0) {
        if (player == null) {
            return;
        }
        int fx = player.getXFixed();
        int fy = player.getYFixed();

        if (session.getCharacter() == Player.REIMU) {
            // Reimu bomb: explosion effect is spawned when bomb bullets end (see BulletSystem).
            if (d0 == 2) {
                int sx = fx + BattleMath.speedCos(240, 0);
                int sy = fy + BattleMath.speedSin(240, 0);
                bombSpawnXa(3, 0, 45, 15, sx, sy);
            } else if (d0 == 8) {
                int sx = fx + BattleMath.speedCos(240, 72);
                int sy = fy + BattleMath.speedSin(240, 72);
                bombSpawnXa(4, 72, 45, 15, sx, sy);
            } else if (d0 == 14) {
                int sx = fx + BattleMath.speedCos(240, 144);
                int sy = fy + BattleMath.speedSin(240, 144);
                bombSpawnXa(5, 144, 45, 15, sx, sy);
            } else if (d0 == 20) {
                int sx = fx + BattleMath.speedCos(240, 216);
                int sy = fy + BattleMath.speedSin(240, 216);
                bombSpawnXa(6, 216, 45, 15, sx, sy);
            } else if (d0 == 26) {
                int sx = fx + BattleMath.speedCos(240, 288);
                int sy = fy + BattleMath.speedSin(240, 288);
                bombSpawnXa(7, 288, 40, 15, sx, sy);
            }
        }

        if (session.getCharacter() == Player.MARISA) {
            if (d0 < 45) {
                int y = fy - Fixed.fromInt(10);
                bullets.spawnOrUpdateMarisaLaser(105, 3, 2, fx, y, 12);
            }

            int d1;
            if (d0 < 30) {
                d1 = d0;
            } else {
                d1 = 60 - d0;
            }
            d1 >>= 1;
            int d2 = Math.abs(random.nextInt()) % 360;
            int se = BattleMath.speedCos(d1 << 3, d2);
            int te = BattleMath.speedSin(d1 << 3, d2);
            mOriginx = se >> 16;
            mOriginy = te >> 16;
        }

        if (session.getCharacter() == Player.ALICE) {
            if (d0 == 10) {
                for (int i = 0; i < 6; i++) {
                    bombSpawnXa(16, 0 + 60 * i, 20, 4, fx, fy);
                }
            }
            if (d0 == 25) {
                for (int i = 0; i < 6; i++) {
                    bombSpawnXa(17, 30 + 60 * i, 20, 5, fx, fy);
                }
            }
        }

        if (bombCnt > 15) {
            scoreSystem.post(ScoreSystem.EVT_BOMB_TICK, bombCnt, 0, 0);
        }
        if (d0 < 60) {
            mResetf = 2;
        }
    }

    // Load stage script data when needed.
    private void ensureStageDatLoaded() {
        stageRuntime.ensureStageDatLoaded();
    }

    // Load dialogue text table for current stage.
    private void ensureTalkTableLoaded() {
        stageRuntime.ensureTalkTableLoaded(mStage, session.getCharacter());
    }

    // Map stage/boss state to script table index.
    private int stageNa() {
        int n = mStage * 5;
        if (mLatterf) {
            ++n;
            if (mBossmodef) {
                n += session.getCharacter() + 1;
            }
        }
        return n;
    }

    // Clear enemy/effect state for a new stage segment.
    private void stageIb() {
        stageRuntime.stageIb();
    }

    // Spawn enemy entry and populate hitbox/motion fields.
    private int stageFb(final int n, final int n2, final int n3, final int n4, final int n5, final int n6, final int n7, final int n8, final int n9) {
        return stageRuntime.stageFb(n, n2, n3, n4, n5, n6, n7, n8, n9);
    }

    // Per-frame enemy/boss update and collision processing.
    private void stageGb() {
        if (optDebugResourceEnabled && mBossmodef) {
            debugLine = "bossf=" + sBossf + " gc=" + mGcnt + " stage=" + mStage + " baria=" + sBbaria;

            int t = bossController.getHpBarTargetIndex();
            int mt = -1;
            int hp = -1;
            int maxHp = -1;
            int e16 = -1;
            int e21 = -1;
            if (t >= 0 && t < 32 && enemylist[t][StageRuntime.ENEMY_ACTIVE] != 0) {
                mt = enemylist[t][StageRuntime.ENEMY_MOVE_PATTERN];
                hp = enemylist[t][StageRuntime.ENEMY_HP];
                maxHp = enemylist[t][StageRuntime.ENEMY_WORK_0];
                e16 = enemylist[t][StageRuntime.ENEMY_TARGETABLE];
                e21 = enemylist[t][StageRuntime.ENEMY_DROP_TYPE];
            }
            debugLine2 = "t=" + t + " mt=" + mt + " ehp=" + hp + "/" + maxHp + " j=" + bossController.getHpBarAnimJ() + " e16=" + e16 + " e21=" + e21 + " baria=" + sBbaria + " k=" + bossController.getHpBarAnimK() + " m=" + bossController.getHpBarAnimMode();
            debugLine3 = buildSummonDebugLine(9);
            debugLine4 = buildSummonDebugLine(10);
            debugLine5 = buildSummonDebugLine(11);
        }
        stageRuntime.stageGb();
    }

    private int lastBossTickMs;

    // Boss update and movement smoothing.
    private void stageUb(final int n) {
        int mt = enemylist[n][StageRuntime.ENEMY_MOVE_PATTERN];
        if (mt == BossController.MIDBOSS_PATTERN || mt == BossController.BOSS_PATTERN || mt == BossController.SECOND_BOSS_PATTERN || mt == BossController.THIRD_BOSS_PATTERN) {
            long bt0 = System.currentTimeMillis();
            bossController.tick(n);
            lastBossTickMs = (int) (System.currentTimeMillis() - bt0);
            int sid = bossController.getSpellId();
            scoreSystem.post(ScoreSystem.EVT_SPELL_ID_CHANGED, sid, 0, 0);
            onStageSpellIdChanged(sid);
            if (mt != BossController.MIDBOSS_PATTERN) {
                enemylist[n][StageRuntime.ENEMY_TARGETABLE] = (sBbaria == 0) ? 1 : 0;
            }
            return;
        }

        bossTargetsUpdateMinimal();

        int idx = 0;
        if (mt == BossController.SECOND_BOSS_PATTERN) {
            idx = 1;
        } else if (mt == BossController.THIRD_BOSS_PATTERN) {
            idx = 2;
        }

        if (mBossmodef && mStage == StageRuntime.STAGE_1 && sBossf == 1 && mt == BossController.SECOND_BOSS_PATTERN) {
            if (mGcnt == 30) {
                enemylist[n][StageRuntime.ENEMY_VX] = 131072;
                enemylist[n][StageRuntime.ENEMY_VY] = 131072;
            } else if (mGcnt == 45) {
                enemylist[n][StageRuntime.ENEMY_VX] = 458752;
                enemylist[n][StageRuntime.ENEMY_VY] = 458752;
            }
        }

        if (mBossmodef && mStage == StageRuntime.STAGE_3 && sBossf == 2 && mGcnt == 0) {
            enemylist[n][StageRuntime.ENEMY_VX] = 393216;
            enemylist[n][StageRuntime.ENEMY_VY] = 393216;
        }

        enemylist[n][StageRuntime.ENEMY_TARGETABLE] = (sBbaria == 0) ? 1 : 0;

        if (sBossx[idx] == 0 && sBossy[idx] == 0) {
            sBossx[idx] = enemylist[n][StageRuntime.ENEMY_X] >> 16;
            sBossy[idx] = enemylist[n][StageRuntime.ENEMY_Y] >> 16;
        }

        if (enemylist[n][StageRuntime.ENEMY_X] < (sBossx[idx] + 1 << 16)) {
            enemylist[n][StageRuntime.ENEMY_X] += enemylist[n][StageRuntime.ENEMY_VX];
        }
        if (enemylist[n][StageRuntime.ENEMY_X] > (sBossx[idx] - 1 << 16)) {
            enemylist[n][StageRuntime.ENEMY_X] -= enemylist[n][StageRuntime.ENEMY_VX];
        }
        if (enemylist[n][StageRuntime.ENEMY_Y] < (sBossy[idx] + 1 << 16)) {
            enemylist[n][StageRuntime.ENEMY_Y] += enemylist[n][StageRuntime.ENEMY_VY];
        }
        if (enemylist[n][StageRuntime.ENEMY_Y] > (sBossy[idx] - 1 << 16)) {
            enemylist[n][StageRuntime.ENEMY_Y] -= enemylist[n][StageRuntime.ENEMY_VY];
        }
    }

    // Update boss target positions and barrier timing.
    private void bossTargetsUpdateMinimal() {
        if (!mBossmodef) {
            return;
        }
        if (sBossf == 0) {
            return;
        }

        if (sBossf == 2 && mGcnt > 10 && mGamemode != BattleSessionConfig.MODE_SPELL_PRACTICE) {
            sBbaria = 0;
            bossWb(3);
        }

        if (sBossf == 2) {
            if (mGcnt == 0) {
                sBbaria = 1;
            } else if (mGcnt > 10) {
                sBbaria = 0;
            }
        }

        if (mStage == StageRuntime.STAGE_1) {
            if (sBossf == 1) {
                sBossx[0] = 97;
                sBossy[0] = 56;
                if (mGcnt >= 45) {
                    sBossx[1] = -30;
                } else if (mGcnt >= 30) {
                    sBossx[1] = 37;
                } else {
                    sBossx[1] = 77;
                }
                sBossy[1] = 44;
                sBossx[2] = 117;
                sBossy[2] = 44;
            } else if (sBossf == 2) {
                sBossx[0] = 97;
                sBossy[0] = 44;
                sBossx[2] = 30;
                sBossy[2] = 80;
            } else if (sBossf == 3) {
                if (mGcnt == 10 || mGcnt == 85 || mGcnt == 185 || mGcnt == 285 || mGcnt == 385) {
                    sBossx[0] = 144;
                    sBossy[0] = 80;
                } else if (mGcnt == 35 || mGcnt == 135 || mGcnt == 235 || mGcnt == 335) {
                    sBossx[0] = 50;
                    sBossy[0] = 80;
                }

                if (mGcnt == 0 || mGcnt == 90 || mGcnt == 190 || mGcnt == 290 || mGcnt == 390) {
                    sBossx[2] = 97;
                    sBossy[2] = 48;
                } else if (mGcnt == 40 || mGcnt == 240) {
                    sBossx[2] = 147;
                    sBossy[2] = 28;
                } else if (mGcnt == 140 || mGcnt == 340) {
                    sBossx[2] = 47;
                    sBossy[2] = 28;
                }
            }
            return;
        }

        if (mStage == StageRuntime.STAGE_2) {
            sBossx[0] = 97;
            sBossy[0] = 56;
            return;
        }

        if (mStage >= StageRuntime.STAGE_3) {
            if (sBossf == 1) {
                sBossx[0] = 97;
                sBossy[0] = 56;
            } else {
                sBossx[0] = 97;
                sBossy[0] = 44;
            }
        }
    }

    // Handle player bullet collision against an enemy.
    private void stageHd(final int n) {
        battleEngine.tickEnemyPlayerBulletCollision(n, enemylist, bombCnt, sBbaria, mBossWrk, disableBossBombShieldCheat, cc, bossController, player);
    }

    // StageJb helper: spawn a drop item if HUD supports drop context.
    private void stageJbSpawnDropIfAny(int dropType, int ex, int ey) {
        if (dropType != 0 && hud != null) {
            BattleHudModel m = hud;
            dropItems.spawn(dropType, 0, 0, ex, ey, mStage, m.getPower(), sBossf != 0);
        }
    }

    // StageJb helper: post spell finish event and update bonus counters.
    private void stageJbPostSpellFinish() {
        int lvl = (hud != null) ? hud.getLevel() : 0;
        boolean bonus = scoreSystem.isSpellBonusArmed();
        scoreSystem.post(ScoreSystem.EVT_SPELL_FINISH, bossController.getBspellcnt(), lvl, mStage);
        if (bonus) {
            runSpellBonusCount++;
        }
        bossController.resetSpellCountersAfterJb();
    }

    private void stageJbHandleMt100(final int n, final int ex, final int ey, final int dropType) {
        timeStop = 0;
        if (stageRuntime.stageJbMt100PreProcess(n, ex, ey)) {
            return;
        }
        stageJbPostSpellFinish();

        if (bossController.getHpBarAnimMode() == 1) {
            stageRuntime.stageJbMt100EnterHpBarAnimMode1Transition(n);
        } else {
            scoreSystem.post(ScoreSystem.EVT_ADD_SCORE, 30000, 0, 0);
            stageJbSpawnDropIfAny(dropType, ex, ey);
            if (session.isSpellPractice()) {
                // Spell practice: boss defeat triggers snow-scatter + end flow (DoJa).
                stageRuntime.stageJbSpellPracticeBossDefeatedEnter(n, ex, ey);
                startSpellPracticeEnd(true, 30);
            } else {
                stageRuntime.stageJbMt100EnterNonPracticeDefeatedState(n);
            }
        }
    }

    public void stageHostSetTargetFps(int fps) {
        if (fps <= 0) {
            fps = TARGET_FPS;
        }
        targetFps = fps;
    }

    private void stageJbHandleMt101to103(final int n, final int ex, final int ey) {
        timeStop = 0;
        stageRuntime.stageJbBossPhaseCleanup();

        stageRuntime.stageJbBossClearStateOnDeath(n);
        // Boss phase clear (mt 101..103) is the common spell finish path in DoJa.
        // Count run-wide spell bonus here so result panels show correct totals.
        stageJbPostSpellFinish();

        if (sBstock != 0) {
            if (hud != null) {
                BattleHudModel m = hud;
                int power = m.getPower();
                boolean bossActive = sBossf != 0;
                int gc = mGcnt;
                if (gc < 0) {
                    gc = -gc;
                }
                dropItems.spawn(DropItemSystem.TYPE_P, 0, 0, ex + ((((gc & 0x1) * 23)) << 16), ey + (((gc % 7) * 7) << 16), mStage, power, bossActive);
                dropItems.spawn(DropItemSystem.TYPE_P, 0, 0, ex + (((gc % 3) * 12) << 16), ey - (((gc % 5) * 13) << 16), mStage, power, bossActive);
                dropItems.spawn(DropItemSystem.TYPE_P, 0, 0, ex - (((gc % 5) * 15) << 16), ey + (((gc % 3) * 14) << 16), mStage, power, bossActive);
                dropItems.spawn(DropItemSystem.TYPE_P, 0, 0, ex - (((gc % 7) << 3) << 16), ey - ((((gc & 0x1) * 22)) << 16), mStage, power, bossActive);
            }
            boolean animMode1 = bossController.getHpBarAnimMode() == 1;
            stageRuntime.stageJbApplyBossStockTransition(n, ex, ey, !animMode1);

            if (animMode1) {
                bossController.enterHpBarMode2ForStockTransition();
            }
            return;
        }

        stageRuntime.stageJbBossDefeatedEnter(ex, ey);

        scoreSystem.post(ScoreSystem.EVT_ADD_SCORE, 50000 + 30000 * mStage, 0, 0);

        stageRuntime.stageJbFreeEnemyIfAlive(n);
        if (session.isSpellPractice()) {
            startSpellPracticeEnd(true, 30);
        }
    }

    private void stageJbHandleNormalEnemyDeath(final int n, final int mt, final int ex, final int ey, final int dropType) {
        stageJbSpawnDropIfAny(dropType, ex, ey);

        int enemyType = stageRuntime.stageJbNormalEnemyDeathFreeAndGetEnemyType(n);
        scoreSystem.post(ScoreSystem.EVT_ADD_SCORE, 500, 0, 0);
        audioSystem.playSeType(SoundEffectSystem.SE_CRASH2);
        stageRuntime.stageJbNormalEnemyDeathSpawnEffects(mt, ex, ey);

        int level = session.getDifficulty();
        int px;
        int py;
        if (player != null) {
            px = player.getXFixed();
            py = player.getYFixed();
        } else {
            px = (PLAY_X + (PLAY_W / 2)) << 16;
            py = (PLAY_Y + PLAY_H - 28) << 16;
        }

        // Death bursts for specific enemy types.
        if (enemyType == 80) {
            int ang = BattleMath.arcTan2Deg(py - ey, px - ex);
            if (level == BattleSessionConfig.DIFFICULTY_EASY) {
                enemyBullets.spawn(81, 0, ang, 32, ex, ey, px, py);
            } else if (level == BattleSessionConfig.DIFFICULTY_NORMAL) {
                enemyBullets.spawn(81, 0, ang, 40, ex, ey, px, py);
                enemyBullets.spawn(81, 0, ang, 36, ex, ey, px, py);
            } else if (level == BattleSessionConfig.DIFFICULTY_HARD) {
                enemyBullets.spawn(81, 0, ang, 48, ex, ey, px, py);
                enemyBullets.spawn(81, 0, ang, 44, ex, ey, px, py);
            } else {
                enemyBullets.spawn(81, 0, ang, 28, ex, ey, px, py);
            }
        }
        if (enemyType == 81) {
            int ang = BattleMath.arcTan2Deg(py - ey, px - ex);
            int step;
            int count;
            if (level == BattleSessionConfig.DIFFICULTY_EASY) {
                step = 30;
                count = 1;
            } else if (level == BattleSessionConfig.DIFFICULTY_NORMAL) {
                step = 25;
                count = 2;
            } else if (level == BattleSessionConfig.DIFFICULTY_HARD) {
                step = 15;
                count = 3;
            } else {
                step = 10;
                count = 4;
            }
            for (int off = -(step * count); off <= step * count; off += step) {
                enemyBullets.spawn(81, 0, ang + off, 36, ex, ey, px, py);
            }
        }
        if (enemyType == 82) {
            int ang = BattleMath.arcTan2Deg(py - ey, px - ex);
            int div;
            if (level == BattleSessionConfig.DIFFICULTY_EASY) {
                div = 8;
            } else if (level == BattleSessionConfig.DIFFICULTY_NORMAL) {
                div = 9;
            } else if (level == BattleSessionConfig.DIFFICULTY_HARD) {
                div = 10;
            } else if (level == BattleSessionConfig.DIFFICULTY_LUNATIC) {
                div = 15;
            } else {
                div = 12;
            }
            for (int k = 0; k < 360; k += 360 / div) {
                enemyBullets.spawn(81, 0, ang + k, 36, ex, ey, px, py);
            }
        }
    }

    // Handle enemy death and boss phase transitions.
    private void stageJb(final int n) {
        if (enemylist[n][StageRuntime.ENEMY_ACTIVE] == 0) {
            return;
        }
        int mt = enemylist[n][StageRuntime.ENEMY_MOVE_PATTERN];
        int ex = enemylist[n][StageRuntime.ENEMY_X];
        int ey = enemylist[n][StageRuntime.ENEMY_Y];
        int dropType = enemylist[n][StageRuntime.ENEMY_DROP_TYPE];

        if (mt == BossController.MIDBOSS_PATTERN) {
            stageJbHandleMt100(n, ex, ey, dropType);
            return;
        }

        if (mt == BossController.BOSS_PATTERN || mt == BossController.SECOND_BOSS_PATTERN || mt == BossController.THIRD_BOSS_PATTERN) {
            stageJbHandleMt101to103(n, ex, ey);
            return;
        }

        stageJbHandleNormalEnemyDeath(n, mt, ex, ey, dropType);
    }

    private void syncScoreSystemForHud() {
        if (hud == null) {
            return;
        }
        BattleHudModel m = hud;

        // Preserve initial HUD hi-score (loaded from persistent score list).
        // attachHud() calls syncHud() and would otherwise overwrite it with ScoreSystem defaults.
        int initialHiScore = m.getHiScore();

        scoreSystem.attachHud(m);
        int[] opt = GameOptions.load();

        optCloseUpAlphaEnabled = GameOptions.isCloseUpAlphaEnabled(opt);

        optBlindMaskFadeEnabled = GameOptions.isBlindMaskFadeEnabled(opt);

        optHeavyBgEnabled = GameOptions.isHeavyBgEnabled(opt);

        optHitSparkEnabled = GameOptions.isHitSparkEnabled(opt);

        optDebugFpsEnabled = GameOptions.isDebugFpsEnabled(opt);
        optDebugPerfEnabled = GameOptions.isDebugPerfEnabled(opt);
        optDebugResourceEnabled = GameOptions.isDebugResourceEnabled(opt);

        // Cheat is ignored in spell practice (no bomb usage in this mode).
        boolean optDisableShield = GameOptions.isCheatDisableBossBombShield(opt);
        boolean optDisableItemCollectLimit = GameOptions.isCheatDisableItemCollectLimit(opt);
        disableBossBombShieldCheat = optDisableShield && !session.isSpellPractice();
        disableItemCollectLimitCheat = optDisableItemCollectLimit;

        boolean anyCheatActive = optDisableShield || optDisableItemCollectLimit;
        scoreSystem.setScoreEnabled(!anyCheatActive);
        replayRecording.setEnabled(!anyCheatActive);

        int optLife = opt[GameOptions.IDX_LIVES];
        scoreSystem.resetForNewRun(initialHiScore, session.getGameMode(), optLife,
                session.getCharacter(), session.getShotType(), session.getDifficulty());
    }

    // Set boss phase state and reset counters.
    private void bossWb(int bossf) {
        mGcnt = -1;
        sBossf = bossf;
    }

    // Update non-boss enemy movement patterns.
    private void stageDb(final int n) {
        enemylist[n][StageRuntime.ENEMY_X] += enemylist[n][StageRuntime.ENEMY_VX];
        enemylist[n][StageRuntime.ENEMY_Y] += enemylist[n][StageRuntime.ENEMY_VY];
        switch (enemylist[n][StageRuntime.ENEMY_MOVE_PATTERN]) {
            case 1:
                if (enemylist[n][StageRuntime.ENEMY_AGE] % 3 == 0) {
                    enemylist[n][StageRuntime.ENEMY_VY] += 65536;
                    return;
                }
                break;
            case 2:
                if (enemylist[n][StageRuntime.ENEMY_AGE] % 5 == 0) {
                    enemylist[n][StageRuntime.ENEMY_VX] += 65536;
                    return;
                }
                break;
            case 3:
                if (enemylist[n][StageRuntime.ENEMY_AGE] % 5 == 0) {
                    enemylist[n][StageRuntime.ENEMY_VX] -= 65536;
                    return;
                }
                break;
            case 4:
                if (enemylist[n][StageRuntime.ENEMY_AGE] > 15 && enemylist[n][StageRuntime.ENEMY_AGE] < 50) {
                    enemylist[n][StageRuntime.ENEMY_MOVE_STATE] = 1;
                    enemylist[n][StageRuntime.ENEMY_X] -= enemylist[n][StageRuntime.ENEMY_VX];
                    enemylist[n][StageRuntime.ENEMY_Y] -= enemylist[n][StageRuntime.ENEMY_VY];
                    return;
                }
                enemylist[n][StageRuntime.ENEMY_MOVE_STATE] = 0;
                return;
            case 8:
                if (enemylist[n][StageRuntime.ENEMY_AGE] > 50) {
                    enemylist[n][StageRuntime.ENEMY_MOVE_STATE] = 0;
                    if ((enemylist[n][StageRuntime.ENEMY_AGE] & 0x1) == 0) {
                        enemylist[n][StageRuntime.ENEMY_SPEED] -= 1;
                    }
                    enemylist[n][StageRuntime.ENEMY_ANGLE] -= 2;
                } else if (enemylist[n][StageRuntime.ENEMY_SPEED] == 0) {
                    enemylist[n][StageRuntime.ENEMY_MOVE_STATE] = 1;
                } else if (enemylist[n][StageRuntime.ENEMY_AGE] > 13) {
                    enemylist[n][StageRuntime.ENEMY_SPEED] -= 1;
                }
                enemylist[n][StageRuntime.ENEMY_VX] = BattleMath.speedCos(enemylist[n][StageRuntime.ENEMY_SPEED] << 3, enemylist[n][StageRuntime.ENEMY_ANGLE]);
                enemylist[n][StageRuntime.ENEMY_VY] = BattleMath.speedSin(enemylist[n][StageRuntime.ENEMY_SPEED] << 3, enemylist[n][StageRuntime.ENEMY_ANGLE]);
                return;
            case 9:
                if (enemylist[n][StageRuntime.ENEMY_AGE] > 50) {
                    enemylist[n][StageRuntime.ENEMY_MOVE_STATE] = 0;
                    if ((enemylist[n][StageRuntime.ENEMY_AGE] & 0x1) == 0) {
                        enemylist[n][StageRuntime.ENEMY_SPEED] -= 1;
                    }
                    enemylist[n][StageRuntime.ENEMY_ANGLE] += 2;
                } else if (enemylist[n][StageRuntime.ENEMY_SPEED] == 0) {
                    enemylist[n][StageRuntime.ENEMY_MOVE_STATE] = 1;
                } else if (enemylist[n][StageRuntime.ENEMY_AGE] > 13) {
                    enemylist[n][StageRuntime.ENEMY_SPEED] -= 1;
                }
                enemylist[n][StageRuntime.ENEMY_VX] = BattleMath.speedCos(enemylist[n][StageRuntime.ENEMY_SPEED] << 3, enemylist[n][StageRuntime.ENEMY_ANGLE]);
                enemylist[n][StageRuntime.ENEMY_VY] = BattleMath.speedSin(enemylist[n][StageRuntime.ENEMY_SPEED] << 3, enemylist[n][StageRuntime.ENEMY_ANGLE]);
                return;
            case 5:
                if (enemylist[n][StageRuntime.ENEMY_AGE] > 14 && enemylist[n][StageRuntime.ENEMY_AGE] % 5 == 0) {
                    enemylist[n][StageRuntime.ENEMY_VX] += 65536;
                    return;
                }
                break;
            case 6:
                if (enemylist[n][StageRuntime.ENEMY_AGE] > 14 && enemylist[n][StageRuntime.ENEMY_AGE] % 5 == 0) {
                    enemylist[n][StageRuntime.ENEMY_VX] -= 65536;
                    return;
                }
                break;
            case 7:
                if (enemylist[n][StageRuntime.ENEMY_AGE] > 50) {
                    enemylist[n][StageRuntime.ENEMY_MOVE_STATE] = 0;
                    if ((enemylist[n][StageRuntime.ENEMY_AGE] & 0x1) == 0) {
                        enemylist[n][StageRuntime.ENEMY_SPEED] -= 1;
                    }
                } else if (enemylist[n][StageRuntime.ENEMY_SPEED] == 0) {
                    enemylist[n][StageRuntime.ENEMY_MOVE_STATE] = 1;
                } else if (enemylist[n][StageRuntime.ENEMY_AGE] > 13) {
                    enemylist[n][StageRuntime.ENEMY_SPEED] -= 1;
                }
                enemylist[n][StageRuntime.ENEMY_VX] = BattleMath.speedCos(enemylist[n][StageRuntime.ENEMY_SPEED] << 3, enemylist[n][StageRuntime.ENEMY_ANGLE]);
                enemylist[n][StageRuntime.ENEMY_VY] = BattleMath.speedSin(enemylist[n][StageRuntime.ENEMY_SPEED] << 3, enemylist[n][StageRuntime.ENEMY_ANGLE]);
                return;
            case 10:
                if (enemylist[n][StageRuntime.ENEMY_AGE] > 100) {
                    enemylist[n][StageRuntime.ENEMY_MOVE_STATE] = 0;
                    if ((enemylist[n][StageRuntime.ENEMY_AGE] & 0x1) == 0) {
                        enemylist[n][StageRuntime.ENEMY_SPEED] -= 1;
                    }
                } else if (enemylist[n][StageRuntime.ENEMY_SPEED] == 0) {
                    enemylist[n][StageRuntime.ENEMY_MOVE_STATE] = 1;
                } else if (enemylist[n][StageRuntime.ENEMY_AGE] > 13) {
                    enemylist[n][StageRuntime.ENEMY_SPEED] -= 1;
                }
                enemylist[n][StageRuntime.ENEMY_VX] = BattleMath.speedCos(enemylist[n][StageRuntime.ENEMY_SPEED] << 3, enemylist[n][StageRuntime.ENEMY_ANGLE]);
                enemylist[n][StageRuntime.ENEMY_VY] = BattleMath.speedSin(enemylist[n][StageRuntime.ENEMY_SPEED] << 3, enemylist[n][StageRuntime.ENEMY_ANGLE]);
                return;
            case 11:
                if (enemylist[n][StageRuntime.ENEMY_AGE] >= 20 && enemylist[n][StageRuntime.ENEMY_AGE] < 30) {
                    enemylist[n][StageRuntime.ENEMY_MOVE_STATE] = 1;
                    enemylist[n][StageRuntime.ENEMY_ANGLE] = (enemylist[n][StageRuntime.ENEMY_ANGLE] - 18 + 360) % 360;
                    enemylist[n][StageRuntime.ENEMY_VX] = BattleMath.speedCos(enemylist[n][StageRuntime.ENEMY_SPEED] << 3, enemylist[n][StageRuntime.ENEMY_ANGLE]);
                    enemylist[n][StageRuntime.ENEMY_VY] = BattleMath.speedSin(enemylist[n][StageRuntime.ENEMY_SPEED] << 3, enemylist[n][StageRuntime.ENEMY_ANGLE]);
                    return;
                }
                break;
            case 12:
                if (enemylist[n][StageRuntime.ENEMY_AGE] >= 20 && enemylist[n][StageRuntime.ENEMY_AGE] < 30) {
                    enemylist[n][StageRuntime.ENEMY_MOVE_STATE] = 1;
                    enemylist[n][StageRuntime.ENEMY_ANGLE] = (enemylist[n][StageRuntime.ENEMY_ANGLE] + 18 + 360) % 360;
                    enemylist[n][StageRuntime.ENEMY_VX] = BattleMath.speedCos(enemylist[n][StageRuntime.ENEMY_SPEED] << 3, enemylist[n][StageRuntime.ENEMY_ANGLE]);
                    enemylist[n][StageRuntime.ENEMY_VY] = BattleMath.speedSin(enemylist[n][StageRuntime.ENEMY_SPEED] << 3, enemylist[n][StageRuntime.ENEMY_ANGLE]);
                    return;
                }
                break;
            case 13:
                if (enemylist[n][StageRuntime.ENEMY_AGE] >= 30) {
                    enemylist[n][StageRuntime.ENEMY_MOVE_STATE] = 0;
                    if (enemylist[n][StageRuntime.ENEMY_AGE] == 30) {
                        enemylist[n][StageRuntime.ENEMY_ANGLE] += 90;
                        enemylist[n][StageRuntime.ENEMY_SPEED] = 8;
                    }
                } else if (enemylist[n][StageRuntime.ENEMY_SPEED] == 0) {
                    enemylist[n][StageRuntime.ENEMY_MOVE_STATE] = 1;
                } else if (enemylist[n][StageRuntime.ENEMY_AGE] > 10) {
                    enemylist[n][StageRuntime.ENEMY_SPEED] -= 1;
                }
                enemylist[n][StageRuntime.ENEMY_VX] = BattleMath.speedCos(enemylist[n][StageRuntime.ENEMY_SPEED] << 3, enemylist[n][StageRuntime.ENEMY_ANGLE]);
                enemylist[n][StageRuntime.ENEMY_VY] = BattleMath.speedSin(enemylist[n][StageRuntime.ENEMY_SPEED] << 3, enemylist[n][StageRuntime.ENEMY_ANGLE]);
                return;
            case 14:
                if (enemylist[n][StageRuntime.ENEMY_AGE] >= 30) {
                    enemylist[n][StageRuntime.ENEMY_MOVE_STATE] = 0;
                    if (enemylist[n][StageRuntime.ENEMY_AGE] == 30) {
                        enemylist[n][StageRuntime.ENEMY_ANGLE] += 270;
                        enemylist[n][StageRuntime.ENEMY_SPEED] = 8;
                    }
                } else if (enemylist[n][StageRuntime.ENEMY_SPEED] == 0) {
                    enemylist[n][StageRuntime.ENEMY_MOVE_STATE] = 1;
                } else if (enemylist[n][StageRuntime.ENEMY_AGE] > 10) {
                    enemylist[n][StageRuntime.ENEMY_SPEED] -= 1;
                }
                enemylist[n][StageRuntime.ENEMY_VX] = BattleMath.speedCos(enemylist[n][StageRuntime.ENEMY_SPEED] << 3, enemylist[n][StageRuntime.ENEMY_ANGLE]);
                enemylist[n][StageRuntime.ENEMY_VY] = BattleMath.speedSin(enemylist[n][StageRuntime.ENEMY_SPEED] << 3, enemylist[n][StageRuntime.ENEMY_ANGLE]);
                return;
            case 15: {
                enemylist[n][StageRuntime.ENEMY_ANGLE] = (enemylist[n][StageRuntime.ENEMY_ANGLE] + 2) % 360;
                int a = (enemylist[n][StageRuntime.ENEMY_ANGLE] + 90) % 360;
                if (enemylist[n][StageRuntime.ENEMY_X] < 6356992) {
                    if (a > 90) {
                        enemylist[n][StageRuntime.ENEMY_ANGLE] = 360;
                    }
                    enemylist[n][StageRuntime.ENEMY_VX] = BattleMath.speedCos(enemylist[n][StageRuntime.ENEMY_SPEED] << 3, enemylist[n][StageRuntime.ENEMY_ANGLE]);
                    enemylist[n][StageRuntime.ENEMY_VY] = BattleMath.speedSin(enemylist[n][StageRuntime.ENEMY_SPEED] << 3, enemylist[n][StageRuntime.ENEMY_ANGLE]);
                } else {
                    if (a < 90) {
                        enemylist[n][StageRuntime.ENEMY_ANGLE] = 360;
                    }
                    if (a > 135) {
                        enemylist[n][StageRuntime.ENEMY_ANGLE] = 405;
                    }
                    enemylist[n][StageRuntime.ENEMY_MOVE_STATE] = 1;
                    enemylist[n][StageRuntime.ENEMY_VX] = BattleMath.speedCos(enemylist[n][StageRuntime.ENEMY_SPEED] << 2, enemylist[n][StageRuntime.ENEMY_ANGLE]);
                    enemylist[n][StageRuntime.ENEMY_VY] = BattleMath.speedSin(enemylist[n][StageRuntime.ENEMY_SPEED] << 2, enemylist[n][StageRuntime.ENEMY_ANGLE]);
                }
                return;
            }
            case 16: {
                enemylist[n][StageRuntime.ENEMY_ANGLE] = (enemylist[n][StageRuntime.ENEMY_ANGLE] + 360 - 2) % 360;
                int a = (enemylist[n][StageRuntime.ENEMY_ANGLE] + 90) % 360;
                if (enemylist[n][StageRuntime.ENEMY_X] > 6356992) {
                    if (a < 270) {
                        enemylist[n][StageRuntime.ENEMY_ANGLE] = 540;
                    }
                    enemylist[n][StageRuntime.ENEMY_VX] = BattleMath.speedCos(enemylist[n][StageRuntime.ENEMY_SPEED] << 3, enemylist[n][StageRuntime.ENEMY_ANGLE]);
                    enemylist[n][StageRuntime.ENEMY_VY] = BattleMath.speedSin(enemylist[n][StageRuntime.ENEMY_SPEED] << 3, enemylist[n][StageRuntime.ENEMY_ANGLE]);
                } else {
                    if (a > 270) {
                        enemylist[n][StageRuntime.ENEMY_ANGLE] = 540;
                    }
                    if (a < 225) {
                        enemylist[n][StageRuntime.ENEMY_ANGLE] = 495;
                    }
                    enemylist[n][StageRuntime.ENEMY_MOVE_STATE] = 1;
                    enemylist[n][StageRuntime.ENEMY_VX] = BattleMath.speedCos(enemylist[n][StageRuntime.ENEMY_SPEED] << 2, enemylist[n][StageRuntime.ENEMY_ANGLE]);
                    enemylist[n][StageRuntime.ENEMY_VY] = BattleMath.speedSin(enemylist[n][StageRuntime.ENEMY_SPEED] << 2, enemylist[n][StageRuntime.ENEMY_ANGLE]);
                }
                return;
            }
            case 17:
                if (enemylist[n][StageRuntime.ENEMY_AGE] == 50) {
                    if (player != null) {
                        enemylist[n][StageRuntime.ENEMY_ANGLE] = BattleMath.arcTan2Deg(player.getYFixed() - enemylist[n][StageRuntime.ENEMY_Y], player.getXFixed() - enemylist[n][StageRuntime.ENEMY_X]);
                    }
                } else if (enemylist[n][StageRuntime.ENEMY_AGE] > 50) {
                    enemylist[n][StageRuntime.ENEMY_MOVE_STATE] = 0;
                    if ((enemylist[n][StageRuntime.ENEMY_AGE] & 0x1) == 0) {
                        enemylist[n][StageRuntime.ENEMY_SPEED] += 1;
                    }
                    if (enemylist[n][StageRuntime.ENEMY_SPEED] > 4) {
                        enemylist[n][StageRuntime.ENEMY_SPEED] = 4;
                    }
                } else if (enemylist[n][StageRuntime.ENEMY_SPEED] == 0) {
                    enemylist[n][StageRuntime.ENEMY_MOVE_STATE] = 1;
                } else if (enemylist[n][StageRuntime.ENEMY_AGE] > 13) {
                    enemylist[n][StageRuntime.ENEMY_SPEED] -= 1;
                }
                enemylist[n][StageRuntime.ENEMY_VX] = BattleMath.speedCos(enemylist[n][StageRuntime.ENEMY_SPEED] << 3, enemylist[n][StageRuntime.ENEMY_ANGLE]);
                enemylist[n][StageRuntime.ENEMY_VY] = BattleMath.speedSin(enemylist[n][StageRuntime.ENEMY_SPEED] << 3, enemylist[n][StageRuntime.ENEMY_ANGLE]);
                return;
            case 18:
                if (enemylist[n][StageRuntime.ENEMY_X] < -1966080 || enemylist[n][StageRuntime.ENEMY_Y] < -1441792 || enemylist[n][StageRuntime.ENEMY_X] > 14680064 || enemylist[n][StageRuntime.ENEMY_Y] > 17301504) {
                    enemylist[n][StageRuntime.ENEMY_AGE] -= 1;
                    return;
                }
                break;
            case 19:
                if (enemylist[n][StageRuntime.ENEMY_MOVE_STATE] == 1) {
                    break;
                }
                if (enemylist[n][StageRuntime.ENEMY_SPEED] == 0) {
                    enemylist[n][StageRuntime.ENEMY_MOVE_STATE] = 1;
                    enemylist[n][StageRuntime.ENEMY_ANGLE] += 90;
                    enemylist[n][StageRuntime.ENEMY_SPEED] = 8;
                    enemylist[n][StageRuntime.ENEMY_VX] = BattleMath.speedCos(enemylist[n][StageRuntime.ENEMY_SPEED] << 3, enemylist[n][StageRuntime.ENEMY_ANGLE]);
                    enemylist[n][StageRuntime.ENEMY_VY] = BattleMath.speedSin(enemylist[n][StageRuntime.ENEMY_SPEED] << 3, enemylist[n][StageRuntime.ENEMY_ANGLE]);
                    return;
                }
                if (enemylist[n][StageRuntime.ENEMY_AGE] > 5) {
                    enemylist[n][StageRuntime.ENEMY_SPEED] -= 1;
                    return;
                }
                break;
            case 20:
                if (enemylist[n][StageRuntime.ENEMY_MOVE_STATE] == 1) {
                    break;
                }
                if (enemylist[n][StageRuntime.ENEMY_SPEED] == 0) {
                    enemylist[n][StageRuntime.ENEMY_MOVE_STATE] = 1;
                    enemylist[n][StageRuntime.ENEMY_ANGLE] += 270;
                    enemylist[n][StageRuntime.ENEMY_SPEED] = 8;
                    enemylist[n][StageRuntime.ENEMY_VX] = BattleMath.speedCos(enemylist[n][StageRuntime.ENEMY_SPEED] << 3, enemylist[n][StageRuntime.ENEMY_ANGLE]);
                    enemylist[n][StageRuntime.ENEMY_VY] = BattleMath.speedSin(enemylist[n][StageRuntime.ENEMY_SPEED] << 3, enemylist[n][StageRuntime.ENEMY_ANGLE]);
                    return;
                }
                if (enemylist[n][StageRuntime.ENEMY_AGE] > 5) {
                    enemylist[n][StageRuntime.ENEMY_SPEED] -= 1;
                    return;
                }
                break;
            case 21:
                if (enemylist[n][StageRuntime.ENEMY_AGE] >= 30) {
                    enemylist[n][StageRuntime.ENEMY_MOVE_STATE] = 0;
                    if (enemylist[n][StageRuntime.ENEMY_AGE] == 30) {
                        if (enemylist[n][StageRuntime.ENEMY_X] < 6356992) {
                            enemylist[n][StageRuntime.ENEMY_ANGLE] = 315;
                        } else {
                            enemylist[n][StageRuntime.ENEMY_ANGLE] = 585;
                        }
                        enemylist[n][StageRuntime.ENEMY_SPEED] = 4;
                    }
                } else if (enemylist[n][StageRuntime.ENEMY_SPEED] == 0) {
                    enemylist[n][StageRuntime.ENEMY_MOVE_STATE] = 1;
                } else if (enemylist[n][StageRuntime.ENEMY_AGE] > 5) {
                    enemylist[n][StageRuntime.ENEMY_SPEED] -= 1;
                }
                enemylist[n][StageRuntime.ENEMY_VX] = BattleMath.speedCos(enemylist[n][StageRuntime.ENEMY_SPEED] << 3, enemylist[n][StageRuntime.ENEMY_ANGLE]);
                enemylist[n][StageRuntime.ENEMY_VY] = BattleMath.speedSin(enemylist[n][StageRuntime.ENEMY_SPEED] << 3, enemylist[n][StageRuntime.ENEMY_ANGLE]);
                return;
            case 22:
                if (enemylist[n][StageRuntime.ENEMY_AGE] >= 30) {
                    enemylist[n][StageRuntime.ENEMY_MOVE_STATE] = 0;
                    if (enemylist[n][StageRuntime.ENEMY_AGE] == 30) {
                        if (enemylist[n][StageRuntime.ENEMY_X] < 6356992) {
                            enemylist[n][StageRuntime.ENEMY_ANGLE] = 405;
                        } else {
                            enemylist[n][StageRuntime.ENEMY_ANGLE] = 495;
                        }
                        enemylist[n][StageRuntime.ENEMY_SPEED] = 4;
                    }
                } else if (enemylist[n][StageRuntime.ENEMY_SPEED] == 0) {
                    enemylist[n][StageRuntime.ENEMY_MOVE_STATE] = 1;
                } else if (enemylist[n][StageRuntime.ENEMY_AGE] > 5) {
                    enemylist[n][StageRuntime.ENEMY_SPEED] -= 1;
                }
                enemylist[n][StageRuntime.ENEMY_VX] = BattleMath.speedCos(enemylist[n][StageRuntime.ENEMY_SPEED] << 3, enemylist[n][StageRuntime.ENEMY_ANGLE]);
                enemylist[n][StageRuntime.ENEMY_VY] = BattleMath.speedSin(enemylist[n][StageRuntime.ENEMY_SPEED] << 3, enemylist[n][StageRuntime.ENEMY_ANGLE]);
                break;
        }
    }

    private void stageEb(final int n) {
        if (enemylist[n][StageRuntime.ENEMY_ACTIVE] == 0) {
            return;
        }

        int px;
        int py;
        if (player != null) {
            px = player.getXFixed();
            py = player.getYFixed();
        } else {
            px = (PLAY_X + (PLAY_W / 2)) << 16;
            py = (PLAY_Y + PLAY_H - 28) << 16;
        }

        int level = session.getDifficulty();
        int enemyType = enemylist[n][StageRuntime.ENEMY_SHOT_PATTERN];
        int ex = enemylist[n][StageRuntime.ENEMY_X];
        int ey = enemylist[n][StageRuntime.ENEMY_Y];

        stageDochu.tickEnemyShootingStageA(n, enemylist, enemyType, ex, ey, px, py, level);
        stageDochu.tickEnemyShootingStageB(n, enemylist, enemyType, ex, ey, px, py, level);
        stageDochu.tickEnemyShootingStageC(n, enemylist, enemyType, ex, ey, px, py, level);
        stageDochu.tickEnemyShootingStageD(n, enemylist, enemyType, ex, ey, px, py, level);
        stageDochu.tickEnemyShootingStageE(n, enemylist, enemyType, ex, ey, px, py, level);
        stageDochu.tickEnemyShootingStageF(n, enemylist, enemyType, ex, ey, px, py, level);
        stageDochu.tickEnemyShootingStageG(n, enemylist, enemyType, ex, ey, px, py, level);
        stageDochu.tickEnemyShootingStageH(n, enemylist, enemyType, ex, ey, px, py, level);
        stageDochu.tickEnemyShootingStageI(n, enemylist, enemyType, ex, ey, px, py, level);
        stageDochu.tickEnemyShootingStageJ(n, enemylist, enemyType, ex, ey, px, py);
        stageDochu.tickEnemyShootingStageK(n, enemylist, enemyType, ex, ey, px, py);
        stageDochu.tickEnemyShootingStageL(n, enemylist, enemyType, ex, ey, px, py, level);

        // Stage 1/2 fodder shooting patterns are handled by StageDochu.tickEnemyShootingStageA/B().

        // Fodder shooting patterns (enemyType 36/37/38/39) are handled by StageDochu.tickEnemyShootingStageD().

        // Stage 5 fodder shooting patterns are handled by StageDochu.tickEnemyShootingStageC().

        // Stage 5 fodder shooting patterns (enemyType 54/55) are handled by StageDochu.tickEnemyShootingStageE().

        // Stage 1/2/3 fodder shooting patterns (enemyType 28/26) are handled by StageDochu.tickEnemyShootingStageF().
        // Stage 2/3 fodder shooting patterns (enemyType 50/51/52) are handled by StageDochu.tickEnemyShootingStageG().
        // Stage 3 fodder shooting patterns (enemyType 29/30) are handled by StageDochu.tickEnemyShootingStageH().
        // Stage 4 fodder shooting patterns (enemyType 32/33/53) are handled by StageDochu.tickEnemyShootingStageI().
        // Stage 4 fodder shooting patterns (enemyType 70) are handled by StageDochu.tickEnemyShootingStageJ().
        // Stage EXTRA fodder shooting patterns (enemyType 90-97) are handled by StageDochu.tickEnemyShootingStageK().
        // Stage 1 fodder shooting patterns (enemyType 31) are handled by StageDochu.tickEnemyShootingStageL().
    }

    // Resolve spell card display name.
    private static String getSpellCardName(int id) {
        String name = SpellText.getName(id);
        if (name != null) {
            return name;
        }
        return "SpellNo." + (id + 1);
    }

    // Force transition to boss stage script.
    private void stageCe() {
        stageRuntime.stageCe();
    }

    // End battle and return to title UI.
    private void endBattleToTitle() {
        if (replayPlayback.isActive()) {
            returnReplayToMenu(null);
            return;
        }

        resultStats.onRunEnd(replayPlayback.isActive());
        clearBattleSession();
        if (ui != null) {
            ui.returnToTitleKeepCursor();
        }
    }

    public void endSpellPracticeToMenu() {
        if (replayPlayback.isActive()) {
            returnReplayToMenu(null);
            return;
        }

        resultStats.onRunEnd(replayPlayback.isActive());
        clearBattleSession();

        if (ui != null) {
            ui.returnToSpellPracticeKeepCursor();
        }
    }

    private void startSpellPracticeEnd(boolean won, int endCnt) {
        if (!session.isSpellPractice()) {
            return;
        }
        if (spellPracticeEndFlg || spellPracticeEndActive) {
            return;
        }

        // Spell practice end flow state machine (mirrors DoJa).
        spellPracticeEndWon = won;
        spellPracticeEndFlg = true;
        spellPracticeEndCnt = endCnt;

        battlePaused = false;
        timeStop = 0;

        replayRecording.stop();

        // Clear bullets with vanish animation so effects continue during countdown.
        bullets.convertEnemyBulletsToDeathVanish();
        enemyBullets.clear();
    }

    private void enterSpellPracticeEndScreen() {
        spellPracticeEndFlg = false;
        spellPracticeEndCnt = 0;
        spellPracticeEndActive = true;

        if (replayPlayback.isActive()) {
            returnReplayToMenu(null);
            return;
        }

        // Arm pending replay regardless of win/loss (spell practice).
        replayRecording.clearPending();
        if (!replayPlayback.isActive()) {
            int lvl = (hud != null) ? hud.getLevel() : session.getDifficulty();
            int score = scoreSystem.getScore();
            int spellId = session.getSpellId();
            if (spellId < 0) {
                spellId = 255;
            }
            replayRecording.armPendingForStageClear(lvl, session.getCharacter(), mStage, score, session.getShotType(), mGamemode, spellId);
        }

        spellPracticeEndScreen.enter(spellPracticeEndWon);
    }

    // Spawn effect entry with optional table lookup.
    private void stageKc(int effectId, int xFixed, int yFixed) {
        stageRuntime.stageKc(effectId, xFixed, yFixed);
    }

    // Allocate and initialize effect slot.
    private void stageKcEffect(int effectId, int xFixed, int yFixed) {
        stageRuntime.stageKcEffect(effectId, xFixed, yFixed);
    }

    // Render effect sprites and special lines.
    private void stageMc(final Graphics g) {
        stageRuntime.stageMc(g);
    }

    // Advance stage script and spawn events.
    private void stageMa() {
        stageRuntime.stageMa();
        stageAssets.update(imgs, mBossmodef, battleRenderAccess.dialogueVisible(), effectlist, enemylist);
    }

    private void resetRunResultCounters() {
        runMissCount = 0;
        runBombUsedCount = 0;
        runSpellSeenCount = 0;
        runSpellBonusCount = 0;
        runInitialContinueCount = continueSystem.getRemainingContinues();
    }

    private void onStageSpellIdChanged(int spellId) {
        if (spellId == stageLastSpellId) {
            return;
        }
        stageLastSpellId = spellId;
        if (spellId >= 0) {
            runSpellSeenCount++;
        }
    }

    private void enterFinalResultPanelAfterStaffRoll() {
        resultStats.onRunEnd(replayPlayback.isActive());

        int hiScore = scoreSystem.getHiScore();
        int score = scoreSystem.getScore();

        int continuesUsed = runInitialContinueCount - continueSystem.getRemainingContinues();
        if (continuesUsed < 0) {
            continuesUsed = 0;
        }

        stageClearResultPanel.enterFinal(hiScore, score, continuesUsed, runMissCount, runBombUsedCount, runSpellBonusCount, runSpellSeenCount);
        stageClearPanelActive = true;
        stageClearPhoto = null;
    }

    private void enterGameOverFinalResultPanel() {
        if (replayPlayback.isActive()) {
            returnReplayToMenu(null);
            return;
        }

        resultStats.onRunEnd(replayPlayback.isActive());
        stopBattleRuntime();

        // DoJa: disable replay save after any continue was used.
        replayRecording.clearPending();
        if (!session.isSpellPractice() && (mGamemode == BattleSessionConfig.MODE_STORY || mGamemode == BattleSessionConfig.MODE_EXTRA)) {
            int continuesUsed = runInitialContinueCount - continueSystem.getRemainingContinues();
            if (continuesUsed < 0) {
                continuesUsed = 0;
            }
            if (continuesUsed == 0) {
                int lvl = (hud != null) ? hud.getLevel() : session.getDifficulty();
                int score = scoreSystem.getScore();
                int spellId = stageLastSpellId;
                if (spellId < 0) {
                    spellId = 255;
                }
                replayRecording.armPendingForStageClear(lvl, session.getCharacter(), mStage, score, session.getShotType(), mGamemode, spellId);
            }
        }

        scoreSystem.commitScoreToProgress();

        int hiScore = scoreSystem.getHiScore();
        int score = scoreSystem.getScore();

        int continuesUsed = runInitialContinueCount - continueSystem.getRemainingContinues();
        if (continuesUsed < 0) {
            continuesUsed = 0;
        }

        stageClearResultPanel.enterFinal(hiScore, score, continuesUsed, runMissCount, runBombUsedCount, runSpellBonusCount, runSpellSeenCount);

        stageClearPanelActive = true;
    }

    private Image captureBattlePhoto() {
        try {
            Image img = Image.createImage(LOGICAL_W, LOGICAL_H);
            Graphics g = img.getGraphics();
            g.setClip(0, 0, LOGICAL_W, LOGICAL_H);
            renderBattleUi(g);
            return img;
        } catch (Throwable t) {
            return null;
        }
    }

    private void enterStageClearResultPanel() {
        if (stageClearPanelActive) {
            return;
        }
        if (replayPlayback.isActive()) {
            returnReplayToMenu(null);
            return;
        }

        // Extra clear: show final result panel directly.
        if (mGamemode == BattleSessionConfig.MODE_EXTRA) {
            enterFinalResultPanelAfterExtraClear();
            return;
        }

        replayRecording.stop();
        replayRecording.clearPending();

        stageClearPhoto = captureBattlePhoto();
        stageAssets.releaseBoss(imgs);

        int hiScore = scoreSystem.getHiScore();
        int score = scoreSystem.getScore();

        // DoJa: stage-clear result uses run-wide continue usage count ("3 - s_continue").
        int continuesUsed = runInitialContinueCount - continueSystem.getRemainingContinues();
        if (continuesUsed < 0) {
            continuesUsed = 0;
        }

        boolean allowReplaySave = false;
        // DoJa: after any continue, replay save is disabled on stage-clear result (a.ci = false).
        if (continuesUsed == 0 && !replayPlayback.isActive() && !session.isSpellPractice() && mGamemode == BattleSessionConfig.MODE_STORY) {
            int lvl = (hud != null) ? hud.getLevel() : session.getDifficulty();
            int spellId = stageLastSpellId;
            if (spellId < 0) {
                spellId = 255;
            }

            allowReplaySave = replayRecording.armPendingForStageClear(
                    lvl, session.getCharacter(), mStage, score, session.getShotType(), mGamemode, spellId);
        }

        // DoJa: stage-clear result shows run-wide totals (ba/ca/ea/da), not per-stage counters.
        stageClearResultPanel.enter(stageClearPhoto, hiScore, score, continuesUsed, runMissCount, runBombUsedCount, runSpellBonusCount, runSpellSeenCount,
                allowReplaySave, session.getCharacter());
        stageClearPanelActive = true;
    }

    private void enterFinalResultPanelAfterExtraClear() {
        if (stageClearPanelActive) {
            return;
        }
        if (replayPlayback.isActive()) {
            returnReplayToMenu(null);
            return;
        }

        // Keep accounting consistent with final result screens.
        resultStats.onRunEnd(replayPlayback.isActive());
        stopBattleRuntime();

        // DoJa : replay save is disabled after using continue (ci=false).
        // Replay recording is also disabled when any cheat is active.
        replayRecording.clearPending();
        int continuesUsed = runInitialContinueCount - continueSystem.getRemainingContinues();
        if (continuesUsed < 0) {
            continuesUsed = 0;
        }
        if (!session.isSpellPractice() && continuesUsed == 0) {
            int lvl = (hud != null) ? hud.getLevel() : session.getDifficulty();
            int score = scoreSystem.getScore();
            int spellId = stageLastSpellId;
            if (spellId < 0) {
                spellId = 255;
            }
            replayRecording.armPendingForStageClear(lvl, session.getCharacter(), mStage, score, session.getShotType(), mGamemode, spellId);
        }

        UnlockFlags.tryUnlockAliceOnExtraClear(mStage, mGamemode);
        scoreSystem.commitScoreToProgress();

        int hiScore = scoreSystem.getHiScore();
        int score = scoreSystem.getScore();

        stageClearResultPanel.enterFinal(hiScore, score, continuesUsed, runMissCount, runBombUsedCount, runSpellBonusCount, runSpellSeenCount);
        stageClearPanelActive = true;
    }

    // Advance to next stage or end the run.
    public void stageAdvanceToNextStageOrEnd() {
        if (replayPlayback.isActive()) {
            returnReplayToMenu(null);
            return;
        }
        resetStageRuntimeState();

        if (mGamemode == BattleSessionConfig.MODE_STAGE_PRACTICE || mGamemode == BattleSessionConfig.MODE_SPELL_PRACTICE) {
            endBattleToTitle();
            return;
        }

        scoreSystem.post(ScoreSystem.EVT_STAGE_ADVANCED, mStage, 0, 0);

        if (mStage >= StageRuntime.STAGE_6) {
            UnlockFlags.tryUnlockExtraOnStoryClear(mStage, mGamemode, session.getDifficulty(), continueSystem.getRemainingContinues(), session.getRunStartStage(), session.isPracticeBoss());
            UnlockFlags.tryUnlockAliceOnExtraClear(mStage, mGamemode);
            scoreSystem.commitScoreToProgress();
            endBattleToTitle();
            return;
        }

        session = session.atStage(mStage + 1);
        mStage = session.getStage();
        bgMoveSpd = 0;
        loadStageAssets();

        stageLastSpellId = -1;
        stageClearPanelActive = false;
        stageClearPhoto = null;

        replayRecording.clearPending();
        ensureTalkTableLoaded();
        createSessionPlayer();
        startSessionRecording();
    }

    // Spawn bomb bullets with character-specific logic.
    private void bombSpawnXa(int n, int i, int shotPower, int speed, int fx, int fy) {
        int baseBulletId;
        int frames;
        int moveType;
        int p = shotPower;
        switch (n) {
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
                baseBulletId = 61 + ((n - 3) << 2);
                frames = 1;
                int dx = fx - 6356992;
                int dy = fy - 7929856;
                int dist2 = (dx * dx) + (dy * dy);
                int d2 = integerSqrt(dist2);
                speed = d2 >> 3;
                if (speed < 8) {
                    speed = 8;
                }
                if (speed > 14) {
                    speed = 14;
                }

                int ang0 = BattleMath.arcTan2Deg(7929856 - fy, 6356992 - fx);
                if (fx < 6356992) {
                    ang0 = BattleMath.normalizeDeg(ang0 + 290);
                    moveType = 4;
                } else {
                    ang0 = BattleMath.normalizeDeg(ang0 + 70);
                    moveType = 5;
                }
                bullets.spawnPlayerEx(baseBulletId, frames, moveType, ang0, speed, fx, fy, p);
                return;
            case 16:
                baseBulletId = 733;
                frames = 4;
                moveType = 6;
                break;
            case 17:
                baseBulletId = 737;
                frames = 4;
                moveType = 7;
                break;
            default:
                return;
        }

        int ang = BattleMath.normalizeDeg(i + 270);
        bullets.spawnPlayerEx(baseBulletId, frames, moveType, ang, speed, fx, fy, p);
    }

    private static int integerSqrt(int value) {
        if (value <= 0) {
            return 0;
        }
        int estimate = 1;
        for (int remainder = value; remainder >= estimate; estimate <<= 1, remainder >>= 1) {
        }
        int previous;
        do {
            previous = estimate;
        } while ((estimate = (value / estimate + estimate) >> 1) < previous);
        return previous;
    }

    public static final class BattleRenderAccess {
        private final GameCore core;

        public BattleRenderAccess(GameCore core) {
            this.core = core;
        }

        public SpriteCatalog cc() {
            return core.cc;
        }

        public boolean debugFpsEnabled() {
            return core.optDebugFpsEnabled;
        }

        public boolean debugPerfEnabled() {
            return core.optDebugPerfEnabled;
        }

        public boolean debugResourceEnabled() {
            return core.optDebugResourceEnabled;
        }

        public ImageBank imgs() {
            return core.imgs;
        }

        public SpriteRenderer sprites() {
            return core.sprites;
        }

        public BattleHudData hud() {
            return core.hud;
        }

        public BulletSystem bullets() {
            return core.bullets;
        }

        public BossController bossController() {
            return core.bossController;
        }

        public int stage() {
            return core.mStage;
        }

        public int gameMode() {
            return core.mGamemode;
        }

        public int currentChara() {
            return core.session.getCharacter();
        }

        public boolean bossMode() {
            return core.mBossmodef;
        }

        public int bossF() {
            return core.sBossf;
        }

        public int bossStock() {
            return core.sBstock;
        }

        public int gcnt() {
            return core.mGcnt;
        }

        public int frameIndex() {
            return core.fi;
        }

        public int taCnt() {
            return core.mTacnt;
        }

        public int timeStop() {
            return core.timeStop;
        }

        public int bgMoveSpd() {
            return core.bgMoveSpd;
        }

        public int bombCnt() {
            return core.bombCnt;
        }

        public int deadcnt() {
            return core.sDeadcnt;
        }

        public int[][] enemylist() {
            return core.enemylist;
        }

        public boolean bossVisualShieldActive() {
            return core.mBossWrk[2] != 0;
        }

        public boolean disableBossBombShieldCheat() {
            return core.disableBossBombShieldCheat;
        }

        public boolean startSpellPractice() {
            return core.session.isSpellPractice();
        }

        public int startSpellId() {
            return core.session.getSpellId();
        }

        public String getSpellCardName(int id) {
            return GameCore.getSpellCardName(id);
        }

        private short[] dialogueCommand() {
            short[][][] table = core.stageRuntime.getStageTable();
            int stageIndex = core.stageNa();
            if (table == null || stageIndex < 0 || stageIndex >= table.length
                    || core.mStep < 0 || core.mStep >= table[stageIndex].length) {
                return null;
            }
            short[] command = table[stageIndex][core.mStep];
            if (command.length < 5 || command[StageDat.OP] != StageDat.DIALOGUE || command[StageDat.FRAME] > core.mGcnt + 1) {
                return null;
            }
            return command;
        }

        public boolean dialogueVisible() {
            return dialogueCommand() != null;
        }

        public int dialoguePortraitType() {
            short[] command = dialogueCommand();
            return (command == null) ? -1 : command[2];
        }

        public String dialogueText() {
            short[] command = dialogueCommand();
            String[] table = core.stageRuntime.getTalkTable();
            if (command == null || table == null) {
                return null;
            }
            int index = command[3];
            return (index >= 0 && index < table.length) ? table[index] : null;
        }

        public int dialogueCounter() {
            return core.stageRuntime.getTalkCnt();
        }

        public int dialoguePage() {
            return core.stageRuntime.getDialoguePage();
        }

        public int dialoguePreviousPage() {
            return core.stageRuntime.getDialoguePrevPage();
        }

        public int dialogueScrollY() {
            return core.stageRuntime.getDialogueScrollY();
        }

        public boolean bossSpellCutInActive() {
            return core.bossSpellCutInActive;
        }

        public int bossSpellCutInStage() {
            return core.bossSpellCutInStage;
        }

        public int bossSpellCutInChara() {
            return core.bossSpellCutInChara;
        }

        public int bossSpellCutInStartFi() {
            return core.bossSpellCutInStartFi;
        }

        public boolean closeUpAlphaEnabled() {
            return core.optCloseUpAlphaEnabled;
        }

        public boolean heavyBgEnabled() {
            return core.optHeavyBgEnabled;
        }

        public DropItemSystem dropItems() {
            return core.dropItems;
        }

        public DropItemSystem.IconDrawer dropItemDrawer() {
            return core.dropItemDrawer;
        }

        public ScoreSystem scoreSystem() {
            return core.scoreSystem;
        }

        public Player player() {
            return core.player;
        }

        public int originX() {
            return core.mOriginx;
        }

        public int originY() {
            return core.mOriginy;
        }

        public boolean battlePaused() {
            return core.battlePaused;
        }

        public boolean stageClearPanelActive() {
            return core.stageClearPanelActive;
        }

        public StageClearResultPanel stageClearResultPanel() {
            return core.stageClearResultPanel;
        }

        public BattlePauseScreen pauseScreen() {
            return core.pauseScreen;
        }

        public boolean spellPracticeEndActive() {
            return core.session.isSpellPractice() && core.spellPracticeEndActive;
        }

        public SpellPracticeEndScreen spellPracticeEndScreen() {
            return core.spellPracticeEndScreen;
        }

        public int endingFadeToWhite() {
            return core.endingFadeToWhite;
        }

        public int hFlash() {
            return core.battleVisualState.getFlash();
        }

        public int iFlash() {
            return core.battleVisualState.getScreenFade();
        }

        public int blindMaskAlpha() {
            return core.battleVisualState.getBlindAlpha();
        }

        public int blindMaskSpellId() {
            return core.battleVisualState.getBlindSpellId();
        }

        public int enemySpriteId(int index) {
            return core.stageRuntime.getEnemySpriteId(index);
        }

        public int enemyAuraSpriteId(int index) {
            return core.stageRuntime.getEnemyAuraSpriteId(index);
        }

        public void stageMc(Graphics g) {
            core.stageMc(g);
        }

        public int[][] effectlist() {
            return core.effectlist;
        }

        public String debugLine() {
            return core.debugLine;
        }

        public String debugLine2() {
            return core.debugLine2;
        }

        public String debugLine3() {
            return core.debugLine3;
        }

        public String debugLine4() {
            return core.debugLine4;
        }

        public String debugLine5() {
            return core.debugLine5;
        }

        public int[][] effectTable() {
            return GameCore.EFFECT_TABLE;
        }
    }
}
