package plplus.formatter;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.editors.text.TextEditor;

public class PlPlusEditor extends TextEditor {

    public PlPlusEditor() {
        super();
        setDocumentProvider(new PlPlusDocumentProvider());
    }

    @Override
    protected void createPartControl(Composite parent) {
        super.createPartControl(parent);
    }

    public void formatDocument() {
        IDocument document = getDocumentProvider().getDocument(getEditorInput());
        String text = document.get();
        String formatted = PlPlusFormatter.format(text);
        document.set(formatted);
    }
}
