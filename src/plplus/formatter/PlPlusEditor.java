package plplus.formatter;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.editors.text.TextEditor;

public class PlPlusEditor extends TextEditor {

    public static final String ID = "plplus.formatter.editor";
    public static final String CONTEXT_ID = "plplus.formatter.editorScope";

    @Override
    protected void initializeKeyBindingScopes() {
        setKeyBindingScopes(new String[] { CONTEXT_ID });
    }

    @Override
    protected void performSave(boolean overwrite, IProgressMonitor progressMonitor) {
        if (PlPlusPreferences.isFormatOnSave()) {
            formatDocument();
        }
        super.performSave(overwrite, progressMonitor);
    }

    /**
     * Formats the whole document as a single undoable change.
     * Only the changed region is replaced, so markers and the caret line outside of it are kept.
     */
    public void formatDocument() {
        if (!validateEditorInputState()) {
            return;
        }
        IDocument document = getDocumentProvider().getDocument(getEditorInput());
        if (document == null) {
            return;
        }
        String source = document.get();
        String formatted = PlPlusFormatter.format(source, PlPlusPreferences.formatterOptions());
        if (formatted.equals(source)) {
            return;
        }

        int prefix = 0;
        int max = Math.min(source.length(), formatted.length());
        while (prefix < max && source.charAt(prefix) == formatted.charAt(prefix)) {
            prefix++;
        }
        int suffix = 0;
        while (suffix < max - prefix
                && source.charAt(source.length() - 1 - suffix) == formatted.charAt(formatted.length() - 1 - suffix)) {
            suffix++;
        }

        int caretLine = 0;
        ISelection selection = getSelectionProvider().getSelection();
        if (selection instanceof ITextSelection) {
            caretLine = Math.max(0, ((ITextSelection) selection).getStartLine());
        }

        try {
            document.replace(prefix, source.length() - prefix - suffix,
                    formatted.substring(prefix, formatted.length() - suffix));
            int line = Math.min(caretLine, document.getNumberOfLines() - 1);
            selectAndReveal(document.getLineOffset(line), 0);
        } catch (BadLocationException e) {
            Activator.log("Cannot apply PL+ formatting", e);
        }
    }
}
