package touhou.ui;

import java.util.Vector;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

import touhou.ImageBank;
import touhou.SpriteSheetCatalog;
import touhou.i18n.I18n;
import touhou.i18n.TextId;
import touhou.i18n.UiText;

public final class StoryScreen {
    private static final int TEXT_X = 24;
    private static final int TEXT_Y = 47;
    private static final int TEXT_W = 198;
    private static final int TEXT_H = 178;
    private static final int TRANSITION_FRAMES = 10;
    private static final int HOLD_DELAY = 6;
    private static final int FLAKE_COUNT = 6;

    private final int[] flakeYfp = new int[FLAKE_COUNT];
    private final FontTextMeasurer textMeasurer = new FontTextMeasurer();
    private Font font;
    private String[] wrapped;
    private int lineH;
    private int maxScrollFp;

    // Pixel fixed-point, with eight fractional bits.
    private int scrollPosFp;
    private int scrollVelFp;
    private int scrollTargetFp;
    private boolean scrollTargetActive;
    private int holdDirection;
    private int holdTicks;
    private int transitionFrame;
    private boolean leaving;
    private int flakeTick;

    public void enter() {
        font = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
        textMeasurer.setFont(font);
        lineH = UiDraw.fontHeight(font) + 5;

        Vector wrappedLines = new Vector();
        String[] lines = I18n.splitLines(I18n.loadText("story.txt"));
        for (int i = 0; i < lines.length; i++) {
            String[] parts = UiTextLayout.wrap(lines[i], TEXT_W, textMeasurer);
            for (int j = 0; j < parts.length; j++) {
                wrappedLines.addElement(parts[j]);
            }
        }
        wrapped = new String[wrappedLines.size()];
        wrappedLines.copyInto(wrapped);
        int contentH = (wrapped.length - 1) * lineH + UiDraw.fontHeight(font);
        maxScrollFp = Math.max(0, contentH - TEXT_H) << 8;

        scrollPosFp = 0;
        scrollVelFp = 0;
        scrollTargetFp = 0;
        scrollTargetActive = false;
        holdDirection = 0;
        holdTicks = 0;
        transitionFrame = 0;
        leaving = false;
        flakeTick = 0;
        for (int i = 0; i < FLAKE_COUNT; i++) {
            flakeYfp[i] = ((i * 43 + 29) % 240) << 8;
        }
    }

    public boolean update(int keys, int pressed) {
        updateSnow();
        if (UiInput.cancel(pressed)) {
            leaving = true;
        }
        if (leaving) {
            if (transitionFrame > 0) {
                transitionFrame--;
                return false;
            }
            return true;
        }
        if (transitionFrame < TRANSITION_FRAMES) {
            transitionFrame++;
            return false;
        }

        if (UiInput.left(pressed)) {
            requestScrollByLines(-6);
        } else if (UiInput.right(pressed) || UiInput.secondary(pressed) || UiInput.confirm(pressed)) {
            requestScrollByLines(6);
        } else if (UiInput.up(pressed)) {
            requestScrollByLines(-1);
        } else if (UiInput.down(pressed)) {
            requestScrollByLines(1);
        }

        int direction = 0;
        if (UiInput.up(keys) != UiInput.down(keys)) {
            direction = UiInput.up(keys) ? -1 : 1;
        }
        if (direction != holdDirection) {
            holdDirection = direction;
            holdTicks = 0;
        }
        if (direction != 0) {
            holdTicks = Math.min(holdTicks + 1, 70);
        }

        if (direction != 0 && holdTicks > HOLD_DELAY) {
            scrollTargetActive = false;
            int speed = 256 + Math.min((holdTicks - HOLD_DELAY) * 20, 1280);
            scrollVelFp += (direction * speed - scrollVelFp) / 4;
            scrollPosFp += scrollVelFp;
        } else if (scrollTargetActive) {
            int remaining = scrollTargetFp - scrollPosFp;
            int step = remaining / 4;
            if (step == 0) {
                scrollPosFp = scrollTargetFp;
                scrollTargetActive = false;
            } else {
                scrollPosFp += step;
            }
        } else {
            scrollVelFp = scrollVelFp * 3 / 4;
            if (scrollVelFp > -16 && scrollVelFp < 16) {
                scrollVelFp = 0;
            }
            scrollPosFp += scrollVelFp;
        }

        if (scrollPosFp <= 0 || scrollPosFp >= maxScrollFp) {
            scrollPosFp = Math.max(0, Math.min(scrollPosFp, maxScrollFp));
            scrollVelFp = 0;
            scrollTargetActive = false;
        }
        return false;
    }

    private void requestScrollByLines(int delta) {
        int start = scrollTargetActive ? scrollTargetFp : scrollPosFp;
        scrollTargetFp = Math.max(0, Math.min(start + ((delta * lineH) << 8), maxScrollFp));
        scrollTargetActive = true;
        scrollVelFp = 0;
        holdTicks = 0;
    }

    public void render(Graphics g, ImageBank imgs) {
        UiDraw.drawRegion(g, imgs.get(SpriteSheetCatalog.ICE_BACKGROUND_IMAGE), 0, 0, 0, 0, 240, 240);
        UiDraw.fillRectAlpha(g, 0, 0, 240, 240, 0x081629, 194);

        int remaining = TRANSITION_FRAMES - transitionFrame;
        int hiddenH = 240 * remaining * remaining * remaining
                / (TRANSITION_FRAMES * TRANSITION_FRAMES * TRANSITION_FRAMES);
        int oldX = g.getClipX();
        int oldY = g.getClipY();
        int oldW = g.getClipWidth();
        int oldH = g.getClipHeight();
        g.clipRect(0, hiddenH / 2, 240, 240 - hiddenH);
        drawSnow(g);
        UiDraw.fillRectAlpha(g, 0, 10, 240, 26, 0x030912, 174);
        UiDraw.drawStringPlain(g, font, UiText.get(TextId.STORY_TITLE), 120,
                17 + UiDraw.fontBaseline(font), 1, 0xD8E5FF);
        drawText(g);
        drawScrollBar(g);
        g.setClip(oldX, oldY, oldW, oldH);
    }

    private void drawText(Graphics g) {
        int oldX = g.getClipX();
        int oldY = g.getClipY();
        int oldW = g.getClipWidth();
        int oldH = g.getClipHeight();
        g.clipRect(TEXT_X, TEXT_Y, TEXT_W, TEXT_H);

        int scrollY = scrollPosFp >> 8;
        int index = scrollY / lineH;
        int baseline = UiDraw.fontBaseline(font);
        int y = TEXT_Y - scrollY % lineH;
        while (index < wrapped.length && y < TEXT_Y + TEXT_H) {
            int color = y < TEXT_Y || y + UiDraw.fontHeight(font) > TEXT_Y + TEXT_H
                    ? 0x7E91B1 : 0xE6EDFC;
            UiDraw.drawStringPlain(g, font, wrapped[index], TEXT_X, y + baseline, 0, color);
            y += lineH;
            index++;
        }
        g.setClip(oldX, oldY, oldW, oldH);
    }

    private void drawScrollBar(Graphics g) {
        if (maxScrollFp == 0) {
            return;
        }
        int contentH = TEXT_H + (maxScrollFp >> 8);
        int knobH = Math.max(8, TEXT_H * TEXT_H / contentH);
        int knobY = TEXT_Y + (int) ((long) (TEXT_H - knobH) * scrollPosFp / maxScrollFp);
        g.setColor(0x36485E);
        g.fillRect(12, TEXT_Y, 1, TEXT_H);
        g.setColor(0xBEDBFF);
        g.fillRect(11, knobY, 3, knobH);
    }

    private void updateSnow() {
        flakeTick++;
        for (int i = 0; i < FLAKE_COUNT; i++) {
            flakeYfp[i] += 64 + (i % 3) * 32;
            if (flakeYfp[i] > (243 << 8)) {
                flakeYfp[i] = -3 << 8;
            }
        }
    }

    private void drawSnow(Graphics g) {
        for (int i = 0; i < FLAKE_COUNT; i++) {
            int phase = (flakeTick + i * 7) & 31;
            int drift = (phase < 16 ? phase : 31 - phase) / 8;
            int x = (i & 1) == 0 ? 5 + drift : 232 - drift;
            int y = flakeYfp[i] >> 8;
            g.setColor((i & 1) == 0 ? 0x687E9E : 0x859CBD);
            g.fillRect(x - 2, y, 5, 1);
            g.fillRect(x, y - 2, 1, 5);
        }
    }
}
