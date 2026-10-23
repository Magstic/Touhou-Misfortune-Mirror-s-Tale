package touhou.i18n;

import java.io.IOException;

import touhou.GameOptions;
import touhou.ResourceException;
import touhou.font.BitmapFont;

public final class I18nBootstrap {
    private static int loadedLanguage = -1;

    private I18nBootstrap() {
    }

    public static void initFromOptions(int[] opt) {
        int lang = opt[GameOptions.IDX_LANGUAGE];
        if (lang == loadedLanguage) {
            return;
        }

        I18n.setLanguageId(lang);
        try {
            I18nPack.loadForCurrentLanguage();
        } catch (IOException e) {
            throw new ResourceException(e.toString());
        }
        UiText.load();
        SpellText.load();
        BitmapFont.loadForCurrentLanguage();
        loadedLanguage = lang;
    }
}
