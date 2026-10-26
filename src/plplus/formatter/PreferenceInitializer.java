package plplus.formatter;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

public class PreferenceInitializer extends AbstractPreferenceInitializer {

    @Override
    public void initializeDefaultPreferences() {
        PlPlusFormatter.Options defaults = PlPlusFormatter.Options.defaults();
        IPreferenceStore store = Activator.getDefault().getPreferenceStore();
        store.setDefault(PlPlusPreferences.AUTO_FORMAT_ON_SAVE, false);
        store.setDefault(PlPlusPreferences.INDENT_SIZE, defaults.indentSize);
        store.setDefault(PlPlusPreferences.KEYWORD_CASE, defaults.keywordCase.name());
        store.setDefault(PlPlusPreferences.MAX_BLANK_LINES, defaults.maxBlankLines);
    }
}
