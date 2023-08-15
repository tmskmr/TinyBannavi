package tainavi;

import java.awt.Component;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.TableCellEditor;

/**
 * カラムの編集
 */
public class EditorColumn extends AbstractCellEditor implements TableCellEditor {

	private static final long serialVersionUID = 1L;

	private final JTextFieldWithPopup editorField = new JTextFieldWithPopup();

	public JTextField getTextField() { return editorField; }
	
	public EditorColumn() {
		
		super();
		
		editorField.addFocusListener(new FocusAdapter() {
			
			@Override
			public void focusGained(FocusEvent e) {
				
				editorField.selectAll();
				
			}
			
		});
		
	}
	
	@Override
	public Object getCellEditorValue() {
		
		return editorField.getText();
		
	}

	@Override
	public Component getTableCellEditorComponent(JTable arg0, Object arg1, boolean arg2, int arg3, int arg4) {
		
		editorField.setText((String)arg1);
		return editorField;
		
	}
	
}