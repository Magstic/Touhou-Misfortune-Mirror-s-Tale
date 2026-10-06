package touhou.replay;

import touhou.Player;
import touhou.ScoreSystem;
import touhou.battle.BattleContinueSystem;
import touhou.battle.BattleHudData;

public final class ReplayRecordingController {
    private final ReplayStageRecorder recorder = new ReplayStageRecorder();
    private byte[] stageSnapshot;

    // Cheat support: when disabled, no recording and no replay save.
    private boolean enabled = true;

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            recorder.stop();
            stageSnapshot = null;
            ReplaySaveService.clearPending();
        }
    }

    public boolean isActive() {
        return enabled && recorder.isRecording();
    }

    public void startSession(boolean bossAtStart) {
        if (!enabled) {
            recorder.stop();
            stageSnapshot = null;
            return;
        }
        recorder.start();
        if (bossAtStart) {
            recorder.markBossStart(0);
        }
        stageSnapshot = null;
    }

    public void stop() {
        recorder.stop();
    }

    public void clearPending() {
        ReplaySaveService.clearPending();
    }

    public void onFrame(int keys, int pressed, int fireActionPressed) {
        if (!enabled) {
            return;
        }
        recorder.onFrame(keys, pressed, fireActionPressed);
    }

    public void recordDialoguePageCount(int encounterIndex, int pageCount) {
        if (!enabled) {
            return;
        }
        recorder.recordDialoguePageCount(encounterIndex, pageCount);
    }

    public void markBossStart() {
        if (enabled) {
            recorder.markBossStart(recorder.getFramePos());
        }
    }

    public void captureStageStartSnapshot(ReplayRng random, BattleContinueSystem continueSystem, BattleHudData hud, ScoreSystem scoreSystem, Player player) {
        if (!enabled) {
            return;
        }
        stageSnapshot = ReplayStageSnapshotService.captureIfNeeded(stageSnapshot, random, continueSystem, hud, scoreSystem, player);
    }

    public boolean armPendingForStageClear(byte[] name32,
            int flag, int difficulty, int chara, int stage, int score, int type, int mode, int spellId) {

        if (!enabled) {
            ReplaySaveService.clearPending();
            return false;
        }

        ReplayHeader h = new ReplayHeader();
        recorder.fillHeaderForSave(h, flag, difficulty, chara, stage, score, type, mode, spellId);
        return ReplaySaveService.armPending(h, name32, recorder, stageSnapshot);
    }
}
