package touhou.ui;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

import touhou.ImageBank;
import touhou.bt.BtSppLink;
import touhou.i18n.TextId;
import touhou.i18n.UiText;
import touhou.replay.ReplayFormat;
import touhou.replay.ReplayHeader;
import touhou.replay.ReplayShareService;
import touhou.replay.ReplaySlotStore;

public final class ReplaySendScreen {
    private static final Font FONT = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
    private static final Font FONT_M = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_MEDIUM);

    private static final int SCENE_SENDING = 0;
    private static final int SCENE_MESSAGE = 1;

    // Worker thread reports results through these volatile fields.
    private volatile int pendingScene;
    private volatile String pendingMsg;

    // Progress is reported as written bytes/total bytes.
    private volatile int sendDone;
    private volatile int sendTotal;

    // Status information from BtSppLink.
    private volatile int btStatus;
    private volatile String btRemoteAddr;
    private volatile String btRemoteName;
    private volatile boolean cancelRequested;

    private int scene;
    private String msg;

    private int decoTick;

    private int slot;
    private final ReplayPreview preview = new ReplayPreview();
    private final ReplayTransferProgress progress = new ReplayTransferProgress();

    private BtSppLink.CancelRef cancelRef;

    // Used to ignore stale callbacks from previous worker threads.
    private int sessionId;

    public void enter(int slot) {
        sessionId++;
        this.slot = slot;
        this.scene = SCENE_SENDING;

        msg = null;
        pendingScene = -1;
        pendingMsg = null;

        sendDone = 0;
        sendTotal = 0;

        btStatus = BtSppLink.STATUS_IDLE;
        btRemoteAddr = null;
        btRemoteName = null;
        cancelRequested = false;

        progress.reset();

        ReplayHeader header = new ReplayHeader();
        byte[] name32 = new byte[ReplayFormat.NAME_SIZE];
        ReplaySlotStore.loadSlotHeaderAndName(slot, header, name32);
        preview.set(header, name32);

        startWorker();
    }

    public boolean update(int pressed) {
        decoTick++;
        applyPendingFromWorker();
        progress.update(System.currentTimeMillis(), sendDone);

        if (UiInput.cancel(pressed)) {
            if (scene == SCENE_SENDING) {
                requestCancel();
            }
            return true;
        }

        if (scene == SCENE_MESSAGE) {
            if (UiInput.dismiss(pressed)) {
                return true;
            }
            return false;
        }

        return false;
    }

    public int getLeftSoftkeyTextId() {
        return scene == SCENE_SENDING ? TextId.SOFTKEY_CANCEL : TextId.SOFTKEY_BACK;
    }

    public void render(Graphics g, ImageBank imgs) {
        ReplayUi.renderBackground(g, imgs);

        UiDraw.drawString2(g, FONT, UiText.get(TextId.BT_SEND_TITLE), 120, 25, 1, 0xFFFFFF, 0x777777);

        renderPreviewCard(g);

        if (scene == SCENE_SENDING) {
            renderSending(g);
        } else {
            renderMessage(g);
        }
    }

    private void renderPreviewCard(Graphics g) {
        ReplayUi.renderPreviewCard(g, FONT, FONT_M, preview,
                UiText.get(TextId.REPLAY_MSG_EMPTY_SLOT), decoTick);
    }

    private void renderSending(Graphics g) {
        ReplayTransferUi.renderProgress(g, FONT, FONT_M, 116, 106,
                ReplayTransferUi.statusText(btStatus, cancelRequested, false),
                ReplayTransferUi.deviceText(btRemoteAddr, btRemoteName),
                sendDone, sendTotal, progress.getBytesPerSecond(), decoTick);
    }

    private void renderMessage(Graphics g) {
        ReplayUi.renderMessageBox(g, FONT, FONT_M, msg, 118, decoTick);
    }

    private void startWorker() {
        if (!BtSppLink.isSupported()) {
            msg = UiText.get(TextId.BT_MSG_NOT_SUPPORTED);
            scene = SCENE_MESSAGE;
            return;
        }

        byte[] packet = null;
        try {
            packet = ReplayShareService.exportSlotPacket(slot);
        } catch (Throwable t) {
            packet = null;
        }

        if (packet == null) {
            msg = UiText.get(TextId.REPLAY_MSG_EMPTY_SLOT);
            scene = SCENE_MESSAGE;
            return;
        }

        cancelRef = new BtSppLink.CancelRef();

        final int sid = sessionId;
        final byte[] sendPacket = packet;
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    BtSppLink.sendPacket(sendPacket, new BtSppLink.ProgressListener() {
                        public void onProgress(int done, int total) {
                            if (sessionId != sid) {
                                return;
                            }
                            sendDone = done;
                            sendTotal = total;
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
                    pendingMsg = UiText.get(TextId.BT_MSG_SENT);
                    pendingScene = SCENE_MESSAGE;
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

            scene = ns;
        }
    }

}
