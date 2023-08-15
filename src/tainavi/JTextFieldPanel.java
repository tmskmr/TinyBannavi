package tainavi;

import java.awt.Dimension;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class JTextFieldPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private JTextField jtextfield = null;
	private JLabel jlabel = null;
	
	public JTextFieldPanel(String s, int labelWidth, String text, int textFieldWidth) {
		
		this.setLayout(new BoxLayout(this,BoxLayout.LINE_AXIS));
		
		Dimension d = null;
		
		jlabel = new JLabel(s);
		d = jlabel.getPreferredSize();
		d.width = labelWidth;
		jlabel.setMaximumSize(d);
		this.add(jlabel);
		
		jtextfield = new JTextField();
		d = jlabel.getPreferredSize();
		d.width = textFieldWidth;
		jtextfield.setMaximumSize(d);
		jtextfield.setText(text);
		jtextfield.setCaretPosition(0);
		this.add(jtextfield);
	}
	
	public String getText() {
		return jtextfield.getText();
	}
	public void setText(String s) {
		jtextfield.setText(s);
	}
	public void setEnabled(boolean b) {
		this.jlabel.setEnabled(b);
		this.jtextfield.setEnabled(b);
	}
}
