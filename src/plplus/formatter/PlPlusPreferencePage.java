package plplus.formatter;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PlPlusPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public PlPlusPreferencePage() {
        super(GRID);
        setPreferenceStore(Activator.getDefault().getPreferenceStore());
        setDescription("Настройки форматирования PL+ для файлов .plp");
    }

    @Override
    public void createFieldEditors() {
        addField(new BooleanFieldEditor(
                PlPlusPreferences.AUTO_FORMAT_ON_SAVE,
                "Форматировать файл при сохранении (*.plp)",
                getFieldEditorParent()));

        addField(new IntegerFieldEditor(
                PlPlusPreferences.INDENT_SIZE,
                "Размер отступа (пробелов)",
                getFieldEditorParent(), 2));
    }

    @Override
    public void init(IWorkbench workbench) {
    }
}
