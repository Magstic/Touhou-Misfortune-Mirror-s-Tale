package touhou.ui;

import touhou.replay.ReplayCodec;
import touhou.replay.ReplayFormat;
import touhou.replay.ReplayHeader;
import touhou.replay.ReplaySlotStore;

/** Owns the loaded metadata and snapshot availability for every replay slot. */
final class ReplaySlotListModel {
    private final ReplayHeader[] headers = new ReplayHeader[ReplayFormat.SLOT_COUNT];
    private final byte[][] names = new byte[ReplayFormat.SLOT_COUNT][ReplayFormat.NAME_SIZE];
    private final boolean[] hasStageSnapshot = new boolean[ReplayFormat.SLOT_COUNT];

    ReplaySlotListModel() {
        for (int i = 0; i < ReplayFormat.SLOT_COUNT; i++) {
            headers[i] = new ReplayHeader();
        }
    }

    boolean reload() {
        return ReplaySlotStore.loadAllSlotSummaries(headers, names, hasStageSnapshot);
    }

    int size() {
        return ReplayFormat.SLOT_COUNT;
    }

    int clampCursor(int cursor) {
        return MenuCursor.clamp(cursor, size());
    }

    ReplayHeader header(int slot) {
        if (slot < 0 || slot >= size()) {
            return null;
        }
        return headers[slot];
    }

    byte[] nameBytes(int slot) {
        if (slot < 0 || slot >= size()) {
            return null;
        }
        return names[slot];
    }

    boolean hasData(int slot) {
        ReplayHeader header = header(slot);
        return header != null && header.getFlag() != 0;
    }

    boolean hasBossOnly(int slot) {
        ReplayHeader header = header(slot);
        return header != null && header.getFlag() != 0 && header.getMode() != 3
                && header.getBossStartFrame() >= 0 && header.getBossStartFrame() < ReplayCodec.FRAME_COUNT
                && hasStageSnapshot[slot];
    }
}
