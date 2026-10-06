package touhou.ui;

import touhou.replay.ReplayHeader;

/** Cached display text for a replay preview card. */
final class ReplayPreview {
    private ReplayHeader header;
    private String name = "";
    private String unit = "";
    private String detail = "";
    private String score = "";

    void set(ReplayHeader header, byte[] nameBytes) {
        this.header = header;
        name = ReplayUi.decodeSlotName(nameBytes);
        unit = ReplayUi.formatUnit(header);
        detail = ReplayUi.formatDetail(header);
        score = header == null ? "" : String.valueOf(header.getScore());
    }

    boolean hasData() {
        return header != null && header.getFlag() != 0;
    }

    boolean isSpellPractice() {
        return hasData() && header.getMode() == 3;
    }

    String title() {
        return name.length() == 0 ? detail : name;
    }

    String unit() {
        return unit;
    }

    String score() {
        return score;
    }
}
