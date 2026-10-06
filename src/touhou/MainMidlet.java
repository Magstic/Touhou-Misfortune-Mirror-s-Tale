package touhou;

import javax.microedition.lcdui.Display;
import javax.microedition.midlet.MIDlet;

public final class MainMidlet extends MIDlet {
    private static final String BUILD_STAMP = "2026-01-07-1";

    private GameCore canvas;

    protected void startApp() {
        System.out.println("TouhouMIDPPort BUILD " + BUILD_STAMP);
        if (canvas == null) {
            canvas = new GameCore(this);
        }
        Display.getDisplay(this).setCurrent(canvas);
        if (!canvas.hasFailure()) {
            try {
                ResultStats.INSTANCE.onAppStart();
            } catch (StorageException e) {
                canvas.reportFailure(e);
            }
        }
        canvas.start();
    }

    public void startGameAfterLanguageSelect() {
        if (canvas == null) {
            canvas = new GameCore(this);
        }
        Display.getDisplay(this).setCurrent(canvas);
        canvas.start();
    }

    protected void pauseApp() {
        if (canvas != null) {
            canvas.pause();
            commitLifecycleStats();
        }
    }

    protected void destroyApp(boolean unconditional) {
        if (canvas != null) {
            canvas.stop();
            commitLifecycleStats();
        }
    }

    private void commitLifecycleStats() {
        if (canvas.hasFailure()) {
            return;
        }
        try {
            ResultStats.INSTANCE.onAppPauseOrStop();
        } catch (StorageException e) {
            canvas.reportFailure(e);
        }
    }
}
