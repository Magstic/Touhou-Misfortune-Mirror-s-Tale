package touhou.ui;

import javax.microedition.lcdui.Graphics;

import touhou.SpriteRenderer;
import touhou.ImageBank;
import touhou.ResultStats;
import touhou.SoundEffectSystem;
import touhou.UnlockFlags;

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

    public void update(int pressed) {
        // Play UI S.E. after the screen update so Options volume changes take effect immediately.
        int stateBefore = state;
        if (state == STATE_LOADING) {
            updateLoading(pressed);
            return;
        }

        if (updateCurrentScreen(pressed)) {
            playUiSe(stateBefore, pressed);
        }
    }

    private void updateLoading(int pressed) {
        loading.update(pressed);
        if (loading.isDone()) {
            startupLanguage.enter();
            transitionTo(STATE_STARTUP_LANGUAGE);
        }
    }

    private boolean updateCurrentScreen(int pressed) {
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
                updateStory(pressed);
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
        StartupLanguageMenu.Result r = startupLanguage.update(pressed);
        if (r != null && r.kind == StartupLanguageMenu.Result.KIND_DONE) {
            openTitle(true);
        }
    }

    private void updateTitle(int pressed) {
        int action = title.update(pressed);
        if (action == TitleScreen.ACTION_START) {
            openStartSetup(0);
        } else if (action == TitleScreen.ACTION_EXTRA_START) {
            openStartSetup(1);
        } else if (action == TitleScreen.ACTION_PRACTICE) {
            openStartSetup(2);
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
        HelpScreen.Result r = help.update(pressed);
        if (r != null && r.kind == HelpScreen.Result.KIND_BACK_TO_TITLE) {
            returnToTitleKeepCursor();
        }
    }

    private void updateStory(int pressed) {
        StoryScreen.Result r = story.update(pressed);
        if (r != null && r.kind == StoryScreen.Result.KIND_BACK_TO_TITLE) {
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
            if (r.chara == 2 && !UnlockFlags.isAliceUnlocked()) {
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
            replayReceive.enter();
            transitionTo(STATE_REPLAY_RECEIVE);
        } else if (r.kind == ReplayScreen.Result.KIND_OPEN_BT_SEND) {
            replaySend.enter(r.slot);
            transitionTo(STATE_REPLAY_SEND);
        }
    }

    private void updateReplayReceive(int pressed) {
        ReplayReceiveScreen.Result r = replayReceive.update(pressed);
        if (r != null && r.kind == ReplayReceiveScreen.Result.KIND_BACK_TO_REPLAY) {
            replay.returnToList();
            transitionTo(STATE_REPLAY);
        }
    }

    private void updateReplaySend(int pressed) {
        ReplaySendScreen.Result r = replaySend.update(pressed);
        if (r != null && r.kind == ReplaySendScreen.Result.KIND_BACK_TO_REPLAY) {
            replay.returnToList();
            transitionTo(STATE_REPLAY);
        }
    }

    private void updateResult(int pressed) {
        ResultScreen.Result r = result.update(pressed);
        if (r != null && r.kind == ResultScreen.Result.KIND_BACK_TO_TITLE) {
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
        OptionScreen.Result r = option.update(pressed);
        if (r != null && r.kind == OptionScreen.Result.KIND_BACK_TO_TITLE) {
            returnToTitleKeepCursor();
        }
    }

    private void updateQuit(int pressed) {
        QuitScreen.Result r = quit.update(pressed);
        if (r == null) {
            return;
        }
        if (r.kind == QuitScreen.Result.KIND_BACK_TO_TITLE) {
            returnToTitleKeepCursor();
        } else if (r.kind == QuitScreen.Result.KIND_QUIT && listener != null) {
            listener.onQuitRequested();
        }
    }

    private void transitionTo(int nextState) {
        int[] sheets;
        switch (nextState) {
            case STATE_STARTUP_LANGUAGE:
                sheets = new int[] { 0, 2, 3, 5, 127 };
                break;
            case STATE_TITLE:
            case STATE_QUIT:
                sheets = new int[] { 0, 1, 2, 3, 5 };
                break;
            case STATE_START_SETUP:
                sheets = new int[] { 6, 7, 8, 28, 37 };
                break;
            case STATE_HELP:
                sheets = new int[] { 4, 10 };
                break;
            case STATE_SPELL_PRACTICE:
                sheets = new int[] { 6 };
                break;
            case STATE_STORY:
                sheets = new int[] { 83 };
                break;
            case STATE_REPLAY:
            case STATE_REPLAY_RECEIVE:
            case STATE_REPLAY_SEND:
                sheets = new int[] { 37 };
                break;
            default:
                sheets = new int[0];
                break;
        }
        images.retain(new int[][] { sheets });
        for (int i = 0; i < sheets.length; i++) images.get(sheets[i]);
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
                spellPractice.render(g, images, sprites);
                break;
            case STATE_REPLAY:
                replay.render(g, images, sprites);
                break;
            case STATE_REPLAY_RECEIVE:
                replayReceive.render(g, images, sprites);
                break;
            case STATE_REPLAY_SEND:
                replaySend.render(g, images, sprites);
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
}
