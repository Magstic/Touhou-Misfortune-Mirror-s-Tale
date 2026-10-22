package touhou.ui;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

import touhou.UnlockFlags;
import touhou.i18n.TextId;
import touhou.i18n.UiText;
import touhou.replay.ReplaySlotStore;

public final class LoadingScreen {
    private static final int MIN_DISPLAY_MS = 500;

    private boolean done;
    private long shownAt;

    public void enter() {
        done = false;
        shownAt = 0;
    }

    public boolean isDone() {
        return done && System.currentTimeMillis() - shownAt >= MIN_DISPLAY_MS;
    }

    public void update() {
        // Let the first loading frame reach the screen before touching RMS.
        if (shownAt == 0 || done) {
            return;
        }
        if (!UnlockFlags.isReplayRmsWarmed() && ReplaySlotStore.warmup()) {
            UnlockFlags.setReplayRmsWarmed(true);
        }
        done = true;
    }

    public void render(Graphics g) {
        if (g == null) {
            return;
        }

        g.setColor(0x000000);
        g.fillRect(0, 0, 240, 240);

        Font f = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
        g.setFont(f);
        g.setColor(0xFFFFFF);

        String title = UiText.get(TextId.LOADING_TITLE);
        int w = UiDraw.stringWidth(f, title);
        int x = (240 - w) >> 1;
        int y = 110;
        UiDraw.drawStringPlain(g, f, title, x, y, 0, 0xFFFFFF);

        int barW = 160;
        int barH = 10;
        int bx = (240 - barW) >> 1;
        int by = 125;

        g.setColor(0xFFFFFF);
        g.drawRect(bx - 1, by - 1, barW + 1, barH + 1);

        g.setColor(0x0000FF);
        g.fillRect(bx, by, done ? barW : 0, barH);
        if (shownAt == 0) {
            shownAt = System.currentTimeMillis();
        }
    }
}
