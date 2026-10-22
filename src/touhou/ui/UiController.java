package touhou.ui;

import javax.microedition.lcdui.Graphics;

import touhou.ImageBank;
import touhou.Player;
import touhou.ResultStats;
import touhou.SoundEffectSystem;
import touhou.SpriteRenderer;
import touhou.SpriteSheetCatalog;
import touhou.UnlockFlags;
import touhou.battle.BattleSessionConfig;
import touhou.i18n.TextId;

public final class UiController {
    public static final int STATE_TITLE = 0;
    public static final int STATE_LOADING = 9;
    public static final int STATE_START_SETUP = 1;
    public static final int STATE_HELP = 2;
    public static final int STATE_SPELL_PRACTICE = 3;
    public static final int STATE_REPLAY = 4;
    public static final int STATE_REPLAY_RECEIVE = 11;
    public static final int STATE_REPLAY_SEND = 12;
    public static final int STATE_RESULT = 5;
    public static final int STATE_MUSIC_ROOM = 6;
    public static final int STATE_OPTION = 7;
    public static final int STATE_QUIT = 8;
    public static final int STATE_STORY = 10;
    // Shown right after LoadingScreen ("少女祈祷中") to apply i18n before entering title.
    public static final int STATE_STARTUP_LANGUAGE = 13;
    public static final int STATE_REPLAY_FILES = 14;

    public interface Listener {
        void onStartGameRequested(int gamemode, int level, int chara, int type, int startStage, boolean pracBoss);

        // Start replay playback from a saved slot.
        void onStartReplayRequested(int slot, boolean bossOnly);

        void onStartSpellPracticeRequested(int chara, int type, int spellId);

        void onQuitRequested();

        void onPlayBgmRequested(int trackId);

        void onPlaySeTypeRequested(int seType);

        void onVolumesChanged(int bgmVol, int seVol);

        void onSeMaskChanged(int mask);

        void onHeavyBgmChanged(boolean enabled);

        void onMusicRoomVolumeChanged(int bgmVol);
    }

    private final ImageBank images;
    private final SpriteRenderer sprites;
    private final Listener listener;

    private TitleScreen title;
    private final LoadingScreen loading;
    private final StartupLanguageMenu startupLanguage;
    private StartSetupScreen setup;
    private HelpScreen help;
    private StoryScreen story;
    private SpellPracticeScreen spellPractice;
    private ReplayScreen replay;
    private ReplayReceiveScreen replayReceive;
    private ReplaySendScreen replaySend;
    private ReplayFileScreen replayFiles;
    private ResultScreen result;
    private MusicRoomScreen musicRoom;
    private OptionScreen option;
    private QuitScreen quit;

    private int state;

    public UiController(ImageBank images, SpriteRenderer sprites, Listener listener) {
        this.images = images;
        this.sprites = sprites;
        this.listener = listener;

        this.loading = new LoadingScreen();
        this.startupLanguage = new StartupLanguageMenu();

        this.loading.enter();
        transitionTo(STATE_LOADING);
    }

    // Defer creating language-dependent screens until after startup language selection.
    private void ensureMainScreens() {
        if (title != null) {
            return;
        }
        title = new TitleScreen();
        setup = new StartSetupScreen();
        help = new HelpScreen();
        story = new StoryScreen();
        spellPractice = new SpellPracticeScreen();
        replay = new ReplayScreen();
        replayReceive = new ReplayReceiveScreen();
        replaySend = new ReplaySendScreen();
        result = new ResultScreen();
        musicRoom = new MusicRoomScreen();
        option = new OptionScreen(new OptionScreen.Listener() {
            public void onVolumesChanged(int bgmVol, int seVol) {
                if (UiController.this.listener != null) {
                    UiController.this.listener.onVolumesChanged(bgmVol, seVol);
                }
            }

            public void onSeMaskChanged(int seMask) {
                if (UiController.this.listener != null) {
                    UiController.this.listener.onSeMaskChanged(seMask);
                }
            }

            public void onHeavyBgmChanged(boolean enabled) {
                if (UiController.this.listener != null) {
                    UiController.this.listener.onHeavyBgmChanged(enabled);
                }
            }
        });
        quit = new QuitScreen();
    }

    public void returnToTitleKeepCursor() {
        openTitle(false);
    }

    private void openTitle(boolean resetCursor) {
        ensureMainScreens();
        if (resetCursor) {
            title.reset();
        } else {
            title.onReturnToTitle();
        }
        transitionTo(STATE_TITLE);
        ResultStats.INSTANCE.onReturnToTitle();
    }

    public void returnToSpellPracticeKeepCursor() {
        ensureMainScreens();
        spellPractice.enter(false);
        transitionTo(STATE_SPELL_PRACTICE);
    }

    // Return from gameplay to replay menu.
    public void returnToReplayKeepCursor() {
        ensureMainScreens();
        replay.returnToList();
        transitionTo(STATE_REPLAY);
    }

    public void returnToReplayKeepCursorWithMessage(String msg) {
        ensureMainScreens();
        replay.returnToList();
        replay.showMessage(msg);
        transitionTo(STATE_REPLAY);
    }

    public void update(int keys, int pressed) {
        // Play UI S.E. after the screen update so Options volume changes take effect immediately.
        int stateBefore = state;
        if (state == STATE_LOADING) {
            updateLoading();
            return;
        }

        if (updateCurrentScreen(keys, pressed)) {
            playUiSe(stateBefore, pressed);
        }
    }

    private void updateLoading() {
        loading.update();
        if (loading.isDone()) {
            startupLanguage.enter();
            transitionTo(STATE_STARTUP_LANGUAGE);
        }
    }

    private boolean updateCurrentScreen(int keys, int pressed) {
        switch (state) {
            case STATE_STARTUP_LANGUAGE:
                updateStartupLanguage(pressed);
                return true;
            case STATE_TITLE:
                updateTitle(pressed);
                return true;
            case STATE_START_SETUP:
                return updateStartSetup(pressed);
            case STATE_HELP:
                updateHelp(pressed);
                return true;
            case STATE_STORY:
                updateStory(keys, pressed);
                return true;
            case STATE_SPELL_PRACTICE:
                return updateSpellPractice(pressed);
            case STATE_REPLAY:
                updateReplay(pressed);
                return true;
            case STATE_REPLAY_RECEIVE:
                updateReplayReceive(pressed);
                return true;
            case STATE_REPLAY_SEND:
                updateReplaySend(pressed);
                return true;
            case STATE_REPLAY_FILES:
                if (replayFiles.update(pressed)) {
                    replayFiles = null;
                    returnToReplayKeepCursor();
                }
                return true;
            case STATE_RESULT:
                updateResult(pressed);
                return true;
            case STATE_MUSIC_ROOM:
                updateMusicRoom(pressed);
                return true;
            case STATE_OPTION:
                updateOption(pressed);
                return true;
            case STATE_QUIT:
                updateQuit(pressed);
                return true;
            default:
                return false;
        }
    }

    private void updateStartupLanguage(int pressed) {
        if (startupLanguage.update(pressed)) {
            openTitle(true);
        }
    }

    private void updateTitle(int pressed) {
        int action = title.update(pressed);
        if (action == TitleScreen.ACTION_START) {
            openStartSetup(BattleSessionConfig.MODE_STORY);
        } else if (action == TitleScreen.ACTION_EXTRA_START) {
            openStartSetup(BattleSessionConfig.MODE_EXTRA);
        } else if (action == TitleScreen.ACTION_PRACTICE) {
            openStartSetup(BattleSessionConfig.MODE_STAGE_PRACTICE);
        } else if (action == TitleScreen.ACTION_HELP) {
            help.enter();
            transitionTo(STATE_HELP);
        } else if (action == TitleScreen.ACTION_STORY) {
            story.enter();
            transitionTo(STATE_STORY);
        } else if (action == TitleScreen.ACTION_SPELL_PRACTICE) {
            spellPractice.enter(true);
            transitionTo(STATE_SPELL_PRACTICE);
        } else if (action == TitleScreen.ACTION_REPLAY) {
            replay.enter();
            transitionTo(STATE_REPLAY);
        } else if (action == TitleScreen.ACTION_RESULT) {
            result.enter();
            transitionTo(STATE_RESULT);
        } else if (action == TitleScreen.ACTION_MUSIC_ROOM) {
            musicRoom.enter();
            transitionTo(STATE_MUSIC_ROOM);
            if (listener != null) {
                listener.onMusicRoomVolumeChanged(musicRoom.getVolume());
            }
        } else if (action == TitleScreen.ACTION_OPTION) {
            option.enter();
            transitionTo(STATE_OPTION);
        } else if (action == TitleScreen.ACTION_QUIT) {
            quit.enter();
            transitionTo(STATE_QUIT);
        }
    }

    private void openStartSetup(int mode) {
        setup.enter(mode);
        transitionTo(STATE_START_SETUP);
    }

    private boolean updateStartSetup(int pressed) {
        StartSetupScreen.Result r = setup.update(pressed);
        if (r == null) {
            return true;
        }
        if (r.kind == StartSetupScreen.Result.KIND_BACK_TO_TITLE) {
            returnToTitleKeepCursor();
        } else if (r.kind == StartSetupScreen.Result.KIND_START_GAME) {
            if (listener != null) {
                listener.onStartGameRequested(r.gamemode, r.level, r.chara, r.type, r.startStage, r.pracBoss);
            }
            return false;
        }
        return true;
    }

    private void updateHelp(int pressed) {
        if (help.update(pressed)) {
            returnToTitleKeepCursor();
        }
    }

    private void updateStory(int keys, int pressed) {
        if (story.update(keys, pressed)) {
            returnToTitleKeepCursor();
        }
    }

    private boolean updateSpellPractice(int pressed) {
        SpellPracticeScreen.Result r = spellPractice.update(pressed);
        if (r == null) {
            return true;
        }
        if (r.kind == SpellPracticeScreen.Result.KIND_BACK_TO_TITLE) {
            returnToTitleKeepCursor();
        } else if (r.kind == SpellPracticeScreen.Result.KIND_START_PRACTICE) {
            if (r.chara == Player.ALICE && !UnlockFlags.isAliceUnlocked()) {
                return false;
            }
            if (listener != null) {
                listener.onStartSpellPracticeRequested(r.chara, r.type, r.spellId);
            }
            return false;
        }
        return true;
    }

    private void updateReplay(int pressed) {
        ReplayScreen.Result r = replay.update(pressed);
        if (r == null) {
            return;
        }
        if (r.kind == ReplayScreen.Result.KIND_BACK_TO_TITLE) {
            returnToTitleKeepCursor();
        } else if (r.kind == ReplayScreen.Result.KIND_START_REPLAY) {
            if (listener != null) {
                listener.onStartReplayRequested(r.slot, r.bossOnly);
            }
        } else if (r.kind == ReplayScreen.Result.KIND_OPEN_BT_RECEIVE) {
            replayReceive.enter(r.slot);
            transitionTo(STATE_REPLAY_RECEIVE);
        } else if (r.kind == ReplayScreen.Result.KIND_OPEN_BT_SEND) {
            replaySend.enter(r.slot);
            transitionTo(STATE_REPLAY_SEND);
        } else if (r.kind == ReplayScreen.Result.KIND_OPEN_EXPORT || r.kind == ReplayScreen.Result.KIND_OPEN_IMPORT) {
            replayFiles = new ReplayFileScreen();
            replayFiles.enter(r.slot, r.kind == ReplayScreen.Result.KIND_OPEN_IMPORT);
            transitionTo(STATE_REPLAY_FILES);
        }
    }

    private void updateReplayReceive(int pressed) {
        if (replayReceive.update(pressed)) {
            replay.returnToList();
            transitionTo(STATE_REPLAY);
        }
    }

    private void updateReplaySend(int pressed) {
        if (replaySend.update(pressed)) {
            replay.returnToList();
            transitionTo(STATE_REPLAY);
        }
    }

    private void updateResult(int pressed) {
        if (result.update(pressed)) {
            returnToTitleKeepCursor();
        }
    }

    private void updateMusicRoom(int pressed) {
        MusicRoomScreen.Result r = musicRoom.update(pressed);
        if (r == null) {
            return;
        }
        if (r.kind == MusicRoomScreen.Result.KIND_BACK_TO_TITLE) {
            returnToTitleKeepCursor();
        } else if (r.kind == MusicRoomScreen.Result.KIND_PLAY_TRACK) {
            if (listener != null) {
                listener.onPlayBgmRequested(r.trackId);
            }
        } else if (r.kind == MusicRoomScreen.Result.KIND_VOLUME_CHANGED) {
            if (listener != null) {
                listener.onMusicRoomVolumeChanged(r.trackId);
            }
        }
    }

    private void updateOption(int pressed) {
        if (option.update(pressed)) {
            returnToTitleKeepCursor();
        }
    }

    private void updateQuit(int pressed) {
        int action = quit.update(pressed);
        if (action == QuitScreen.ACTION_BACK_TO_TITLE) {
            returnToTitleKeepCursor();
        } else if (action == QuitScreen.ACTION_QUIT && listener != null) {
            listener.onQuitRequested();
        }
    }

    private void transitionTo(int nextState) {
        int[] sheets;
        switch (nextState) {
            case STATE_STARTUP_LANGUAGE:
                sheets = new int[] { SpriteSheetCatalog.TITLE_BACKGROUND_IMAGE, SpriteSheetCatalog.TITLE_GLYPHS_IMAGE,
                    SpriteSheetCatalog.TITLE_CAPTION_IMAGE, SpriteSheetCatalog.TITLE_PORTRAIT_IMAGE, 127 };
                break;
            case STATE_TITLE:
            case STATE_QUIT:
                sheets = new int[] { SpriteSheetCatalog.TITLE_BACKGROUND_IMAGE, 1, SpriteSheetCatalog.TITLE_GLYPHS_IMAGE,
                    SpriteSheetCatalog.TITLE_CAPTION_IMAGE, SpriteSheetCatalog.TITLE_PORTRAIT_IMAGE };
                break;
            case STATE_START_SETUP:
                sheets = new int[] { SpriteSheetCatalog.CHARACTER_PORTRAITS_IMAGE,
                    setup.getGameMode() == BattleSessionConfig.MODE_EXTRA ? SpriteSheetCatalog.EXTRA_DIFFICULTY_IMAGE : SpriteSheetCatalog.DIFFICULTY_IMAGE,
                    28, SpriteSheetCatalog.MENU_BACKGROUND_IMAGE };
                break;
            case STATE_HELP:
                sheets = new int[] { SpriteSheetCatalog.HELP_ARROWS_IMAGE, 10 };
                break;
            case STATE_SPELL_PRACTICE:
                sheets = new int[] { SpriteSheetCatalog.CHARACTER_PORTRAITS_IMAGE };
                break;
            case STATE_STORY:
                sheets = new int[] { SpriteSheetCatalog.ICE_BACKGROUND_IMAGE };
                break;
            case STATE_REPLAY:
                sheets = new int[] { SpriteSheetCatalog.CHARACTER_PORTRAITS_IMAGE, SpriteSheetCatalog.MENU_BACKGROUND_IMAGE };
                break;
            case STATE_REPLAY_RECEIVE:
            case STATE_REPLAY_SEND:
            case STATE_REPLAY_FILES:
                sheets = new int[] { SpriteSheetCatalog.MENU_BACKGROUND_IMAGE };
                break;
            default:
                sheets = new int[0];
                break;
        }
        images.retain(new int[][] { sheets });
        for (int i = 0; i < sheets.length; i++) {
            images.get(sheets[i]);
        }
        state = nextState;
    }

    private void playUiSe(int stateBefore, int pressed) {
        if (UiInput.anyDirection(pressed)) {
            playSe(SoundEffectSystem.SE_NAV);
        }
        if (UiInput.confirm(pressed)) {
            playSe(SoundEffectSystem.SE_SELECT);
        }
        if (stateBefore == STATE_TITLE && UiInput.cancel(pressed)) {
            playSe(SoundEffectSystem.SE_SELECT);
        }
        if (stateBefore != STATE_TITLE && UiInput.cancel(pressed)) {
            playSe(SoundEffectSystem.SE_BACK);
        }
    }

    private void playSe(int seType) {
        if (listener == null) {
            return;
        }
        listener.onPlaySeTypeRequested(seType);
    }

    public void render(Graphics g) {
        switch (state) {
            case STATE_LOADING:
                loading.render(g);
                break;
            case STATE_STARTUP_LANGUAGE:
                startupLanguage.render(g, images, sprites);
                break;
            case STATE_TITLE:
                title.render(g, images, sprites);
                break;
            case STATE_START_SETUP:
                setup.render(g, images, sprites);
                break;
            case STATE_HELP:
                help.render(g, sprites);
                break;
            case STATE_STORY:
                story.render(g, images);
                break;
            case STATE_SPELL_PRACTICE:
                spellPractice.render(g, sprites);
                break;
            case STATE_REPLAY:
                replay.render(g, images, sprites);
                break;
            case STATE_REPLAY_RECEIVE:
                replayReceive.render(g, images);
                break;
            case STATE_REPLAY_SEND:
                replaySend.render(g, images);
                break;
            case STATE_REPLAY_FILES:
                replayFiles.render(g, images);
                break;
            case STATE_RESULT:
                result.render(g);
                break;
            case STATE_MUSIC_ROOM:
                musicRoom.render(g);
                break;
            case STATE_OPTION:
                option.render(g);
                break;
            case STATE_QUIT:
                title.render(g, images, sprites);
                quit.render(g);
                break;
        }
    }

    public int getState() {
        return state;
    }

    public int getLeftSoftkeyTextId() {
        switch (state) {
            case STATE_LOADING:
                return -1;
            case STATE_STARTUP_LANGUAGE:
                return TextId.SOFTKEY_OK;
            case STATE_TITLE:
                return TextId.SOFTKEY_HELP;
            case STATE_REPLAY_RECEIVE:
                return replayReceive.getLeftSoftkeyTextId();
            case STATE_REPLAY_SEND:
                return replaySend.getLeftSoftkeyTextId();
            case STATE_REPLAY_FILES:
                return replayFiles.getLeftSoftkeyTextId();
            default:
                return TextId.SOFTKEY_BACK;
        }
    }

    public int getRightSoftkeyTextId() {
        switch (state) {
            case STATE_TITLE:
                return TextId.SOFTKEY_STORY;
            case STATE_STORY:
                return TextId.SOFTKEY_NEXT;
            case STATE_SPELL_PRACTICE:
                return TextId.SOFTKEY_TYPE;
            case STATE_REPLAY_FILES:
                return replayFiles.getRightSoftkeyTextId();
            default:
                return -1;
        }
    }
}
