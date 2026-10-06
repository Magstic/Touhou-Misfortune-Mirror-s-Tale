package touhou.battle;

/** Read-only sprite, aura and shield-alpha rules. No simulation state is changed here. */
public final class BattleEnemyVisuals {
    public static final int NO_SPRITE = -1;

    private static final int[] ENEMY20_ANIMATION = new int[] { 0, 1, 2, 3, 2, 1, 0, 4, 5, 6, 5, 4 };

    private BattleEnemyVisuals() {
    }

    public static int resolveSprite(int enemyType, int age, int attackState, int slot,
            int xFixed, int[] bossX, int motion, int transition) {
        switch (enemyType) {
            case 0:
            case 1:
            case 2:
            case 3:
                return enemyType * 4 + ((age / 3) & 3);
            case 4:
            case 5:
            case 6:
            case 7:
                return 16 + (enemyType - 4) * 2 + ((age / 3) & 1);
            case 8:
                return 24 + ((age / 3) % 3);
            case 9:
                return resolveSimpleBoss(xFixed, bossX, 0, 166, 167, 168, 169, attackState, 4);
            case 10:
                return resolveSimpleBoss(xFixed, bossX, 2, 179, 180, 181, 182, attackState, 3);
            case 11:
                return resolveSimpleBoss(xFixed, bossX, 1, 173, 174, 175, 176, attackState, 3);
            case 13:
                return resolveType13(age, attackState, motion);
            case 14:
                return resolveType14(age, attackState, motion, transition);
            case 15:
                return resolveType15(attackState, motion, transition);
            case 16:
                return resolveType16(attackState, motion);
            case 17:
                return resolveType17(attackState, motion);
            case 18:
                return 718;
            case 19:
                return resolveType19(attackState, motion);
            case 20:
            case 21:
                return 380 + ENEMY20_ANIMATION[(age / 3) % ENEMY20_ANIMATION.length];
            case 22:
                return 494 + (slot % 6);
            default:
                return NO_SPRITE;
        }
    }

    public static int resolveAuraSprite(int enemyType, int age) {
        if (enemyType == 20) {
            return 388 + ((age >> 1) & 3);
        }
        if (enemyType == 21) {
            return 388 + (3 - ((age >> 1) & 3));
        }
        return NO_SPRITE;
    }

    public static int bossShieldAlpha(int bombCounter) {
        int alpha = (bombCounter << 4) & 127;
        if (alpha > 63) {
            alpha = 127 - alpha;
        }
        return alpha + 63;
    }

    private static int resolveSimpleBoss(int xFixed, int[] bossX, int bossSlot,
            int center, int left, int right, int attackBase, int attackState, int attackCount) {
        int sprite = center;
        if (bossX != null && bossSlot >= 0 && bossSlot < bossX.length) {
            if (xFixed < ((bossX[bossSlot] - 10) << 16)) {
                sprite = left;
            } else if (xFixed > ((bossX[bossSlot] + 10) << 16)) {
                sprite = right;
            }
        }
        if (attackState >= 1 && attackState <= attackCount) {
            sprite = attackBase + attackState - 1;
        }
        return sprite;
    }

    private static int resolveType13(int age, int attackState, int motion) {
        int sprite;
        if (motion > 0) {
            sprite = 196 + ((age >> 1) % 3);
            if (motion > 3) {
                sprite = 200;
            } else if (motion > 1) {
                sprite = 199;
            }
        } else {
            sprite = 206 + ((age >> 1) % 3);
            if (motion < -3) {
                sprite = 210;
            } else if (motion < -1) {
                sprite = 209;
            }
        }
        if (attackState == 5) {
            return motion > 0 ? 205 : 215;
        }
        return attackState >= 1 && attackState <= 4 ? 200 + attackState : sprite;
    }

    private static int resolveType14(int age, int attackState, int motion, int transition) {
        int sprite = 216;
        if (motion > 1) {
            sprite = 221 + ((age >> 1) & 7);
        } else if (motion < -1) {
            sprite = 229 + ((age >> 1) & 7);
        } else if (transition > 1) {
            sprite = transition > 3 ? 238 : 237;
        }
        switch (attackState) {
            case 1:
                return motion == 0 ? 219 : 217;
            case 2:
                return motion == 0 ? 220 : 218;
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

    private static int resolveType15(int attackState, int motion, int transition) {
        int sprite;
        if (motion > 0) {
            sprite = motion > 3 ? 359 : motion > 1 ? 358 : 356;
        } else {
            sprite = motion < -3 ? 361 : motion < -1 ? 360 : 357;
        }
        if (attackState >= 1 && attackState <= 8) {
            sprite = 361 + attackState;
        }
        if (attackState <= 4 && transition == 1) {
            sprite += 14;
        }
        return sprite;
    }

    private static int resolveType16(int attackState, int motion) {
        int sprite = 504;
        if (motion > 0) {
            sprite = motion > 3 ? 508 : motion > 1 ? 507 : 504;
        } else if (motion < -3) {
            sprite = 506;
        } else if (motion < -1) {
            sprite = 505;
        }
        return attackState >= 1 && attackState <= 12 ? 508 + attackState : sprite;
    }

    private static int resolveType17(int attackState, int motion) {
        int sprite = 711;
        if (motion > 0) {
            sprite = motion > 3 ? 715 : motion > 1 ? 714 : 711;
        } else if (motion < -3) {
            sprite = 713;
        } else if (motion < -1) {
            sprite = 712;
        }
        return attackState >= 1 && attackState <= 2 ? 715 + attackState : sprite;
    }

    private static int resolveType19(int attackState, int motion) {
        int sprite = 719;
        if (motion > 0) {
            sprite = motion > 3 ? 721 : motion > 1 ? 720 : 719;
        } else if (motion < -3) {
            sprite = 723;
        } else if (motion < -1) {
            sprite = 722;
        }
        return attackState >= 1 && attackState <= 4 ? 723 + attackState : sprite;
    }
}
