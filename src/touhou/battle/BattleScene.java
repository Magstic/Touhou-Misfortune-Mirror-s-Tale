package touhou.battle;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.game.GameCanvas;

import touhou.AudioSystem;
import touhou.GameCore;
import touhou.SoundEffectSystem;
import touhou.stage.BossController;
import touhou.stage.StageRuntime;
import touhou.ui.BattlePauseScreen;
import touhou.ui.SpellPracticeEndScreen;
import touhou.ui.StageClearResultPanel;

public final class BattleScene {

    private final BattleRenderer battleRenderer;
    private final GameCore.BattleRenderAccess battleRenderAccess;
    private final AudioSystem audioSystem;

    private boolean stageClearAdvancedUnderTransition;

    public BattleScene(BattleRenderer battleRenderer, GameCore.BattleRenderAccess battleRenderAccess, AudioSystem audioSystem) {
        this.battleRenderer = battleRenderer;
        this.battleRenderAccess = battleRenderAccess;
        this.audioSystem = audioSystem;
        this.stageClearAdvancedUnderTransition = false;
    }

    // BossController.Host is provided by BattleScene to keep GameCore thinner.
    public BossController.Host createBossHost(GameCore core, StageRuntime stageRuntime) {
        return new BattleBossHost(core, stageRuntime, audioSystem);
    }

    public boolean update(GameCore core, int keys, int pressed, int fireActionPressed) {
        if (!core.sceneIsInGame()) {
            return false;
        }

        if (core.battleFlowTickSpellPracticeEndCountdownIfNeeded()) {
            core.battleMainHandleResetFlagSideEffects();
            return true;
        }

        if (tickEndingFadeToWhite(core)
                || tickStageClearPanel(core, pressed)
                || tickSpellPracticeEndFlow(core, pressed)
                || tickPaused(core, pressed)
                || tickPauseEnter(core, pressed)) {
            core.battleMainHandleResetFlagSideEffects();
            return true;
        }

        core.battleFrameResetOrigin();
        core.battleVisualBeginFrame();
        try {
            tickMain(core, keys, pressed, fireActionPressed);
            core.battleMainHandleResetFlagSideEffects();
            return true;
        } finally {
            core.battleVisualEndFrame();
        }
    }

    public void render(Graphics g) {
        battleRenderer.render(battleRenderAccess, g);
    }

    private boolean tickEndingFadeToWhite(GameCore core) {
        int v = core.battleFlowGetEndingFadeToWhite();
        if (v <= 0) {
            return false;
        }

        v++;
        if (v > 15) {
            v = 0;
            core.beginIntroShatterWhite();
            core.enterEnding();
        }
        core.battleFlowSetEndingFadeToWhite(v);
        return true;
    }

    private boolean tickStageClearPanel(GameCore core, int pressed) {
        if (!core.battleFlowIsStageClearPanelActive()) {
            stageClearAdvancedUnderTransition = false;
            return false;
        }

        if ((pressed & (GameCanvas.UP_PRESSED | GameCanvas.DOWN_PRESSED)) != 0) {
            this.audioSystem.playSeType(SoundEffectSystem.SE_NAV);
        }
        if ((pressed & GameCanvas.FIRE_PRESSED) != 0) {
            this.audioSystem.playSeType(SoundEffectSystem.SE_SELECT);
        }
        if ((pressed & GameCanvas.GAME_A_PRESSED) != 0) {
            this.audioSystem.playSeType(SoundEffectSystem.SE_BACK);
        }
        if ((pressed & GameCore.KEY_POUND_PRESSED) != 0) {
            this.audioSystem.playSeType(SoundEffectSystem.SE_SELECT);
        }

        if (core.battleFlowIsReplayPlaybackActive()) {
            pressed |= GameCanvas.FIRE_PRESSED;
        }

        StageClearResultPanel panel = core.battleFlowStageClearResultPanel();
        boolean wasLeaving = panel.isLeaving();
        int nextStage = -1;
        if (core.bossHostGetGameMode() == 0) {
            int st = core.bossHostGetStage();
            if (st >= 0 && st < 5) {
                nextStage = st + 1;
            }
        }
        int action = panel.update(pressed, nextStage);

        if (!wasLeaving && panel.isLeaving()) {
            core.battleFlowClearStageClearPhoto();
            core.battleFlowClearReplayPending();

            // Prepare the next stage underneath the exit (corner expand) animation.
            // This matches DoJa's feel: a short stall happens before leaving starts, and the transition reveals the next stage.
            if (!stageClearAdvancedUnderTransition && nextStage >= StageRuntime.STAGE_1) {
                stageClearAdvancedUnderTransition = true;
                core.stageAdvanceToNextStageOrEnd();
                if (core.sceneIsInGame()) {
                    core.battleFlowSetStageClearPanelActive(true);
                }
            }
        }

        if (action == StageClearResultPanel.ACTION_ADVANCE_STAGE) {
            panel.leave(core.battleFlowImgs());
            core.battleFlowSetStageClearPanelActive(false);
            core.battleFlowClearStageClearPhoto();
            core.battleFlowClearReplayPending();

            if (!stageClearAdvancedUnderTransition) {
                core.stageAdvanceToNextStageOrEnd();
            }
            stageClearAdvancedUnderTransition = false;
        }
        return true;
    }

    private boolean tickSpellPracticeEndFlow(GameCore core, int pressed) {
        if (!core.battleFlowIsSpellPractice()) {
            return false;
        }

        if (core.battleFlowIsSpellPracticeEndActive()) {
            if ((pressed & (GameCanvas.UP_PRESSED | GameCanvas.DOWN_PRESSED)) != 0) {
                this.audioSystem.playSeType(SoundEffectSystem.SE_NAV);
            }
            if ((pressed & GameCanvas.FIRE_PRESSED) != 0) {
                this.audioSystem.playSeType(SoundEffectSystem.SE_SELECT);
            }
            if ((pressed & GameCanvas.GAME_A_PRESSED) != 0) {
                this.audioSystem.playSeType(SoundEffectSystem.SE_BACK);
            }

            int action = core.battleFlowSpellPracticeEndScreen().update(pressed);
            if (action == SpellPracticeEndScreen.ACTION_RETRY) {
                core.restartBattle();
            } else if (action == SpellPracticeEndScreen.ACTION_EXIT) {
                core.endSpellPracticeToMenu();
            }
            return true;
        }

        return false;
    }

    private boolean tickPaused(GameCore core, int pressed) {
        if (!core.battlePauseIsPaused()) {
            return false;
        }

        if ((pressed & (GameCanvas.UP_PRESSED | GameCanvas.DOWN_PRESSED)) != 0) {
            this.audioSystem.playSeType(SoundEffectSystem.SE_NAV);
        }
        if ((pressed & GameCanvas.FIRE_PRESSED) != 0) {
            this.audioSystem.playSeType(SoundEffectSystem.SE_SELECT);
        }
        if ((pressed & GameCanvas.GAME_B_PRESSED) != 0) {
            audioSystem.playSeType(SoundEffectSystem.SE_BACK);
        }
        if ((pressed & GameCanvas.GAME_A_PRESSED) != 0) {
            audioSystem.playSeType(SoundEffectSystem.SE_SELECT);
        }

        core.battlePauseSyncPlayerBombActive();

        BattlePauseScreen pauseScreen = core.battlePauseScreen();
        if (pauseScreen != null) {
            int action = pauseScreen.update(pressed);
            if (action == BattlePauseScreen.ACTION_RESUME) {
                core.battlePauseResume();
            } else if (action == BattlePauseScreen.ACTION_TOGGLE_BGM) {
                audioSystem.toggleBgmEnabled();
            } else if (action == BattlePauseScreen.ACTION_QUIT_TO_TITLE) {
                core.battlePauseQuitToTitle();
            } else if (action == BattlePauseScreen.ACTION_RESTART) {
                if (!core.battleFlowIsSpellPractice() && core.bossHostGetGameMode() == BattleSessionConfig.MODE_STORY) {
                    core.battlePauseRestartFromBeginning();
                } else {
                    core.battlePauseRestart();
                }
            } else if (action == BattlePauseScreen.ACTION_CONTINUE_YES) {
                core.battlePauseTryContinueYes();
            } else if (action == BattlePauseScreen.ACTION_CONTINUE_NO) {
                core.battlePauseContinueNo();
            }
        }
        return true;
    }

    private static boolean tickPauseEnter(GameCore core, int pressed) {
        if ((pressed & GameCanvas.GAME_A_PRESSED) == 0) {
            return false;
        }

        core.battlePauseEnterFromGame();
        return true;
    }

    // Main battle gameplay tick orchestrator.
    private static void tickMain(GameCore core, int keys, int pressed, int fireActionPressed) {
        int deadcntBefore = core.battleMainGetDeadcnt();
        boolean dialogueActive = core.battleMainIsDialogueActiveForInput();

        core.battleMainTickInput(pressed, fireActionPressed, dialogueActive);

        if (core.battleMainAdvanceAndHandleDeath()) {
            return;
        }

        core.battleMainTickBomb();

        if (core.battleMainTickTimeStop()) {
            return;
        }

        // DoJa : while s_deadcnt > 0, the main world tick is frozen.
        if (deadcntBefore > 0) {
            return;
        }

        if (core.battleMainTickWorldAndStage(keys)) {
            return;
        }

        core.battleMainTickBulletsAndCollisionAndHpBar();
    }
}
