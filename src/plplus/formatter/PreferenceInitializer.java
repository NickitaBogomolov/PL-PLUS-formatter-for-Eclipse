package plplus.formatter;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

public class PreferenceInitializer extends AbstractPreferenceInitializer {

    @Override
    public void initializeDefaultPreferences() {
        IPreferenceStore store = Activator.getDefault().getPreferenceStore();
        store.setDefault(PlPlusPreferences.AUTO_FORMAT_ON_SAVE, true);
        store.setDefault(PlPlusPreferences.FORMAT_ON_SAVE, true);
        store.setDefault(PlPlusPreferences.INDENT_SIZE, 2);
    }
}
