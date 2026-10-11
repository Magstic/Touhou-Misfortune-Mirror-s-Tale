package touhou;

/** Stable logical IDs; all asset names and geometry live in cc.dat. */
public final class SpriteSheetCatalog {
    public static final int PLAYER_IMAGES_BASE = 8;
    public static final int PLAYER_DIALOGUE_IMAGES_BASE = 111;
    public static final int PLAYER_OPTIONS_IMAGES_BASE = PLAYER_DIALOGUE_IMAGES_BASE + 3;
    public static final int PLAYER_SHOTS_IMAGES_BASE = PLAYER_OPTIONS_IMAGES_BASE + 3;
    public static final int PLAYER_BOMB_SHOTS_IMAGES_BASE = PLAYER_SHOTS_IMAGES_BASE + 3;
    public static final int MARISA_SHOT_LASERS_IMAGE = PLAYER_BOMB_SHOTS_IMAGES_BASE + 3;
    public static final int STAGE_OPENING_BASE = 400;
    public static final int STAGE_OPENING_FRAME_COUNT = 6;
    public static final int PLAYER_FRAME_COUNT = 9;
    public static final int CC_COUNT = 784;
    public static final int TITLE_GLYPHS_IMAGE = 96;
    public static final int TITLE_CAPTION_IMAGE = 97;
    public static final int HELP_ARROWS_IMAGE = 98;
    public static final int REIMU_SHOTS_IMAGE = 15;
    public static final int REIMU_BOMB_QUARTERS_IMAGE = 107;
    public static final int REIMU_BOMB_FIRST_IMAGE = 108;
    public static final int TITLE_GLYPH_BASE = CC_COUNT;
    public static final int TITLE_FRAME = TITLE_GLYPH_BASE + 5;
    public static final int TITLE_CAPTION = TITLE_FRAME + 1;
    public static final int HELP_ARROW_BASE = TITLE_CAPTION + 1;
    public static final int PLAYER_FRAME_BASE = HELP_ARROW_BASE + 2;
    public static final int DIALOGUE_BASE = PLAYER_FRAME_BASE + 3 * PLAYER_FRAME_COUNT;
    public static final int STAR_DIALOGUE_EXPRESSION = DIALOGUE_BASE + 13;
    public static final int PLAYER_AURA_BASE = STAR_DIALOGUE_EXPRESSION + 1;
    public static final int HUD_LABEL_BASE = PLAYER_AURA_BASE + 4;
    public static final int HUD_DIGIT_BASE = HUD_LABEL_BASE + 6;
    public static final int HUD_LIFE = HUD_DIGIT_BASE + 10;
    public static final int HUD_BOMB = HUD_LIFE + 1;
    public static final int BOSS_STAR = HUD_BOMB + 1;
    public static final int BOSS_MARKER = BOSS_STAR + 1;
    public static final int SETUP_BLOCK_BASE = BOSS_MARKER + 1;
    public static final int SETUP_DIGIT_BASE = SETUP_BLOCK_BASE + 5;
    public static final int LANGUAGE_ITEM_BASE = SETUP_DIGIT_BASE + 6;
    public static final int SUNNY_CUTIN = LANGUAGE_ITEM_BASE + 6;
    public static final int LUNA_CUTIN = SUNNY_CUTIN + 1;
    public static final int STAR_CUTIN = LUNA_CUTIN + 1;
    public static final int SINGLE_CUTIN_BASE = STAR_CUTIN + 1;
    public static final int MASK_BASE = SINGLE_CUTIN_BASE + 7;
    public static final int CLOCK_FACE = MASK_BASE + 2;
    public static final int BOMB_PORTRAIT_BASE = CLOCK_FACE + 1;
    public static final int SPRITE_COUNT = BOMB_PORTRAIT_BASE + 3;

    private SpriteSheetCatalog() {
    }
}
