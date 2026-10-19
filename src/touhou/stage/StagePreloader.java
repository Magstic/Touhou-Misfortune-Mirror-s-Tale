package touhou.stage;

import touhou.SpriteSheetCatalog;
import touhou.SpriteRenderer;
import touhou.ImageBank;

public final class StagePreloader {
    private int stage;
    private int chara;
    private boolean playerShotAlphaEnabled = true;
    private boolean bossSpellCutInAlphaEnabled = true;
    private int step;
    private int stepMax;
    private boolean done;

    public void setPlayerShotAlphaEnabled(boolean enabled) {
        playerShotAlphaEnabled = enabled;
    }

    public void setBossSpellCutInAlphaEnabled(boolean enabled) {
        bossSpellCutInAlphaEnabled = enabled;
    }

    public void enter(int nextStage, int chara) {
        stage = nextStage;
        this.chara = chara;
        step = 0;
        done = false;
        // Keep this in sync with tickOneStep().
        stepMax = 12;
    }

    public boolean isDone() {
        return done;
    }

    public int getStepMax() {
        return stepMax;
    }

    public void update(ImageBank imgs, SpriteRenderer sprites) {
        if (done) {
            return;
        }
        if (imgs == null || sprites == null) throw new IllegalArgumentException("Sprite resources");
        tickOneStep(imgs, sprites);
    }

    private void tickOneStep(ImageBank imgs, SpriteRenderer sprites) {
        switch (step) {
            case 0:
                // Common HUD/frame images and always-present sprite sheets.
                imgs.get(39);
                imgs.get(40);
                imgs.get(41);
                imgs.get(42);

                // Effects / enemies / bullets+items.
                imgs.get(9);
                imgs.get(10);
                imgs.get(11);
                imgs.get(38);
                break;
            case 1:
                // Common base background texture (stage 0/1/2).
                imgs.get(45);
                break;
            case 2:
                // Stage background images.
                preloadStageBackground(imgs, stage);
                // Opening label and vertical subtitle: all three phases of each.
                int opening = SpriteSheetCatalog.STAGE_OPENING_BASE
                        + stage * SpriteSheetCatalog.STAGE_OPENING_FRAME_COUNT;
                preloadSpriteRange(imgs, sprites, opening,
                        opening + SpriteSheetCatalog.STAGE_OPENING_FRAME_COUNT - 1, 255);
                break;
            case 3:
                // Stage background decoration sprites (cc icons).
                preloadStageDecorationSprites(imgs, sprites, stage);
                break;
            case 4:
                // Common small enemy sprites.
                preloadSpriteRange(imgs, sprites, 0, 26, 255);
                break;
            case 5:
                // Enemy bullets and drop items; player assets belong to step 9.
                preloadSpriteRange(imgs, sprites, 81, 96, 255);
                preloadSpriteRange(imgs, sprites, 111, 165, 255);
                break;
            case 6:
                // Boss/enemy sprite ids commonly used by stage renderer.
                preloadSpriteRange(imgs, sprites, 166, 185, 255);

                // Boss sprites (enemy kinds 13..19 in BattleRenderer).
                // kind13
                preloadSpriteRange(imgs, sprites, 196, 215, 255);
                // kind14
                preloadSpriteRange(imgs, sprites, 216, 238, 255);
                // kind15 (includes +14 offset frames)
                preloadSpriteRange(imgs, sprites, 356, 379, 255);
                // Split orb and magic-circle sheets, including all seven orb frames.
                preloadSpriteRange(imgs, sprites, 380, 386, 255);
                preloadSpriteRange(imgs, sprites, 388, 391, 255);
                // kind16
                preloadSpriteRange(imgs, sprites, 504, 520, 255);

                // kind17/18/19 (boss main body parts)
                preloadSpriteRange(imgs, sprites, 711, 727, 255);
                // Split celestial spheres, including the shared white explosion flash and its tail.
                preloadSpriteRange(imgs, sprites, 494, 503, 255);
                // Common boss bullet sprites seen in stage boss patterns.
                preloadSpriteIds(imgs, sprites, new int[] { 81, 82, 84, 89, 585, 603, 639 }, 255);
                break;
            case 7:
                // Boss dialogue portraits (ImageBank indices) used by BattleRenderer.renderDialogue.
                preloadBossDialoguePortraitSprites(imgs, sprites);
                break;
            case 8:
                // Boss spell cut-in overlay images.
                preloadBossSpellCutInSprites(imgs, sprites, stage, bossSpellCutInAlphaEnabled);
                break;
            case 9:
                // Selected character: animation, options, ordinary shots and bomb shots.
                preloadPlayerAssets(imgs, sprites, chara, playerShotAlphaEnabled);
                break;
            case 10:
                // Selected character's bomb portrait; solid darkness draws directly.
                preloadBombPortrait(imgs, chara);
                break;
            case 11:
                // Final step: proactive GC to start stage with a clean heap.
                System.gc();
                break;
            default:
                done = true;
                return;
        }

        step++;
        if (step >= stepMax) {
            done = true;
        }
    }

    private static void preloadStageBackground(ImageBank imgs, int stage) {
        // Stage index: 0..6 (Stage1..Stage6 + StageEX).
        switch (stage) {
            case 0:
                imgs.get(45);
                break;
            case 1:
                imgs.get(45);
                imgs.get(59);
                imgs.getAlphaImage(59, 128);
                break;
            case 2:
                imgs.get(45);
                imgs.get(72);
                imgs.get(73);
                imgs.getAlphaImage(72, 128);
                imgs.getAlphaImage(73, 128);
                break;
            case 3:
                imgs.get(92);
                imgs.get(83);
                imgs.getAlphaImage(83, 127);
                break;
            case 4:
                imgs.get(96);
                imgs.get(97);
                imgs.get(98);
                imgs.get(100);
                imgs.getAlphaImage(98, 128);
                break;
            case 5:
                imgs.get(106);
                break;
            case 6:
                // StageEX base/overlay phases.
                imgs.get(114);
                imgs.get(128);
                imgs.get(129);
                imgs.get(130);
                imgs.get(115);
                imgs.get(131);
                imgs.get(132);
                imgs.get(133);
                break;
            default:
                break;
        }
    }

    private static void preloadBossDialoguePortraitSprites(ImageBank imgs, SpriteRenderer sprites) {
        preloadSpriteRange(imgs, sprites, SpriteSheetCatalog.DIALOGUE_BASE,
                SpriteSheetCatalog.DIALOGUE_BASE + 12, 255);
        preloadSpriteRange(imgs, sprites, SpriteSheetCatalog.STAR_DIALOGUE_EXPRESSION,
                SpriteSheetCatalog.STAR_DIALOGUE_EXPRESSION, 255);
    }

    private static void preloadBossSpellCutInSprites(ImageBank imgs, SpriteRenderer sprites, int stage, boolean alphaEnabled) {
        int alpha = alphaEnabled ? 128 : 255;
        if (stage == 0) {
            preloadSpriteIds(imgs, sprites, new int[] {
                    SpriteSheetCatalog.SUNNY_CUTIN, SpriteSheetCatalog.LUNA_CUTIN, SpriteSheetCatalog.STAR_CUTIN }, alpha);
        } else if (stage >= 1 && stage <= 6) {
            int id = SpriteSheetCatalog.SINGLE_CUTIN_BASE + (stage <= 3 ? stage - 1 : stage);
            preloadSpriteRange(imgs, sprites, id, id, alpha);
            if (stage == 3) {
                int lastWord = SpriteSheetCatalog.SINGLE_CUTIN_BASE + 3;
                preloadSpriteRange(imgs, sprites, lastWord, lastWord, alpha);
            }
        }
    }

    private static void preloadPlayerAssets(ImageBank imgs, SpriteRenderer sprites, int chara, boolean playerShotAlphaEnabled) {
        imgs.get(SpriteSheetCatalog.PLAYER_IMAGES[chara]);
        imgs.get(SpriteSheetCatalog.PLAYER_OPTIONS_IMAGES[chara]);
        int shots = SpriteSheetCatalog.PLAYER_SHOTS_IMAGES[chara];
        int bombShots = SpriteSheetCatalog.PLAYER_BOMB_SHOTS_IMAGES[chara];
        imgs.get(shots);
        imgs.get(bombShots);
        for (int i = 0; i < 4; i++) sprites.precacheAlphaRegion(SpriteSheetCatalog.PLAYER_AURA_BASE + i, 128);

        // Additional projectile sheets for this character.
        if (chara == 0) {
            imgs.get(SpriteSheetCatalog.REIMU_SHOTS_IMAGE);
            imgs.get(SpriteSheetCatalog.REIMU_BOMB_QUARTERS_IMAGE);
            imgs.get(SpriteSheetCatalog.REIMU_BOMB_FIRST_IMAGE);
        } else if (chara == 1) {
            imgs.get(SpriteSheetCatalog.MARISA_SHOT_LASERS_IMAGE);
            imgs.get(35);
            imgs.get(36);
        }
        if (!playerShotAlphaEnabled) return;
        // BulletSystem.drawFast uses sheet alpha caches, not compact sprite-region caches.
        imgs.getAlphaImage(shots, 128);
        imgs.getAlphaImage(bombShots, 128);
        if (chara == 0) {
            imgs.getAlphaImage(SpriteSheetCatalog.REIMU_SHOTS_IMAGE, 128);
            imgs.getAlphaImage(SpriteSheetCatalog.REIMU_BOMB_QUARTERS_IMAGE, 128);
            imgs.getAlphaImage(SpriteSheetCatalog.REIMU_BOMB_FIRST_IMAGE, 128);
        } else if (chara == 1) {
            imgs.getAlphaImage(SpriteSheetCatalog.MARISA_SHOT_LASERS_IMAGE, 128);
            imgs.getAlphaImage(35, 128);
            imgs.getAlphaImage(36, 128);
            for (int id = 100; id <= 102; id++) sprites.precacheAlphaRegion(id, 128);
        }
    }

    private static void preloadStageDecorationSprites(ImageBank imgs, SpriteRenderer sprites, int stage) {
        if (stage == 0) {
            preloadSpriteIds(imgs, sprites, new int[] { 191, 192 }, 255);
            return;
        }
        if (stage == 1) {
            preloadSpriteIds(imgs, sprites, new int[] { 351, 352, 353, 354, 355 }, 255);
            return;
        }
        if (stage == 2) {
            preloadSpriteIds(imgs, sprites, new int[] { 485, 486, 487, 488, 489 }, 255);
            return;
        }
        if (stage == 5) {
            // Stage 5 falling particles.
            preloadSpriteIds(imgs, sprites, new int[] { 728, 729 }, 128);
            return;
        }
    }

    private static void preloadBombPortrait(ImageBank imgs, int chara) {
        // Preload the bomb overlay to avoid first-bomb stutter.
        int bombImgIndex = 29 + chara;
        if (bombImgIndex >= 29 && bombImgIndex <= 31) {
            imgs.get(bombImgIndex);
        }
    }

    private static void preloadSpriteIds(ImageBank imgs, SpriteRenderer sprites, int[] ids, int alpha) {
        if (ids == null) {
            return;
        }
        for (int i = 0; i < ids.length; i++) {
            int id = ids[i];
            int imgIndex = sprites.getImageIndex(id);
            imgs.get(imgIndex);
            if (alpha != 255) {
                sprites.precacheAlphaRegion(id, alpha);
            }
        }
    }

    private static void preloadSpriteRange(ImageBank imgs, SpriteRenderer sprites, int idFrom, int idTo, int alpha) {
        for (int id = idFrom; id <= idTo; id++) {
            int imgIndex = sprites.getImageIndex(id);
            imgs.get(imgIndex);
            if (alpha != 255) {
                sprites.precacheAlphaRegion(id, alpha);
            }
        }
    }
}
