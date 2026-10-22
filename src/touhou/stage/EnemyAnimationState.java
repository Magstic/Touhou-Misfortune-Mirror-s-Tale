package touhou.stage;

import touhou.battle.BattleEnemyVisuals;

/** Stage-owned animation progression; motion is also an input to Stage6 simulation. */
final class EnemyAnimationState {
    private static final int NO_SPRITE = BattleEnemyVisuals.NO_SPRITE;
    private final int[] spriteIds = new int[StageRuntime.ENEMY_CAPACITY];
    private final int[] auraSpriteIds = new int[StageRuntime.ENEMY_CAPACITY];

    EnemyAnimationState() {
        reset();
    }

    void reset() {
        for (int i = 0; i < spriteIds.length; i++) {
            spriteIds[i] = NO_SPRITE;
            auraSpriteIds[i] = NO_SPRITE;
        }
    }

    int spriteId(int index) {
        requireIndex(index);
        return spriteIds[index];
    }

    int auraSpriteId(int index) {
        requireIndex(index);
        return auraSpriteIds[index];
    }

    private static void requireIndex(int index) {
        if (index < 0 || index >= StageRuntime.ENEMY_CAPACITY) {
            throw new IllegalArgumentException("Invalid enemy animation index: " + index);
        }
    }

    void advance(int[][] enemies, int[] bossX, int[] bossWork) {
        // StageRuntime owns fixed-size, fully allocated tables; invalid tables are programming errors.
        for (int i = 0; i < StageRuntime.ENEMY_CAPACITY; i++) {
            spriteIds[i] = NO_SPRITE;
            auraSpriteIds[i] = NO_SPRITE;
            int[] enemy = enemies[i];
            if (enemy[StageRuntime.ENEMY_ACTIVE] == 0) {
                continue;
            }
            int type = enemy[StageRuntime.ENEMY_APPEARANCE];
            int age = enemy[StageRuntime.ENEMY_AGE];
            int attackState = enemy[StageRuntime.ENEMY_ATTACK_STATE];
            if (type == 14) {
                spriteIds[i] = advanceType14AndResolve(age, attackState, enemy[StageRuntime.ENEMY_X], bossX[0], bossWork);
            } else {
                advance(type, enemy[StageRuntime.ENEMY_X], attackState, bossX[0], bossWork);
                spriteIds[i] = BattleEnemyVisuals.resolveSprite(type, age, attackState, i, enemy[StageRuntime.ENEMY_X], bossX, bossWork[0], bossWork[24]);
            }
            auraSpriteIds[i] = BattleEnemyVisuals.resolveAuraSprite(type, age);
        }
    }

    private static void advance(int type, int xFixed, int attackState, int bossX, int[] work) {
        if (type < 13 || type > 19 || type == 14 || type == 18) {
            return;
        }
        int max = (type == 19) ? 10 : 15;
        if (type == 13) {
            max = Integer.MAX_VALUE;
        }

        if (xFixed < ((bossX - 10) << 16)) {
            if (work[0] < 0) {
                work[0] = 0;
            }
            work[0] += 2;
        } else if (xFixed > ((bossX + 10) << 16)) {
            if (work[0] > 0) {
                work[0] = 0;
            }
            work[0] -= 2;
        }

        if (work[0] > max) {
            work[0] = max;
        }
        if (work[0] < -max) {
            work[0] = -max;
        }
        if (work[0] < 0) {
            work[0]++;
        }
        if (work[0] > 1) {
            work[0]--;
        }

        if (type == 15) {
            if (attackState == 4) {
                work[0] = 1;
            } else if (attackState == 8) {
                work[24] = 1;
            }
        }
    }

    private static int advanceType14AndResolve(int age, int attackState, int xFixed, int bossX, int[] work) {
        if (xFixed < ((bossX - 10) << 16)) {
            work[24] = 0;
            if (work[0] < 0) {
                work[0] = 0;
            }
            work[0] += 2;
        } else if (xFixed > ((bossX + 10) << 16)) {
            work[24] = 0;
            if (work[0] > 0) {
                work[0] = 0;
            }
            work[0] -= 2;
        }
        if (work[0] > 15) {
            work[0] = 15;
        } else if (work[0] < -15) {
            work[0] = -15;
        }
        if (work[0] < 0) {
            work[0]++;
        }
        if (work[0] > 1) {
            work[0]--;
        }

        int sprite = 216;
        if (work[0] > 1) {
            sprite = 221 + ((age >> 1) & 7);
            if (work[0] > 10) {
                work[24] = 1;
            }
        } else if (work[0] < -1) {
            sprite = 229 + ((age >> 1) & 7);
            if (work[0] < -10) {
                work[24] = 1;
            }
        } else if (work[24] == 1) {
            work[24] = 2;
        } else if (work[24] > 1) {
            work[24]++;
            sprite = work[24] > 3 ? 238 : 237;
            if (work[24] > 10) {
                sprite = 237;
                work[24] = 0;
            }
        }

        switch (attackState) {
            case 1:
                return work[0] == 0 ? 219 : 217;
            case 2:
                return work[0] == 0 ? 220 : 218;
            case 3:
                return 237;
            case 4:
                return 238;
            case 5:
                return 221 + ((age >> 1) & 7);
            default:
                return sprite;
        }
    }

}
