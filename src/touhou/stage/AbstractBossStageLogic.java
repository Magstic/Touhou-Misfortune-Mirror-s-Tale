package touhou.stage;

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

    void transitionEnemyState(int enemyIdx, int state) {
        host.setMGCnt(-1);
        host.getEnemyList()[enemyIdx][StageRuntime.ENEMY_MOVE_STATE] = state;
    }

    void transitionBossPhase(int bossf) {
        host.setMGCnt(-1);
        host.setBossF(bossf);
    }

    void clearBossMinions() {
        int[][] enemylist = host.getEnemyList();
        int[] bossWrk = host.getBossWrk();
        for (int i = 0; i < 10; ++i) {
            int si = bossWrk[8 + i];
            if (si < 0 || si >= enemylist.length) {
                continue;
            }
            if (enemylist[si][StageRuntime.ENEMY_ACTIVE] != 0) {
                int mt = enemylist[si][StageRuntime.ENEMY_MOVE_PATTERN];
                if (mt != BossController.MIDBOSS_PATTERN && mt != BossController.BOSS_PATTERN && mt != BossController.SECOND_BOSS_PATTERN && mt != BossController.THIRD_BOSS_PATTERN) {
                    host.stageJb(si);
                    bossWrk[8 + i] = 0;
                }
            }
        }
    }
}
