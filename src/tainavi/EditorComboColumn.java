package tainavi;

import java.awt.Component;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.AbstractCellEditor;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.TableCellEditor;

public class EditorComboColumn extends AbstractCellEditor implements TableCellEditor {

	private static final long serialVersionUID = 1L;

	private final JComboBoxWithPopup editorCombo;

	public EditorComboColumn() {
		
		super();
		
		editorCombo = new JComboBoxWithPopup();
		editorCombo.setEditable(true);
		editorCombo.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent e) {
				((JTextField) editorCombo.getEditor().getEditorComponent()).selectAll();	// 効かない？
			}
		});
	}
	
	public JComboBox getComboBox() { return editorCombo; }
	
	@Override
	public Object getCellEditorValue() {
		return editorCombo.getEditor().getItem();
	}

	@Override
	public Component getTableCellEditorComponent(JTable arg0, Object arg1, boolean arg2, int arg3, int arg4) {
		editorCombo.setSelectedItem((String) arg1);
		return editorCombo;
	}

}
