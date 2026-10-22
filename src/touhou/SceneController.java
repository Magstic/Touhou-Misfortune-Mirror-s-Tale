package touhou;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.game.GameCanvas;

import touhou.battle.BattleScene;
import touhou.ui.EndingScreen;
import touhou.ui.StaffRollScreen;
import touhou.ui.StageClearResultPanel;
import touhou.ui.UiController;

public final class SceneController {

    private final BattleScene battleScene;
    private final EndingScreen endingScreen;
    private final StaffRollScreen staffRollScreen;
    private final StageClearResultPanel stageClearResultPanel;
    private final AudioSystem audioSystem;

    SceneController(BattleScene battleScene, EndingScreen endingScreen, StaffRollScreen staffRollScreen, StageClearResultPanel stageClearResultPanel, AudioSystem audioSystem) {
        this.battleScene = battleScene;
        this.endingScreen = endingScreen;
        this.staffRollScreen = staffRollScreen;
        this.stageClearResultPanel = stageClearResultPanel;
        this.audioSystem = audioSystem;
    }

    public void update(GameCore core, int keys, int pressed) {
        if (core.sceneIsEndingActive()) {
            if (endingScreen.update(pressed)) {
                core.sceneSetEndingActive(false);
                endingScreen.leave();
                staffRollScreen.enter(core.sceneImgs(), core.sceneSprites());
                core.sceneSetStaffRollActive(true);
            }
            return;
        }

        if (core.sceneIsStaffRollActive()) {
            if (staffRollScreen.update(keys, pressed)) {
                core.sceneSetStaffRollActive(false);
                staffRollScreen.leave();
                core.sceneEnterFinalResultPanelAfterStaffRoll();
            }
            return;
        }

        if (core.battleFlowIsStageClearPanelActive() && !core.sceneIsInGame()) {
            if ((pressed & (GameCanvas.UP_PRESSED | GameCanvas.DOWN_PRESSED)) != 0) {
                audioSystem.playSeType(SoundEffectSystem.SE_NAV);
            }
            if ((pressed & GameCanvas.FIRE_PRESSED) != 0) {
                audioSystem.playSeType(SoundEffectSystem.SE_SELECT);
            }
            if ((pressed & GameCanvas.GAME_A_PRESSED) != 0) {
                audioSystem.playSeType(SoundEffectSystem.SE_BACK);
            }
            if ((pressed & GameCore.KEY_POUND_PRESSED) != 0) {
                audioSystem.playSeType(SoundEffectSystem.SE_SELECT);
            }

            if (stageClearResultPanel.update(pressed) == StageClearResultPanel.ACTION_BACK_TO_TITLE) {
                core.battleFlowSetStageClearPanelActive(false);
                core.battleFlowClearStageClearPhoto();
                core.battleFlowClearReplayPending();
                stageClearResultPanel.leave(core.sceneImgs());

                UiController ui = core.sceneUi();
                if (ui != null) {
                    ui.returnToTitleKeepCursor();
                }
            }
            return;
        }

        UiController ui = core.sceneUi();
        if (ui != null) {
            ui.update(keys, pressed);
        }
    }

    public void render(GameCore core, Graphics g) {
        if (core.sceneIsEndingActive()) {
            endingScreen.render(g, core.sceneImgs());
            return;
        }

        if (core.sceneIsStaffRollActive()) {
            staffRollScreen.render(g, core.sceneImgs());
            return;
        }

        if (core.battleFlowIsStageClearPanelActive() && !core.sceneIsInGame()) {
            stageClearResultPanel.render(g, core.sceneSprites());
            return;
        }

        if (!core.sceneIsInGame()) {
            UiController ui = core.sceneUi();
            if (ui != null) {
                ui.render(g);
            }
            return;
        }

        battleScene.render(g);
    }
}
