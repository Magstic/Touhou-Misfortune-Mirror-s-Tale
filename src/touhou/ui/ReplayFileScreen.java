package touhou.ui;

import java.io.IOException;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

import touhou.ImageBank;
import touhou.i18n.TextId;
import touhou.i18n.UiText;
import touhou.replay.ReplayFileService;
import touhou.replay.ReplayShareService;
import touhou.replay.ReplaySlotData;

public final class ReplayFileScreen {
    private static final Font FONT = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
    private static final int SCENE_BROWSE = 0;
    private static final int SCENE_BUSY = 1;
    private static final int SCENE_MESSAGE = 2;
    private static final int VISIBLE_ROWS = 7;

    private volatile int scene;
    private int slot;
    private boolean importing;
    private String path;
    private String[] entries;
    private int cursor;
    private int tick;
    private int status;
    private String resultPath;

    public void enter(int slot, boolean importing) {
        this.slot = slot;
        this.importing = importing;
        path = "";
        entries = new String[0];
        cursor = 0;
        tick = 0;
        resultPath = null;
        if (System.getProperty("microedition.io.file.FileConnection.version") == null) {
            showMessage(TextId.REPLAY_FILE_UNSUPPORTED);
        } else {
            listDirectory("");
        }
    }

    public boolean update(int pressed) {
        tick++;
        if (scene == SCENE_BUSY) {
            return false;
        }
        if (scene == SCENE_MESSAGE) {
            return UiInput.dismiss(pressed);
        }

        if (UiInput.cancel(pressed) || UiInput.left(pressed)) {
            if (path.length() == 0) {
                return true;
            }
            int slash = path.lastIndexOf('/', path.length() - 2);
            listDirectory(path.substring(0, slash + 1));
        } else if (!importing && UiInput.secondary(pressed) && path.length() > 0) {
            startWorker(path, true);
        } else if (entries.length > 0) {
            if (UiInput.up(pressed)) {
                cursor = MenuCursor.move(cursor, -1, entries.length);
            } else if (UiInput.down(pressed)) {
                cursor = MenuCursor.move(cursor, 1, entries.length);
            } else if (UiInput.confirm(pressed) || UiInput.right(pressed)
                    || (importing && UiInput.secondary(pressed) && !entries[cursor].endsWith("/"))) {
                String target = path + entries[cursor];
                if (entries[cursor].endsWith("/")) {
                    listDirectory(target);
                } else {
                    startWorker(target, true);
                }
            }
        }
        return false;
    }

    public int getLeftSoftkeyTextId() {
        return scene == SCENE_BUSY ? -1 : TextId.SOFTKEY_BACK;
    }

    public int getRightSoftkeyTextId() {
        if (scene != SCENE_BROWSE) {
            return -1;
        }
        if (importing) {
            return entries.length > 0 && !entries[cursor].endsWith("/") ? TextId.REPLAY_IMPORT_SAVE : -1;
        }
        return path.length() > 0 ? TextId.REPLAY_EXPORT_SAVE : -1;
    }

    public void render(Graphics g, ImageBank images) {
        int currentScene = scene;
        ReplayUi.renderBackground(g, images);
        ReplayUi.renderTitle(g, FONT, UiText.get(importing ? TextId.REPLAY_ACTION_IMPORT : TextId.REPLAY_ACTION_EXPORT));
        UiDraw.drawStringMarquee2(g, FONT,
                path.length() == 0 ? UiText.get(TextId.REPLAY_FILE_STORAGE) : path,
                12, 54, 216, 0, 0xB0B0B0, 0x404040, tick);
        UiDraw.drawString2(g, FONT, UiText.get(TextId.COMMON_SLOT_PREFIX) + (slot + 1),
                12, 236, 0, 0xB0B0B0, 0x404040);

        if (currentScene != SCENE_BROWSE) {
            UiDraw.drawStringMarquee2(g, FONT, UiText.get(status),
                    12, 120, 216, 1, 0xFFFFFF, 0x303040, tick);
            if (resultPath != null) {
                UiDraw.drawStringMarquee2(g, FONT, resultPath,
                        12, 147, 216, 1, 0xB0B0B0, 0x404040, tick);
            }
            return;
        }

        int start = MenuCursor.visibleStart(cursor, entries.length, VISIBLE_ROWS);
        for (int row = 0; row < VISIBLE_ROWS && start + row < entries.length; row++) {
            int index = start + row;
            int y = 84 + row * 22;
            boolean selected = index == cursor;
            int color = selected ? 0xFF4F4F : 0xFFFFFF;
            int shadow = selected ? 0x771F1F : 0x303040;
            if (selected) {
                ReplayUi.renderSelection(g, y - 14, 22);
                UiDraw.drawString2(g, FONT, ">", 12, y, 0, color, shadow);
            }
            UiDraw.drawStringMarquee2(g, FONT, entries[index],
                    26, y, 202, 0, color, shadow, tick);
        }
        if (entries.length == 0) {
            UiDraw.drawString2(g, FONT, UiText.get(TextId.REPLAY_LABEL_EMPTY),
                    120, 120, 1, 0xB0B0B0, 0x404040);
        } else {
            UiDraw.drawString2(g, FONT, (cursor + 1) + "/" + entries.length,
                    228, 236, 2, 0xB0B0B0, 0x404040);
        }
    }

    private void listDirectory(String target) {
        startWorker(target, false);
    }

    private void startWorker(final String target, final boolean transfer) {
        status = transfer ? (importing ? TextId.REPLAY_IMPORT_WORKING : TextId.REPLAY_EXPORT_WRITING)
                : TextId.REPLAY_FILE_READING;
        scene = SCENE_BUSY;
        new Thread(new Runnable() {
            public void run() {
                try {
                    if (transfer && importing) {
                        ReplaySlotData data = ReplayFileService.readReplay(target);
                        if (data == null) {
                            showMessage(TextId.REPLAY_IMPORT_INVALID);
                        } else if (ReplayShareService.importToSlot(slot, data)) {
                            resultPath = target;
                            showMessage(TextId.REPLAY_IMPORT_DONE);
                        } else {
                            showMessage(TextId.REPLAY_IMPORT_FAILED);
                        }
                    } else if (transfer) {
                        resultPath = ReplayFileService.exportSlot(slot, target);
                        showMessage(TextId.REPLAY_EXPORT_DONE);
                    } else {
                        String[] listed = ReplayFileService.listEntries(target, importing);
                        path = target;
                        entries = listed;
                        cursor = 0;
                        scene = SCENE_BROWSE;
                    }
                } catch (SecurityException e) {
                    showMessage(TextId.REPLAY_FILE_DENIED);
                } catch (IOException e) {
                    showMessage(transfer ? (importing ? TextId.REPLAY_IMPORT_FAILED : TextId.REPLAY_EXPORT_FAILED)
                            : TextId.REPLAY_FILE_READ_FAILED);
                }
            }
        }).start();
    }

    private void showMessage(int textId) {
        status = textId;
        scene = SCENE_MESSAGE;
    }
}
