package touhou.battle;

/** Immutable settings for one battle session, including its retry stage. */
public final class BattleSessionConfig {
    public static final int MODE_SPELL_PRACTICE = 3;

    private final int gameMode;
    private final int difficulty;
    private final int character;
    private final int shotType;
    private final int runStartStage;
    private final int stage;
    private final boolean practiceBoss;
    private final int spellId;

    private BattleSessionConfig(int gameMode, int difficulty, int character, int shotType,
            int runStartStage, int stage, boolean practiceBoss, int spellId) {
        this.gameMode = gameMode;
        this.difficulty = difficulty;
        this.character = character;
        this.shotType = shotType;
        this.runStartStage = runStartStage;
        this.stage = stage;
        this.practiceBoss = practiceBoss;
        this.spellId = spellId;
    }

    public static BattleSessionConfig standard(int gameMode, int difficulty, int character,
            int shotType, int startStage, boolean practiceBoss) {
        return new BattleSessionConfig(gameMode, difficulty, character, shotType,
                startStage, startStage, practiceBoss, -1);
    }

    public static BattleSessionConfig spellPractice(int character, int shotType, int spellId) {
        if (!SpellPracticeCatalog.isValid(spellId)) {
            throw new IllegalArgumentException("Invalid spell practice id: " + spellId);
        }
        int stage = SpellPracticeCatalog.getStage(spellId);
        return new BattleSessionConfig(MODE_SPELL_PRACTICE, 0, character, shotType,
                stage, stage, true, spellId);
    }

    public BattleSessionConfig atStage(int nextStage) {
        if (isSpellPractice()) {
            throw new IllegalArgumentException("Spell practice cannot advance stages");
        }
        return new BattleSessionConfig(gameMode, difficulty, character, shotType,
                runStartStage, nextStage, practiceBoss, spellId);
    }

    public BattleSessionConfig atRunStartWithoutBossSkip() {
        if (isSpellPractice()) {
            throw new IllegalArgumentException("Spell practice has no run restart");
        }
        return new BattleSessionConfig(gameMode, difficulty, character, shotType,
                runStartStage, runStartStage, false, spellId);
    }

    public int getGameMode() {
        return gameMode;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public int getCharacter() {
        return character;
    }

    public int getShotType() {
        return shotType;
    }

    public int getRunStartStage() {
        return runStartStage;
    }

    public int getStage() {
        return stage;
    }

    public boolean isPracticeBoss() {
        return practiceBoss;
    }

    public boolean isSpellPractice() {
        return gameMode == MODE_SPELL_PRACTICE;
    }

    public int getSpellId() {
        return spellId;
    }

    public int getContinueGameMode() {
        return isSpellPractice() ? 0 : gameMode;
    }
}
