package touhou.stage;

import touhou.battle.BattleSessionConfig;


/** Stage patterns operate on the Controller's sole state, including during Host callbacks. */
abstract class AbstractBossStageLogic implements BossStageLogic {
    BossController core;
    BossController.Host host;

    public final void tickBoss(BossController core, int enemyIdx) {
        this.core = core;
        this.host = core.host();
        try {
            tickBossImpl(enemyIdx);
        } finally {
            this.core = null;
            this.host = null;
        }
    }

    public final void tickMidboss(BossController core, int enemyIdx) {
        this.core = core;
        this.host = core.host();
        try {
            tickMidbossImpl(enemyIdx);
        } finally {
            this.core = null;
            this.host = null;
        }
    }

    abstract void tickBossImpl(int enemyIdx);

    abstract void tickMidbossImpl(int enemyIdx);

    void vb(int enemyIdx, int state) {
        host.setMGCnt(-1);
        host.getEnemyList()[enemyIdx][7] = state;
    }

    void wb(int bossf) {
        host.setMGCnt(-1);
        host.setBossF(bossf);
    }

    void yb(int targetIndex, int nameIndex, int mode) {
        if (host.getGameMode() != BattleSessionConfig.MODE_SPELL_PRACTICE || mode == 3) {
            core.k = 1;
            core.l = targetIndex;
            core.m = mode;
            core.n = nameIndex;
            for (int i = 0; i < 3; ++i) {
                core.o[i] = 0;
            }
            core.o[1] = core.j;
            core.o[2] = core.j;
            core.p[1] = 0;
            core.p[2] = (mode == 1) ? 40 : 0;
        }
    }
}
