package tainavi;


public class JComboBoxWithPopup extends JWideComboBox {

	private static final long serialVersionUID = 1L;

	private TextEditPopupMenu tepm = new TextEditPopupMenu();
	
	public JComboBoxWithPopup() {
		super();
	}
	
	public void setEditable(boolean b) {
		super.setEditable(b);
		if (b) {
			this.getEditor().getEditorComponent().addMouseListener(tepm);
		}
		else {
			this.getEditor().getEditorComponent().removeMouseListener(tepm);
		}
	}
}
