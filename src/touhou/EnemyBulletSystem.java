package touhou;

import touhou.battle.BattleMath;

public final class EnemyBulletSystem {
    private static final int GRAZE_MARGIN_FIXED = Fixed.fromInt(8);

    private final BulletSystem bullets;
    private final int max;

    private final boolean[] wasActive;
    private final boolean[] canGraze;

    private int grazeCount;
    private boolean playerHit;

    public EnemyBulletSystem(BulletSystem bullets) {
        if (bullets == null) {
            throw new IllegalArgumentException("Enemy bullets require a bullet store");
        }
        this.bullets = bullets;
        this.max = bullets.capacity();
        wasActive = new boolean[max];
        canGraze = new boolean[max];
    }

    public void clear() {
        for (int i = 0; i < max; i++) {
            wasActive[i] = false;
            canGraze[i] = false;
        }
        grazeCount = 0;
        playerHit = false;
    }

    public int spawn(int bulletId, int moveType, int angleDeg, int speedParam,
            int spawnXFixed, int spawnYFixed, int playerXFixed, int playerYFixed) {
        if (moveType == 1 || moveType == 28) {
            return bullets.spawnEnemyAccel(bulletId, moveType, angleDeg, speedParam, spawnXFixed, spawnYFixed);
        }
        if (moveType == 6 || moveType == 7 || moveType == 8 || moveType == 9) {
            return bullets.spawnEnemyStopTurnAccel(bulletId, moveType, angleDeg, speedParam, spawnXFixed, spawnYFixed);
        }
        if (moveType == 26) {
            return bullets.spawn(bulletId, moveType, angleDeg, speedParam, spawnXFixed, spawnYFixed);
        }
        if (moveType == 3) {
            int ang = BattleMath.arcTan2Deg(playerYFixed - spawnYFixed, playerXFixed - spawnXFixed) % 360;
            return bullets.spawn(bulletId, 0, ang, speedParam, spawnXFixed, spawnYFixed);
        }
        if (moveType == 2) {
            return bullets.spawnEnemyDecel(bulletId, angleDeg, speedParam >> 3, 1, 4, spawnXFixed, spawnYFixed);
        }
        // Stage EXTRA fodder: spiral curve bullets that transition to straight flight.
        if (moveType == 21 || moveType == 22) {
            return bullets.spawnEnemyCurve(bulletId, moveType, angleDeg, speedParam, spawnXFixed, spawnYFixed);
        }
        // Stage EXTRA fodder: accelerating burst and spiral split bullets.
        if (moveType == 40) {
            return bullets.spawnEnemyAccel40(bulletId, angleDeg, speedParam, spawnXFixed, spawnYFixed);
        }
        if (moveType == 43 || moveType == 44) {
            return bullets.spawnEnemySpiral43_44(bulletId, moveType, angleDeg, speedParam, spawnXFixed, spawnYFixed);
        }
        if (moveType == 41) {
            return bullets.spawnEnemyRamp41(bulletId, angleDeg, speedParam, spawnXFixed, spawnYFixed);
        }
        if (moveType == 0) {
            return bullets.spawn(bulletId, 0, angleDeg, speedParam, spawnXFixed, spawnYFixed);
        }
        return bullets.spawn(bulletId, moveType, angleDeg, speedParam, spawnXFixed, spawnYFixed);
    }

    public int getGrazeCount() {
        return grazeCount;
    }

    public boolean isPlayerHit() {
        return playerHit;
    }

    public void clearPlayerHit() {
        playerHit = false;
    }

    public void applyPlayerCollision(int playerXFixed, int playerYFixed, int playerRadiusFixed, boolean allowGraze, boolean allowHit) {
        if (bullets == null) {
            return;
        }

        grazeCount = 0;
        playerHit = false;

        int playerSize = (playerRadiusFixed << 1) + Fixed.fromInt(1);
        int playerLeft = playerXFixed - playerRadiusFixed;
        int playerTop = playerYFixed - playerRadiusFixed;

        // Direct array access avoids per-slot getter + bounds-check overhead.
        final boolean[] bActive = bullets.activeArray();
        final boolean[] bFromPlayer = bullets.fromPlayerArray();
        final int[] bx = bullets.xArray();
        final int[] by = bullets.yArray();
        final int[] bBulletId = bullets.bulletIdArray();

        for (int i = 0; i < max; i++) {
            if (!bActive[i] || bFromPlayer[i]) {
                wasActive[i] = false;
                canGraze[i] = false;
                continue;
            }

            if (!wasActive[i]) {
                wasActive[i] = true;
                canGraze[i] = true;
            }

            int ex = bx[i];
            int ey = by[i];

            int bId = bBulletId[i];
            if (bId < 0) {
                continue;
            }

            int br = resolveEnemyBulletRadiusFixed(bId);
            int bl = ex - br;
            int bt = ey - br;
            int bw = (br << 1);
            int bh = (br << 1);

            if (allowGraze && canGraze[i]) {
                if (BattleMath.rectsOverlapFixed(bl - GRAZE_MARGIN_FIXED, bt - GRAZE_MARGIN_FIXED, bw + (GRAZE_MARGIN_FIXED << 1), bh + (GRAZE_MARGIN_FIXED << 1),
                        playerLeft, playerTop, playerSize, playerSize)) {
                    canGraze[i] = false;
                    grazeCount++;
                }
            }

            if (allowHit) {
                if (BattleMath.rectsOverlapFixed(bl, bt, bw, bh, playerLeft, playerTop, playerSize, playerSize)) {
                    playerHit = true;
                    bullets.deactivate(i);
                }
            }
        }
    }

    private static int resolveEnemyBulletRadiusFixed(int bulletId) {
        if (bulletId >= 87 && bulletId <= 92) {
            return 393216;
        }
        if (bulletId >= 93 && bulletId <= 96) {
            return 655360;
        }
        return 131072;
    }

}
