package touhou.stage;

import java.io.IOException;

import javax.microedition.lcdui.Graphics;

import touhou.GameProgress;
import touhou.Player;
import touhou.ResourceException;
import touhou.SpriteSheetCatalog;
import touhou.UnlockFlags;
import touhou.battle.BattleMath;
import touhou.battle.BattleSessionConfig;
import touhou.i18n.I18n;
import touhou.ui.FontTextMeasurer;
import touhou.ui.UiTextLayout;

public final class StageRuntime {
    public static final int STAGE_1 = 0;
    public static final int STAGE_2 = 1;
    public static final int STAGE_3 = 2;
    public static final int STAGE_4 = 3;
    public static final int STAGE_5 = 4;
    public static final int STAGE_6 = 5;
    public static final int STAGE_EXTRA = 6;
    public static final int STAGE_COUNT = 7;

    // Enemy table fields. Pattern scratch values are reused; bosses store maximum HP in ENEMY_WORK_0.
    public static final int ENEMY_ACTIVE = 0;
    public static final int ENEMY_APPEARANCE = 1;
    public static final int ENEMY_AGE = 2;
    public static final int ENEMY_ATTACK_STATE = 3;
    public static final int ENEMY_HP = 4;
    public static final int ENEMY_X = 5;
    public static final int ENEMY_Y = 6;
    public static final int ENEMY_MOVE_STATE = 7;
    public static final int ENEMY_SHOT_PATTERN = 8;
    public static final int ENEMY_WORK_0 = 9;
    public static final int ENEMY_WORK_1 = 10;
    public static final int ENEMY_MOVE_PATTERN = 11;
    public static final int ENEMY_SPEED = 12;
    public static final int ENEMY_ANGLE = 13;
    public static final int ENEMY_VX = 14;
    public static final int ENEMY_VY = 15;
    public static final int ENEMY_TARGETABLE = 16;
    public static final int ENEMY_HIT_LEFT = 17;
    public static final int ENEMY_HIT_RIGHT = 18;
    public static final int ENEMY_HIT_TOP = 19;
    public static final int ENEMY_HIT_BOTTOM = 20;
    public static final int ENEMY_DROP_TYPE = 21;
    public static final int ENEMY_FIELD_COUNT = 22;

    // Effect table fields.
    public static final int EFFECT_ACTIVE = 0;
    public static final int EFFECT_TIMER = 1;
    public static final int EFFECT_FRAME = 2;
    public static final int EFFECT_ID = 3;
    public static final int EFFECT_X = 4;
    public static final int EFFECT_Y = 5;
    public static final int EFFECT_VX = 6;
    public static final int EFFECT_VY = 7;
    public static final int EFFECT_DELAY = 8;
    public static final int EFFECT_FIELD_COUNT = 9;

    public static final int ENEMY_CAPACITY = 32;
    public static final int EFFECT_CAPACITY = 64;
    private static final int BOSS_WORK_SIZE = 28;
    private static final UiTextLayout.Measurer DIALOGUE_MEASURER = new FontTextMeasurer();

    private static void fillPlayRect(Graphics g, int x, int y, int w, int h, int playX, int playY, int playW, int playH) {
        if (w <= 0 || h <= 0) {
            return;
        }
        int right = playX + playW;
        int bottom = playY + playH;
        if (x > right || y > bottom || x < playX - w || y < playY - h) {
            return;
        }

        if (x + w > right) {
            w -= (x + w - right);
        }
        if (y + h > bottom) {
            h -= (y + h - bottom);
        }
        if (x < playX) {
            w -= (playX - x);
            x = playX;
        }
        if (y < playY) {
            h -= (playY - y);
            y = playY;
        }
        if (w <= 0 || h <= 0) {
            return;
        }
        g.fillRect(x, y, w, h);
    }

    public interface StageServices {
        void requestBgm(int trackId);

        // Toggle rendering of red hit spark rectangles (DoJa effectId 2/21).
        boolean isHitSparkEnabled();

        int[][] getEffectTable();

        void spawnBulletEffect(int effectId, int xFixed, int yFixed);

        int randomNextInt();

        void drawSprite(Graphics g, int spriteId, int x, int y);
    }

    public interface StageFlow {
        boolean shouldEnterEndingAfterStageClear();

        void beginEndingTransition();

        void enterStageClearResultPanel();
    }

    public interface Host extends StageServices, StageFlow {
        void clearDropItems();

        void clearEnemies();

        void resetBossController();

        int getStage();

        int getGameMode();

        int getBstock();

        void setBstock(int v);

        void setEnemyAabb(int idx, int xFixed, int yFixed,
                int leftExtentFixed, int rightExtentFixed, int topExtentFixed, int bottomExtentFixed,
                boolean canTarget);

        int stageNa();

        int getStep();

        void setStep(int v);

        int getTaCnt();

        void setTaCnt(int v);

        void setIFlash(int v);

        void stageHd(int enemyIdx);

        void stageUb(int enemyIdx);

        void stageDb(int enemyIdx);

        void stageEb(int enemyIdx);

        void freeEnemy(int enemyIdx);

        int getOriginX();

        int getOriginY();

        int getPlayX();

        int getPlayY();

        int getPlayW();

        int getPlayH();

        int getBossF();

        void setBossF(int v);

        int getMGCnt();

        void setMGCnt(int v);

        void setBbaria(int v);

        void setLatterFlag(boolean v);

        void setBossMode(boolean v);

        int[] getBossX();

        int[] getBossY();

        int getStartLevel();

        boolean isReplayPlaybackActive();

        boolean isReplayRecordingActive();

        void recordDialoguePageCount(int encounterIndex, int pageCount);

        int getRecordedDialoguePageCount(int encounterIndex);

        void markBossStart();

        void setResetFlag(int v);

        void setHFlash(int v);

        void setTargetFps(int fps);
    }

    private final Host host;

    private StageDat stageDat;
    private short[][][] stageTable;

    private final int[][] enemyList = new int[ENEMY_CAPACITY][ENEMY_FIELD_COUNT];
    private final int[] bossWrk = new int[BOSS_WORK_SIZE];
    private final int[][] effectList = new int[EFFECT_CAPACITY][EFFECT_FIELD_COUNT];
    private final EnemyAnimationState enemyAnimation = new EnemyAnimationState();

    private String[] talkTable;
    private int talkLoadedStage = -1;
    private int talkLoadedChara = -1;

    private int talkCnt = 3;

    // Dialogue pagination state for opcode 246.
    private int dialogueTalkIndex = -1;
    private int dialoguePage = 0;
    private int dialoguePrevPage = 0;
    private int dialogueScrollY = 0;

    private int dialogueEncounterIndex = -1;

    public StageRuntime(Host host) {
        this.host = host;
    }

    public void ensureStageDatLoaded() {
        if (stageTable != null) {
            return;
        }
        try {
            stageDat = StageDat.load();
        } catch (IOException e) {
            throw new ResourceException(e.toString());
        }
        stageTable = stageDat.getTable();
    }

    public void advanceEnemyAnimations() {
        enemyAnimation.advance(enemyList, host.getBossX(), bossWrk);
    }

    public int getEnemySpriteId(int index) {
        return enemyAnimation.spriteId(index);
    }

    public int getEnemyAuraSpriteId(int index) {
        return enemyAnimation.auraSpriteId(index);
    }

    public int[][] getEnemyList() {
        return enemyList;
    }

    public int[] getBossWrk() {
        return bossWrk;
    }

    // BossController.Host bridge: expose boss helper buffers via StageRuntime.
    public int[] getBossX() {
        return host.getBossX();
    }

    public int[] getBossY() {
        return host.getBossY();
    }

    public int getBstock() {
        return host.getBstock();
    }

    // StageJb core: cleanup boss summons and related helper slots.
    public void stageJbBossPhaseCleanup() {
        for (int i = 0; i != 10; ++i) {
            int si = bossWrk[8 + i];
            if (si < 0 || si >= enemyList.length) {
                continue;
            }
            if (enemyList[si][ENEMY_ACTIVE] == 0) {
                continue;
            }
            int mt = enemyList[si][ENEMY_MOVE_PATTERN];
            if (mt == BossController.MIDBOSS_PATTERN || mt == BossController.BOSS_PATTERN || mt == BossController.SECOND_BOSS_PATTERN || mt == BossController.THIRD_BOSS_PATTERN) {
                continue;
            }
            stageKcEffect(0, enemyList[si][ENEMY_X], enemyList[si][ENEMY_Y]);
            enemyList[si][ENEMY_ACTIVE] = 0;
            host.freeEnemy(si);
            bossWrk[8 + i] = 0;
        }

        for (int i = 0; i != ENEMY_CAPACITY; ++i) {
            if (enemyList[i][ENEMY_ACTIVE] == 0) {
                continue;
            }
            int id = enemyList[i][ENEMY_APPEARANCE];
            if (id == 20 || id == 21) {
                enemyList[i][ENEMY_ACTIVE] = 0;
                host.freeEnemy(i);
            }
        }

        for (int i = 8; i <= 13; i++) {
            bossWrk[i] = 0;
        }
    }

    // StageJb core: resolve snow-scatter effectId for spell practice boss defeat.
    public int stageJbResolveSpellPracticeBossDefeatEffectId() {
        int stage = host.getStage();
        int effectId = -1;
        if (stage == STAGE_1) {
            effectId = 6;
        } else {
            switch (stage) {
                case STAGE_2:
                    effectId = 9;
                    break;
                case STAGE_3:
                    effectId = 10;
                    break;
                case STAGE_4:
                    effectId = (bossWrk[24] == 0) ? 11 : 12;
                    break;
                case STAGE_5:
                    effectId = 13;
                    break;
                case STAGE_6:
                    effectId = 14;
                    break;
                case STAGE_EXTRA:
                    effectId = 15;
                    break;
            }
        }
        return effectId;
    }

    // StageJb core: mt==BossController.MIDBOSS_PATTERN early handling and state transition.
    // Return true if caller should return immediately.
    public boolean stageJbMt100PreProcess(final int n, final int ex, final int ey) {
        if (enemyList[n][ENEMY_MOVE_STATE] == 0) {
            return true;
        }

        if (enemyList[n][ENEMY_MOVE_STATE] == 2) {
            enemyList[n][ENEMY_ACTIVE] = 0;
            host.freeEnemy(n);
            host.setStep(0);
            host.setMGCnt(0);
            host.setLatterFlag(true);
            return true;
        }

        enemyList[n][ENEMY_ATTACK_STATE] = 0;
        host.setResetFlag(4);
        stageKcEffect(0, ex, ey);
        return false;
    }

    // StageJb core: boss defeated (mt 101..103, no stock) enter sequence.
    // Does not post score / practice-end; those remain in upper layer.
    public void stageJbBossDefeatedEnter(final int ex, final int ey) {
        host.setTargetFps(5);
        host.setMGCnt(4990);
        host.setBossF(0);

        int stage = host.getStage();
        if (stage == STAGE_1) {
            for (int i = 0; i < 3; ++i) {
                if (enemyList[i][ENEMY_ACTIVE] == 0) {
                    continue;
                }
                stageKcEffect(6 + i, enemyList[i][ENEMY_X], enemyList[i][ENEMY_Y]);
                enemyList[i][ENEMY_ACTIVE] = 0;
                host.freeEnemy(i);
            }
            return;
        }

        int stageEffect = -1;
        switch (stage) {
            case STAGE_2:
                stageEffect = 9;
                break;
            case STAGE_3:
                stageEffect = 10;
                break;
            case STAGE_4:
                stageEffect = (bossWrk[24] == 0) ? 11 : 12;
                break;
            case STAGE_5:
                stageEffect = 13;
                break;
            case STAGE_6:
                stageEffect = 14;
                break;
            case STAGE_EXTRA:
                stageEffect = 15;
                break;
        }
        if (stageEffect >= 0) {
            stageKcEffect(stageEffect, ex, ey);
        }
    }

    // StageJb core: spell practice boss defeated (mt==BossController.MIDBOSS_PATTERN) enter sequence.
    public void stageJbSpellPracticeBossDefeatedEnter(final int bossEnemyIdx, final int ex, final int ey) {
        host.setTargetFps(5);
        int effectId = stageJbResolveSpellPracticeBossDefeatEffectId();
        if (effectId >= 0) {
            stageKcEffect(effectId, ex, ey);
        }
        stageJbMt100SpellPracticeBossDefeatedCleanup(bossEnemyIdx);
    }

    // StageJb core: apply boss stock transition state changes (no HUD/score/drop/bossController).
    public void stageJbApplyBossStockTransition(final int bossEnemyIdx, final int ex, final int ey, final boolean consumeStock) {
        host.setResetFlag(4);
        stageKcEffect(0, ex, ey);

        if (consumeStock) {
            int stock = host.getBstock();
            if (stock > 0) {
                host.setBstock(stock - 1);
            }
            host.setMGCnt(-1);
            host.setBossF(2);
        }

        host.setBbaria(1);

        int maxHp = enemyList[bossEnemyIdx][ENEMY_WORK_0];
        if (maxHp <= 0) {
            maxHp = 1;
        }
        enemyList[bossEnemyIdx][ENEMY_HP] = maxHp;
        enemyList[bossEnemyIdx][ENEMY_TARGETABLE] = 0;
    }

    // StageJb core: mt==BossController.MIDBOSS_PATTERN, hp bar anim mode 1 transition state.
    public void stageJbMt100EnterHpBarAnimMode1Transition(final int bossEnemyIdx) {
        host.setMGCnt(-1);
        enemyList[bossEnemyIdx][ENEMY_TARGETABLE] = 0;
        enemyList[bossEnemyIdx][ENEMY_MOVE_STATE] = 8;
    }

    // StageJb core: mt==BossController.MIDBOSS_PATTERN, non-practice boss defeat state enter (enemy remains for script).
    public void stageJbMt100EnterNonPracticeDefeatedState(final int bossEnemyIdx) {
        enemyList[bossEnemyIdx][ENEMY_HP] = 1;
        enemyList[bossEnemyIdx][ENEMY_TARGETABLE] = 0;
        host.setMGCnt(-1);
        enemyList[bossEnemyIdx][ENEMY_MOVE_STATE] = 2;
    }

    // StageJb core: mt==BossController.MIDBOSS_PATTERN, spell practice boss defeated cleanup (no end flow).
    public void stageJbMt100SpellPracticeBossDefeatedCleanup(final int bossEnemyIdx) {
        host.setBossF(0);
        if (enemyList[bossEnemyIdx][ENEMY_ACTIVE] != 0) {
            enemyList[bossEnemyIdx][ENEMY_ACTIVE] = 0;
            host.freeEnemy(bossEnemyIdx);
        }
    }

    // StageJb core: mt==BossController.BOSS_PATTERN..103 boss on death common state clear.
    public void stageJbBossClearStateOnDeath(final int bossEnemyIdx) {
        enemyList[bossEnemyIdx][ENEMY_ATTACK_STATE] = 0;
    }

    // StageJb core: free enemy slot if still alive.
    public void stageJbFreeEnemyIfAlive(final int enemyIdx) {
        if (enemyList[enemyIdx][ENEMY_ACTIVE] != 0) {
            enemyList[enemyIdx][ENEMY_ACTIVE] = 0;
            host.freeEnemy(enemyIdx);
        }
    }

    // StageJb core: normal enemy death - free and return enemy type for upper-layer side effects.
    public int stageJbNormalEnemyDeathFreeAndGetEnemyType(final int enemyIdx) {
        int enemyType = enemyList[enemyIdx][ENEMY_SHOT_PATTERN];
        enemyList[enemyIdx][ENEMY_ACTIVE] = 0;
        host.freeEnemy(enemyIdx);
        return enemyType;
    }

    // StageJb core: normal enemy death - spawn stage effects (no score/sound/drop/bullets).
    public void stageJbNormalEnemyDeathSpawnEffects(final int mt, final int ex, final int ey) {
        stageKcEffect(0, ex, ey);

        if (mt >= BossController.MIDBOSS_PATTERN) {
            int stage = host.getStage();
            int stageEffect = -1;
            if (stage == STAGE_4) {
                stageEffect = 11;
            } else if (stage == STAGE_5) {
                stageEffect = 13;
            } else if (stage == STAGE_6) {
                stageEffect = 14;
            } else if (stage == STAGE_EXTRA) {
                stageEffect = 15;
            }
            if (stageEffect != -1) {
                stageKcEffect(stageEffect, ex, ey);
            } else {
                for (int i = 0; i < 3; i++) {
                    stageKcEffect(3, ex, ey);
                    stageKcEffect(4, ex, ey);
                    stageKcEffect(5, ex, ey);
                }
            }
        }
    }

    public int[][] getEffectList() {
        return effectList;
    }

    public short[][][] getStageTable() {
        return stageTable;
    }

    public int getTalkCnt() {
        return talkCnt;
    }

    public void setTalkCnt(int v) {
        talkCnt = v;
    }

    public void decTalkCntIfPositive() {
        if (talkCnt > 0) {
            talkCnt--;
        }
    }

    public void ensureTalkTableLoaded(int stage, int chara) {
        if (talkTable != null && talkLoadedStage == stage && talkLoadedChara == chara) {
            return;
        }

        talkLoadedStage = stage;
        talkLoadedChara = chara;

        String prefix;
        switch (chara) {
            case Player.REIMU:
                prefix = "r";
                break;
            case Player.MARISA:
                prefix = "m";
                break;
            default:
                prefix = "a";
                break;
        }

        String suffix;
        switch (stage) {
            case STAGE_1:
                suffix = "01";
                break;
            case STAGE_2:
                suffix = "02";
                break;
            case STAGE_3:
                suffix = "03";
                break;
            case STAGE_4:
                suffix = "04";
                break;
            case STAGE_5:
                suffix = "05";
                break;
            case STAGE_6:
                suffix = "06";
                break;
            default:
                suffix = "ex";
                break;
        }

        String text = I18n.loadText("dialog/" + prefix + suffix + ".txt");

        String[] lines = I18n.splitLines(text);
        for (int i = 0; i < lines.length; i++) {
            String line = replaceBrTag(lines[i]);
            lines[i] = wrapDialogueLine(line);
        }
        talkTable = lines;

        // Dialogue state is tied to talkTable indices.
        dialogueTalkIndex = -1;
        dialoguePage = 0;
        dialoguePrevPage = 0;
        dialogueScrollY = 0;
        dialogueEncounterIndex = -1;
    }

    public String[] getTalkTable() {
        return talkTable;
    }

    public int getDialoguePage() {
        return dialoguePage;
    }

    public int getDialoguePrevPage() {
        return dialoguePrevPage;
    }

    public int getDialogueScrollY() {
        return dialogueScrollY;
    }

    public boolean isDialogueActiveForInput(int stageNa, int step, int gcntPlusOne) {
        if (stageTable == null) {
            return false;
        }

        if (stageNa < 0 || stageNa >= stageTable.length) {
            return false;
        }
        if (step < 0 || step >= stageTable[stageNa].length) {
            return false;
        }
        if (stageTable[stageNa][step].length < 5) {
            return false;
        }
        if (stageTable[stageNa][step][StageDat.OP] != StageDat.DIALOGUE) {
            return false;
        }
        if (stageTable[stageNa][step][StageDat.FRAME] > gcntPlusOne) {
            return false;
        }
        return true;
    }

    public void stageIb() {
        enemyAnimation.reset();
        for (int i = 0; i != ENEMY_CAPACITY; ++i) {
            for (int j = 0; j != ENEMY_FIELD_COUNT; ++j) {
                enemyList[i][j] = 0;
            }
        }
        for (int k = 0; k < BOSS_WORK_SIZE; ++k) {
            bossWrk[k] = 0;
        }
        for (int i = 0; i < EFFECT_CAPACITY; i++) {
            for (int j = 0; j < EFFECT_FIELD_COUNT; j++) {
                effectList[i][j] = 0;
            }
        }

        host.clearDropItems();
        host.clearEnemies();
        host.resetBossController();
    }

    public int stageFb(final int n, final int n2, final int n3, final int n4, final int n5, final int n6, final int n7, final int n8, final int n9) {
        for (int i = 0; i != ENEMY_CAPACITY; ++i) {
            if (enemyList[i][ENEMY_ACTIVE] == 0) {
                enemyList[i][ENEMY_ACTIVE] = 1;
                enemyList[i][ENEMY_AGE] = 0;
                enemyList[i][ENEMY_WORK_0] = 0;
                enemyList[i][ENEMY_X] = n8;
                enemyList[i][ENEMY_Y] = n9;
                enemyList[i][ENEMY_HP] = n3;
                enemyList[i][ENEMY_MOVE_PATTERN] = n5;
                enemyList[i][ENEMY_SHOT_PATTERN] = n2;
                enemyList[i][ENEMY_APPEARANCE] = n;
                enemyList[i][ENEMY_DROP_TYPE] = n4;
                enemyList[i][ENEMY_MOVE_STATE] = 0;
                enemyList[i][ENEMY_ATTACK_STATE] = 0;
                enemyList[i][ENEMY_TARGETABLE] = 1;
                enemyList[i][ENEMY_WORK_1] = 0;
                enemyList[i][ENEMY_ANGLE] = n6 + 270;
                enemyList[i][ENEMY_SPEED] = n7;

                int sp = enemyList[i][ENEMY_SPEED] << 3;
                int dir = enemyList[i][ENEMY_ANGLE];
                enemyList[i][ENEMY_VX] = BattleMath.speedCos(sp, dir);
                enemyList[i][ENEMY_VY] = BattleMath.speedSin(sp, dir);

                switch (n) {
                    case 0:
                    case 1:
                    case 2:
                    case 3:
                    case 4:
                    case 5:
                    case 6:
                    case 7:
                        enemyList[i][ENEMY_HIT_LEFT] = 458752;
                        enemyList[i][ENEMY_HIT_RIGHT] = 458752;
                        enemyList[i][ENEMY_HIT_TOP] = 458752;
                        enemyList[i][ENEMY_HIT_BOTTOM] = 196608;
                        break;
                    case 8:
                        enemyList[i][ENEMY_HIT_LEFT] = 524288;
                        enemyList[i][ENEMY_HIT_RIGHT] = 524288;
                        enemyList[i][ENEMY_HIT_TOP] = 524288;
                        enemyList[i][ENEMY_HIT_BOTTOM] = 262144;
                        break;
                    case 20:
                    case 21:
                    case 22:
                        enemyList[i][ENEMY_TARGETABLE] = 0;
                        break;
                    case 9:
                    case 10:
                    case 11:
                    case 12:
                    case 13:
                    case 14:
                    case 15:
                    case 16:
                        enemyList[i][ENEMY_HIT_LEFT] = 1048576;
                        enemyList[i][ENEMY_HIT_RIGHT] = 1048576;
                        enemyList[i][ENEMY_HIT_TOP] = 1048576;
                        enemyList[i][ENEMY_HIT_BOTTOM] = 524288;
                        break;
                    case 17:
                    case 19:
                        enemyList[i][ENEMY_HIT_LEFT] = 1048576;
                        enemyList[i][ENEMY_HIT_RIGHT] = 1048576;
                        enemyList[i][ENEMY_HIT_TOP] = 1048576;
                        enemyList[i][ENEMY_HIT_BOTTOM] = 524288;
                        bossWrk[26] = 10;
                        break;
                    case 18:
                        enemyList[i][ENEMY_TARGETABLE] = 0;
                        break;
                }

                if (enemyList[i][ENEMY_MOVE_PATTERN] == BossController.MIDBOSS_PATTERN || enemyList[i][ENEMY_MOVE_PATTERN] == BossController.BOSS_PATTERN || enemyList[i][ENEMY_MOVE_PATTERN] == BossController.SECOND_BOSS_PATTERN || enemyList[i][ENEMY_MOVE_PATTERN] == BossController.THIRD_BOSS_PATTERN) {
                    enemyList[i][ENEMY_TARGETABLE] = 0;
                    enemyList[i][ENEMY_VX] = 524288;
                    enemyList[i][ENEMY_VY] = 524288;
                    enemyList[i][ENEMY_WORK_0] = enemyList[i][ENEMY_HP];

                    int stage = host.getStage();
                    int bstock;
                    switch (stage) {
                        case STAGE_1:
                            bstock = 1;
                            break;
                        case STAGE_2:
                        case STAGE_3:
                            bstock = 2;
                            break;
                        case STAGE_4:
                        case STAGE_5:
                            bstock = 3;
                            break;
                        case STAGE_6:
                            bstock = 4;
                            break;
                        case STAGE_EXTRA:
                            bstock = 8;
                            break;
                        default:
                            bstock = 0;
                            break;
                    }
                    if (enemyList[i][ENEMY_MOVE_PATTERN] == BossController.MIDBOSS_PATTERN) {
                        ++bstock;
                    }
                    if (host.getGameMode() == BattleSessionConfig.MODE_SPELL_PRACTICE) {
                        bstock = 0;
                    }
                    host.setBstock(bstock);
                }

                host.setEnemyAabb(i, enemyList[i][ENEMY_X], enemyList[i][ENEMY_Y], enemyList[i][ENEMY_HIT_LEFT], enemyList[i][ENEMY_HIT_RIGHT], enemyList[i][ENEMY_HIT_TOP], enemyList[i][ENEMY_HIT_BOTTOM], enemyList[i][ENEMY_TARGETABLE] != 0);
                return i;
            }
        }
        return -1;
    }

    // Per-frame enemy/boss update and collision processing.
    public void stageGb() {
        for (int i = 0; i != ENEMY_CAPACITY; ++i) {
            if (enemyList[i][ENEMY_ACTIVE] != 0) {
                int mt = enemyList[i][ENEMY_MOVE_PATTERN];
                if (mt >= BossController.MIDBOSS_PATTERN && mt <= BossController.THIRD_BOSS_PATTERN) {
                    // Boss: update state first so bbaria/phase changes apply immediately before bullet collision.
                    host.stageUb(i);
                    if (enemyList[i][ENEMY_ACTIVE] == 0) {
                        host.freeEnemy(i);
                        continue;
                    }

                    host.stageHd(i);
                    if (enemyList[i][ENEMY_ACTIVE] == 0) {
                        host.freeEnemy(i);
                        continue;
                    }
                } else {
                    host.stageHd(i);
                    if (enemyList[i][ENEMY_ACTIVE] == 0) {
                        host.freeEnemy(i);
                        continue;
                    }

                    host.stageDb(i);
                    host.stageEb(i);

                    if (enemyList[i][ENEMY_AGE] > 10 && enemyList[i][ENEMY_MOVE_PATTERN] != 50
                            && (enemyList[i][ENEMY_X] < -1966080 || enemyList[i][ENEMY_Y] < -1441792 || enemyList[i][ENEMY_X] > 14680064 || enemyList[i][ENEMY_Y] > 17301504)) {
                        enemyList[i][ENEMY_ACTIVE] = 0;
                    }
                }

                if (enemyList[i][ENEMY_ACTIVE] != 0) {
                    enemyList[i][ENEMY_AGE] += 1;
                    host.setEnemyAabb(i, enemyList[i][ENEMY_X], enemyList[i][ENEMY_Y], enemyList[i][ENEMY_HIT_LEFT], enemyList[i][ENEMY_HIT_RIGHT], enemyList[i][ENEMY_HIT_TOP], enemyList[i][ENEMY_HIT_BOTTOM], enemyList[i][ENEMY_TARGETABLE] != 0);
                } else {
                    host.freeEnemy(i);
                }
            } else {
                host.freeEnemy(i);
            }
        }
    }

    // Advance stage script and spawn events.
    public void stageMa() {
        ensureStageDatLoaded();
        if (stageTable == null) {
            return;
        }

        int na = host.stageNa();
        if (na < 0 || na >= stageTable.length) {
            return;
        }

        int step = host.getStep();
        int gcnt = host.getMGCnt();
        int tacnt = host.getTaCnt();

        if (dialogueScrollY > 0) {
            dialogueScrollY -= 4;
            if (dialogueScrollY < 0) {
                dialogueScrollY = 0;
            }
        }

        if (step < 0 || step >= stageTable[na].length) {
            return;
        } while (step >= 0 && step < stageTable[na].length && stageTable[na][step][StageDat.FRAME] <= gcnt) {
            int op = stageTable[na][step][StageDat.OP];
            if (op == StageDat.RESTART) {
                step = 0;
                gcnt = 0;
            } else if (op == StageDat.DIALOGUE) {
                int talkIndex = -1;
                if (stageTable[na][step].length > 3) {
                    talkIndex = stageTable[na][step][3];
                }
                boolean newTalk = syncDialogueStateForTalkIndex(talkIndex);
                if (newTalk) {
                    dialogueEncounterIndex++;
                }

                int pageCount = dialoguePageCountForTalkIndex(talkIndex);
                int effectivePageCount = pageCount;
                if (host.isReplayPlaybackActive()) {
                    int recorded = host.getRecordedDialoguePageCount(dialogueEncounterIndex);
                    if (recorded > 0) {
                        effectivePageCount = recorded;
                    }
                }

                if (tacnt < 80) {
                    --step;
                    --gcnt;
                    ++tacnt;
                    decTalkCntIfPositive();
                } else {
                    if (effectivePageCount > 1 && dialoguePage + 1 < effectivePageCount) {
                        dialoguePrevPage = dialoguePage;
                        dialoguePage++;

                        if (dialoguePage + 1 <= pageCount) {
                            dialogueScrollY = 32;
                        } else {
                            dialogueScrollY = 0;
                            dialoguePrevPage = dialoguePage;
                        }

                        tacnt = 0;

                        --step;
                        --gcnt;
                    } else {
                        tacnt = 0;
                        if (!host.isReplayPlaybackActive() && host.isReplayRecordingActive()) {
                            host.recordDialoguePageCount(dialogueEncounterIndex, pageCount);
                        }
                        if (stageTable[na][step].length > 4 && stageTable[na][step][4] == 0) {
                            setTalkCnt(3);
                        }
                    }
                }
            } else {
                if (op == StageDat.BOSS_START) {
                    stageIb();
                    host.setBossF(1);
                    host.setBbaria(1);
                    host.setStep(0);
                    host.setMGCnt(0);
                    host.setBossMode(true);

                    int[] bx = host.getBossX();
                    int[] by = host.getBossY();
                    if (bx != null && bx.length > 0) {
                        bx[0] = 99;
                    }
                    if (by != null && by.length > 0) {
                        by[0] = 44;
                    }

                    host.setTaCnt(tacnt);
                    host.markBossStart();
                    return;
                }
                if (op == StageDat.BOSS_ATTACK) {
                    gcnt = -1;
                    host.setBossF(2);
                    host.setHFlash(1);
                } else if (op == StageDat.STAGE_CLEAR || op == StageDat.EXTRA_CLEAR || op == StageDat.STORY_CLEAR) {
                    if (op == StageDat.STORY_CLEAR) {
                        if (host.getGameMode() != BattleSessionConfig.MODE_STAGE_PRACTICE && !host.isReplayPlaybackActive()) {
                            UnlockFlags.tryUnlockSpellPracticeOnAnyEnding();
                        }
                    }
                    host.setStep(step);
                    host.setMGCnt(gcnt);
                    host.setTaCnt(tacnt);
                    if (host.shouldEnterEndingAfterStageClear()) {
                        host.beginEndingTransition();
                    } else {
                        host.enterStageClearResultPanel();
                    }
                    return;
                } else if (op == StageDat.FLASH) {
                    host.setIFlash(10);
                } else if (op == StageDat.EFFECT) {
                    int id = stageTable[na][step][2];
                    int xFixed = stageTable[na][step][3] << 16;
                    int yFixed = stageTable[na][step][4] << 16;
                    stageKc(id, xFixed, yFixed);
                } else if (op == StageDat.BGM) {
                    host.requestBgm(stageTable[na][step][2]);
                } else if (op == StageDat.SAVE_PROGRESS) {
                    stagePd(stageTable[na][step][2]);
                } else if (op == StageDat.UNLOCK) {
                    if (host.getGameMode() != BattleSessionConfig.MODE_SPELL_PRACTICE && !host.isReplayPlaybackActive()) {
                        UnlockFlags.tryUnlockFlag1FromStageOp(host.getGameMode());
                    }
                } else if (op == StageDat.LATTER_HALF) {
                    step = -1;
                    gcnt = 0;
                    host.setLatterFlag(true);
                } else {
                    if (stageTable[na][step].length >= 10) {
                        stageFb(stageTable[na][step][StageDat.OP], stageTable[na][step][2], stageTable[na][step][3], stageTable[na][step][4], stageTable[na][step][5],
                                stageTable[na][step][6], stageTable[na][step][7], stageTable[na][step][8] << 16, stageTable[na][step][9] << 16);
                    }
                }
            }
            ++step;
        }

        host.setStep(step);
        host.setMGCnt(gcnt);
        host.setTaCnt(tacnt);
    }

    // Force transition to boss stage script.
    public void stageCe() {
        ensureStageDatLoaded();

        if (stageTable == null) {
            return;
        }
        if (host.getBossF() != 0) {
            return;
        }

        host.setMGCnt(0);
        host.setLatterFlag(true);
        host.setStep(0);

        int n = host.getStage() * 5;
        ++n;
        int step = 0;
        while (n >= 0 && n < stageTable.length && step >= 0 && step < stageTable[n].length && stageTable[n][step][StageDat.OP] != StageDat.BOSS_START) {
            ++step;
        }
        host.setStep(step);

        stageIb();
        host.setBossF(1);
        host.setBbaria(1);
        host.setStep(0);
        host.setMGCnt(0);
        host.setBossMode(true);

        int[] bx = host.getBossX();
        int[] by = host.getBossY();
        if (bx != null && bx.length > 0) {
            bx[0] = 99;
        }
        if (by != null && by.length > 0) {
            by[0] = 44;
        }

        host.markBossStart();
    }

    // Persist stage progress for score/clear tracking.
    public void stagePd(final int n) {
        if (host.isReplayPlaybackActive()) {
            return;
        }

        if (host.getGameMode() == BattleSessionConfig.MODE_SPELL_PRACTICE) {
            return;
        }
        int level = host.getStartLevel();
        if (level < 0 || level > BattleSessionConfig.DIFFICULTY_LUNATIC) {
            return;
        }

        GameProgress.INSTANCE.recordStageReached(level, n);
    }

    public void stageMc(final Graphics g) {
        int ox = host.getOriginX();
        int oy = host.getOriginY();
        int playX = host.getPlayX();
        int playY = host.getPlayY();
        int playW = host.getPlayW();
        int playH = host.getPlayH();

        for (int i = 0; i < EFFECT_CAPACITY; i++) {
            if (effectList[i][EFFECT_ACTIVE] == 0) {
                continue;
            }

            int id = effectList[i][EFFECT_ID];
            int x = ox + (effectList[i][EFFECT_X] >> 16);
            int y = oy + (effectList[i][EFFECT_Y] >> 16);

            if (id == 21) {
                if (host.isHitSparkEnabled()) {
                    int n = effectList[i][EFFECT_DELAY] - effectList[i][EFFECT_TIMER];
                    int c = (255 - (n << 4)) & 0xFF;
                    int size = 1 + (n >> 2);
                    g.setColor(0xFF0000 | (c << 8) | c);
                    int rx = (effectList[i][EFFECT_X] >> 16);
                    int ry = (effectList[i][EFFECT_Y] >> 16);
                    fillPlayRect(g, rx, ry, size, size, playX, playY, playW, playH);
                }
                continue;
            }

            int[] row = null;
            int[][] table = host.getEffectTable();
            if (table != null && id >= 0 && id < table.length) {
                row = table[id];
            }
            if (row == null || row.length == 0) {
                continue;
            }

            int frame = effectList[i][EFFECT_FRAME];
            int spriteIdx = (frame << 1) + 1;
            if (spriteIdx < 0 || spriteIdx >= row.length) {
                continue;
            }
            int spriteId = row[spriteIdx];
            if (spriteId != -1) {
                if (id >= 24 && id <= 36 && (id & 1) == 0 && I18n.getLanguageId() == I18n.LANG_ZHT) {
                    int openingStage = (id - 24) / 2;
                    int openingFrame = spriteId - (SpriteSheetCatalog.STAGE_OPENING_TEXT_BASE + openingStage * 6);
                    spriteId = SpriteSheetCatalog.CHINESE_OPENING_BASE + openingStage * 3 + openingFrame;
                }
                host.drawSprite(g, spriteId, x, y);
            }

            //stage start connection lines
            if (id == 23 || id == 25 || id == 27 || id == 29 || id == 31 || id == 33 || id == 35) {
                int intensity = 255 - (effectList[i][EFFECT_FRAME] * 64);
                if (intensity < 0) {
                    intensity = 0;
                } else if (intensity > 255) {
                    intensity = 255;
                }

                int rgb;
                if (id == 23 || id == 29) {
                    rgb = intensity;
                } else if (id == 25 || id == 31) {
                    rgb = intensity << 8;
                } else {
                    rgb = intensity << 16;
                }

                int lineY = 3 + ((effectList[i][EFFECT_Y] >> 16) + 10);
                int lineX = playX + 3 + ((effectList[i][EFFECT_X] >> 16) + 25);

                g.setColor(rgb);
                g.drawLine(playX, lineY, playX + playW - 1, lineY);
                g.drawLine(lineX, playY, lineX, playY + playH - 1);
            }

            // DoJa : effectId 2 and 21 both render a small red spark rectangle.
            if (id == 2 && host.isHitSparkEnabled()) {
                int n = effectList[i][EFFECT_DELAY] - effectList[i][EFFECT_TIMER];
                int c = (255 - (n << 4)) & 0xFF;
                int size = 1 + (n >> 2);
                g.setColor(0xFF0000 | (c << 8) | c);
                int rx = (effectList[i][EFFECT_X] >> 16);
                int ry = (effectList[i][EFFECT_Y] >> 16);
                fillPlayRect(g, rx, ry, size, size, playX, playY, playW, playH);
            }
        }

        // Boss connection lines (stage 6, celestial sphere).
        if (host.getBossF() == 21) {
            int d2 = (host.getMGCnt() << 4) & 0xFF;
            if (d2 > 127) {
                d2 = 255 - d2;
            }
            int c = 127 + d2;
            int rgb = (c << 16);
            g.setColor(rgb);

            int cnt = bossWrk[6];
            if (cnt > 1) {
                for (int j = 0; j < cnt; ++j) {
                    int a = bossWrk[8 + j];
                    int b = bossWrk[8 + ((j + 1) % cnt)];
                    if (a < 0 || b < 0 || a >= enemyList.length || b >= enemyList.length) {
                        continue;
                    }
                    if (enemyList[a][ENEMY_ACTIVE] == 0 || enemyList[b][ENEMY_ACTIVE] == 0) {
                        continue;
                    }
                    int x1 = ox + (enemyList[a][ENEMY_X] >> 16);
                    int y1 = oy + (enemyList[a][ENEMY_Y] >> 16);
                    int x2 = ox + (enemyList[b][ENEMY_X] >> 16);
                    int y2 = oy + (enemyList[b][ENEMY_Y] >> 16);
                    g.drawLine(x1, y1, x2, y2);
                }
            }
            if (cnt > 3) {
                for (int j = 0; j < cnt; ++j) {
                    int a = bossWrk[8 + j];
                    int b = bossWrk[8 + ((j + 2) % cnt)];
                    if (a < 0 || b < 0 || a >= enemyList.length || b >= enemyList.length) {
                        continue;
                    }
                    if (enemyList[a][ENEMY_ACTIVE] == 0 || enemyList[b][ENEMY_ACTIVE] == 0) {
                        continue;
                    }
                    int x1 = ox + (enemyList[a][ENEMY_X] >> 16);
                    int y1 = oy + (enemyList[a][ENEMY_Y] >> 16);
                    int x2 = ox + (enemyList[b][ENEMY_X] >> 16);
                    int y2 = oy + (enemyList[b][ENEMY_Y] >> 16);
                    g.drawLine(x1, y1, x2, y2);
                }
            }
        }
    }

    public void stageKc(int effectId, int xFixed, int yFixed) {
        int[] row = null;
        int[][] table = host.getEffectTable();
        if (table != null && effectId >= 0 && effectId < table.length) {
            row = table[effectId];
        }
        if (row != null && row.length > 0) {
            stageKcEffect(effectId, xFixed, yFixed);
            return;
        }
        host.spawnBulletEffect(effectId, xFixed, yFixed);
    }

    public void stageKcEffect(int effectId, int xFixed, int yFixed) {
        int slot = -1;
        for (int i = 0; i < EFFECT_CAPACITY; i++) {
            if (effectList[i][EFFECT_ACTIVE] == 0) {
                slot = i;
                break;
            }
        }
        if (slot == -1) {
            int best = 0;
            int bestScore = -1;
            for (int i = 0; i < EFFECT_CAPACITY; i++) {
                int score = (effectList[i][EFFECT_FRAME] << 8) + (effectList[i][EFFECT_TIMER] & 0xFF);
                if (score > bestScore) {
                    bestScore = score;
                    best = i;
                }
            }
            slot = best;
        }

        effectList[slot][EFFECT_ACTIVE] = 1;
        effectList[slot][EFFECT_TIMER] = 0;
        effectList[slot][EFFECT_FRAME] = 0;
        effectList[slot][EFFECT_ID] = effectId;
        effectList[slot][EFFECT_X] = xFixed;
        effectList[slot][EFFECT_Y] = yFixed;
        effectList[slot][EFFECT_VX] = 0;
        effectList[slot][EFFECT_VY] = 0;
        effectList[slot][EFFECT_DELAY] = 0;

        if (effectId == 21) {
            effectList[slot][EFFECT_VX] = (abs(host.randomNextInt()) % 15 - 7) << 16;
            effectList[slot][EFFECT_VY] = (abs(host.randomNextInt()) % 12 + 4) << 16;
            effectList[slot][EFFECT_DELAY] = (abs(host.randomNextInt()) % 16);
            return;
        }
        if (effectId >= 23 && effectId <= 36) {
            if ((effectId & 1) != 0) {
                effectList[slot][EFFECT_VX] = 1 << 16;
                effectList[slot][EFFECT_VY] = 0;
            } else {
                effectList[slot][EFFECT_VX] = 0;
                effectList[slot][EFFECT_VY] = 2 << 16;
            }
            return;
        }
        if (effectId == 3 || effectId == 4 || effectId == 5) {
            effectList[slot][EFFECT_VX] = (abs(host.randomNextInt()) % 17 - 8) << 16;
            effectList[slot][EFFECT_VY] = (abs(host.randomNextInt()) % 17 - 8) << 16;
            effectList[slot][EFFECT_DELAY] = (abs(host.randomNextInt()) % 4);
        }
    }

    private static int abs(int v) {
        if (v < 0) {
            return -v;
        }
        return v;
    }

    private boolean syncDialogueStateForTalkIndex(int talkIndex) {
        if (talkIndex == dialogueTalkIndex) {
            return false;
        }
        dialogueTalkIndex = talkIndex;
        dialoguePage = 0;
        dialoguePrevPage = 0;
        dialogueScrollY = 0;

        return true;
    }

    private int dialoguePageCountForTalkIndex(int talkIndex) {
        if (talkTable == null || talkIndex < 0 || talkIndex >= talkTable.length) {
            return 1;
        }
        String s = talkTable[talkIndex];
        if (s == null || s.length() == 0) {
            return 1;
        }
        int lines = 1;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '|') {
                lines++;
            }
        }
        return (lines + 1) / 2;
    }

    // Replace HTML line breaks with wrap markers.
    private static String replaceBrTag(String s) {
        if (s == null) {
            return null;
        }
        int idx = s.indexOf("<br>");
        if (idx < 0) {
            idx = s.indexOf("<BR>");
        }
        if (idx < 0) {
            return s;
        }
        StringBuffer out = new StringBuffer();
        int p = 0;
        while (true) {
            int i = s.indexOf("<br>", p);
            int len = 4;
            if (i < 0) {
                i = s.indexOf("<BR>", p);
            }
            if (i < 0) {
                out.append(s.substring(p));
                break;
            }
            out.append(s.substring(p, i));
            out.append('|');
            p = i + len;
            if (p >= s.length()) {
                break;
            }
        }
        return out.toString();
    }

    private static String wrapDialogueLine(String text) {
        // Dialogue text has 168 pixels within the 174-pixel panel.
        String[] lines = UiTextLayout.wrap(text.replace('|', '\n'), 168, DIALOGUE_MEASURER);
        StringBuffer out = new StringBuffer();
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                out.append('|');
            }
            out.append(lines[i]);
        }
        return out.toString();
    }
}
