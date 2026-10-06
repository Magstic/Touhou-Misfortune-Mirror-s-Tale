package touhou.battle;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.Font;

import touhou.CcTable;
import touhou.Debug;
import touhou.GameCore;
import touhou.ImageBank;
import touhou.Player;
import touhou.Trig;
import touhou.i18n.I18n;
import touhou.i18n.TextId;
import touhou.i18n.UiText;
import touhou.ui.StageClearResultPanel;
import touhou.ui.UiDraw;

/**
 * Battle renderer for the in-game playfield.
 *
 * Responsibilities:
 * - Draw the battle playfield (background, stage objects, enemies, bullets, player, items).
 * - Draw overlays (boss spell cut-in, bomb cut-in, dialogue, special spell effects).
 * - Draw HUD (score/power/graze) and the outer frame.
 *
 * Design notes:
 * - This class should be treated as a read-only renderer: it reads state via
 *   {@link GameCore.BattleRenderAccess} and draws a single frame.
 * - Coordinate system is a fixed 240x240 logical screen.
 *   The playfield is a clipped sub-rect; the right side is reserved for HUD.
 */
public final class BattleRenderer {

    private static final int LOGICAL_W = BattleRenderRules.LOGICAL_WIDTH;
    private static final int LOGICAL_H = BattleRenderRules.LOGICAL_HEIGHT;
    private static final int PLAY_X = BattleRenderRules.PLAY_X;
    private static final int PLAY_Y = BattleRenderRules.PLAY_Y;
    private static final int PLAY_W = BattleRenderRules.PLAY_WIDTH;
    private static final int PLAY_H = BattleRenderRules.PLAY_HEIGHT;

    // Stage 5 background particle shuffle table.
    private static final int[] BG_STAGE5_TABLE = new int[] { 0, 5, 6, 5, 2, 9, 8, 1, 4, 6, 3, 4, 7, 2, 4, 8 };

    // Pre-built timer strings "00".."99" to avoid per-frame String concatenation.
    private static final String[] TIMER_STRINGS = {
            "00","01","02","03","04","05","06","07","08","09",
            "10","11","12","13","14","15","16","17","18","19",
            "20","21","22","23","24","25","26","27","28","29",
            "30","31","32","33","34","35","36","37","38","39",
            "40","41","42","43","44","45","46","47","48","49",
            "50","51","52","53","54","55","56","57","58","59",
            "60","61","62","63","64","65","66","67","68","69",
            "70","71","72","73","74","75","76","77","78","79",
            "80","81","82","83","84","85","86","87","88","89",
            "90","91","92","93","94","95","96","97","98","99"
    };

    // Boss spell cut-in (screen overlay) timing.
    private static final int BOSS_SPELL_CUTIN_PHASE_FADE_OUT = BattleRenderRules.CUT_IN_PHASE_FADE_OUT;

    // Boss display names used by the boss HUD.
    private static final String[] BOSS_NAMES = new String[] { "Sunny Milk", "Luna Child", "Star Sapphire", "Sunny Luna Star", "Mystia Lorelei", "Chen", "Keine Kamishirasawa", "Sakuya Izayoi", "Marisa Vision", "Alice Vision" };

    // Stage 6 (Extra Stage) background: caches for additive RGB blending.
    // We cache RGB arrays to avoid calling Image.getRGB every frame.
    private int stage6CacheD4;
    private Image stage6CacheBaseImg;
    private int[] stage6CacheBaseRgb;
    private int stage6CacheBaseW;
    private int stage6CacheBaseH;

    private Image stage6CacheOverlayImg;
    private int[] stage6CacheOverlayRgb;
    private int stage6CacheOverlayW;
    private int stage6CacheOverlayH;

    private int[] stage6BlendOutRgb;

    // Cached bomb spell title banner (pre-rendered to avoid per-frame text rasterization).
    private final Image[] bombSpellTitleImg = new Image[3];
    private final int[] bombSpellTitleImgMainRgb = new int[3];
    private final int[] bombSpellTitleImgYOffset = new int[3];

    // Pre-built death checkerboard overlay (replaces 200 drawLine calls per frame).
    private Image deathCheckerboard;

    // Scratch arrays used by background renderers to sort draw order.
    private final int[] bgKeys4 = new int[4];
    private final int[] bgRanks4 = new int[4];
    private final int[] bgKeys3 = new int[3];
    private final int[] bgRanks3 = new int[3];
    private final int[] bgKeys24 = new int[24];
    private final int[] bgRanks24 = new int[24];
    private final int[] dialoguePortrait = new int[8];
    private final int[] clippedSourceRegion = new int[6];
    private final int[] clippedPlayfieldRegion = new int[6];

    /**
     * Entry point for battle rendering.
     *
     * This method decides whether to render the live battle frame or the stage-clear
     * result panel, while keeping the underlying gameplay frame consistent.
     */
    public void render(GameCore.BattleRenderAccess a, Graphics g) {
        if (a.stageClearPanelActive()) {
            StageClearResultPanel panel = a.stageClearResultPanel();
            if (panel.isLeaving()) {
                renderBattleUi(a, g);
            } else {
                ImageBank imgs = a.imgs();
                renderBattleHudPanel(g, imgs);
                renderBattleHudText(a, g);
                renderBattleFrame(g, imgs);
            }
            panel.render(g, a.bulletSprites());
            return;
        }

        renderBattleUi(a, g);
    }

    /**
     * Renders the full in-game battle UI (background + playfield + HUD + overlays).
     *
     * Render order overview:
     * - Background
     * - HUD panel (right side)
     * - Stage objects, enemies, bullets, player, items (clipped to playfield)
     * - Overlays (boss cut-in, bomb cut-in, dialogue, special spell effects)
     * - Frame (top/bottom border)
     */
    public void renderBattleUi(GameCore.BattleRenderAccess a, Graphics g) {
        ImageBank imgs = a.imgs();
        if (imgs == null) {
            g.setColor(0x000000);
            g.fillRect(0, 0, LOGICAL_W, LOGICAL_H);
            return;
        }

        renderBattleBackground(a, g);
        renderBattleHudPanel(g, imgs);
        renderBattleHudText(a, g);

        // Boss spell cut-in sits above the background but below sprites.
        g.setClip(0, 0, LOGICAL_W, LOGICAL_H);
        long ci0 = System.currentTimeMillis();
        renderBossSpellCutInOverlay(a, g);
        long ci1 = System.currentTimeMillis();
        int ciMs = (int) (ci1 - ci0);
        if (ciMs > 10) {
            Debug.perfSetCutInRenderMs(ciMs);
        }
        g.setClip(PLAY_X, PLAY_Y, PLAY_W, PLAY_H);

        // Effects (Power Full text, explosions, etc.) render after bomb overlay
        // so they remain visible during bomb activation.
        a.stageMc(g);

        stageHb(a, g);
        renderBossCrossLineOverlay(a, g);
        if (a.debugPerfEnabled()) {
            long br0 = System.currentTimeMillis();
            a.bullets().renderPlayerBullets(g);
            long br1 = System.currentTimeMillis();
            Debug.perfSetBulletRenderMs((int) (br1 - br0));
        } else {
            a.bullets().renderPlayerBullets(g);
        }

        if (a.deadcnt() > 0) {
            Image cb = getDeathCheckerboard();
            if (cb != null) {
                g.drawImage(cb, PLAY_X, PLAY_Y, Graphics.TOP | Graphics.LEFT);
            }
        }
        g.setClip(0, 0, LOGICAL_W, LOGICAL_H);

        Player player = a.player();
        if (player != null) {
            g.setClip(PLAY_X, PLAY_Y, PLAY_W, PLAY_H);
            player.render(g, a.bulletSprites());
            g.setClip(0, 0, LOGICAL_W, LOGICAL_H);
        }

        g.setClip(PLAY_X, PLAY_Y, PLAY_W, PLAY_H);
        a.bullets().renderEnemyBullets(g);
        g.setClip(0, 0, LOGICAL_W, LOGICAL_H);

        g.setClip(PLAY_X, PLAY_Y, PLAY_W, PLAY_H);
        a.dropItems().render(g, a.dropItemDrawer(), a.originX(), a.originY(), PLAY_Y);
        if (a.debugPerfEnabled()) {
            long bo0 = System.currentTimeMillis();
            renderBlindServiceOverlay(a, g);
            long bo1 = System.currentTimeMillis();
            Debug.perfSetBlindOverlayMs((int) (bo1 - bo0));
        } else {
            renderBlindServiceOverlay(a, g);
        }
        // Bomb cut-in renders after blind mask (matches DoJa order: ia → od).
        renderBombOverlay(a, g);
        g.setClip(0, 0, LOGICAL_W, LOGICAL_H);

        a.scoreSystem().renderSpellBonusOverlay(g);
        renderBattleFrame(g, imgs);

        renderBossTrackIndicators(a, g);
        renderBossHud(a, g);
        renderDialogue(a, g);

        if (a.battlePaused() && a.pauseScreen() != null) {
            a.pauseScreen().render(g);
        }

        if (a.hFlash() != 0) {
            if (a.hFlash() == 1) {
                g.setColor(0xFFFFFF);
            } else {
                g.setColor(0x000000);
            }
            g.fillRect(PLAY_X, PLAY_Y, PLAY_W, PLAY_H);
        }

        if (a.endingFadeToWhite() > 0) {
            int alpha = BattleRenderRules.endingFadeAlpha(a.endingFadeToWhite());
            UiDraw.fillRectAlpha(g, 0, 0, LOGICAL_W, LOGICAL_H, 0xFFFFFF, alpha);
        }

        if (a.iFlash() != 0) {
            int alpha = BattleRenderRules.screenFadeAlpha(a.iFlash());
            UiDraw.fillRectAlpha(g, 0, 0, LOGICAL_W, LOGICAL_H, 0x000000, alpha);
        }

        renderBattleDebugText(a, g);
        if (a.spellPracticeEndActive()) {
            a.spellPracticeEndScreen().render(g, a.bulletSprites());
        }
    }

    /**
     * Renders enemies and boss sprites.
     *
     * Uses {@link CcTable} sprite definitions (cc indices) and stage/boss work arrays
     * to select the correct animation frames.
     */
    private void stageHb(final GameCore.BattleRenderAccess a, final Graphics g) {
        if (a.cc() == null || a.imgs() == null) {
            return;
        }
        int[][] enemylist = a.enemylist();
        for (int i = 0; i < 32; i++) {
            if (enemylist[i][0] == 0) {
                continue;
            }
            int x = BattleRenderRules.fixedToScreen(a.originX(), enemylist[i][5]);
            int y = BattleRenderRules.fixedToScreen(a.originY(), enemylist[i][6]);
            int auraSprite = a.enemyAuraSpriteId(i);
            if (auraSprite != BattleEnemyVisuals.NO_SPRITE) {
                drawCcIconAlphaSafe(a, g, auraSprite, x, y, 127);
            }
            int spriteId = a.enemySpriteId(i);
            if (spriteId == BattleEnemyVisuals.NO_SPRITE) {
                continue;
            }
            int type = enemylist[i][1];
            if ((type == 17 || type == 19) && !a.disableBossBombShieldCheat()
                    && a.bossVisualShieldActive() && a.bombCnt() > 0) {
                drawCcIconAlphaSafe(a, g, spriteId, x, y,
                        BattleEnemyVisuals.bossShieldAlpha(a.bombCnt()));
            } else {
                drawCcIconSafe(a, g, spriteId, x, y);
            }
        }
    }

    // Debug text overlay shown on top of the playfield.
    private void renderBattleDebugText(GameCore.BattleRenderAccess a, Graphics g) {
        Debug.renderBattleDebugText(g,
                a.debugFpsEnabled(), a.debugPerfEnabled(), a.debugResourceEnabled(),
                PLAY_X, PLAY_Y, PLAY_W,
                a.bullets(), a.enemylist(), a.effectlist(), a.cc(), a.effectTable(), a.imgs(),
                a.debugLine(), a.debugLine2(), a.debugLine3(), a.debugLine4(), a.debugLine5());
    }

    /**
     * Bomb overlay / cut-in.
     *
     * Draws character-specific overlay graphics and a spell title banner during bomb frames.
     */
    private void renderBombOverlay(GameCore.BattleRenderAccess a, Graphics g) {
        if (a.bombCnt() <= 0) {
            return;
        }

        ImageBank imgs = a.imgs();
        if (imgs == null) {
            return;
        }

        int d0 = 60 - a.bombCnt();

        int oldClipX = g.getClipX();
        int oldClipY = g.getClipY();
        int oldClipW = g.getClipWidth();
        int oldClipH = g.getClipHeight();

        try {
            g.setClip(PLAY_X, PLAY_Y, PLAY_W, PLAY_H);

            int imgIndex = BattleRenderRules.bombOverlayImage(a.currentChara());
            if (imgIndex >= 12 && imgIndex <= 14 && d0 < 25) {
                Image overlay;
                if (a.bombOverlayAlphaEnabled()) {
                    overlay = imgs.getAlphaImage(imgIndex, 128);
                    if (overlay == null) {
                        overlay = imgs.get(imgIndex);
                    }
                } else {
                    overlay = imgs.get(imgIndex);
                }
                if (overlay != null) {
                    int x = BattleRenderRules.bombOverlayX(a.currentChara(), d0);
                    int y = BattleRenderRules.bombOverlayY(d0);
                    g.drawImage(overlay, x, y, Graphics.TOP | Graphics.LEFT);
                }
            }

            if (a.currentChara() == 0) {
                if (d0 == 17) {
                    g.setColor(0xB60A14);
                    g.fillRect(PLAY_X, PLAY_Y, PLAY_W, PLAY_H);
                } else if (d0 == 23) {
                    g.setColor(0x290AA6);
                    g.fillRect(PLAY_X, PLAY_Y, PLAY_W, PLAY_H);
                } else if (d0 == 29) {
                    g.setColor(0x98981B);
                    g.fillRect(PLAY_X, PLAY_Y, PLAY_W, PLAY_H);
                } else if (d0 == 35) {
                    g.setColor(0x0DB914);
                    g.fillRect(PLAY_X, PLAY_Y, PLAY_W, PLAY_H);
                } else if (d0 == 41) {
                    g.setColor(0x1C938F);
                    g.fillRect(PLAY_X, PLAY_Y, PLAY_W, PLAY_H);
                }
            }

            int ty = BattleRenderRules.bombTitleY(d0);

            String s;
            int main;
            if (a.currentChara() == 0) {
                s = UiText.get(TextId.BOMB_REIMU);
                main = 8323072;
            } else if (a.currentChara() == 1) {
                s = UiText.get(TextId.BOMB_MARISA);
                main = 4791679;
            } else {
                s = UiText.get(TextId.BOMB_ALICE);
                main = 4791679;
            }

            Image title = getBombSpellTitleImage(a.currentChara(), s, main);
            if (title != null) {
                int yOff = bombSpellTitleImgYOffset[BattleRenderRules.safeCharacterIndex(a.currentChara())];
                g.drawImage(title, 5, ty - yOff, Graphics.TOP | Graphics.LEFT);
            } else {
                UiDraw.drawString2(g, null, s, 5, ty, 0, main, 16777215);
            }
        } finally {
            g.setClip(oldClipX, oldClipY, oldClipW, oldClipH);
        }
    }

    private Image getBombSpellTitleImage(int chara, String s, int mainRgb) {
        int idx = BattleRenderRules.safeCharacterIndex(chara);
        if (s == null) {
            s = "";
        }

        Image cached = bombSpellTitleImg[idx];
        if (cached != null && bombSpellTitleImgMainRgb[idx] == mainRgb) {
            return cached;
        }

        Font font = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
        int w = UiDraw.stringWidth(font, s) + 2;
        int h = UiDraw.fontHeight(font) + 2;
        if (w <= 0) {
            w = 1;
        }
        if (h <= 0) {
            h = 1;
        }

        try {
            Image img = Image.createImage(w, h);
            Graphics ig = img.getGraphics();
            ig.setFont(font);

            // Some MIDP implementations initialize mutable images with opaque pixels.
            // Use a unique key color and convert it to transparent to guarantee a clear background.
            final int keyRgb = 0x00FF00;
            ig.setColor(keyRgb);
            ig.fillRect(0, 0, w, h);

            int y = 1 + UiDraw.fontBaseline(font);
            // 1px outline to avoid the "shifted shadow" look.
            int x = 1;
            int outlineRgb = 0xFFFFFF;
            for (int oy = -1; oy <= 1; oy++) {
                for (int ox = -1; ox <= 1; ox++) {
                    if (ox == 0 && oy == 0) {
                        continue;
                    }
                    UiDraw.drawStringPlain(ig, font, s, x + ox, y + oy, 0, outlineRgb);
                }
            }
            UiDraw.drawStringPlain(ig, font, s, x, y, 0, mainRgb);

            int[] rgb = new int[w * h];
            img.getRGB(rgb, 0, w, 0, 0, w, h);
            int key = 0xFF000000 | (keyRgb & 0x00FFFFFF);
            for (int i = 0; i < rgb.length; i++) {
                if (rgb[i] == key) {
                    rgb[i] = 0x00000000;
                }
            }

            Image out = Image.createRGBImage(rgb, w, h, true);
            bombSpellTitleImg[idx] = out;
            bombSpellTitleImgMainRgb[idx] = mainRgb;
            bombSpellTitleImgYOffset[idx] = y;
            return out;
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Dialogue overlay driven by stage script event (opcode 246).
     *
     * Draws portrait, dialogue box, and two-line text extracted from the talk table.
     */
    private void renderDialogue(GameCore.BattleRenderAccess a, Graphics g) {
        ImageBank imgs = a.imgs();
        if (imgs == null) {
            return;
        }

        if (!a.dialogueVisible()) {
            return;
        }

        int talkCnt = a.dialogueCounter();
        int portraitType = a.dialoguePortraitType();

        int colorMain = renderDialoguePortrait(g, imgs, portraitType, talkCnt);

        UiDraw.fillRectAlpha(g, PLAY_X + 10, PLAY_Y + 183, 174, 38, 0x000000, 128);

        String text = a.dialogueText();
        if (talkCnt < 1 && text != null) {

            int oldClipX = g.getClipX();
            int oldClipY = g.getClipY();
            int oldClipW = g.getClipWidth();
            int oldClipH = g.getClipHeight();

            try {
                g.setClip(PLAY_X + 10, PLAY_Y + 183, 174, 38);

                int page = a.dialoguePage();
                int prevPage = a.dialoguePreviousPage();
                int scrollY = a.dialogueScrollY();

                if (scrollY > 0 && prevPage != page) {
                    drawDialoguePageLines(g, text, prevPage, PLAY_X + 13, PLAY_Y + 199 + scrollY - 32, colorMain);
                    drawDialoguePageLines(g, text, page, PLAY_X + 13, PLAY_Y + 199 + scrollY, colorMain);
                } else {
                    drawDialoguePageLines(g, text, page, PLAY_X + 13, PLAY_Y + 199, colorMain);
                }
            } finally {
                g.setClip(oldClipX, oldClipY, oldClipW, oldClipH);
            }
        }
    }

    private int renderDialoguePortrait(Graphics g, ImageBank imgs, int portraitType, int talkCounter) {
        if (!BattleRenderRules.dialoguePortrait(portraitType, talkCounter, dialoguePortrait)) {
            return 0xFFFFFF;
        }
        Image image = imgs.get(dialoguePortrait[0]);
        if (image != null) {
            drawEkiImage(g, image, dialoguePortrait[5], dialoguePortrait[6],
                    dialoguePortrait[1], dialoguePortrait[2], dialoguePortrait[3], dialoguePortrait[4]);
            if (portraitType == 6) {
                drawEkiImage(g, image, dialoguePortrait[5], dialoguePortrait[6] + 46, 76, 124, 76, 8);
            }
        }
        return dialoguePortrait[7];
    }

    private void drawDialoguePageLines(Graphics g, String s, int page, int x, int y, int colorMain) {
        int line0 = page * 2;

        String s1 = BattleRenderRules.dialogueLine(s, line0);
        String s2 = BattleRenderRules.dialogueLine(s, line0 + 1);

        if (s1 != null) {
            UiDraw.drawStringOutline(g, null, s1.trim(), x, y, 0, colorMain, 0x000000);
        }
        if (s2 != null) {
            UiDraw.drawStringOutline(g, null, s2.trim(), x, y + 16, 0, colorMain, 0x000000);
        }
    }

    /**
     * Background renderer for the playfield.
     *
     * Each stage has its own background function. Some stages include special overlays
     * (e.g., boss spell darkness tiles).
     */
    private void renderBattleBackground(GameCore.BattleRenderAccess a, Graphics g) {
        int oldClipX = g.getClipX();
        int oldClipY = g.getClipY();
        int oldClipW = g.getClipWidth();
        int oldClipH = g.getClipHeight();

        try {
            g.setClip(PLAY_X, PLAY_Y, PLAY_W, PLAY_H);

            switch (a.stage()) {
                case 0:
                    renderBgStage0(a, g);
                    break;
                case 1:
                    renderBgStage1(a, g);
                    break;
                case 2:
                    renderBgStage2(a, g);
                    break;
                case 3:
                    renderBgStage3(a, g);
                    break;
                case 4:
                    renderBgStage4(a, g);
                    break;
                case 5:
                    renderBgStage5(a, g);
                    break;
                case 6:
                    renderBgStage6(a, g);
                    break;
                default:
                    renderBgStage0(a, g);
                    break;
            }

            // Darkness overlay: single semi-transparent black layer over the playfield.
            // Triggers: boss spell active (bspellstep != 0) OR player bomb active.
            // Only one layer is rendered regardless of overlap (no stacking).
            // During boss cut-in fade-in/hold, the cut-in provides its own darkness,
            // so spell darkness is suppressed; during fade-out it takes over (both α128).
            if (a.bossSpellCutInAlphaEnabled() || a.bombOverlayAlphaEnabled()) {
                boolean needsDark = false;
                if (a.timeStop() <= 0 && a.bossController().getBspellstep() != 0) {
                    int cutInCnt = a.bossSpellCutInCnt();
                    if (cutInCnt <= 0 || cutInCnt == BOSS_SPELL_CUTIN_PHASE_FADE_OUT) {
                        needsDark = true;
                    }
                }
                if (a.bombCnt() > 0) {
                    needsDark = true;
                }
                if (needsDark) {
                    UiDraw.fillRectAlpha(g, PLAY_X, PLAY_Y, PLAY_W, PLAY_H, 0x000000, 128);
                }
            }
        } finally {
            g.setClip(oldClipX, oldClipY, oldClipW, oldClipH);
        }
    }

    // Stage 0 (Stage 1) background: basic scrolling texture + a few large decorations.
    private void renderBgStage0(GameCore.BattleRenderAccess a, Graphics g) {
        ImageBank imgs = a.imgs();
        if (imgs == null) {
            g.setColor(0x7F7F7F);
            g.fillRect(PLAY_X, PLAY_Y, PLAY_W, PLAY_H);
            return;
        }

        g.setColor(0x7F7F7F);
        g.fillRect(PLAY_X, PLAY_Y, PLAY_W, PLAY_H);

        Image bg = imgs.get(BattleRenderRules.backgroundBaseImage(0, 0));
        if (bg != null) {
            int scroll = a.bgMoveSpd() % 240;
            int x0 = PLAY_X + a.originX();
            int y0 = PLAY_Y + a.originY() + scroll;
            for (int x = x0 - 240; x <= x0 + 240; x += 240) {
                g.drawImage(bg, x, y0, Graphics.TOP | Graphics.LEFT);
                g.drawImage(bg, x, y0 - 240, Graphics.TOP | Graphics.LEFT);
            }
        }

        // Decorations: depth-sorted scrolling sprites.
        // Skipped in simplified-background mode.
        if (a.heavyBgEnabled()) {
            int[] keys = bgKeys4;
            int[] ranks = bgRanks4;
            keys[0] = a.bgMoveSpd() % 344;
            keys[1] = a.bgMoveSpd() % 404;
            keys[2] = a.bgMoveSpd() % 434;
            keys[3] = a.bgMoveSpd() % 312;
            BattleRenderRules.buildStableRanks(ranks, keys, 4);

            for (int target = 0; target < 4; target++) {
                int idx = -1;
                for (int i = 0; i < 4; i++) {
                    if (ranks[i] == target) {
                        idx = i;
                        break;
                    }
                }
                if (idx < 0) {
                    continue;
                }
                int x;
                int y = PLAY_Y + a.originY() + keys[idx];
                int spriteId;
                switch (idx) {
                    case 0:
                        x = PLAY_X + a.originX();
                        spriteId = 191;
                        break;
                    case 1:
                        x = PLAY_X + a.originX() + 150;
                        spriteId = 191;
                        break;
                    case 2:
                        x = PLAY_X + a.originX() + 30;
                        spriteId = 191;
                        break;
                    default:
                        x = PLAY_X + a.originX() + 80;
                        spriteId = 192;
                        break;
                }
                drawCcIconSafe(a, g, spriteId, x, y);
            }
        }
    }

    // Stage 1 (Stage 2) background: scrolling base texture with many decorations sorted by depth.
    private void renderBgStage1(GameCore.BattleRenderAccess a, Graphics g) {
        ImageBank imgs = a.imgs();
        if (imgs == null) {
            g.setColor(0x7F7F7F);
            g.fillRect(PLAY_X, PLAY_Y, PLAY_W, PLAY_H);
            return;
        }

        g.setColor(0x7F7F7F);
        g.fillRect(PLAY_X, PLAY_Y, PLAY_W, PLAY_H);

        Image bg = imgs.get(BattleRenderRules.backgroundBaseImage(1, 0));
        if (bg != null) {
            int scroll = a.bgMoveSpd() % 240;
            int x0 = PLAY_X + a.originX();
            int y0 = PLAY_Y + a.originY() + scroll;
            for (int x = x0 - 240; x <= x0 + 240; x += 240) {
                g.drawImage(bg, x, y0, Graphics.TOP | Graphics.LEFT);
                g.drawImage(bg, x, y0 - 240, Graphics.TOP | Graphics.LEFT);
            }
        }

        // Decorations: 24 depth-sorted sprites + semi-transparent scroll overlay.
        // Skipped in simplified-background mode.
        if (a.heavyBgEnabled()) {
            int[] keys = bgKeys24;
            int[] ranks = bgRanks24;
            keys[0] = a.bgMoveSpd() % 401;
            keys[1] = (a.bgMoveSpd() + 55) % 401;
            keys[2] = (a.bgMoveSpd() + 120) % 401;
            keys[3] = (a.bgMoveSpd() + 190) % 401;
            keys[4] = (a.bgMoveSpd() + 240) % 401;
            keys[5] = (a.bgMoveSpd() + 310) % 401;
            keys[6] = (a.bgMoveSpd() + 380) % 401;
            keys[7] = (a.bgMoveSpd() + 5) % 401;
            keys[8] = (a.bgMoveSpd() + 50) % 401;
            keys[9] = (a.bgMoveSpd() + 130) % 401;
            keys[10] = (a.bgMoveSpd() + 200) % 401;
            keys[11] = (a.bgMoveSpd() + 260) % 401;
            keys[12] = (a.bgMoveSpd() + 320) % 401;
            keys[13] = (a.bgMoveSpd() + 400) % 401;
            keys[14] = (a.bgMoveSpd() + 5) % 277;
            keys[15] = (a.bgMoveSpd() + 45) % 277;
            keys[16] = (a.bgMoveSpd() + 115) % 277;
            keys[17] = (a.bgMoveSpd() + 165) % 277;
            keys[18] = (a.bgMoveSpd() + 220) % 277;
            keys[19] = (a.bgMoveSpd() + 20) % 266;
            keys[20] = (a.bgMoveSpd() + 100) % 266;
            keys[21] = (a.bgMoveSpd() + 167) % 266;
            keys[22] = (a.bgMoveSpd() + 70) % 254;
            keys[23] = (a.bgMoveSpd() + 150) % 254;

            BattleRenderRules.buildStableRanks(ranks, keys, 24);

            for (int target = 0; target < 24; target++) {
                int idx = -1;
                for (int i = 0; i < 24; i++) {
                    if (ranks[i] == target) {
                        idx = i;
                        break;
                    }
                }
                if (idx < 0) {
                    continue;
                }

                int x;
                int y = PLAY_Y + a.originY() + keys[idx];
                int spriteId;
                if (idx >= 0 && idx <= 6) {
                    x = PLAY_X + a.originX() - ((idx & 0x3) << 2);
                    spriteId = 351;
                } else if (idx >= 7 && idx <= 13) {
                    x = PLAY_X + a.originX() + 194 + ((idx & 0x3) << 2);
                    spriteId = 352;
                } else if (idx >= 14 && idx <= 18) {
                    x = PLAY_X + a.originX() + 30 + (idx * 42) % 150;
                    spriteId = 353;
                } else if (idx >= 19 && idx <= 21) {
                    x = PLAY_X + a.originX() + 40 + (idx * 75) % 140;
                    spriteId = 354;
                } else {
                    x = PLAY_X + a.originX() + 20 + (idx * 54) % 160;
                    spriteId = 355;
                }
                drawCcIconSafe(a, g, spriteId, x, y);
            }

            int overlayImage = BattleRenderRules.backgroundOverlayImage(1, 0);
            Image overlay = imgs.getAlphaImage(overlayImage, 128);
            if (overlay == null) {
                overlay = imgs.get(overlayImage);
            }
            if (overlay != null) {
                int scroll = a.bgMoveSpd() % 240;
                int x0 = PLAY_X + a.originX();
                int y0 = PLAY_Y + a.originY() + scroll;
                g.drawImage(overlay, x0, y0, Graphics.TOP | Graphics.LEFT);
                g.drawImage(overlay, x0, y0 - 240, Graphics.TOP | Graphics.LEFT);
            }
        }
    }

    // Stage 2 (Stage 3) background: fewer, larger decorations (3 layers) over a scrolling base texture.
    private void renderBgStage2(GameCore.BattleRenderAccess a, Graphics g) {
        ImageBank imgs = a.imgs();
        if (imgs == null) {
            g.setColor(0x7F7F7F);
            g.fillRect(PLAY_X, PLAY_Y, PLAY_W, PLAY_H);
            return;
        }

        g.setColor(0x7F7F7F);
        g.fillRect(PLAY_X, PLAY_Y, PLAY_W, PLAY_H);

        Image bg = imgs.get(BattleRenderRules.backgroundBaseImage(2, 0));
        if (bg != null) {
            int scroll = a.bgMoveSpd() % 240;
            int x0 = PLAY_X + a.originX();
            int y0 = PLAY_Y + a.originY() + scroll;
            for (int x = x0 - 240; x <= x0 + 240; x += 240) {
                g.drawImage(bg, x, y0, Graphics.TOP | Graphics.LEFT);
                g.drawImage(bg, x, y0 - 240, Graphics.TOP | Graphics.LEFT);
            }
        }

        // Decorations: 3 large sprite groups + side alpha panels + center tint.
        // Skipped in simplified-background mode.
        if (a.heavyBgEnabled()) {
            int[] keys = bgKeys3;
            int[] ranks = bgRanks3;
            keys[0] = a.bgMoveSpd() % 640;
            keys[1] = (a.bgMoveSpd() + 160) % 640;
            keys[2] = (a.bgMoveSpd() + 340) % 640;
            BattleRenderRules.buildStableRanks(ranks, keys, 3);

            for (int target = 0; target < 3; target++) {
                int idx = -1;
                for (int i = 0; i < 3; i++) {
                    if (ranks[i] == target) {
                        idx = i;
                        break;
                    }
                }
                if (idx < 0) {
                    continue;
                }

                int x = PLAY_X + a.originX() + 20 + (idx * 54) % 160;
                int y = PLAY_Y + a.originY() + keys[idx];

                if (idx == 2) {
                    drawCcIconSafe(a, g, 486, x - 90, y);
                    drawCcIconSafe(a, g, 487, x - 90, y);
                    drawCcIconSafe(a, g, 488, x - 90, y);
                    drawCcIconSafe(a, g, 489, x - 90, y);
                } else if (idx == 1) {
                    drawCcIconSafe(a, g, 486, x - 90, y);
                    drawCcIconSafe(a, g, 487, x - 90, y);
                    drawCcIconSafe(a, g, 488, x - 90, y);
                    drawCcIconSafe(a, g, 489, x - 90, y);
                    drawCcIconSafe(a, g, 486, x - 10 + 0, y);
                    drawCcIconSafe(a, g, 486, x - 10 + 21, y);
                    drawCcIconSafe(a, g, 486, x - 10 + 42, y);
                    drawCcIconSafe(a, g, 487, x - 10 + 42, y);
                }
                y -= 50;
                drawCcIconSafe(a, g, 485, x, y);
            }

            int scroll = (a.bgMoveSpd() << 1) % 240;
            Image left = imgs.getAlphaImage(44, 128);
            if (left == null) {
                left = imgs.get(44);
            }
            if (left != null) {
                int x0 = PLAY_X + a.originX();
                int y0 = PLAY_Y + a.originY() + scroll;
                UiDraw.drawRegion(g, left, x0, y0, 0, 0, 73, 240);
                UiDraw.drawRegion(g, left, x0, y0 - 240, 0, 0, 73, 240);
            }

            UiDraw.fillRectAlpha(g, PLAY_X + a.originX() + 73, PLAY_Y + a.originY(), 40, PLAY_H, 0xCFE2FF, 128);

            Image right = imgs.getAlphaImage(45, 128);
            if (right == null) {
                right = imgs.get(45);
            }
            if (right != null) {
                int x0 = PLAY_X + a.originX() + 194 - 81;
                int y0 = PLAY_Y + a.originY() + scroll;
                UiDraw.drawRegion(g, right, x0, y0, 0, 0, 81, 240);
                UiDraw.drawRegion(g, right, x0, y0 - 240, 0, 0, 81, 240);
            }
        }
    }

    // Stage 3 (Stage 4) background: single base + two scrolling overlays (different speeds).
    private void renderBgStage3(GameCore.BattleRenderAccess a, Graphics g) {
        ImageBank imgs = a.imgs();
        if (imgs == null) {
            g.setColor(0x303030);
            g.fillRect(PLAY_X, PLAY_Y, PLAY_W, PLAY_H);
            return;
        }

        Image bg = imgs.get(BattleRenderRules.backgroundBaseImage(3, 0));
        if (bg != null) {
            int scroll = (a.bgMoveSpd() >> 1) % 240;
            int x0 = PLAY_X + a.originX();
            int y0 = PLAY_Y + a.originY() + scroll;
            g.drawImage(bg, x0, y0, Graphics.TOP | Graphics.LEFT);
            g.drawImage(bg, x0, y0 - 240, Graphics.TOP | Graphics.LEFT);
        } else {
            g.setColor(0x303030);
            g.fillRect(PLAY_X, PLAY_Y, PLAY_W, PLAY_H);
        }

        // Two semi-transparent scrolling overlays at different speeds.
        // Skipped in simplified-background mode.
        if (a.heavyBgEnabled()) {
            int overlayImage = BattleRenderRules.backgroundOverlayImage(3, 0);
            Image overlay = imgs.getAlphaImage(overlayImage, 127);
            if (overlay == null) {
                overlay = imgs.get(overlayImage);
            }
            if (overlay != null) {
                int x0 = PLAY_X + a.originX();

                int scroll = a.bgMoveSpd() % 240;
                int y0 = PLAY_Y + a.originY() + scroll;
                g.drawImage(overlay, x0, y0, Graphics.TOP | Graphics.LEFT);
                g.drawImage(overlay, x0, y0 - 240, Graphics.TOP | Graphics.LEFT);

                int scroll2 = (a.bgMoveSpd() << 1) % 240;
                int y1 = PLAY_Y + a.originY() + scroll2;
                g.drawImage(overlay, x0, y1, Graphics.TOP | Graphics.LEFT);
                g.drawImage(overlay, x0, y1 - 240, Graphics.TOP | Graphics.LEFT);
            }
        }
    }

    // Stage 4 (Stage 5) background: base image pair + overlay, with an optional time-stop distortion effect.
    private void renderBgStage4(GameCore.BattleRenderAccess a, Graphics g) {
        ImageBank imgs = a.imgs();
        if (imgs == null) {
            g.setColor(0x303030);
            g.fillRect(PLAY_X, PLAY_Y, PLAY_W, PLAY_H);
            return;
        }

        int d0 = a.fi() >> 2;
        if (d0 > 240) {
            d0 = 240;
        }

        Image a0 = imgs.get(BattleRenderRules.backgroundBaseImage(4, 0));
        Image a1 = imgs.get(BattleRenderRules.backgroundSecondaryImage(4));
        if (a0 != null) {
            int x0 = PLAY_X + a.originX();
            int y0 = PLAY_Y + a.originY() + d0;
            g.drawImage(a0, x0, y0, Graphics.TOP | Graphics.LEFT);
        }
        if (a1 != null) {
            int x0 = PLAY_X + a.originX();
            int y0 = PLAY_Y + a.originY() + d0 - 240;
            g.drawImage(a1, x0, y0, Graphics.TOP | Graphics.LEFT);
        }
        if (a0 == null && a1 == null) {
            g.setColor(0x303030);
            g.fillRect(PLAY_X, PLAY_Y, PLAY_W, PLAY_H);
        }

        // Semi-transparent scrolling overlay layer.
        // Skipped in simplified-background mode.
        if (a.heavyBgEnabled()) {
            int overlayImage = BattleRenderRules.backgroundOverlayImage(4, 0);
            Image overlay = imgs.getAlphaImage(overlayImage, 128);
            if (overlay == null) {
                overlay = imgs.get(overlayImage);
            }
            if (overlay != null) {
                int scroll = a.bgMoveSpd() % 240;
                int x0 = PLAY_X + a.originX();
                int y0 = PLAY_Y + a.originY() + scroll;
                g.drawImage(overlay, x0, y0, Graphics.TOP | Graphics.LEFT);
                g.drawImage(overlay, x0, y0 - 240, Graphics.TOP | Graphics.LEFT);
            }
        }

        // Time-stop effect: gameplay mechanic, always rendered regardless of bg mode.
        if (a.timeStop() > 0) {
            // Time-stop visual: wavy clock stripes + gray tint.
            Image clock = imgs.get(66);
            if (clock != null) {
                int baseX = PLAY_X + a.originX();
                int t = a.timeStop() << 2;
                for (int i = 0; i < PLAY_H; i++) {
                    int dx = baseX + (-23) + (Trig.sin(t + i) >> 13);
                    drawEkiImage(g, clock, dx, PLAY_Y + i, 0, i, 240, 1);
                }
            }

            UiDraw.fillRectAlpha(g, PLAY_X, PLAY_Y, PLAY_W, PLAY_H, 0xBFBFBF, 128);
        }
    }

    // Stage 5 (Stage 6) background: scrolling base + semi-random falling particles.
    private void renderBgStage5(GameCore.BattleRenderAccess a, Graphics g) {
        ImageBank imgs = a.imgs();
        if (imgs == null) {
            g.setColor(0x303030);
            g.fillRect(PLAY_X, PLAY_Y, PLAY_W, PLAY_H);
            return;
        }

        Image bg = imgs.get(BattleRenderRules.backgroundBaseImage(5, 0));
        if (bg != null) {
            int scroll = (a.bgMoveSpd() >> 3) % 240;
            int x0 = PLAY_X + a.originX();
            int y0 = PLAY_Y + a.originY() + scroll;
            g.drawImage(bg, x0, y0, Graphics.TOP | Graphics.LEFT);
            g.drawImage(bg, x0, y0 - 240, Graphics.TOP | Graphics.LEFT);
        } else {
            g.setColor(0x303030);
            g.fillRect(PLAY_X, PLAY_Y, PLAY_W, PLAY_H);
        }

        // Semi-random falling particles (alpha-blended sprites).
        // Skipped in simplified-background mode.
        if (a.heavyBgEnabled()) {
            int base = a.bgMoveSpd() >> 3;
            for (int i = 0; i < 32; i++) {
                int d4 = (i * 54 + (BG_STAGE5_TABLE[i >> 1] << 6)) % 194;
                int d5 = a.bgMoveSpd() + (i << 5) + (BG_STAGE5_TABLE[i >> 1] << 6);
                if ((i & 0x3) == 0x0) {
                    d5 >>= 1;
                } else if ((i & 0x3) == 0x2) {
                    d5 >>= 2;
                }
                d5 %= 640;

                int x = PLAY_X + a.originX() + d4;
                int y = PLAY_Y + a.originY() - 10 + d5;
                int spriteId = 728 + ((base + i) & 0x1);
                drawCcIconAlphaSafe(a, g, spriteId, x, y, 128);
            }
        }
    }

    // Stage 6 (Extra Stage) background: additive blending between base and overlay layers.
    private void renderBgStage6(GameCore.BattleRenderAccess a, Graphics g) {
        boolean heavy = a.heavyBgEnabled();
        int d4 = (a.bgMoveSpd() >> 9) & 0x3;

        ensureStage6Cache(a, d4, heavy);

        // Simplified-background mode: base image + overlay scrolling (normal draw).
        // Skips additive blend and ratio color oscillation.
        if (!heavy) {
            if (this.stage6CacheBaseImg != null) {
                g.drawImage(this.stage6CacheBaseImg, 0, 0, Graphics.TOP | Graphics.LEFT);
            } else {
                g.setColor(0x303030);
                g.fillRect(PLAY_X, PLAY_Y, PLAY_W, PLAY_H);
            }
            // Draw overlay with normal compositing for scrolling effect.
            if (this.stage6CacheOverlayImg != null && this.stage6CacheOverlayH > 0) {
                int scroll = (a.bgMoveSpd() >> 1) % this.stage6CacheOverlayH;
                int x0 = PLAY_X + a.originX();
                int y0 = PLAY_Y + a.originY() + scroll;
                g.drawImage(this.stage6CacheOverlayImg, x0, y0, Graphics.TOP | Graphics.LEFT);
                g.drawImage(this.stage6CacheOverlayImg, x0, y0 - this.stage6CacheOverlayH, Graphics.TOP | Graphics.LEFT);
            }
            return;
        }

        if (this.stage6CacheBaseImg == null || this.stage6CacheBaseRgb == null) {
            g.setColor(0x303030);
            g.fillRect(PLAY_X, PLAY_Y, PLAY_W, PLAY_H);
            return;
        }

        if (this.stage6CacheOverlayImg == null || this.stage6CacheOverlayRgb == null || this.stage6CacheOverlayW <= 0 || this.stage6CacheOverlayH <= 0) {
            g.drawImage(this.stage6CacheBaseImg, 0, 0, Graphics.TOP | Graphics.LEFT);
            return;
        }

        int d3 = (a.bgMoveSpd() & 0x1FF);
        if (d3 > 384) {
            d3 = 511 - d3;
        } else if (d3 > 127) {
            d3 = 127;
        }

        int scroll = (a.bgMoveSpd() >> 1) % this.stage6CacheOverlayH;
        blendStage6Add(a, g, d3, scroll);
    }

    // Builds/refreshes Stage 6 (Extra Stage) cached RGB buffers when the phase changes.
    // When heavy=false (simplified bg), only resolves base Image; skips RGB
    // extraction, overlay loading, and blend buffer to save memory.
    private void ensureStage6Cache(GameCore.BattleRenderAccess a, int d4, boolean heavy) {
        ImageBank imgs = a.imgs();
        if (imgs == null) {
            return;
        }

        boolean cached = (this.stage6CacheD4 == d4);
        if (!heavy) {
            // Simplified mode: need base + overlay Image refs for scrolling.
            if (cached && this.stage6CacheBaseImg != null && this.stage6CacheOverlayImg != null) {
                return;
            }
        } else {
            if (cached && this.stage6CacheBaseRgb != null && this.stage6CacheOverlayRgb != null) {
                return;
            }
        }

        this.stage6CacheD4 = d4;

        Image base = imgs.get(BattleRenderRules.backgroundBaseImage(6, d4));
        if (base != null) {
            try {
                if (base.getWidth() <= 1 || base.getHeight() <= 1) {
                    base = null;
                }
            } catch (Throwable t) {
                base = null;
            }
        }
        if (base == null) {
            base = imgs.get(BattleRenderRules.backgroundBaseImage(6, 0));
        }

        this.stage6CacheBaseImg = base;

        Image overlay = imgs.get(BattleRenderRules.backgroundOverlayImage(6, d4));
        if (overlay != null) {
            try {
                if (overlay.getWidth() <= 1 || overlay.getHeight() <= 1) {
                    overlay = null;
                }
            } catch (Throwable t) {
                overlay = null;
            }
        }
        if (overlay == null) {
            overlay = imgs.get(BattleRenderRules.backgroundOverlayImage(6, 0));
        }

        this.stage6CacheOverlayImg = overlay;

        this.stage6CacheBaseRgb = null;
        this.stage6CacheOverlayRgb = null;
        this.stage6CacheBaseW = 0;
        this.stage6CacheBaseH = 0;
        this.stage6CacheOverlayW = 0;
        this.stage6CacheOverlayH = 0;

        // Simplified mode: Image refs are enough for drawImage scrolling.
        // Skip RGB extraction and blend buffer to save memory.
        if (!heavy) {
            // Resolve overlay dimensions for scroll modulo.
            if (overlay != null) {
                try {
                    this.stage6CacheOverlayW = overlay.getWidth();
                    this.stage6CacheOverlayH = overlay.getHeight();
                } catch (Throwable t) {
                    this.stage6CacheOverlayW = 240;
                    this.stage6CacheOverlayH = 240;
                }
            }
            return;
        }

        if (base != null) {
            try {
                int w = base.getWidth();
                int h = base.getHeight();
                int[] rgb = new int[w * h];
                base.getRGB(rgb, 0, w, 0, 0, w, h);
                this.stage6CacheBaseRgb = rgb;
                this.stage6CacheBaseW = w;
                this.stage6CacheBaseH = h;
            } catch (Throwable t) {
                this.stage6CacheBaseRgb = null;
                this.stage6CacheBaseW = 0;
                this.stage6CacheBaseH = 0;
            }
        }

        if (overlay != null) {
            try {
                int w = overlay.getWidth();
                int h = overlay.getHeight();
                int[] rgb = new int[w * h];
                overlay.getRGB(rgb, 0, w, 0, 0, w, h);
                this.stage6CacheOverlayRgb = rgb;
                this.stage6CacheOverlayW = w;
                this.stage6CacheOverlayH = h;
            } catch (Throwable t) {
                this.stage6CacheOverlayRgb = null;
                this.stage6CacheOverlayW = 0;
                this.stage6CacheOverlayH = 0;
            }
        }

        if (this.stage6BlendOutRgb == null || this.stage6BlendOutRgb.length != PLAY_W * PLAY_H) {
            try {
                this.stage6BlendOutRgb = new int[PLAY_W * PLAY_H];
            } catch (Throwable t) {
                this.stage6BlendOutRgb = null;
            }
        }
    }

    // CPU-side additive blend for the Extra Stage background; draw the base image when blending is unavailable.
    private void blendStage6Add(GameCore.BattleRenderAccess a, Graphics g, int ratio, int scroll) {
        if (this.stage6BlendOutRgb == null) {
            g.drawImage(this.stage6CacheBaseImg, 0, 0, Graphics.TOP | Graphics.LEFT);
            return;
        }

        int baseW = this.stage6CacheBaseW;
        int baseH = this.stage6CacheBaseH;
        int overlayW = this.stage6CacheOverlayW;
        int overlayH = this.stage6CacheOverlayH;
        if (baseW <= 0 || baseH <= 0 || overlayW <= 0 || overlayH <= 0) {
            g.drawImage(this.stage6CacheBaseImg, 0, 0, Graphics.TOP | Graphics.LEFT);
            return;
        }

        int outIndex = 0;
        for (int y = 0; y < PLAY_H; y++) {
            int by = PLAY_Y + y;
            if (by < 0) {
                by = 0;
            } else if (by >= baseH) {
                by = baseH - 1;
            }
            int baseRow = by * baseW;

            int oy = y - a.originY() - scroll;
            oy %= overlayH;
            if (oy < 0) {
                oy += overlayH;
            }
            int overlayRow = oy * overlayW;

            for (int x = 0; x < PLAY_W; x++) {
                int dx = x;
                if (dx < 0) {
                    dx = 0;
                } else if (dx >= baseW) {
                    dx = baseW - 1;
                }

                int dst = this.stage6CacheBaseRgb[baseRow + dx];

                int ox = x - a.originX();
                if (ox < 0 || ox >= overlayW) {
                    this.stage6BlendOutRgb[outIndex++] = 0xFF000000 | (dst & 0x00FFFFFF);
                    continue;
                }
                int src = this.stage6CacheOverlayRgb[overlayRow + ox];

                int srcA = (src >>> 24) & 0xFF;
                if (srcA == 0) {
                    this.stage6BlendOutRgb[outIndex++] = 0xFF000000 | (dst & 0x00FFFFFF);
                    continue;
                }

                int dstR = (dst >> 16) & 0xFF;
                int dstG = (dst >> 8) & 0xFF;
                int dstB = dst & 0xFF;

                int srcR = (src >> 16) & 0xFF;
                int srcG = (src >> 8) & 0xFF;
                int srcB = src & 0xFF;

                int outR = (dstR * ratio + srcR * ratio) / 255;
                int outG = (dstG * ratio + srcG * ratio) / 255;
                int outB = (dstB * ratio + srcB * ratio) / 255;

                this.stage6BlendOutRgb[outIndex++] = 0xFF000000 | (outR << 16) | (outG << 8) | outB;
            }
        }

        try {
            g.drawRGB(this.stage6BlendOutRgb, 0, PLAY_W, PLAY_X, PLAY_Y, PLAY_W, PLAY_H, false);
        } catch (Throwable t) {
            try {
                Image img = Image.createRGBImage(this.stage6BlendOutRgb, PLAY_W, PLAY_H, false);
                g.drawImage(img, PLAY_X, PLAY_Y, Graphics.TOP | Graphics.LEFT);
            } catch (Throwable ignore) {
                g.drawImage(this.stage6CacheBaseImg, 0, 0, Graphics.TOP | Graphics.LEFT);
            }
        }
    }

    /**
     * Draws a sprite by ccIndex with bounds checks.
     *
     * This is used by many systems (enemies, backgrounds, HUD decorations).
     */
    public void drawCcIconSafe(GameCore.BattleRenderAccess a, Graphics g, int ccIndex, int x, int y) {
        CcTable cc = a.cc();
        ImageBank imgs = a.imgs();
        if (cc == null || imgs == null) {
            return;
        }
        if (!cc.hasSpriteMeta(ccIndex)) {
            return;
        }

        int imgIndex = cc.getImgIndex(ccIndex);
        int srcX = cc.getSrcX(ccIndex);
        int srcY = cc.getSrcY(ccIndex);
        int w = cc.getW(ccIndex);
        int h = cc.getH(ccIndex);
        int ax = cc.getAx(ccIndex);
        int ay = cc.getAy(ccIndex);

        srcX = imgs.toPhysicalSourceX(imgIndex, srcX);
        srcY = imgs.toPhysicalSourceY(imgIndex, srcY);

        Image img = imgs.get(imgIndex);
        if (img == null) {
            return;
        }

        drawRegionSafe(g, img, x, y, srcX, srcY, w, h, ax, ay);
    }

    // Alpha variant of drawCcIconSafe using ImageBank alpha cache.
    private void drawCcIconAlphaSafe(GameCore.BattleRenderAccess a, Graphics g, int ccIndex, int x, int y, int alpha) {
        CcTable cc = a.cc();
        ImageBank imgs = a.imgs();
        if (cc == null || imgs == null) {
            return;
        }
        if (!cc.hasSpriteMeta(ccIndex)) {
            return;
        }

        int imgIndex = cc.getImgIndex(ccIndex);
        int srcX = cc.getSrcX(ccIndex);
        int srcY = cc.getSrcY(ccIndex);
        int w = cc.getW(ccIndex);
        int h = cc.getH(ccIndex);
        int ax = cc.getAx(ccIndex);
        int ay = cc.getAy(ccIndex);

        srcX = imgs.toPhysicalSourceX(imgIndex, srcX);
        srcY = imgs.toPhysicalSourceY(imgIndex, srcY);

        Image img = imgs.get(imgIndex);
        if (img == null) {
            return;
        }

        int a0 = alpha;
        if (a0 < 0) {
            a0 = 0;
        }
        if (a0 > 255) {
            a0 = 255;
        }
        if (a0 != 255) {
            Image alphaImg = imgs.getAlphaImage(imgIndex, a0);
            if (alphaImg != null) {
                img = alphaImg;
            }
        }

        drawRegionSafe(g, img, x, y, srcX, srcY, w, h, ax, ay);
    }

    /**
     * Safe region drawing helper.
     *
     * Clamps the source rect to the source image bounds and avoids exceptions.
     */
    private boolean drawRegionSafe(Graphics g, Image img, int x, int y, int srcX, int srcY, int w, int h, int ax, int ay) {
        if (img == null) {
            return false;
        }
        if (!BattleRenderRules.clipSourceRegion(x, y, srcX, srcY, w, h, ax, ay, 1,
                img.getWidth(), img.getHeight(), clippedSourceRegion)) {
            return false;
        }
        UiDraw.drawRegion(g, img, clippedSourceRegion[0], clippedSourceRegion[1],
                clippedSourceRegion[2], clippedSourceRegion[3],
                clippedSourceRegion[4], clippedSourceRegion[5]);
        return true;
    }

    /**
     * HUD text renderer (score/power/graze/player/bomb and difficulty level icon).
     *
     * Right-side HUD background is rendered by renderBattleHudPanel().
     */
    public void renderBattleHudText(GameCore.BattleRenderAccess a, Graphics g) {
        BattleHudData hud = a.hud();
        ImageBank imgs = a.imgs();
        if (hud == null || imgs == null) {
            return;
        }

        Image sheet = imgs.get(19);
        if (sheet == null) {
            return;
        }

        int level = hud.getLevel();
        drawCcIconSafe(a, g, 185 + level, 216, 4);

        UiDraw.drawRegion(g, sheet, 196, 13, 2, 45, 36, 9);
        drawDigitsFixed(g, sheet, hud.getHiScore(), 8, 197, 23);

        UiDraw.drawRegion(g, sheet, 196, 32, 2, 54, 26, 9);
        drawDigitsFixed(g, sheet, hud.getScore(), 8, 197, 42);

        UiDraw.drawRegion(g, sheet, 196, 51, 2, 63, 31, 9);
        drawStars(g, sheet, 0, hud.getPlayer() - 1, 196, 60);

        UiDraw.drawRegion(g, sheet, 196, 69, 2, 72, 23, 9);
        drawStars(g, sheet, 7, hud.getBomb(), 196, 78);

        UiDraw.drawRegion(g, sheet, 196, 87, 2, 81, 30, 9);
        drawDigitsFixed(g, sheet, hud.getPower(), BattleRenderRules.digitCount(hud.getPower(), 3), 197, 97);

        UiDraw.drawRegion(g, sheet, 196, 106, 2, 90, 27, 9);
        drawDigitsFixed(g, sheet, hud.getGraze(), BattleRenderRules.digitCount(hud.getGraze(), 5), 197, 116);
    }

    // Boss spell cut-in overlay (black fade + character cut-in image).
    private void renderBossSpellCutInOverlay(GameCore.BattleRenderAccess a, Graphics g) {
        if (a.bossSpellCutInCnt() <= 0) {
            return;
        }

        ImageBank imgs = a.imgs();
        if (imgs == null) {
            return;
        }
        if (a.dialogueVisible() || a.taCnt() != 0) {
            return;
        }

        int oldClipX = g.getClipX();
        int oldClipY = g.getClipY();
        int oldClipW = g.getClipWidth();
        int oldClipH = g.getClipHeight();

        try {
            g.setClip(PLAY_X, PLAY_Y, PLAY_W, PLAY_H);

            boolean alphaEnabled = a.bossSpellCutInAlphaEnabled();
            int alpha = BattleRenderRules.CUT_IN_ALPHA_MAX;
            if (alphaEnabled) {
                alpha = BattleRenderRules.bossSpellCutInAlpha(
                        a.bossSpellCutInCnt(), a.dynamicCutInEnabled(),
                        a.fi() - a.bossSpellCutInPhaseStartFi());
                if (alpha <= 0) {
                    return;
                }
                // Draw dark overlay only during fade-in and hold phases.
                // During fade-out, renderBattleBackground's spell darkness handles it.
                if (a.bossSpellCutInCnt() != BOSS_SPELL_CUTIN_PHASE_FADE_OUT) {
                    UiDraw.fillRectAlpha(g, PLAY_X, PLAY_Y, PLAY_W, PLAY_H, 0x000000, alpha);
                }
            }

            int imgIndex = BattleRenderRules.bossSpellCutInImage(
                    a.bossSpellCutInStage(), a.gameMode(),
                    a.bossController().host().getSpellPracticeBossStep());

            Image overlay;
            if (alphaEnabled) {
                overlay = imgs.getAlphaImage(imgIndex, alpha);
                if (overlay == null) {
                    overlay = imgs.get(imgIndex);
                }
            } else {
                overlay = imgs.get(imgIndex);
            }

            if (overlay != null) {
                int gc = a.fi() - a.bossSpellCutInStartFi();
                if (gc < 0) {
                    gc = 0;
                }

                switch (a.bossSpellCutInStage()) {
                    case 0:
                        switch (a.bossSpellCutInChara()) {
                            case 108:
                                drawEkiImage(g, overlay, 130, 20 - (gc >> 2), 65, 0, 64, 240);
                                break;
                            case 110:
                                drawEkiImage(g, overlay, 130, 20 - (gc >> 2), 130, 10, 64, 230);
                                break;
                            case 109:
                                drawEkiImage(g, overlay, 0, 20 - (gc >> 2), 0, 21, 64, 219);
                                break;
                            case 111:
                                drawEkiImage(g, overlay, 0, 20 - (gc >> 2), 0, 21, 64, 219);
                                drawEkiImage(g, overlay, 64, gc >> 2, 65, 0, 64, 240);
                                drawEkiImage(g, overlay, 130, 20 - (gc >> 2), 130, 10, 64, 230);
                                break;
                        }
                        break;
                    case 1:
                        drawEkiImage(g, overlay, 0, 8 + (gc >> 3), 0, 0, 194, 226);
                        break;
                    case 2:
                        drawEkiImage(g, overlay, 14 - (gc >> 2), 8 + (gc >> 2), 0, 0, 240, 240);
                        break;
                    case 3:
                        drawEkiImage(g, overlay, 0, gc >> 2, 0, 0, 205, 240);
                        break;
                    case 4:
                        drawEkiImage(g, overlay, 0 - (gc >> 2), gc >> 2, 0, 0, 240, 240);
                        break;
                    case 5:
                        drawEkiImage(g, overlay, -20 + (gc >> 1), gc >> 2, 0, 0, 240, 240);
                        break;
                    case 6:
                        drawEkiImage(g, overlay, 0, 8 - (gc >> 2), 0, 0, 206, 240);
                        break;
                }
            }
        } finally {
            g.setClip(oldClipX, oldClipY, oldClipW, oldClipH);
        }
    }

    // Special-case overlay for a specific boss spell (crosshair lines over the playfield).
    private void renderBossCrossLineOverlay(GameCore.BattleRenderAccess a, Graphics g) {
        if (!a.bossMode()) {
            return;
        }
        if (a.bossController() == null || a.bossController().getSpellId() != 102) {
            return;
        }
        if (!(a.bossF() == 23 || a.bossF() == 24)) {
            return;
        }

        int d3 = a.gcnt();
        if (d3 > 30 || a.bossF() == 24) {
            d3 = 30;
        }

        int d4 = (a.gcnt() << 3) % 255;
        if (d4 > 127) {
            d4 = 255 - d4;
        }
        int d5 = d4 + 127;
        int color = (d5 << 16) | (d5 << 8) | d5;
        int alpha = 64 + ((d4 * 64) / 127);
        if (alpha > 128) {
            alpha = 128;
        }

        int lenV = (114 * d3) / 30;
        int lenH = (98 * d3) / 30;

        int xMid = PLAY_X + (PLAY_W / 2);
        int yTop = PLAY_Y;
        int yBottom = PLAY_Y + PLAY_H - 1;
        int yMid = PLAY_Y + (PLAY_H / 2);

        int xLeft = PLAY_X;
        int xRight = PLAY_X + PLAY_W - 1;

        UiDraw.fillRectAlpha(g, xMid, yTop, 1, lenV + 1, color, alpha);
        UiDraw.fillRectAlpha(g, xMid, yBottom - lenV, 1, lenV + 1, color, alpha);
        UiDraw.fillRectAlpha(g, xLeft, yMid, lenH + 1, 1, color, alpha);
        UiDraw.fillRectAlpha(g, xRight - lenH, yMid, lenH + 1, 1, color, alpha);
    }

    // Spell effect: "blind service" vignette mask around the player.
    private void renderBlindServiceOverlay(GameCore.BattleRenderAccess a, Graphics g) {
        int maskAlpha = a.blindMaskAlpha();
        int spellId = a.blindMaskSpellId();
        if (maskAlpha <= 0 || spellId < 26 || spellId > 29) {
            return;
        }
        ImageBank imgs = a.imgs();
        if (imgs == null) {
            return;
        }

        int cx = PLAY_X + (PLAY_W / 2);
        int cy = PLAY_Y + (PLAY_H / 2);
        Player player = a.player();
        if (player != null) {
            cx = player.getXFixed() >> 16;
            cy = player.getYFixed() >> 16;
        }

        int imageIndex = spellId < 28 ? 38 : 37;
        int size = spellId < 28 ? 140 : 120;
        int half = size >> 1;
        int alpha = maskAlpha << 1;
        if (alpha > 255) {
            alpha = 255;
        }

        Image overlay = imgs.getAlphaImageColorKey(imageIndex, alpha, 0xFF00FF);
        if (overlay == null) {
            overlay = imgs.getAlphaImage(imageIndex, alpha);
        }
        if (overlay != null) {
            drawEkiImage(g, overlay, cx - half, cy - half, 0, 0, size, size);
        }

        int top = cy - half;
        int bottom = cy + half;
        int right = cx + half;
        if (top > PLAY_Y) {
            UiDraw.fillRectAlpha(g, PLAY_X, PLAY_Y, PLAY_W, top - PLAY_Y, 0x000000, alpha);
        }
        if (bottom < PLAY_Y + PLAY_H) {
            UiDraw.fillRectAlpha(g, PLAY_X, bottom, PLAY_W, PLAY_Y + PLAY_H - bottom, 0x000000, alpha);
        }
        if (cx - half > PLAY_X) {
            UiDraw.fillRectAlpha(g, PLAY_X, top, cx - half - PLAY_X, size, 0x000000, alpha);
        }
        if (right < PLAY_X + PLAY_W) {
            UiDraw.fillRectAlpha(g, right, top, PLAY_X + PLAY_W - right, size, 0x000000, alpha);
        }
    }

    // Lazily build a PLAY_W x PLAY_H checkerboard overlay for the death flash effect.
    private Image getDeathCheckerboard() {
        if (deathCheckerboard != null) {
            return deathCheckerboard;
        }
        try {
            int w = PLAY_W;
            int h = PLAY_H;
            int[] rgb = new int[w * h];
            // Original draws 0xFF7F7F lines on odd columns and odd rows.
            // Equivalent: pixels where (col is odd) OR (row is odd) get the color.
            int color = 0x80FF7F7F;
            for (int y = 0; y < h; y++) {
                int rowOdd = y & 1;
                int base = y * w;
                for (int x = 0; x < w; x++) {
                    if (rowOdd != 0 || (x & 1) != 0) {
                        rgb[base + x] = color;
                    }
                }
            }
            deathCheckerboard = Image.createRGBImage(rgb, w, h, true);
        } catch (Throwable t) {
            // Fall through; callers handle null.
        }
        return deathCheckerboard;
    }

    // Boss HUD: HP bar, name, remaining lives/stocks, timer, and current spell name.
    private void renderBossHud(GameCore.BattleRenderAccess a, Graphics g) {
        if (a.bossController().getHpBarAnimK() != 1) {
            return;
        }

        if (a.dialogueVisible() || a.taCnt() != 0) {
            return;
        }

        int[][] enemylist = a.enemylist();

        int idx = -1;
        idx = a.bossController().getHpBarTargetIndex();
        if (idx < 0 || idx >= 32 || enemylist[idx][0] == 0) {
            idx = -1;
        } else {
            int mt = enemylist[idx][11];
            if (mt < 101 || mt > 103) {
                idx = -1;
            }
        }
        if (idx == -1) {
            for (int i = 0; i < 32; i++) {
                if (enemylist[i][0] == 0) {
                    continue;
                }
                int mt = enemylist[i][11];
                if (mt >= 101 && mt <= 103) {
                    idx = i;
                    break;
                }
            }
        }
        if (idx == -1) {
            for (int i = 0; i < 32; i++) {
                if (enemylist[i][0] == 0) {
                    continue;
                }
                int mt = enemylist[i][11];
                if (mt >= 100 && mt <= 103) {
                    idx = i;
                    break;
                }
            }
        }
        if (idx < 0) {
            return;
        }

        int barX = 4;
        int barY = 13;
        int barW = 172;
        int barH = 2;

        int mode = a.bossController().getHpBarAnimMode();
        int o0 = a.bossController().getHpBarAnimO0();
        int o2 = a.bossController().getHpBarAnimO2();
        if (o2 <= 0) {
            o2 = 1;
        }

        if (o0 < 0) {
            o0 = 0;
        }
        if (o0 > o2) {
            o0 = o2;
        }

        switch (mode) {
            case 0: {
                int w = (o0 * barW) / o2;
                if (w != 0) {
                    g.setColor(0x000000);
                    g.fillRect(barX + 1, barY + 1, w, barH);
                    g.setColor(0xFFFFFF);
                    g.fillRect(barX, barY, w, barH);
                }
                break;
            }
            case 1: {
                int p0 = a.bossController().getHpBarAnimP0();
                int p2 = a.bossController().getHpBarAnimP2();
                if (p2 <= 0) {
                    p2 = 1;
                }

                int redW = (p0 * (((barW << 2) / 10))) / p2;
                if (redW < 0) {
                    redW = 0;
                }
                if (redW > barW) {
                    redW = barW;
                }

                if (redW != 0) {
                    g.setColor(0x000000);
                    g.fillRect(barX + 1, barY + 1, redW, barH);
                    g.setColor(0xFF0000);
                    g.fillRect(barX, barY, redW, barH);
                }

                if (p0 >= p2) {
                    int x = barX + redW;
                    int w = (o0 * (barW * 6 / 10)) / o2;
                    if (w < 0) {
                        w = 0;
                    }
                    if (w > barW - redW) {
                        w = barW - redW;
                    }
                    if (w != 0) {
                        g.setColor(0x000000);
                        g.fillRect(x + 1, barY + 1, w, barH);
                        g.setColor(0xFFFFFF);
                        g.fillRect(x, barY, w, barH);
                    }
                }
                break;
            }
            case 2: {
                int w = (o0 * (((barW << 2) / 10))) / o2;
                if (w != 0) {
                    g.setColor(0x000000);
                    g.fillRect(barX + 1, barY + 1, w, barH);
                    g.setColor(0xFF0000);
                    g.fillRect(barX, barY, w, barH);
                }
                break;
            }
            case 3: {
                int w = (o0 * barW) / o2;
                if (w != 0) {
                    g.setColor(0x000000);
                    g.fillRect(barX + 1, barY + 1, w, barH);
                    g.setColor(0xFF0000);
                    g.fillRect(barX, barY, w, barH);
                }
                break;
            }
        }

        String bossName = null;
        if (a.stage() == 0) {
            bossName = resolveStage1BossHudName(a);
        }
        if (bossName == null) {
            int nameIdx = a.bossController().getBossNameIndex();
            if (nameIdx < 0 || nameIdx >= BOSS_NAMES.length) {
                nameIdx = 0;
            }
            bossName = BOSS_NAMES[nameIdx];
        }
        UiDraw.drawString2(g, null, bossName, 2, 26, 0, 0xFFFFFF, 0x000000);

        ImageBank imgs = a.imgs();
        int stock = a.bossStock();
        if (stock < 0) {
            stock = 0;
        }
        if (stock > 10) {
            stock = 10;
        }
        Image starImg = (imgs != null) ? imgs.get(5) : null;
        if (starImg != null) {
            for (int i = 0; i < stock; ++i) {
                UiDraw.drawRegion(g, starImg, 2 + i * 6 + 1, 28, 96, 31, 5, 5);
            }
        }

        int bspellcnt = a.bossController().getBspellcnt();
        String timeText;
        int timeColor;
        if (bspellcnt < 0) {
            timeText = TIMER_STRINGS[0];
            timeColor = 0xFF0000;
        } else if (bspellcnt > 99) {
            timeText = TIMER_STRINGS[99];
            timeColor = 0x3F9FFF;
        } else {
            timeText = TIMER_STRINGS[bspellcnt];
            if (bspellcnt < 10) {
                timeColor = 0xFF0000;
            } else if (bspellcnt < 20) {
                timeColor = 0x7F007F;
            } else {
                timeColor = 0x3F9FFF;
            }
        }
        UiDraw.drawString2(g, null, timeText, 193, 20, 2, timeColor, 0x000000);

        if (a.bossController().getBspellstep() != 0) {
            int spellId = a.bossController().getSpellId();
            if (spellId < 0 && a.startSpellPractice()) {
                spellId = a.startSpellId();
            }

            // Limit spell name width to 2/3 of the battle playfield and ellipsize (no scrolling).
            int maxW = (PLAY_W * 2) / 3;
            if (maxW > 8) {
                maxW -= 8;
            }
            String spellName = a.getSpellCardName(spellId);

            // Only ellipsize for languages that can overflow horizontally.
            // Extend by adding language ids here if needed.
            boolean ellipsize = (I18n.getLanguageId() == I18n.LANG_EN);
            if (ellipsize) {
                spellName = UiDraw.ellipsize(null, spellName, maxW);
            }
            UiDraw.drawString2(g, null, spellName, 189, 43, 2, 0xFF0033, 0x770033);
        }
    }

    // Indicator for bosses near the bottom edge (stage 1 only uses multiple bosses).
    private void renderBossTrackIndicators(GameCore.BattleRenderAccess a, Graphics g) {
        ImageBank imgs = a.imgs();
        if (imgs == null) {
            return;
        }
        Image img = imgs.get(6);
        if (img == null) {
            return;
        }

        int[][] enemylist = a.enemylist();

        int xMax = PLAY_W << 16;
        int yMin = PLAY_Y << 16;
        int yMax = (PLAY_Y + PLAY_H) << 16;

        for (int i = 0; i < 32; i++) {
            if (enemylist[i][0] == 0) {
                continue;
            }
            int mt = enemylist[i][11];
            if (mt != 100 && mt != 101 && mt != 102 && mt != 103) {
                continue;
            }
            int xFixed = enemylist[i][5];
            int yFixed = enemylist[i][6];
            if (xFixed <= 0 || xFixed >= xMax) {
                continue;
            }
            if (yFixed <= yMin || yFixed >= yMax) {
                continue;
            }

            int x = (xFixed >> 16) - 8;
            UiDraw.drawRegion(g, img, PLAY_X + x, PLAY_Y + PLAY_H, 156, 76, 16, 5);

            if (a.stage() != 0) {
                break;
            }
        }
    }

    private String resolveStage1BossHudName(GameCore.BattleRenderAccess a) {
        switch (a.bossF()) {
            case 1:
            case 2:
            case 3:
                return "Sunny Luna";
            case 10:
            case 11:
            case 12:
            case 40:
            case 41:
            case 42:
                return BOSS_NAMES[1];
            case 13:
            case 14:
            case 15:
            case 43:
            case 44:
            case 45:
                return BOSS_NAMES[2];
            case 19:
            case 20:
            case 21:
            case 37:
            case 38:
            case 39:
                return BOSS_NAMES[0];
            case 16:
            case 17:
            case 18:
                return BOSS_NAMES[3];
        }
        return null;
    }

    // Digit renderer for HUD numbers (right-aligned fixed width).
    private static void drawDigitsFixed(Graphics g, Image sheet, int v, int digits, int x, int y) {
        int[] arr = new int[digits];
        int n = v;
        if (n < 0) {
            n = 0;
        }
        for (int i = 0; i < digits; i++) {
            arr[i] = n % 10;
            n /= 10;
        }

        int px = x + digits * 5;
        for (int j = 0; j < digits; j++) {
            px -= 5;
            UiDraw.drawRegion(g, sheet, px, y, arr[j] * 5, 100, 5, 8);
        }
    }

    // Star icon renderer for life/bomb counters.
    private static void drawStars(Graphics g, Image sheet, int srcX, int count, int x, int y) {
        if (count < 0) {
            count = 0;
        }
        if (count > 7) {
            count = 7;
        }
        for (int i = 0; i < count; i++) {
            UiDraw.drawRegion(g, sheet, x + i * 6, y, srcX, 108, 7, 7);
        }
    }

    // Clipped draw helper for large images (portraits / overlays) limited to the playfield.
    private void drawEkiImage(Graphics g, Image img, int x, int y, int srcX, int srcY, int w, int h) {
        if (img == null || !BattleRenderRules.clipToPlayfield(
                x, y, srcX, srcY, w, h, clippedPlayfieldRegion)) {
            return;
        }
        int dx = clippedPlayfieldRegion[0];
        int dy = clippedPlayfieldRegion[1];
        srcX = clippedPlayfieldRegion[2];
        srcY = clippedPlayfieldRegion[3];
        w = clippedPlayfieldRegion[4];
        h = clippedPlayfieldRegion[5];

        int oldClipX = g.getClipX();
        int oldClipY = g.getClipY();
        int oldClipW = g.getClipWidth();
        int oldClipH = g.getClipHeight();

        try {
            int clipX = PLAY_X;
            int clipY = PLAY_Y;
            int clipW = PLAY_W;
            int clipH = PLAY_H;

            if (oldClipW > 0 && oldClipH > 0) {
                int ox1 = oldClipX + oldClipW;
                int oy1 = oldClipY + oldClipH;
                int px1 = PLAY_X + PLAY_W;
                int py1 = PLAY_Y + PLAY_H;

                int ix0 = (oldClipX > clipX) ? oldClipX : clipX;
                int iy0 = (oldClipY > clipY) ? oldClipY : clipY;
                int ix1 = (ox1 < px1) ? ox1 : px1;
                int iy1 = (oy1 < py1) ? oy1 : py1;
                clipW = ix1 - ix0;
                clipH = iy1 - iy0;
                clipX = ix0;
                clipY = iy0;
                if (clipW <= 0 || clipH <= 0) {
                    return;
                }
            }

            int dx1 = dx + w;
            int dy1 = dy + h;
            int cx1 = clipX + clipW;
            int cy1 = clipY + clipH;
            int ix0 = (dx > clipX) ? dx : clipX;
            int iy0 = (dy > clipY) ? dy : clipY;
            int ix1 = (dx1 < cx1) ? dx1 : cx1;
            int iy1 = (dy1 < cy1) ? dy1 : cy1;
            int iw = ix1 - ix0;
            int ih = iy1 - iy0;
            if (iw <= 0 || ih <= 0) {
                return;
            }

            g.setClip(ix0, iy0, iw, ih);
            g.drawImage(img, dx - srcX, dy - srcY, Graphics.TOP | Graphics.LEFT);
        } finally {
            g.setClip(oldClipX, oldClipY, oldClipW, oldClipH);
        }
    }

    // Draws the static frame around the playfield (top border + bottom border).
    public void renderBattleFrame(Graphics g, ImageBank imgs) {
        Image top = (imgs != null) ? imgs.get(22) : null;
        if (top != null) {
            UiDraw.drawRegion(g, top, 0, 0, 0, 0, 194, 8);
        }

        Image bottom = (imgs != null) ? imgs.get(23) : null;
        if (bottom != null) {
            UiDraw.drawRegion(g, bottom, 0, 234, 0, 0, 240, 6);
        }
    }

    // Draws the right-side HUD background panel.
    public void renderBattleHudPanel(Graphics g, ImageBank imgs) {
        if (imgs == null) {
            return;
        }

        int hudX = PLAY_X + PLAY_W;
        int hudW = LOGICAL_W - hudX;

        int oldClipX = g.getClipX();
        int oldClipY = g.getClipY();
        int oldClipW = g.getClipWidth();
        int oldClipH = g.getClipHeight();

        try {
            g.setClip(hudX, 0, hudW, LOGICAL_H);

            Image top = imgs.get(20);
            if (top != null) {
                UiDraw.drawRegion(g, top, 194, 0, 0, 0, 46, 123);
            }
            Image bottom = imgs.get(21);
            if (bottom != null) {
                UiDraw.drawRegion(g, bottom, 194, 123, 0, 0, 46, 111);
            }
        } finally {
            g.setClip(oldClipX, oldClipY, oldClipW, oldClipH);
        }
    }
}
