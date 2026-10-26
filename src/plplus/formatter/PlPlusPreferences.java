package plplus.formatter;

import org.eclipse.jface.preference.IPreferenceStore;

public final class PlPlusPreferences {

    public static final String AUTO_FORMAT_ON_SAVE = "plplus.formatter.autoFormatOnSave";
    public static final String INDENT_SIZE = "plplus.formatter.indentSize";
    public static final String KEYWORD_CASE = "plplus.formatter.keywordCase";
    public static final String MAX_BLANK_LINES = "plplus.formatter.maxBlankLines";

    private PlPlusPreferences() {
    }

    public static boolean isFormatOnSave() {
        IPreferenceStore store = Activator.getPreferenceStoreInstance();
        return store != null && store.getBoolean(AUTO_FORMAT_ON_SAVE);
    }

    public static PlPlusFormatter.Options formatterOptions() {
        IPreferenceStore store = Activator.getPreferenceStoreInstance();
        if (store == null) {
            return PlPlusFormatter.Options.defaults();
        }
        PlPlusFormatter.KeywordCase keywordCase;
        try {
            keywordCase = PlPlusFormatter.KeywordCase.valueOf(store.getString(KEYWORD_CASE));
        } catch (IllegalArgumentException e) {
            keywordCase = PlPlusFormatter.Options.defaults().keywordCase;
        }
        return new PlPlusFormatter.Options(store.getInt(INDENT_SIZE), keywordCase, store.getInt(MAX_BLANK_LINES));
    }
}
