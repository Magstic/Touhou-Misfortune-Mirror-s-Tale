package touhou;

import javax.microedition.lcdui.Graphics;

import touhou.battle.BattleMath;

public final class DropItemSystem {
    private static final int CAPACITY = 64;

    public static final int TYPE_NONE = 0;
    public static final int TYPE_SCORE = 1;
    public static final int TYPE_P = 2;
    public static final int TYPE_BIG_P = 3;
    public static final int TYPE_FULL_POWER = 4;
    public static final int TYPE_BOMB = 5;
    public static final int TYPE_1UP = 6;
    public static final int TYPE_CRISIS = 7;

    public interface Listener {
        void addPower(int amount);

        void addBomb(int amount);

        void addPlayer(int amount);

        void addScore(int amount);

        void onScoreItemCollected();
    }

    public interface IconDrawer {
        void draw(Graphics g, int ccIndex, int x, int y);
    }

    private final FixedSlotPool slots = new FixedSlotPool(CAPACITY);
    private final boolean[] active = slots.activeArray();
    private final int[] timer = new int[CAPACITY];
    private final int[] spriteBase = new int[CAPACITY];
    private final int[] type = new int[CAPACITY];
    private final int[] state = new int[CAPACITY];
    private final int[] x = new int[CAPACITY];
    private final int[] y = new int[CAPACITY];
    private final int[] r = new int[CAPACITY];
    private final int[] vel = new int[CAPACITY];
    private final int[] dir = new int[CAPACITY];
    private final int[] x0 = new int[CAPACITY];
    private final int[] y0 = new int[CAPACITY];

    private Listener listener;

    public DropItemSystem() {
        clear();
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public int capacity() {
        return slots.capacity();
    }

    public int activeCount() {
        return slots.activeCount();
    }

    public boolean isActive(int index) {
        return slots.isActive(index);
    }

    public void clear() {
        slots.clear();
        for (int i = 0; i < CAPACITY; i++) {
            timer[i] = 0;
            spriteBase[i] = 0;
            type[i] = 0;
            state[i] = 0;
            x[i] = 0;
            y[i] = 0;
            r[i] = 0;
            vel[i] = 0;
            dir[i] = 0;
            x0[i] = 0;
            y0[i] = 0;
        }
    }

    public int spawn(int itemType, int spawnState, int angleOffsetDeg, int xFixed, int yFixed, int stageIndex, int power, boolean bossActive) {
        if (itemType == TYPE_NONE) {
            return FixedSlotPool.NO_SLOT;
        }

        int n = itemType;
        if (stageIndex == 0 && power > 64 && !bossActive) {
            if (n == TYPE_P) {
                n = TYPE_SCORE;
            }
            if (n == TYPE_BIG_P) {
                n = TYPE_P;
            }
        }
        if (power == 128 && (n == TYPE_P || n == TYPE_BIG_P)) {
            n = TYPE_SCORE;
        }

        int base;
        switch (n) {
            case TYPE_SCORE:
                base = 111;
                break;
            case TYPE_P:
                base = 120;
                break;
            case TYPE_BIG_P:
                base = 129;
                break;
            case TYPE_FULL_POWER:
                base = 138;
                break;
            case TYPE_BOMB:
                base = 147;
                break;
            case TYPE_1UP:
                base = 156;
                break;
            case TYPE_CRISIS:
                base = 165;
                break;
            default:
                return FixedSlotPool.NO_SLOT;
        }

        int index = slots.allocate();
        if (index == FixedSlotPool.NO_SLOT) {
            return FixedSlotPool.NO_SLOT;
        }
        timer[index] = 0;
        type[index] = n;
        spriteBase[index] = base;
        dir[index] = angleOffsetDeg + 270;
        r[index] = 655360;
        x[index] = xFixed;
        y[index] = yFixed;
        x0[index] = xFixed;
        y0[index] = yFixed;
        vel[index] = 0;
        state[index] = spawnState;
        return index;
    }

    public void update(int playerXFixed, int playerYFixed, boolean playerSlow, int stageIndex, int power, boolean playerDead,
            boolean disableItemCollectLimitCheat, int chara, int crisis) {
        int figcs = Fixed.fromInt(1);
        int figisqrt = (chara == 1) ? Fixed.fromInt(40) : Fixed.fromInt(30);
        int collectRange = figisqrt;
        if (!playerSlow) {
            collectRange >>= 2;
        }

        if (playerYFixed < 4849664 && (disableItemCollectLimitCheat || power == 128 || stageIndex == 6)) {
            forceCollect(0);
        }

        for (int i = 0; i < CAPACITY; i++) {
            if (!active[i]) {
                continue;
            }

            if (!playerDead && (state[i] == 2 || timer[i] > 3)) {
                int d0 = collectRange;
                if (state[i] == 0 && BattleMath.rectsOverlapFixed(x[i] - r[i], y[i] - r[i], r[i] << 1, r[i] << 1, playerXFixed - (figcs + d0), playerYFixed - (figcs + d0), (figcs + d0) << 1, (figcs + d0) << 1)) {
                    timer[i] = 0;
                    state[i] = 2;
                }

                if (BattleMath.rectsOverlapFixed(x[i] - r[i], y[i] - r[i], r[i] << 1, r[i] << 1, playerXFixed - (figcs + 131072), playerYFixed - (figcs + 131072), (figcs + 131072) << 1, (figcs + 131072) << 1)) {
                    applyPickup(type[i], playerYFixed, crisis);
                    slots.release(i);
                    continue;
                }
            }

            switch (state[i]) {
                case 0:
                    y[i] += vel[i];
                    if (timer[i] < 8) {
                        if (vel[i] > -327680) {
                            vel[i] -= 65536;
                        }
                    } else {
                        if (vel[i] < 262144) {
                            vel[i] += 65536;
                        }
                    }
                    break;
                case 1:
                    int ang = dir[i];
                    x[i] += BattleMath.speedCos(80, ang);
                    y[i] += BattleMath.speedSin(80, ang);
                    if (y[i] < 3145728 || x[i] < 1179648 || x[i] > 12582912) {
                        state[i] = 0;
                    }
                    break;
                case 2:
                    int arc = BattleMath.arcTan2Deg(playerYFixed - y[i], playerXFixed - x[i]);
                    x0[i] = x[i];
                    y0[i] = y[i];
                    int dist = timer[i] * 3;
                    x[i] = x0[i] + (Trig.cos(arc) * dist);
                    y[i] = y0[i] + (Trig.sin(arc) * dist);
                    if (timer[i] > 3) {
                        timer[i] = 3;
                    }
                    break;
                default:
                    break;
            }

            timer[i]++;
            if (y[i] > 15990784) {
                slots.release(i);
            }
        }
    }

    public void render(Graphics g, IconDrawer drawer, int originX, int originY, int playY) {
        if (g == null || drawer == null) {
            return;
        }
        for (int i = 0; i < CAPACITY; i++) {
            if (!active[i]) {
                continue;
            }

            int frame = 0;
            if (state[i] == 0 && timer[i] < 8) {
                frame = timer[i];
            }

            int drawX = originX + (x[i] >> 16);
            if (type[i] == TYPE_CRISIS) {
                int drawY = originY + (y[i] >> 16);
                drawer.draw(g, spriteBase[i], drawX, drawY);
                continue;
            }

            if (y[i] < 0) {
                drawer.draw(g, spriteBase[i] + 8, drawX, originY + playY + 2);
                continue;
            }

            int drawY = originY + (y[i] >> 16);
            drawer.draw(g, spriteBase[i] + frame, drawX, drawY);
        }
    }

    public void forceCollect(int mode) {
        for (int i = 0; i < CAPACITY; i++) {
            if (!active[i] || state[i] == 2) {
                continue;
            }
            if (mode == 0) {
                timer[i] = 0;
                state[i] = 2;
            } else if (mode == 1) {
                if (type[i] == TYPE_CRISIS) {
                    timer[i] = 0;
                    state[i] = 2;
                }
            } else if (mode == 2) {
                if (type[i] != TYPE_FULL_POWER) {
                    timer[i] = 0;
                    state[i] = 2;
                }
            }
        }
    }

    // Mirrors DoJa : yc() cancels collecting items and gives them a small initial speed.
    public void cancelCollectingAndScatter() {
        for (int i = 0; i < CAPACITY; i++) {
            if (!active[i] || state[i] != 2) {
                continue;
            }
            vel[i] = 196608;
            state[i] = 0;
        }
    }

    private void applyPickup(int itemType, int playerYFixed, int crisis) {
        if (listener == null) {
            return;
        }
        switch (itemType) {
            case TYPE_FULL_POWER:
                listener.addPower(128);
                return;
            case TYPE_BIG_P:
                listener.addPower(8);
                return;
            case TYPE_P:
                listener.addPower(1);
                return;
            case TYPE_SCORE:
                listener.addScore(ScoreSystem.calcScoreItemAdd(playerYFixed));
                listener.onScoreItemCollected();
                return;
            case TYPE_BOMB:
                listener.addBomb(1);
                return;
            case TYPE_1UP:
                listener.addPlayer(1);
                return;
            case TYPE_CRISIS:
                listener.addScore(ScoreSystem.calcCrisisItemAdd(crisis));
                return;
            default:
                return;
        }
    }

}
