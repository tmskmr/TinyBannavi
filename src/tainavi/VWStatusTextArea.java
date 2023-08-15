package tainavi;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

public class VWStatusTextArea extends JPanel implements StatusTextArea {

	private static final long serialVersionUID = 1L;

	/*
	 * 部品
	 */

	private JScrollPane jsp = null;
	private JTextPane jtp = null;

	/*
	 * コンストラクタ
	 */

	public VWStatusTextArea() {

		super();

		this.setLayout(new BorderLayout());

		this.add(getJScrollPane_statusarea());

	}

	/*
	 * 行数から高さを計算する
	 */
	public int getHeightFromRows(int rows){
		int hl = jtp.getFont().getSize();
		return CommonUtils.getPixelFromPoint(hl*rows)+8;
	}

	private JScrollPane getJScrollPane_statusarea() {
		if (jsp == null) {
			jsp = new JScrollPane(getJTextPane_statusarea(),JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			setRows(5);
		}
		return(jsp);
	}

	private JTextPane getJTextPane_statusarea() {
		if (jtp == null) {
			jtp = new JTextPane();
			jtp.setEditable(false);			// 編集、させない！
		}
		return jtp;
	}

	/*
	 * StatusTextArea用のメソッド(non-Javadoc)
	 */

	@Override
	public void clear() {
		jtp.setText("");
	}

    private void appendToPane(String msg, Color c){
		SimpleAttributeSet attr = new SimpleAttributeSet();
		StyleConstants.setForeground(attr, c);

		Document doc = jtp.getDocument();
		if (doc == null)
			return;

		try {
			doc.insertString(doc.getLength(), msg, attr);
			jtp.setCaretPosition(doc.getLength());
		} catch (BadLocationException e) {
			  e.printStackTrace();
		}
    }

	@Override
	public void appendMessage(String message) {
		String msg = CommonUtils.getNow() + message;
		appendToPane("\n" + msg, Color.BLACK);
		System.out.println(msg);
	}

	@Override
	public void appendError(String message) {
		String msg = CommonUtils.getNow() + message;
		appendToPane("\n" + msg, Color.RED);
		System.err.println(msg);
	}

	@Override
	public int getRows() {
		return (int)(jtp.getHeight() / jtp.getFont().getSize()/1.35);
	}

	@Override
	public void setRows(int rows) {
		if (jsp == null)
			return;
		Dimension dim = jsp.getPreferredSize();
		dim.height = getHeightFromRows(rows);
		jsp.setPreferredSize(dim);
	}
}
