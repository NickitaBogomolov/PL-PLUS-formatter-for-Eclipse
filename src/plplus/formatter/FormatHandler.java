package plplus.formatter;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

public class FormatHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
        if (window != null && window.getActivePage() != null) {
            var part = window.getActivePage().getActiveEditor();
            if (part instanceof PlPlusEditor editor) {
                editor.formatDocument();
            }
        }
        return null;
    }
}
