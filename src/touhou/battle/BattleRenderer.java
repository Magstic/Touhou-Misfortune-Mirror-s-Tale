package touhou.battle;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import touhou.AdditiveBackground;
import touhou.Debug;
import touhou.GameCore;
import touhou.ImageBank;
import touhou.Player;
import touhou.SpriteCatalog;
import touhou.SpriteRenderer;
import touhou.SpriteSheetCatalog;
import touhou.Trig;
import touhou.i18n.I18n;
import touhou.i18n.TextId;
import touhou.i18n.UiText;
import touhou.stage.BossController;
import touhou.stage.StageAssets;
import touhou.stage.StageRuntime;
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

    // Boss display names used by the boss HUD.
    private static final String[] BOSS_NAMES = new String[] { "Sunny Milk", "Luna Child", "Star Sapphire", "Sunny Luna Star", "Mystia Lorelei", "Chen", "Keine Kamishirasawa", "Sakuya Izayoi", "Marisa Vision", "Alice Vision" };

    private int backgroundDarkness;
    private SpriteRenderer.AlphaSprite bossShield;
    // EX uses indexed pixels for additive mode and native images for simplified mode.
    private int stage6CacheD4 = -1;
    private int stage6CacheDarkness = -1;
    private boolean stage6CacheHeavy;
    private Image stage6CacheBaseImg;

    private Image stage6CacheOverlayImg;
    private int stage6CacheOverlayH;
    private AdditiveBackground stage6Blend;

    // Scratch arrays used by background renderers to sort draw order.
    private final int[] bgKeys4 = new int[4];
    private final int[] bgRanks4 = new int[4];
    private final int[] bgKeys3 = new int[3];
    private final int[] bgRanks3 = new int[3];
    private final int[] bgKeys24 = new int[24];
    private final int[] bgRanks24 = new int[24];
    private final int[] dialoguePortrait = new int[4];

    public void release() {
        backgroundDarkness = 0;
        bossShield = null;
        stage6CacheD4 = -1;
        stage6CacheDarkness = -1;
        stage6CacheBaseImg = null;
        stage6CacheOverlayImg = null;
        stage6Blend = null;
    }

    /**
     * Entry point for battle rendering.
     *
     * This method decides whether to render the live battle frame or the stage-clear
     * result panel, while keeping the underlying gameplay frame consistent.
     */
    public void render(GameCore.BattleRenderAccess a, Graphics g) {
        if (a.stageClearPanelActive()) {
            bossShield = null;
            StageClearResultPanel panel = a.stageClearResultPanel();
            if (panel.isLeaving()) {
                renderBattleUi(a, g);
            } else {
                ImageBank imgs = a.imgs();
                renderBattleHudPanel(g, imgs);
                renderBattleHudText(a, g);
                renderBattleFrame(g, imgs);
            }
            panel.render(g, a.sprites());
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
        int bulletRenderMs = 0;
        if (a.debugPerfEnabled()) {
            long br0 = System.currentTimeMillis();
            a.bullets().renderPlayerBullets(g);
            long br1 = System.currentTimeMillis();
            bulletRenderMs = (int) (br1 - br0);
        } else {
            a.bullets().renderPlayerBullets(g);
        }

        if (a.deadcnt() > 0) {
            UiDraw.fillCheckerboardAlpha(g, PLAY_X, PLAY_Y, PLAY_W, PLAY_H, 0xFF7F7F, 128);
        }
        g.setClip(0, 0, LOGICAL_W, LOGICAL_H);

        Player player = a.player();
        if (player != null) {
            g.setClip(PLAY_X, PLAY_Y, PLAY_W, PLAY_H);
            player.render(g, a.sprites());
            g.setClip(0, 0, LOGICAL_W, LOGICAL_H);
        }

        g.setClip(PLAY_X, PLAY_Y, PLAY_W, PLAY_H);
        if (a.debugPerfEnabled()) {
            long br0 = System.currentTimeMillis();
            a.bullets().renderEnemyBullets(g);
            long br1 = System.currentTimeMillis();
            Debug.perfSetBulletRenderMs(bulletRenderMs + (int) (br1 - br0));
        } else {
            a.bullets().renderEnemyBullets(g);
        }
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
            a.spellPracticeEndScreen().render(g, a.sprites());
        }
    }

    /**
     * Renders enemies and boss sprites.
     *
     * Uses {@link SpriteCatalog} sprite definitions (cc indices) and stage/boss work arrays
     * to select the correct animation frames.
     */
    private void stageHb(final GameCore.BattleRenderAccess a, final Graphics g) {
        if (a.cc() == null || a.imgs() == null) {
            return;
        }
        int[][] enemylist = a.enemylist();
        boolean shielding = false;
        for (int i = 0; i < 32; i++) {
            if (enemylist[i][StageRuntime.ENEMY_ACTIVE] == 0) {
                continue;
            }
            int x = BattleRenderRules.fixedToScreen(a.originX(), enemylist[i][StageRuntime.ENEMY_X]);
            int y = BattleRenderRules.fixedToScreen(a.originY(), enemylist[i][StageRuntime.ENEMY_Y]);
            int auraSprite = a.enemyAuraSpriteId(i);
            if (auraSprite != BattleEnemyVisuals.NO_SPRITE) {
                drawSpriteAlpha(a, g, auraSprite, x, y, 127);
            }
            int spriteId = a.enemySpriteId(i);
            if (spriteId == BattleEnemyVisuals.NO_SPRITE) {
                continue;
            }
            int type = enemylist[i][StageRuntime.ENEMY_APPEARANCE];
            if ((type == 17 || type == 19) && !a.disableBossBombShieldCheat()
                    && a.bossVisualShieldActive() && a.bombCnt() > 0) {
                shielding = true;
                if (bossShield == null) bossShield = a.sprites().createAlphaSprite(spriteId);
                else bossShield.setSprite(spriteId);
                bossShield.draw(g, x, y, BattleEnemyVisuals.bossShieldAlpha(a.bombCnt()));
            } else {
                drawSprite(a, g, spriteId, x, y);
            }
        }
        if (!shielding) bossShield = null;
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

            int sprite = BattleRenderRules.bombPortraitSprite(a.currentChara());
            if (sprite >= 0 && d0 < 25) {
                int x = BattleRenderRules.bombOverlayX(a.currentChara(), d0);
                int y = BattleRenderRules.bombOverlayY(d0);
                a.sprites().draw(g, sprite, x, y);
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

            // Preserve the cached banner's one-pixel inset and baseline without a title image.
            UiDraw.drawStringOutline(g, null, s, 6, ty, 0, main, 0xFFFFFF);
        } finally {
            g.setClip(oldClipX, oldClipY, oldClipW, oldClipH);
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

        int colorMain = renderDialoguePortrait(g, a, portraitType, talkCnt);

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

    private int renderDialoguePortrait(Graphics g, GameCore.BattleRenderAccess a, int portraitType, int talkCounter) {
        if (!BattleRenderRules.dialoguePortrait(a.cc(), portraitType, talkCounter, dialoguePortrait)) return 0xFFFFFF;
        int cx = g.getClipX(), cy = g.getClipY(), cw = g.getClipWidth(), ch = g.getClipHeight();
        try {
            g.clipRect(PLAY_X, PLAY_Y, PLAY_W, PLAY_H);
            a.sprites().draw(g, dialoguePortrait[0], dialoguePortrait[1], dialoguePortrait[2]);
            if (portraitType == 6) a.sprites().draw(g, SpriteSheetCatalog.STAR_DIALOGUE_EXPRESSION,
                    dialoguePortrait[1], dialoguePortrait[2] + 46);
        } finally {
            g.setClip(cx, cy, cw, ch);
        }
        return dialoguePortrait[3];
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
     * Spell darkness is applied to background palettes instead of blending a full-screen layer.
     */
    private void renderBattleBackground(GameCore.BattleRenderAccess a, Graphics g) {
        backgroundDarkness = 0;
        if (a.closeUpAlphaEnabled()
                && (a.bombCnt() > 0 || (a.timeStop() <= 0 && a.bossController().getBspellstep() != 0
                && !a.bossSpellCutInActive()))) {
            backgroundDarkness++;
        }
        if (a.bossSpellCutInActive() && a.closeUpAlphaEnabled()
                && !a.dialogueVisible() && a.taCnt() == 0) {
            backgroundDarkness++;
        }
        // EX's additive mode darkens its final RGB output instead of its source layers.
        StageAssets.setBackgroundDarkness(a.imgs(), a.stage(),
                a.stage() == 6 && a.heavyBgEnabled() ? 0 : backgroundDarkness);
        int oldClipX = g.getClipX();
        int oldClipY = g.getClipY();
        int oldClipW = g.getClipWidth();
        int oldClipH = g.getClipHeight();

        try {
            g.setClip(PLAY_X, PLAY_Y, PLAY_W, PLAY_H);

            switch (a.stage()) {
                case StageRuntime.STAGE_1:
                    renderBgStage0(a, g);
                    break;
                case StageRuntime.STAGE_2:
                    renderBgStage1(a, g);
                    break;
                case StageRuntime.STAGE_3:
                    renderBgStage2(a, g);
                    break;
                case StageRuntime.STAGE_4:
                    renderBgStage3(a, g);
                    break;
                case StageRuntime.STAGE_5:
                    renderBgStage4(a, g);
                    break;
                case StageRuntime.STAGE_6:
                    renderBgStage5(a, g);
                    break;
                case StageRuntime.STAGE_EXTRA:
                    renderBgStage6(a, g);
                    break;
                default:
                    renderBgStage0(a, g);
                    break;
            }
        } finally {
            g.setClip(oldClipX, oldClipY, oldClipW, oldClipH);
        }
    }

    // Stage 0 (Stage 1) background: basic scrolling texture + a few large decorations.
    private void renderBgStage0(GameCore.BattleRenderAccess a, Graphics g) {
        ImageBank imgs = a.imgs();
        if (imgs == null) {
            g.setColor(ImageBank.darkenColor(0x7F7F7F, backgroundDarkness));
            g.fillRect(PLAY_X, PLAY_Y, PLAY_W, PLAY_H);
            return;
        }

        g.setColor(ImageBank.darkenColor(0x7F7F7F, backgroundDarkness));
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
                drawSprite(a, g, spriteId, x, y);
            }
        }
    }

    // Stage 1 (Stage 2) background: scrolling base texture with many decorations sorted by depth.
    private void renderBgStage1(GameCore.BattleRenderAccess a, Graphics g) {
        ImageBank imgs = a.imgs();
        if (imgs == null) {
            g.setColor(ImageBank.darkenColor(0x7F7F7F, backgroundDarkness));
            g.fillRect(PLAY_X, PLAY_Y, PLAY_W, PLAY_H);
            return;
        }

        g.setColor(ImageBank.darkenColor(0x7F7F7F, backgroundDarkness));
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
                drawSprite(a, g, spriteId, x, y);
            }

            int overlayImage = BattleRenderRules.backgroundOverlayImage(1, 0);
            Image overlay = imgs.get(overlayImage);
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
            g.setColor(ImageBank.darkenColor(0x7F7F7F, backgroundDarkness));
            g.fillRect(PLAY_X, PLAY_Y, PLAY_W, PLAY_H);
            return;
        }

        g.setColor(ImageBank.darkenColor(0x7F7F7F, backgroundDarkness));
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
                    drawSprite(a, g, 486, x - 90, y);
                    drawSprite(a, g, 487, x - 90, y);
                    drawSprite(a, g, 488, x - 90, y);
                    drawSprite(a, g, 489, x - 90, y);
                } else if (idx == 1) {
                    drawSprite(a, g, 486, x - 90, y);
                    drawSprite(a, g, 487, x - 90, y);
                    drawSprite(a, g, 488, x - 90, y);
                    drawSprite(a, g, 489, x - 90, y);
                    drawSprite(a, g, 486, x - 10 + 0, y);
                    drawSprite(a, g, 486, x - 10 + 21, y);
                    drawSprite(a, g, 486, x - 10 + 42, y);
                    drawSprite(a, g, 487, x - 10 + 42, y);
                }
                y -= 50;
                drawSprite(a, g, 485, x, y);
            }

            int scroll = (a.bgMoveSpd() << 1) % 240;
            Image left = imgs.get(72);
            if (left != null) {
                int x0 = PLAY_X + a.originX();
                int y0 = PLAY_Y + a.originY() + scroll;
                UiDraw.drawRegion(g, left, x0, y0, 0, 0, 73, 240);
                UiDraw.drawRegion(g, left, x0, y0 - 240, 0, 0, 73, 240);
            }

            UiDraw.fillRectAlpha(g, PLAY_X + a.originX() + 73, PLAY_Y + a.originY(), 40, PLAY_H,
                    ImageBank.darkenColor(0xCFE2FF, backgroundDarkness), 128);

            Image right = imgs.get(73);
            if (right != null) {
                int x0 = PLAY_X + a.originX() + 194 - 81;
                int y0 = PLAY_Y + a.originY() + scroll;
                UiDraw.drawRegion(g, right, x0, y0, 0, 0, 81, 240);
                UiDraw.drawRegion(g, right, x0, y0 - 240, 0, 0, 81, 240);
            }
        }
    }

    // Stage 3 (Stage 4) background: base + one overlay scrolling at twice the base speed.
    private void renderBgStage3(GameCore.BattleRenderAccess a, Graphics g) {
        ImageBank imgs = a.imgs();
        if (imgs == null) {
            g.setColor(ImageBank.darkenColor(0x303030, backgroundDarkness));
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
            g.setColor(ImageBank.darkenColor(0x303030, backgroundDarkness));
            g.fillRect(PLAY_X, PLAY_Y, PLAY_W, PLAY_H);
        }

        // One semi-transparent scrolling overlay.
        // Skipped in simplified-background mode.
        if (a.heavyBgEnabled()) {
            int overlayImage = BattleRenderRules.backgroundOverlayImage(3, 0);
            Image overlay = imgs.get(overlayImage);
            if (overlay != null) {
                int x0 = PLAY_X + a.originX();

                int scroll = a.bgMoveSpd() % 240;
                int y0 = PLAY_Y + a.originY() + scroll;
                g.drawImage(overlay, x0, y0, Graphics.TOP | Graphics.LEFT);
                g.drawImage(overlay, x0, y0 - 240, Graphics.TOP | Graphics.LEFT);
            }
        }
    }

    // Stage 4 (Stage 5): route backdrop transition, then only the boss backdrop + overlay.
    private void renderBgStage4(GameCore.BattleRenderAccess a, Graphics g) {
        if (a.timeStop() > 0) {
            // The opaque 240px clock covers the 194px playfield, including bomb shake (up to 15px).
            // Its gray tint is baked into the palette; underlying layers are fully occluded.
            int baseX = PLAY_X + a.originX();
            int t = a.timeStop() << 2;
            for (int i = 0; i < PLAY_H; i++) {
                int dx = baseX - 23 + (Trig.sin(t + i) >> 13);
                a.sprites().drawWindow(g, SpriteSheetCatalog.CLOCK_FACE, dx, PLAY_Y, 0, i, 240, 1, 255);
            }
            return;
        }
        ImageBank imgs = a.imgs();
        if (imgs == null) {
            g.setColor(ImageBank.darkenColor(0x303030, backgroundDarkness));
            g.fillRect(PLAY_X, PLAY_Y, PLAY_W, PLAY_H);
            return;
        }

        boolean boss = a.bossMode();
        int d0 = boss ? 240 : a.fi() >> 2;
        if (d0 > 240) {
            d0 = 240;
        }

        Image a0 = boss ? null : imgs.get(BattleRenderRules.backgroundBaseImage(4, 0));
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
            // Continue the boss texture below the viewport when bomb shake exposes its lower edge.
            if (boss) g.drawImage(a1, x0, y0 + 240, Graphics.TOP | Graphics.LEFT);
        }
        if (a0 == null && a1 == null) {
            g.setColor(ImageBank.darkenColor(0x303030, backgroundDarkness));
            g.fillRect(PLAY_X, PLAY_Y, PLAY_W, PLAY_H);
        }

        // Semi-transparent scrolling overlay layer.
        // Skipped in simplified-background mode.
        if (a.heavyBgEnabled()) {
            int overlayImage = BattleRenderRules.backgroundOverlayImage(4, 0);
            Image overlay = imgs.get(overlayImage);
            if (overlay != null) {
                int scroll = a.bgMoveSpd() % 240;
                int x0 = PLAY_X + a.originX();
                int y0 = PLAY_Y + a.originY() + scroll;
                g.drawImage(overlay, x0, y0, Graphics.TOP | Graphics.LEFT);
                g.drawImage(overlay, x0, y0 - 240, Graphics.TOP | Graphics.LEFT);
            }
        }
    }

    // Stage 5 (Stage 6) background: scrolling base + semi-random falling particles.
    private void renderBgStage5(GameCore.BattleRenderAccess a, Graphics g) {
        ImageBank imgs = a.imgs();
        if (imgs == null) {
            g.setColor(ImageBank.darkenColor(0x303030, backgroundDarkness));
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
            g.setColor(ImageBank.darkenColor(0x303030, backgroundDarkness));
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
                drawSprite(a, g, spriteId, x, y);
            }
        }
    }

    // Stage 6 (Extra Stage) background: additive blending between base and overlay layers.
    private void renderBgStage6(GameCore.BattleRenderAccess a, Graphics g) {
        boolean heavy = a.heavyBgEnabled();
        int d4 = (a.bgMoveSpd() >> 9) & 0x3;

        ensureStage6Cache(a.imgs(), d4, heavy);

        // Simplified-background mode: base image + overlay scrolling (normal draw).
        // Skips additive blend and ratio color oscillation.
        if (!heavy) {
            g.drawImage(stage6CacheBaseImg, 0, 0, Graphics.TOP | Graphics.LEFT);
            int scroll = (a.bgMoveSpd() >> 1) % stage6CacheOverlayH;
            int x0 = PLAY_X + a.originX();
            int y0 = PLAY_Y + a.originY() + scroll;
            g.drawImage(stage6CacheOverlayImg, x0, y0, Graphics.TOP | Graphics.LEFT);
            g.drawImage(stage6CacheOverlayImg, x0, y0 - stage6CacheOverlayH, Graphics.TOP | Graphics.LEFT);
            return;
        }

        int d3 = (a.bgMoveSpd() & 0x1FF);
        if (d3 > 384) {
            d3 = 511 - d3;
        } else if (d3 > 127) {
            d3 = 127;
        }

        int scroll = (a.bgMoveSpd() >> 1) % this.stage6CacheOverlayH;
        stage6Blend.draw(g, d3, scroll, a.originX(), a.originY(), backgroundDarkness);
    }

    // Keep only the current EX palette pair and its optional indexed compositor.
    private void ensureStage6Cache(ImageBank images, int phase, boolean heavy) {
        int darkness = heavy ? 0 : backgroundDarkness;
        if (stage6CacheD4 == phase && stage6CacheHeavy == heavy && stage6CacheDarkness == darkness) return;
        int previousPhase = stage6CacheD4 < 0 ? 0 : stage6CacheD4;
        if (previousPhase != phase) {
            images.release(BattleRenderRules.backgroundBaseImage(6, previousPhase));
            images.release(BattleRenderRules.backgroundOverlayImage(6, previousPhase));
        }
        stage6CacheBaseImg = null;
        stage6CacheOverlayImg = null;
        stage6Blend = null;

        stage6CacheD4 = phase;
        stage6CacheDarkness = darkness;
        stage6CacheHeavy = heavy;
        stage6CacheBaseImg = images.get(BattleRenderRules.backgroundBaseImage(6, phase));
        stage6CacheOverlayImg = images.get(BattleRenderRules.backgroundOverlayImage(6, phase));
        stage6CacheOverlayH = stage6CacheOverlayImg.getHeight();
        if (heavy) {
            stage6Blend = new AdditiveBackground(stage6CacheBaseImg, stage6CacheOverlayImg);
            images.release(BattleRenderRules.backgroundBaseImage(6, phase));
            images.release(BattleRenderRules.backgroundOverlayImage(6, phase));
            stage6CacheBaseImg = null;
            stage6CacheOverlayImg = null;
        }
    }

    /**
     * Draws a sprite by ccIndex with bounds checks.
     *
     * This is used by many systems (enemies, backgrounds, HUD decorations).
     */
    private void drawSprite(GameCore.BattleRenderAccess a, Graphics g, int id, int x, int y) {
        a.sprites().draw(g, id, x, y);
    }

    private void drawSpriteAlpha(GameCore.BattleRenderAccess a, Graphics g, int id, int x, int y, int alpha) {
        a.sprites().drawAlpha(g, id, x, y, alpha);
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

        SpriteRenderer sprites = a.sprites();

        int level = hud.getLevel();
        drawSprite(a, g, 185 + level, 216, 4);

        sprites.draw(g, SpriteSheetCatalog.HUD_LABEL_BASE + 0, 196, 13);
        drawDigitsFixed(g, sprites, hud.getHiScore(), 8, 197, 23);

        sprites.draw(g, SpriteSheetCatalog.HUD_LABEL_BASE + 1, 196, 32);
        drawDigitsFixed(g, sprites, hud.getScore(), 8, 197, 42);

        sprites.draw(g, SpriteSheetCatalog.HUD_LABEL_BASE + 2, 196, 51);
        drawStars(g, sprites, SpriteSheetCatalog.HUD_LIFE, hud.getPlayer() - 1, 196, 60);

        sprites.draw(g, SpriteSheetCatalog.HUD_LABEL_BASE + 3, 196, 69);
        drawStars(g, sprites, SpriteSheetCatalog.HUD_BOMB, hud.getBomb(), 196, 78);

        sprites.draw(g, SpriteSheetCatalog.HUD_LABEL_BASE + 4, 196, 87);
        drawDigitsFixed(g, sprites, hud.getPower(), BattleRenderRules.digitCount(hud.getPower(), 3), 197, 97);

        sprites.draw(g, SpriteSheetCatalog.HUD_LABEL_BASE + 5, 196, 106);
        drawDigitsFixed(g, sprites, hud.getGraze(), BattleRenderRules.digitCount(hud.getGraze(), 5), 197, 116);
    }

    // Boss spell cut-in: instant appearance/removal, with fixed alpha.
    private void renderBossSpellCutInOverlay(GameCore.BattleRenderAccess a, Graphics g) {
        if (!a.bossSpellCutInActive()) {
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

            SpriteRenderer sprites = a.sprites();
            int sprite = BattleRenderRules.bossSpellCutInSprite(a.bossSpellCutInStage(), a.gameMode(),
                    a.bossController().host().getSpellPracticeBossStep());
            int gc = a.fi() - a.bossSpellCutInStartFi();
            if (gc < 0) {
                gc = 0;
            }

            switch (a.bossSpellCutInStage()) {
                case StageRuntime.STAGE_1:
                    switch (a.bossSpellCutInChara()) {
                        case 108:
                            sprites.draw(g, SpriteSheetCatalog.SUNNY_CUTIN, 130, 20 - (gc >> 2));
                            break;
                        case 110:
                            sprites.draw(g, SpriteSheetCatalog.LUNA_CUTIN, 130, 20 - (gc >> 2));
                            break;
                        case 109:
                            sprites.draw(g, SpriteSheetCatalog.STAR_CUTIN, 0, 20 - (gc >> 2));
                            break;
                        case 111:
                            sprites.draw(g, SpriteSheetCatalog.STAR_CUTIN, 0, 20 - (gc >> 2));
                            sprites.draw(g, SpriteSheetCatalog.SUNNY_CUTIN, 64, gc >> 2);
                            sprites.draw(g, SpriteSheetCatalog.LUNA_CUTIN, 130, 20 - (gc >> 2));
                            break;
                    }
                    break;
                case StageRuntime.STAGE_2:
                    sprites.draw(g, sprite, 0, 8 + (gc >> 3));
                    break;
                case StageRuntime.STAGE_3:
                    sprites.draw(g, sprite, 14 - (gc >> 2), 8 + (gc >> 2));
                    break;
                case StageRuntime.STAGE_4:
                    sprites.draw(g, sprite, 0, gc >> 2);
                    break;
                case StageRuntime.STAGE_5:
                    sprites.draw(g, sprite, 0 - (gc >> 2), gc >> 2);
                    break;
                case StageRuntime.STAGE_6:
                    sprites.draw(g, sprite, -20 + (gc >> 1), gc >> 2);
                    break;
                case StageRuntime.STAGE_EXTRA:
                    sprites.draw(g, sprite, 0, 8 - (gc >> 2));
                    break;
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

        int size = spellId < 28 ? 140 : 120;
        int half = size >> 1;
        int alpha = maskAlpha << 1;
        if (alpha > 255) {
            alpha = 255;
        }

        a.sprites().drawMirroredColorKey(g, SpriteSheetCatalog.MASK_BASE + (spellId < 28 ? 1 : 0),
                cx - half, cy - half, alpha, 0xFF00FF);

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
        if (idx < 0 || idx >= 32 || enemylist[idx][StageRuntime.ENEMY_ACTIVE] == 0) {
            idx = -1;
        } else {
            int mt = enemylist[idx][StageRuntime.ENEMY_MOVE_PATTERN];
            if (mt < 101 || mt > 103) {
                idx = -1;
            }
        }
        if (idx == -1) {
            for (int i = 0; i < 32; i++) {
                if (enemylist[i][StageRuntime.ENEMY_ACTIVE] == 0) {
                    continue;
                }
                int mt = enemylist[i][StageRuntime.ENEMY_MOVE_PATTERN];
                if (mt >= BossController.BOSS_PATTERN && mt <= BossController.THIRD_BOSS_PATTERN) {
                    idx = i;
                    break;
                }
            }
        }
        if (idx == -1) {
            for (int i = 0; i < 32; i++) {
                if (enemylist[i][StageRuntime.ENEMY_ACTIVE] == 0) {
                    continue;
                }
                int mt = enemylist[i][StageRuntime.ENEMY_MOVE_PATTERN];
                if (mt >= BossController.MIDBOSS_PATTERN && mt <= BossController.THIRD_BOSS_PATTERN) {
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
        for (int i = 0; i < stock; i++) {
            a.sprites().draw(g, SpriteSheetCatalog.BOSS_STAR, 2 + i * 6 + 1, 28);
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
        Image img = imgs.get(10);
        if (img == null) {
            return;
        }

        int[][] enemylist = a.enemylist();

        int xMax = PLAY_W << 16;
        int yMin = PLAY_Y << 16;
        int yMax = (PLAY_Y + PLAY_H) << 16;

        for (int i = 0; i < 32; i++) {
            if (enemylist[i][StageRuntime.ENEMY_ACTIVE] == 0) {
                continue;
            }
            int mt = enemylist[i][StageRuntime.ENEMY_MOVE_PATTERN];
            if (mt != BossController.MIDBOSS_PATTERN && mt != BossController.BOSS_PATTERN && mt != BossController.SECOND_BOSS_PATTERN && mt != BossController.THIRD_BOSS_PATTERN) {
                continue;
            }
            int xFixed = enemylist[i][StageRuntime.ENEMY_X];
            int yFixed = enemylist[i][StageRuntime.ENEMY_Y];
            if (xFixed <= 0 || xFixed >= xMax) {
                continue;
            }
            if (yFixed <= yMin || yFixed >= yMax) {
                continue;
            }

            int x = (xFixed >> 16) - 8;
            a.sprites().draw(g, SpriteSheetCatalog.BOSS_MARKER, PLAY_X + x, PLAY_Y + PLAY_H);

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
    private static void drawDigitsFixed(Graphics g, SpriteRenderer sprites, int v, int digits, int x, int y) {
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
            sprites.draw(g, SpriteSheetCatalog.HUD_DIGIT_BASE + arr[j], px, y);
        }
    }

    // Star icon renderer for life/bomb counters.
    private static void drawStars(Graphics g, SpriteRenderer sprites, int id, int count, int x, int y) {
        if (count < 0) {
            count = 0;
        }
        if (count > 7) {
            count = 7;
        }
        for (int i = 0; i < count; i++) {
            sprites.draw(g, id, x + i * 6, y);
        }
    }

    // Draws the static frame around the playfield (top border + bottom border).
    public void renderBattleFrame(Graphics g, ImageBank imgs) {
        Image top = (imgs != null) ? imgs.get(41) : null;
        if (top != null) {
            UiDraw.drawRegion(g, top, 0, 0, 0, 0, 194, 8);
        }

        Image bottom = (imgs != null) ? imgs.get(42) : null;
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

            Image top = imgs.get(39);
            if (top != null) {
                UiDraw.drawRegion(g, top, 194, 0, 0, 0, 46, 123);
            }
            Image bottom = imgs.get(40);
            if (bottom != null) {
                UiDraw.drawRegion(g, bottom, 194, 123, 0, 0, 46, 111);
            }
        } finally {
            g.setClip(oldClipX, oldClipY, oldClipW, oldClipH);
        }
    }
}
