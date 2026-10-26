package plplus.formatter;

import org.eclipse.ui.IStartup;

public class PlPlusStartup implements IStartup {

    @Override
    public void earlyStartup() {
        PlPlusAutoFormatter.register();
    }
}
