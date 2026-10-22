package touhou.stage;

import touhou.SoundEffectSystem;
import touhou.battle.BattleSessionConfig;

public final class BossController {
    public static final int MIDBOSS_PATTERN = 100;
    public static final int BOSS_PATTERN = 101;
    public static final int SECOND_BOSS_PATTERN = 102;
    public static final int THIRD_BOSS_PATTERN = 103;
    public static final int LAST_WORD_STEP = 37;
    public interface Host extends BossState, BossWorld, BossServices {
    }

    public interface BossState {
        boolean isBossMode();

        int getStage();

        int getGameMode();

        int getChara();

        int getLevel();

        int getPower();

        int getFi();

        int getMGCnt();

        void setMGCnt(int v);

        int getBossF();

        void setBossF(int v);

        void setBbaria(int v);

        int getBstock();

        int getPlayerXFixed();

        int getPlayerYFixed();

        int getSpellPracticeBossStep();

        int getTimeStop();

        void setTimeStop(int v);
    }

    public interface BossWorld {
        void stageJb(int enemyIdx);

        int stageFb(int n, int n2, int n3, int n4, int n5, int n6, int n7, int n8, int n9);

        int[] getBossX();

        int[] getBossY();

        int[] getBossWrk();

        int[][] getEnemyList();
    }

    public interface BossServices {
        void setResetFlag(int v);

        void setQ(boolean v);

        void playSe(int id);

        void aimEnemyBulletsToPlayer();

        void setEnemyBulletGlobalDir(int deg);

        void setEnemyBulletLock(int idx, int value);

        void spawnStageEffect(int effectId, int xFixed, int yFixed);

        int spawnEnemyBullet(int bulletId, int moveType, int angleDeg, int speedParam, int xFixed, int yFixed);

        void convertEnemyBulletsTo81Rain(int bossYFixed, int gc);

        void setEnemyBulletsDeltaForIdAndMoveType(int bulletId, int moveType, int newDelta);

        void setEnemyBulletsDeltaForIdIfDeltaZero(int bulletId, int baseDelta, int deltaStep);
    }

    public Host host() {
        return host;
    }

    private final Host host;

    private final BossStageLogic[] stageLogic = new BossStageLogic[StageRuntime.STAGE_COUNT];

    final int[] bwave = new int[3];
    int bspellcnt = 255;
    int bspellstep;

    int k;
    int l;
    int m;
    int n;
    final int[] o = new int[3];
    final int[] p = new int[3];
    int j;
    int zbLastFi = -1;
    int spellId = -1;

    public BossController(Host host) {
        this.host = host;

        stageLogic[StageRuntime.STAGE_1] = new Stage1BossLogic();
        stageLogic[StageRuntime.STAGE_2] = new Stage2BossLogic();
        stageLogic[StageRuntime.STAGE_3] = new Stage3BossLogic();
        stageLogic[StageRuntime.STAGE_4] = new Stage4BossLogic();
        stageLogic[StageRuntime.STAGE_5] = new Stage5BossLogic();
        stageLogic[StageRuntime.STAGE_6] = new Stage6BossLogic();
        stageLogic[StageRuntime.STAGE_EXTRA] = new StageExtraBossLogic();
    }

    private BossStageLogic getStageLogic(int stage) {
        if (stage < 0 || stage >= stageLogic.length) {
            return null;
        }
        return stageLogic[stage];
    }

    public void reset() {
        bwave[0] = 0;
        bwave[1] = 0;
        bwave[2] = 0;
        bspellcnt = 255;
        bspellstep = 0;

        k = 0;
        l = 0;
        m = 0;
        n = 0;
        o[0] = 0;
        o[1] = 0;
        o[2] = 0;
        p[0] = 0;
        p[1] = 0;
        p[2] = 0;
        j = 0;
        zbLastFi = -1;
        spellId = -1;
    }

    public int getBspellcnt() {
        return bspellcnt;
    }

    public int getBspellstep() {
        return bspellstep;
    }

    public int getBossNameIndex() {
        return n;
    }

    public int getHpBarAnimK() {
        return k;
    }

    public int getHpBarAnimMode() {
        return m;
    }

    public int getHpBarTargetIndex() {
        return l;
    }

    public int getHpBarAnimJ() {
        return j;
    }

    public int getHpBarAnimO0() {
        return o[0];
    }

    public int getHpBarAnimO2() {
        return o[2];
    }

    public int getHpBarAnimP0() {
        return p[0];
    }

    public int getHpBarAnimP2() {
        return p[2];
    }

    public int getSpellId() {
        return spellId;
    }

    public void resetSpellCountersAfterJb() {
        bspellstep = 0;
        bspellcnt = 255;
        spellId = -1;
    }

    public void tickHpBarOncePerFrame() {
        zbTickOncePerFrame();
    }

    public int applyBossDamage(int dmg) {
        if (dmg <= 0) {
            dmg = 1;
        }
        j -= dmg;
        if (j < 0) {
            j = 0;
        }
        return j;
    }

    public void enterHpBarMode2ForStockTransition() {
        j = 65535;
        beginHpBarAnimation(l, n, 2);
    }

    public void tick(int enemyIdx) {
        int[][] enemylist = host.getEnemyList();
        int mt = enemylist[enemyIdx][StageRuntime.ENEMY_MOVE_PATTERN];
        int beforeBossf = host.getBossF();

        if (mt == MIDBOSS_PATTERN) {
            if (host.isBossMode()) {
                int bossf = host.getBossF();
                if (bossf == 2) {
                    int gc = host.getMGCnt();
                    if (gc == 0) {
                        host.setBbaria(1);
                    } else if (gc > 10) {
                        host.setBbaria(0);
                    }
                }
            }
            tickBossPattern(enemyIdx);
            if (bspellstep == 1) {
                // Spell cut-in/chant phase: boss must be invincible.
                host.setBbaria(2);
            } else if (bspellstep == 2) {
                // Spell main phase: boss must be damageable.
                host.setBbaria(0);
            }
            int afterBossf = host.getBossF();
            if (host.getGameMode() == BattleSessionConfig.MODE_SPELL_PRACTICE && beforeBossf == 2 && afterBossf != 2) {
                // Spell practice: entering a spell step via transitionBossPhase(step) must be invincible immediately (same frame).
                host.setBbaria(2);
            }
            return;
        }

        if (!host.isBossMode()) {
            // Ensure spell-only state does not leak into stage sections.
            bspellstep = 0;
            return;
        }

        int bossf = host.getBossF();
        if (bossf == 0) {
            return;
        }

        if (bossf == 2) {
            int gc = host.getMGCnt();
            if (gc == 0) {
                host.setBbaria(1);
            } else if (gc > 10) {
                host.setBbaria(0);
            }
        }

        tickBossPattern(enemyIdx);
        if (bspellstep == 1) {
            // Spell cut-in/chant phase: boss must be invincible.
            host.setBbaria(2);
        } else if (bspellstep == 2) {
            // Spell main phase: boss must be damageable.
            host.setBbaria(0);
        }
        int afterBossf = host.getBossF();
        if (host.getGameMode() == BattleSessionConfig.MODE_SPELL_PRACTICE && beforeBossf == 2 && afterBossf != 2) {
            // Spell practice: entering a spell step via transitionBossPhase(step) must be invincible immediately (same frame).
            host.setBbaria(2);
        }
    }

    private void zbTickOncePerFrame() {
        int fi = host.getFi();
        if (fi == zbLastFi) {
            return;
        }
        zbLastFi = fi;
        zbTick();
    }

    private void zbTick() {
        if (k != 1) {
            return;
        }

        switch (m) {
            case 0:
            case 3:
                if (o[0] < o[1]) {
                    o[0] += o[2] / 20;
                }
                if (o[0] > o[1]) {
                    o[0] = o[1];
                }
                o[1] = j;
                break;

            case 1:
                if (p[0] >= p[2]) {
                    if (o[0] < o[1]) {
                        o[0] += o[2] / 12;
                    }
                    if (o[0] > o[1]) {
                        o[0] = o[1];
                    }
                } else if (p[0] < p[2]) {
                    p[0] += p[2] / 8;
                }
                o[1] = j;
                break;

            case 2:
                o[0] = j;
                break;
        }

        if (o[0] < 0) {
            o[0] = 0;
        }
    }

    void beginHpBarAnimation(int targetIndex, int nameIndex, int mode) {
        if (host.getGameMode() != BattleSessionConfig.MODE_SPELL_PRACTICE || mode == 3) {
            k = 1;
            l = targetIndex;
            m = mode;
            n = nameIndex;
            for (int i = 0; i < 3; ++i) {
                o[i] = 0;
            }
            o[1] = j;
            o[2] = j;
            p[1] = 0;
            p[2] = mode == 1 ? 40 : 0;
        }
    }

    private void tickBossPattern(int enemyIdx) {
        int[][] enemylist = host.getEnemyList();
        int[] bossx = host.getBossX();
        int[] bossy = host.getBossY();

        int idx = 0;
        int mt = enemylist[enemyIdx][StageRuntime.ENEMY_MOVE_PATTERN];
        if (mt == SECOND_BOSS_PATTERN) {
            idx = 1;
        } else if (mt == THIRD_BOSS_PATTERN) {
            idx = 2;
        }

        if (bossx[idx] == 0 && bossy[idx] == 0) {
            bossx[idx] = enemylist[enemyIdx][StageRuntime.ENEMY_X] >> 16;
            bossy[idx] = enemylist[enemyIdx][StageRuntime.ENEMY_Y] >> 16;
        }

        if (enemylist[enemyIdx][StageRuntime.ENEMY_X] < ((bossx[idx] + 1) << 16)) {
            enemylist[enemyIdx][StageRuntime.ENEMY_X] += enemylist[enemyIdx][StageRuntime.ENEMY_VX];
        }
        if (enemylist[enemyIdx][StageRuntime.ENEMY_X] > ((bossx[idx] - 1) << 16)) {
            enemylist[enemyIdx][StageRuntime.ENEMY_X] -= enemylist[enemyIdx][StageRuntime.ENEMY_VX];
        }
        if (enemylist[enemyIdx][StageRuntime.ENEMY_Y] < ((bossy[idx] + 1) << 16)) {
            enemylist[enemyIdx][StageRuntime.ENEMY_Y] += enemylist[enemyIdx][StageRuntime.ENEMY_VY];
        }
        if (enemylist[enemyIdx][StageRuntime.ENEMY_Y] > ((bossy[idx] - 1) << 16)) {
            enemylist[enemyIdx][StageRuntime.ENEMY_Y] -= enemylist[enemyIdx][StageRuntime.ENEMY_VY];
        }

        bwave[idx] = (bwave[idx] + 15) % 360;

        if (bspellcnt >= 0 && bspellcnt != 255 && (host.getFi() & 0xF) == 0 && idx == 0 && host.getTimeStop() == 0) {
            --bspellcnt;
            if (bspellcnt < 10) {
                host.playSe(SoundEffectSystem.SE_CD);
            }
        }

        if (mt == MIDBOSS_PATTERN) {
            BossStageLogic logic = getStageLogic(host.getStage());
            if (logic != null) {
                logic.tickMidboss(this, enemyIdx);
            }
            return;
        }

        BossStageLogic logic = getStageLogic(host.getStage());
        if (logic != null) {
            logic.tickBoss(this, enemyIdx);
        }

        int bossf = host.getBossF();
        if (bossf > 0 && bossf < 10) {
            bspellstep = 0;
            spellId = -1;
        }
    }
}
