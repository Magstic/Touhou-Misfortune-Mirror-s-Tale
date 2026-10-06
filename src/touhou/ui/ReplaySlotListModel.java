package touhou.ui;

import touhou.replay.ReplayBossOnlyPolicy;
import touhou.replay.ReplayFormat;
import touhou.replay.ReplayHeader;
import touhou.replay.ReplaySlotStore;

/** Owns the loaded metadata and snapshot availability for every replay slot. */
final class ReplaySlotListModel {
    private final ReplayHeader[] headers = new ReplayHeader[ReplayFormat.SLOT_COUNT];
    private final byte[][] names = new byte[ReplayFormat.SLOT_COUNT][ReplayFormat.NAME_SIZE];
    private final boolean[] hasStageSnapshot = new boolean[ReplayFormat.SLOT_COUNT];
    private final boolean[] hasBossSnapshot = new boolean[ReplayFormat.SLOT_COUNT];
    private final boolean[] hasBossOnly = new boolean[ReplayFormat.SLOT_COUNT];

    ReplaySlotListModel() {
        for (int i = 0; i < ReplayFormat.SLOT_COUNT; i++) {
            headers[i] = new ReplayHeader();
        }
    }

    boolean reload() {
        boolean loaded = ReplaySlotStore.loadAllSlotSummaries(
                headers, names, hasStageSnapshot, hasBossSnapshot);
        for (int slot = 0; slot < ReplayFormat.SLOT_COUNT; slot++) {
            hasBossOnly[slot] = computeHasBossOnly(slot);
        }
        return loaded;
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
        return slot >= 0 && slot < size() && hasBossOnly[slot];
    }

    private boolean computeHasBossOnly(int slot) {
        ReplayHeader header = headers[slot];
        if (header == null || header.getFlag() == 0 || header.getBossStartFrame() < 0) {
            return false;
        }
        if (ReplayBossOnlyPolicy.isFastForwardBossOnlyStage(header.getStage())) {
            return hasStageSnapshot[slot];
        }
        return hasBossSnapshot[slot];
    }
}
