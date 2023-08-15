package tainavi;

import javax.swing.JTextArea;

public class JTextAreaWithPopup extends JTextArea {

	private static final long serialVersionUID = 1L;

	public JTextAreaWithPopup() {
		super();
		this.addMouseListener(new TextEditPopupMenu());
	}

	public JTextAreaWithPopup(int rows, int columns) {
		super(rows, columns);
		this.addMouseListener(new TextEditPopupMenu());
	}
}
