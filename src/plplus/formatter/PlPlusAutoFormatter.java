package plplus.formatter;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.swt.widgets.Display;

public final class PlPlusAutoFormatter {

    private static final Set<String> trackedEditors = new HashSet<>();

    private static final IResourceChangeListener LISTENER = new IResourceChangeListener() {
        @Override
        public void resourceChanged(IResourceChangeEvent event) {
            if (event == null || event.getDelta() == null) {
                return;
            }

            processDelta(event.getDelta());
        }

        private void processDelta(IResourceDelta delta) {
            IResource resource = delta.getResource();
            if (resource instanceof IFile file && shouldFormatOnSave(file)) {
                if ((delta.getFlags() & IResourceDelta.CONTENT) != 0) {
                    formatIfNeeded(file);
                    return;
                }
            }

            for (IResourceDelta child : delta.getAffectedChildren()) {
                processDelta(child);
            }
        }
    };

    private PlPlusAutoFormatter() {
    }

    public static void register() {
        Display.getDefault().asyncExec(() -> {
            IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
            for (IWorkbenchWindow window : windows) {
                IWorkbenchPage page = window.getActivePage();
                if (page == null) {
                    continue;
                }
                IEditorPart editor = page.getActiveEditor();
                if (editor instanceof TextEditor textEditor) {
                    attachListener(textEditor);
                }
            }
        });
        ResourcesPlugin.getWorkspace().addResourceChangeListener(LISTENER, IResourceChangeEvent.POST_CHANGE);
    }

    public static void attachListener(TextEditor editor) {
        String key = Integer.toHexString(editor.hashCode());
        if (trackedEditors.contains(key)) {
            return;
        }
        trackedEditors.add(key);

        editor.getDocumentProvider().addDocumentListener(new org.eclipse.jface.text.IDocumentListener() {
            @Override
            public void documentChanged(org.eclipse.jface.text.IDocumentEvent event) {
                // no-op; listener is just a hook kept intentionally
            }

            @Override
            public void documentAboutToBeChanged(org.eclipse.jface.text.IDocumentEvent event) {
                // no-op
            }
        });
    }

    public static boolean shouldFormatOnSave(IFile file) {
        if (file == null) {
            return false;
        }

        String extension = file.getFileExtension();
        if (extension == null || !"plp".equalsIgnoreCase(extension)) {
            return false;
        }

        IPreferenceStore store = Activator.getDefault() != null ? Activator.getDefault().getPreferenceStore() : null;
        return store != null && store.getBoolean(PlPlusPreferences.AUTO_FORMAT_ON_SAVE);
    }

    public static void formatIfNeeded(IFile file) {
        if (!shouldFormatOnSave(file)) {
            return;
        }

        try {
            if (!file.exists()) {
                return;
            }

            String original = new String(file.getContents().readAllBytes(), StandardCharsets.UTF_8);
            String formatted = PlPlusFormatter.format(original);
            if (!formatted.equals(original)) {
                file.setContents(new ByteArrayInputStream(formatted.getBytes(StandardCharsets.UTF_8)), true, false, null);
            }
        } catch (CoreException e) {
            e.printStackTrace();
        }
    }
}
