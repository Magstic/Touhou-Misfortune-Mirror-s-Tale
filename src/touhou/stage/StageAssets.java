package touhou.stage;

import touhou.GameOptions;
import touhou.ImageBank;
import touhou.SpriteRenderer;
import touhou.SpriteSheetCatalog;
import touhou.battle.BattleVisualState;

/** Shared stage sheets, separate route/boss assets and temporary encounter sheets. */
public final class StageAssets {
    private static final int[] EMPTY = new int[0];
    private static final int[] COMMON = { 9, 10, 38, 39, 40, 41, 42 };
    private static final int[] ROUTE = { 11 };
    private static final int[][] BACKGROUNDS = {
            { 45, 46, 47 }, { 45, 59, 60, 61, 62, 63 },
            { 45, 71, 72, 73, 74, 75, 76, 77 }, { 83, 92 },
            { 96, 97, 98 }, { 106, 107 }, { 114, 115 }
    };
    private static final int[][] BOSSES = {
            { 51, 52, 53 }, { 68 }, { 80, 81 }, { 86 }, { 101 }, { 102 }, { 112 }
    };
    private static final int[][] MIDBOSSES = {
            { 51 }, { 68 }, { 80, 81 }, EMPTY, EMPTY, EMPTY, EMPTY
    };
    private static final int[] MIDBOSS_EFFECTS = { 6, 9, 10, -1, -1, -1, -1 };
    private static final int[] LAST_WORD = { 87 };
    private static final int[] SPELL_IMAGES = { 54, 55, 56, 64, 65, 67, 79, 84, 85, 88, 91, 99, 100, 103, 104, 105, 113 };
    private static final int[][] PLAYERS = {
            { 13, 14, 15, 16, 29, 32, 33, 34 },
            { 18, 19, 20, 21, 22, 30, 35, 36 },
            { 24, 25, 26, 27, 31 }
    };
    private static final int[][] PLAYER_SHOT_ALPHA_IMAGES = {
            { 15, 16, 32 }, { 19, 20, 21, 22, 35, 36 }, { 26 }
    };
    private static final int[][] OPENINGS = {
            { 43, 44 }, { 57, 58 }, { 69, 70 }, { 89, 90 },
            { 94, 95 }, { 109, 110 }, { 116, 117 }
    };
    private static final int[] PORTRAITS = { 12, 17, 23, 48, 49, 50, 66, 78, 82, 93, 108, 111 };

    private int stage;
    private int bossStep;
    private boolean bossPhase;
    private boolean bossCutInAlpha;
    private boolean midbossActive;
    private boolean openingActive;
    private boolean dialogueActive;
    private final boolean[] activeSpellImages = new boolean[SPELL_IMAGES.length];

    public void enter(ImageBank images, SpriteRenderer sprites, int stage, int character, int bossStep,
            boolean bossOnly, int[] options) {
        this.stage = stage;
        setBackgroundDarkness(images, stage, 0);
        setBackgroundOpacity(images, stage, GameOptions.isHeavyBgEnabled(options));
        images.setOpacity(sprites.getImageIndex(SpriteSheetCatalog.BOMB_PORTRAIT_BASE + character),
                GameOptions.isBombOverlayAlphaEnabled(options) ? 128 : 255);
        int shotAlpha = GameOptions.isPlayerShotAlphaEnabled(options) ? 128 : 255;
        int[] shots = PLAYER_SHOT_ALPHA_IMAGES[character];
        for (int i = 0; i < shots.length; i++) images.setOpacity(shots[i], shotAlpha);
        this.bossStep = bossStep;
        bossPhase = bossOnly;
        bossCutInAlpha = GameOptions.isBossSpellCutInAlphaEnabled(options);
        midbossActive = false;
        openingActive = false;
        dialogueActive = false;
        for (int i = 0; i < activeSpellImages.length; i++) activeSpellImages[i] = false;
        int[][] groups = { COMMON, BACKGROUNDS[stage], PLAYERS[character], bossPhase ? bossImages() : ROUTE,
                bossOnly ? EMPTY : OPENINGS[stage] };
        images.retain(groups);
        for (int i = 0; i < groups.length; i++) load(images, groups[i]);

        for (int i = 0; i < 4; i++) sprites.precacheAlphaRegion(SpriteSheetCatalog.PLAYER_AURA_BASE + i, 128);
    }

    private int[] bossImages() {
        return stage == 3 && bossStep == 37 ? LAST_WORD : BOSSES[stage];
    }

    public static void setBackgroundDarkness(ImageBank images, int stage, int levels) {
        int[] backgrounds = BACKGROUNDS[stage];
        for (int i = 0; i < backgrounds.length; i++) images.setDarkness(backgrounds[i], levels);
        if (stage == 4) images.setDarkness(100, levels);
    }

    public static void setBackgroundOpacity(ImageBank images, int stage, boolean heavy) {
        int alpha = heavy ? 128 : 255;
        switch (stage) {
            case 1: images.setOpacity(59, alpha); break;
            case 2:
                images.setOpacity(72, alpha);
                images.setOpacity(73, alpha);
                break;
            case 3: images.setOpacity(83, heavy ? 127 : 255); break;
            case 4:
                images.setOpacity(98, alpha);
                images.setTint(100, 0x80BFBFBF);
                break;
            case 5: images.setOpacity(107, alpha); break;
        }
    }

    public void update(ImageBank images, boolean bossMode,
            boolean dialogueVisible, int[][] effects, int[][] enemies) {
        if (bossMode && !bossPhase) {
            release(images, ROUTE);
            release(images, OPENINGS[stage]);
            load(images, bossImages());
            bossPhase = true;
            midbossActive = false;
        }
        if (!bossPhase) {
            boolean visible = false;
            for (int i = 0; i < enemies.length; i++) {
                if (enemies[i][0] != 0 && enemies[i][11] == 100) {
                    visible = true;
                    break;
                }
            }
            for (int i = 0; i < effects.length && !visible; i++) {
                visible = effects[i][0] != 0 && effects[i][3] == MIDBOSS_EFFECTS[stage];
            }
            if (visible && !midbossActive) load(images, MIDBOSSES[stage]);
            if (!visible && midbossActive) release(images, MIDBOSSES[stage]);
            midbossActive = visible;
        }
        if (dialogueActive && !dialogueVisible) release(images, PORTRAITS);
        dialogueActive = dialogueVisible;

        boolean visible = false;
        for (int i = 0; i < effects.length; i++) {
            // Effect IDs 23..36 are the seven pairs of opening animations.
            if (effects[i][0] != 0 && effects[i][3] >= 23 && effects[i][3] <= 36) {
                visible = true;
                break;
            }
        }
        if (openingActive && !visible) release(images, OPENINGS[stage]);
        openingActive = visible;
    }

    public void updateSpellAssets(ImageBank images, SpriteRenderer sprites, BossController boss,
            BattleVisualState visuals, int cutInCharacter, int timeStop, int[][] enemies, int[][] effects) {
        boolean orbs = false;
        boolean spheres = false;
        for (int i = 0; i < enemies.length; i++) {
            if (enemies[i][0] == 0) continue;
            int type = enemies[i][1];
            if (type == 20 || type == 21) orbs = true;
            if (type == 22) spheres = true;
        }
        for (int i = 0; i < effects.length && !spheres; i++) {
            spheres = effects[i][0] != 0 && effects[i][3] == 22;
        }

        boolean spellActive = boss.getBspellstep() != 0;
        int spellId = boss.getSpellId();
        int maskImage = visuals.getBlindAlpha() > 0 ? (visuals.getBlindSpellId() < 28 ? 65 : 64) : -1;
        int offset = stage <= 3 ? stage - 1 : stage;
        if (stage == 3 && bossStep == 37) offset = 3;
        int cutInSprite = SpriteSheetCatalog.SINGLE_CUTIN_BASE + offset;
        int cutInImage = stage == 0 ? -1 : sprites.getImageIndex(cutInSprite);
        for (int i = 0; i < SPELL_IMAGES.length; i++) {
            int image = SPELL_IMAGES[i];
            boolean needed;
            switch (image) {
                case 54:
                case 55:
                case 56:
                    int character = image == 55 ? 108 : image == 56 ? 110 : 109;
                    needed = stage == 0 && spellActive && (cutInCharacter == character || cutInCharacter == 111);
                    break;
                case 64:
                case 65:
                    needed = image == maskImage;
                    break;
                case 84:
                case 85:
                    // These summons also appear in non-spell attacks.
                    needed = orbs;
                    break;
                case 100:
                    needed = stage == 4 && (timeStop > 0 || (spellActive && spellId >= 62 && spellId <= 73));
                    break;
                case 103:
                case 104:
                    // Keep the sheets until the last sphere explosion finishes.
                    needed = spheres;
                    break;
                default:
                    needed = spellActive && image == cutInImage;
                    break;
            }
            if (needed && !activeSpellImages[i]) {
                if (image == cutInImage || (stage == 0 && image >= 54 && image <= 56)) {
                    images.setOpacity(image, bossCutInAlpha ? 128 : 255);
                }
                images.get(image);
            }
            if (!needed && activeSpellImages[i]) images.release(image);
            activeSpellImages[i] = needed;
        }
    }

    private static void release(ImageBank images, int[] ids) {
        for (int i = 0; i < ids.length; i++) images.release(ids[i]);
    }

    private static void load(ImageBank images, int[] ids) {
        for (int i = 0; i < ids.length; i++) images.get(ids[i]);
    }
}
