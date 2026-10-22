package touhou.stage;

import java.io.IOException;

import touhou.ResourceLoader;
import touhou.ResourcePaths;

public final class StageDat {
    public static final int FRAME = 0;
    public static final int OP = 1;
    public static final int UNLOCK = 243;
    public static final int LATTER_HALF = 244;
    public static final int RESTART = 245;
    public static final int DIALOGUE = 246;
    public static final int BOSS_START = 247;
    public static final int BOSS_ATTACK = 248;
    public static final int STAGE_CLEAR = 249;
    public static final int EFFECT = 250;
    public static final int BGM = 251;
    public static final int EXTRA_CLEAR = 252;
    public static final int SAVE_PROGRESS = 253;
    public static final int FLASH = 254;
    public static final int STORY_CLEAR = 255;

    private final short[][][] table;

    private StageDat(short[][][] table) {
        this.table = table;
    }

    public short[][][] getTable() {
        return table;
    }

    public static StageDat load() throws IOException {
        return parse(ResourceLoader.readRequiredBytes(ResourcePaths.STAGE_DATA));
    }

    private static StageDat parse(byte[] b) throws IOException {
        if (b == null || b.length == 0) {
            throw new IOException("Invalid stage.dat (empty)");
        }

        int n = 0;
        int scriptCount = b[n++] & 0xFF;
        if (scriptCount == 0 || n + scriptCount > b.length) {
            throw new IOException("Invalid stage.dat (script table)");
        }

        short[][][] table = new short[scriptCount][][];

        for (int i = 0; i < table.length; i++) {
            int steps = b[n++] & 0xFF;
            table[i] = new short[steps][];
        }

        for (int j = 0; j < table.length; j++) {
            for (int k = 0; k < table[j].length; k++) {
                if (n >= b.length) {
                    throw new IOException("Invalid stage.dat (row table)");
                }
                int len = b[n++] & 0xFF;
                table[j][k] = new short[len];
            }
        }

        for (int l = 0; l < table.length; l++) {
            for (int n2 = 0; n2 < table[l].length; n2++) {
                for (int n3 = 0; n3 < table[l][n2].length; n3++) {
                    if (n > b.length - 2) {
                        throw new IOException("Invalid stage.dat (payload)");
                    }
                    table[l][n2][n3] = (short) (((b[n] & 0xFF) << 8) + (b[n + 1] & 0xFF));
                    n += 2;
                }
            }
        }

        if (n != b.length) {
            throw new IOException("Invalid stage.dat (trailing data)");
        }

        return new StageDat(table);
    }
}
