package touhou.ui;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

import touhou.bt.BtSppLink;
import touhou.i18n.TextId;
import touhou.i18n.UiText;

/** Shared presentation rules for Bluetooth replay send/receive screens. */
final class ReplayTransferUi {
    private ReplayTransferUi() {
    }

    static String statusText(int status, boolean cancelRequested, boolean receiving) {
        if (cancelRequested) {
            return UiText.get(TextId.BT_MSG_CANCELING);
        }
        if (status == BtSppLink.STATUS_SELECT_SERVICE) {
            return UiText.get(TextId.BT_STATUS_SELECT_SERVICE);
        }
        if (status == BtSppLink.STATUS_CONNECTING) {
            return UiText.get(TextId.BT_STATUS_CONNECTING);
        }
        if (status == BtSppLink.STATUS_CONNECTED) {
            return UiText.get(TextId.BT_STATUS_CONNECTED);
        }
        if (receiving && status == BtSppLink.STATUS_RECEIVING) {
            return UiText.get(TextId.BT_STATUS_RECEIVING);
        }
        if (!receiving && status == BtSppLink.STATUS_SENDING) {
            return UiText.get(TextId.BT_STATUS_SENDING);
        }
        if (status == BtSppLink.STATUS_DONE) {
            return UiText.get(TextId.BT_STATUS_DONE);
        }
        if (status == BtSppLink.STATUS_CANCELED) {
            return UiText.get(TextId.BT_STATUS_CANCELED);
        }
        return UiText.get(TextId.BT_STATUS_WAITING);
    }

    static String deviceText(String address, String name) {
        boolean hasAddress = address != null && address.length() != 0;
        boolean hasName = name != null && name.length() != 0;
        if (!hasAddress && !hasName) {
            return "";
        }
        String text = UiText.get(TextId.BT_LABEL_DEVICE) + ": ";
        if (hasName) {
            text += name;
            if (hasAddress) {
                text += " (" + address + ")";
            }
            return text;
        }
        return text + address;
    }

    static void renderProgress(Graphics g, Font font, Font mediumFont,
            int boxY, int boxH, String status, String device,
            int completed, int total, int bytesPerSecond, int animationTick) {
        int boxX = 16;
        int boxW = 208;
        int layoutExtra = boxH - 106;
        if (layoutExtra < 0) {
            layoutExtra = 0;
        }
        int deviceOffset = 34 + layoutExtra / 3;
        int lowerOffset = (layoutExtra * 2) / 3;

        UiDraw.fillRectAlpha(g, boxX, boxY, boxW, boxH, 0x000000, 160);
        g.setColor(0x00AA00);
        g.drawRect(boxX, boxY, boxW - 1, boxH - 1);

        UiDraw.drawString2(g, mediumFont, status, 120, boxY + 18, 1, 0xFFFFFF, 0x222222);
        if (device != null && device.length() != 0) {
            UiDraw.drawStringMarquee2(g, font, device, boxX + 8, boxY + deviceOffset,
                    boxW - 16, 1, 0xDDDDDD, 0x222222, animationTick);
        }

        g.setColor(0x005500);
        g.drawLine(boxX + 8, boxY + 44 + lowerOffset, boxX + boxW - 8, boxY + 44 + lowerOffset);

        int barX = boxX + 12;
        int barY = boxY + 50 + lowerOffset;
        int barW = boxW - 24;
        int barH = 12;
        UiDraw.fillRectAlpha(g, barX, barY, barW, barH, 0x001A00, 200);
        g.setColor(0x005500);
        g.drawRect(barX, barY, barW - 1, barH - 1);

        int filledWidth = ReplayTransferProgress.barWidth(completed, total, barW);
        if (filledWidth > 2) {
            UiDraw.fillRectAlpha(g, barX + 1, barY + 1, filledWidth - 2, barH - 2, 0x00FF00, 160);
        }
        int barMidY = barY + ((barH - UiDraw.fontHeight(font)) >> 1) + UiDraw.fontBaseline(font);
        UiDraw.drawString2(g, font, ReplayTransferProgress.percent(completed, total) + "%",
                120, barMidY, 1, 0xFFFFFF, 0x003300);

        String bytes = completed + "/" + (total > 0 ? String.valueOf(total) : "?") + "B";
        UiDraw.drawString2(g, font, bytes, boxX + 12, boxY + 78 + lowerOffset, 0, 0xAAAAAA, 0x222222);
        UiDraw.drawString2(g, font, ReplayTransferProgress.formatSpeed(bytesPerSecond),
                boxX + boxW - 12, boxY + 78 + lowerOffset, 2, 0xAAAAAA, 0x222222);
        UiDraw.drawString2(g, font, "[LS] " + UiText.get(TextId.BT_HINT_CANCEL),
                120, boxY + 96 + lowerOffset, 1, 0x558855, 0x112211);
    }

}
