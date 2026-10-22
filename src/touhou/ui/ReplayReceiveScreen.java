package touhou.ui;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

import touhou.ImageBank;
import touhou.bt.BtSppLink;
import touhou.i18n.TextId;
import touhou.i18n.UiText;
import touhou.replay.ReplayShareCodec;
import touhou.replay.ReplayShareService;
import touhou.replay.ReplaySlotData;

public final class ReplayReceiveScreen {
    private static final Font FONT = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
    private static final Font FONT_M = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_MEDIUM);

    private static final int SCENE_RECEIVING = 0;
    private static final int SCENE_MESSAGE = 1;
    private static final int SCENE_RECV_CONFIRM = 2;

    // Worker thread reports results through these volatile fields.
    private volatile int pendingScene;
    private volatile String pendingMsg;
    private volatile ReplaySlotData pendingDecoded;

    // Progress is reported as read bytes/total bytes.
    private volatile int recvDone;
    private volatile int recvTotal;

    // Status information from BtSppLink.
    private volatile int btStatus;
    private volatile String btRemoteAddr;
    private volatile String btRemoteName;
    private volatile boolean cancelRequested;

    // Used to ignore stale callbacks from previous worker threads.
    private int sessionId;
    private final ReplayTransferProgress progress = new ReplayTransferProgress();

    private int scene;
    private int targetSlot;

    private int decoTick;

    private String msg;

    private ReplaySlotData receivedReplay;

    private final ReplayPreview preview = new ReplayPreview();

    private BtSppLink.CancelRef cancelRef;

    public void enter(int slot) {
        sessionId++;
        scene = SCENE_RECEIVING;
        targetSlot = slot;
        msg = null;
        receivedReplay = null;

        pendingScene = -1;
        pendingMsg = null;
        pendingDecoded = null;

        preview.set(null, null);

        recvDone = 0;
        recvTotal = 0;

        btStatus = BtSppLink.STATUS_IDLE;
        btRemoteAddr = null;
        btRemoteName = null;
        cancelRequested = false;
        progress.reset();

        startWorker();
    }

    public boolean update(int pressed) {
        decoTick++;
        applyPendingFromWorker();
        progress.update(System.currentTimeMillis(), recvDone);

        if (UiInput.cancel(pressed)) {
            if (scene == SCENE_RECEIVING) {
                requestCancel();
            } else if (scene == SCENE_RECV_CONFIRM) {
                // Reject received data, go back
                receivedReplay = null;
            }
            return true;
        }

        if (scene == SCENE_MESSAGE) {
            if (UiInput.dismiss(pressed)) {
                return true;
            }
            return false;
        }

        if (scene == SCENE_RECV_CONFIRM) {
            if (UiInput.confirm(pressed)) {
                doSaveToSlot(targetSlot);
            }
            return false;
        }

        return false;
    }

    public int getLeftSoftkeyTextId() {
        return scene == SCENE_RECEIVING || scene == SCENE_RECV_CONFIRM
                ? TextId.SOFTKEY_CANCEL : TextId.SOFTKEY_BACK;
    }

    public void render(Graphics g, ImageBank imgs) {
        ReplayUi.renderBackground(g, imgs);

        UiDraw.drawString2(g, FONT, UiText.get(TextId.BT_RECEIVE_TITLE), 120, 25, 1, 0xFFFFFF, 0x777777);

        if (scene == SCENE_RECEIVING) {
            renderReceiving(g);
        } else if (scene == SCENE_RECV_CONFIRM) {
            renderRecvConfirm(g);
        } else {
            renderMessage(g);
        }
    }

    private void renderReceiving(Graphics g) {
        ReplayTransferUi.renderProgress(g, FONT, FONT_M, 42, 112,
                ReplayTransferUi.statusText(btStatus, cancelRequested, true),
                ReplayTransferUi.deviceText(btRemoteAddr, btRemoteName),
                recvDone, recvTotal, progress.getBytesPerSecond(), decoTick);
    }

    private void renderRecvConfirm(Graphics g) {
        // Preview card at top
        renderPreviewCard(g);

        // Confirmation dialog below preview
        int dlgX = 16;
        int dlgY = 118;
        int dlgW = 208;
        int dlgH = 80;

        UiDraw.fillRectAlpha(g, dlgX, dlgY, dlgW, dlgH, 0x000000, 180);
        g.setColor(0x00FF00);
        g.drawRect(dlgX, dlgY, dlgW - 1, dlgH - 1);

        // "收到來自 [DeviceName] 的 Replay 數據"
        String devName = btRemoteName;
        if (devName == null || devName.length() == 0) {
            devName = btRemoteAddr;
        }
        if (devName == null || devName.length() == 0) {
            devName = "???";
        }
        String line1 = UiText.get(TextId.BT_RECV_FROM_PREFIX) + devName;
        String line2 = UiText.get(TextId.BT_RECV_FROM_SUFFIX);
        UiDraw.drawStringMarquee2(g, FONT, line1, dlgX + 8, dlgY + 18, dlgW - 16, 1, 0xDDDDDD, 0x222222, decoTick);
        UiDraw.drawString2(g, FONT, line2, 120, dlgY + 32, 1, 0xDDDDDD, 0x222222);

        // "是否接受？"
        UiDraw.drawString2(g, FONT_M, UiText.get(TextId.BT_RECV_CONFIRM), 120, dlgY + 50, 1, 0xFFFFFF, 0x222222);

        // Action hints
        String hintAccept = "[OK] " + UiText.get(TextId.BT_HINT_ACCEPT);
        String hintReject = "[LS] " + UiText.get(TextId.BT_HINT_REJECT);
        UiDraw.drawString2(g, FONT, hintAccept, 72, dlgY + 70, 1, 0x88FF88, 0x224422);
        UiDraw.drawString2(g, FONT, hintReject, 168, dlgY + 70, 1, 0x558855, 0x112211);
    }

    private void renderPreviewCard(Graphics g) {
        ReplayUi.renderPreviewCard(g, FONT, FONT_M, preview,
                UiText.get(TextId.BT_MSG_NO_DATA), decoTick);
    }

    private void renderMessage(Graphics g) {
        ReplayUi.renderMessageBox(g, FONT, FONT_M, msg, 104, decoTick);
    }

    private void startWorker() {
        if (!BtSppLink.isSupported()) {
            msg = UiText.get(TextId.BT_MSG_NOT_SUPPORTED);
            scene = SCENE_MESSAGE;
            return;
        }

        cancelRef = new BtSppLink.CancelRef();

        final int sid = sessionId;
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    byte[] p = BtSppLink.waitAndReceivePacket(new BtSppLink.ProgressListener() {
                        public void onProgress(int done, int total) {
                            if (sessionId != sid) {
                                return;
                            }
                            recvDone = done;
                            recvTotal = total;
                        }
                    }, cancelRef, new BtSppLink.StatusListener() {
                        public void onStatus(int status) {
                            if (sessionId != sid) {
                                return;
                            }
                            btStatus = status;
                        }

                        public void onRemoteDevice(String addr, String name) {
                            if (sessionId != sid) {
                                return;
                            }
                            btRemoteAddr = addr;
                            btRemoteName = name;
                        }
                    });

                    if (sessionId != sid) {
                        return;
                    }
                    if (p == null) {
                        pendingMsg = UiText.get(TextId.BT_MSG_CANCELED);
                        pendingScene = SCENE_MESSAGE;
                        return;
                    }

                    ReplaySlotData d = ReplayShareCodec.decode(p);
                    if (d == null || d.header == null) {
                        pendingMsg = UiText.get(TextId.BT_MSG_INVALID_DATA);
                        pendingScene = SCENE_MESSAGE;
                        return;
                    }

                    pendingDecoded = d;
                    pendingScene = SCENE_RECV_CONFIRM;
                } catch (Throwable t) {
                    if (sessionId != sid) {
                        return;
                    }
                    pendingMsg = UiText.get(cancelRequested ? TextId.BT_MSG_CANCELED : TextId.BT_MSG_ERROR);
                    pendingScene = SCENE_MESSAGE;
                }
            }
        });
        t.start();
    }

    private void requestCancel() {
        BtSppLink.CancelRef r = cancelRef;
        if (r != null) {
            cancelRequested = true;
            r.cancel();
        }
    }

    private void applyPendingFromWorker() {
        int ns = pendingScene;
        if (ns != -1) {
            pendingScene = -1;

            String m = pendingMsg;
            if (m != null) {
                pendingMsg = null;
                msg = m;
            }

            ReplaySlotData d = pendingDecoded;
            if (d != null) {
                pendingDecoded = null;
                receivedReplay = d;
                preview.set(d.header, d.name);
            }

            scene = ns;
        }
    }

    private void doSaveToSlot(int slot) {
        if (receivedReplay == null) {
            msg = UiText.get(TextId.BT_MSG_NO_DATA);
            scene = SCENE_MESSAGE;
            return;
        }
        boolean ok;
        try {
            ok = ReplayShareService.importToSlot(slot, receivedReplay);
        } catch (Throwable t) {
            ok = false;
        }
        msg = ok ? (UiText.get(TextId.BT_MSG_SAVED_PREFIX) + (slot + 1) + UiText.get(TextId.BT_MSG_SAVED_SUFFIX)) : UiText.get(TextId.BT_MSG_SAVE_FAILED);
        scene = SCENE_MESSAGE;
    }

}
