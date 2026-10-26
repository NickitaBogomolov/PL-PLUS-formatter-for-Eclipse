package plplus.formatter;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class Activator extends AbstractUIPlugin {

    public static final String PLUGIN_ID = "plplus.formatter";

    private static Activator plugin;

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }

    public static Activator getDefault() {
        return plugin;
    }

    public static IPreferenceStore getPreferenceStoreInstance() {
        return getDefault() != null ? getDefault().getPreferenceStore() : null;
    }

    public static void log(String message, Throwable error) {
        if (getDefault() != null) {
            getDefault().getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, message, error));
        }
    }
}
