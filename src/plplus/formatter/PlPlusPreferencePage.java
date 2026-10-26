package plplus.formatter;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PlPlusPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public PlPlusPreferencePage() {
        super(GRID);
        setDescription("Настройки форматирования PL+ / PL/SQL");
    }

    @Override
    public void init(IWorkbench workbench) {
        setPreferenceStore(Activator.getDefault().getPreferenceStore());
    }

    @Override
    public void createFieldEditors() {
        addField(new BooleanFieldEditor(
                PlPlusPreferences.AUTO_FORMAT_ON_SAVE,
                "Форматировать при сохранении в редакторе PL+",
                getFieldEditorParent()));

        IntegerFieldEditor indentSize = new IntegerFieldEditor(
                PlPlusPreferences.INDENT_SIZE,
                "Размер отступа (пробелов)",
                getFieldEditorParent(), 2);
        indentSize.setValidRange(0, 16);
        addField(indentSize);

        addField(new ComboFieldEditor(
                PlPlusPreferences.KEYWORD_CASE,
                "Регистр ключевых слов",
                new String[][] {
                    { "строчные (begin, end)", PlPlusFormatter.KeywordCase.LOWER.name() },
                    { "ПРОПИСНЫЕ (BEGIN, END)", PlPlusFormatter.KeywordCase.UPPER.name() },
                    { "Не изменять", PlPlusFormatter.KeywordCase.PRESERVE.name() }
                },
                getFieldEditorParent()));

        IntegerFieldEditor maxBlankLines = new IntegerFieldEditor(
                PlPlusPreferences.MAX_BLANK_LINES,
                "Максимум пустых строк подряд",
                getFieldEditorParent(), 2);
        maxBlankLines.setValidRange(0, 10);
        addField(maxBlankLines);
    }
}
