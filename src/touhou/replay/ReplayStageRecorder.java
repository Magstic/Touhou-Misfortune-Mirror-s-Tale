package touhou.replay;

public final class ReplayStageRecorder {
    private ReplayInputBuffer full;

    private final byte[] dialoguePageCounts = new byte[ReplayHeader.DIALOGUE_MAX];
    private int dialoguePageCountLen;

    private boolean recording;
    private int bossStartFrame = -1;

    public void reset() {
        if (full != null) {
            full.clear();
        }
        recording = false;
        bossStartFrame = -1;

        for (int i = 0; i < dialoguePageCounts.length; i++) {
            dialoguePageCounts[i] = 0;
        }
        dialoguePageCountLen = 0;
    }

    public void start() {
        if (full == null) {
            full = new ReplayInputBuffer();
        }
        reset();
        recording = true;
    }

    public void stop() {
        recording = false;
    }

    public boolean isRecording() {
        return recording;
    }

    public int getFramePos() {
        return full == null ? 0 : full.getPos();
    }

    public void markBossStart(int frameIndex) {
        if (!recording) {
            return;
        }
        if (bossStartFrame >= 0) {
            return;
        }
        if (frameIndex < 0) {
            frameIndex = 0;
        }
        if (frameIndex >= ReplayInputBuffer.FRAME_COUNT) {
            return;
        }
        bossStartFrame = frameIndex;
    }

    public void onFrame(int keys, int pressed, int fireActionPressed) {
        if (!recording) {
            return;
        }
        byte b = ReplayKeyCodec.encode(keys, pressed, fireActionPressed);
        full.putNext(b);
    }

    public void recordDialoguePageCount(int encounterIndex, int pageCount) {
        if (!recording) {
            return;
        }
        if (encounterIndex < 0 || encounterIndex >= ReplayHeader.DIALOGUE_MAX) {
            return;
        }
        if (pageCount < 0) {
            pageCount = 0;
        }
        if (pageCount > 255) {
            pageCount = 255;
        }
        dialoguePageCounts[encounterIndex] = (byte) (pageCount & 0xFF);
        if (encounterIndex >= dialoguePageCountLen) {
            dialoguePageCountLen = encounterIndex + 1;
        }
    }

    public byte[] encodeFull() {
        if (full == null) {
            full = new ReplayInputBuffer();
        }
        return ReplayCodec.encode(full.raw());
    }

    public void fillHeaderForSave(ReplayHeader header, int difficulty, int chara, int stage, int score, int type, int mode, int spellId) {
        header.clear();
        header.setFlag(ReplayHeader.FLAG_LOCAL);
        header.setDifficulty(difficulty);
        header.setChara(chara);
        header.setStage(stage);
        header.setScore(score);
        header.setType(type);
        header.setMode(mode);
        header.setSpellId(spellId);
        header.setBossStartFrame(bossStartFrame);

        header.setDialoguePageCounts(dialoguePageCounts, dialoguePageCountLen);
    }
}
